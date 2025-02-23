package com.github.daiwx.kafka.backlog.service;

import com.github.daiwx.kafka.backlog.core.MessageProcessor;
import com.github.daiwx.kafka.backlog.core.MessageProcessorFactory;
import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ConsumerGroupConfig;
import com.github.daiwx.kafka.backlog.model.ProcessingStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of the ConsumerGroupManager interface.
 */
@Service
public class DefaultConsumerGroupManager implements ConsumerGroupManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultConsumerGroupManager.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(100);

    private final Map<String, ConsumerGroupConfig> groupConfigs;
    private final Map<String, BacklogMetrics> groupMetrics;
    private final Map<String, ExecutorService> groupExecutors;
    private final Map<String, AtomicInteger> activeConsumerCounts;
    private final Map<String, MessageProcessor<?, ?>> messageProcessors;
    private final ConsumerFactory<?, ?> consumerFactory;

    public DefaultConsumerGroupManager(ConsumerFactory<?, ?> consumerFactory) {
        this.consumerFactory = consumerFactory;
        this.groupConfigs = new ConcurrentHashMap<>();
        this.groupMetrics = new ConcurrentHashMap<>();
        this.groupExecutors = new ConcurrentHashMap<>();
        this.activeConsumerCounts = new ConcurrentHashMap<>();
        this.messageProcessors = new ConcurrentHashMap<>();
    }

    @Override
    public boolean startConsumerGroup(ConsumerGroupConfig config) {
        if (groupConfigs.containsKey(config.getGroupId())) {
            log.warn("Consumer group {} already exists", config.getGroupId());
            return false;
        }

        groupConfigs.put(config.getGroupId(), config);
        BacklogMetrics metrics = new BacklogMetrics(config.getTopicName(), config.getGroupId());
        groupMetrics.put(config.getGroupId(), metrics);
        groupExecutors.put(config.getGroupId(), Executors.newFixedThreadPool(config.getMaxConsumers()));
        activeConsumerCounts.put(config.getGroupId(), new AtomicInteger(0));

        // Create message processor for this group
        MessageProcessor<?, ?> processor = MessageProcessorFactory.createProcessor(
            record -> processRecord(record),
            config.getProcessingStrategy(),
            metrics
        );
        messageProcessors.put(config.getGroupId(), processor);

        return adjustConsumerCount(config.getGroupId(), config.getMinConsumers());
    }

    @Override
    public boolean stopConsumerGroup(String groupId) {
        ConsumerGroupConfig config = groupConfigs.get(groupId);
        if (config == null) {
            log.warn("Consumer group {} does not exist", groupId);
            return false;
        }

        ExecutorService executor = groupExecutors.get(groupId);
        if (executor != null) {
            executor.shutdown();
        }

        groupConfigs.remove(groupId);
        groupMetrics.remove(groupId);
        groupExecutors.remove(groupId);
        activeConsumerCounts.remove(groupId);
        messageProcessors.remove(groupId);

        return true;
    }

    @Override
    public boolean adjustConsumerCount(String groupId, int targetCount) {
        ConsumerGroupConfig config = groupConfigs.get(groupId);
        if (config == null) {
            log.warn("Consumer group {} does not exist", groupId);
            return false;
        }

        if (targetCount < config.getMinConsumers() || targetCount > config.getMaxConsumers()) {
            log.warn("Target consumer count {} is outside allowed range [{}, {}] for group {}",
                    targetCount, config.getMinConsumers(), config.getMaxConsumers(), groupId);
            return false;
        }

        AtomicInteger currentCount = activeConsumerCounts.get(groupId);
        int delta = targetCount - currentCount.get();

        if (delta > 0) {
            // Add consumers
            for (int i = 0; i < delta; i++) {
                startNewConsumer(groupId);
            }
        } else if (delta < 0) {
            // Remove consumers (implementation depends on how consumers are tracked)
            // This would require additional implementation to gracefully stop specific consumers
            log.warn("Reducing consumer count is not implemented yet");
            return false;
        }

        return true;
    }

    @Override
    public <K, V> boolean updateProcessingStrategy(String groupId, ProcessingStrategy<K, V> strategy) {
        ConsumerGroupConfig config = groupConfigs.get(groupId);
        if (config == null) {
            log.warn("Consumer group {} does not exist", groupId);
            return false;
        }

        // Create a new config with the updated strategy
        ConsumerGroupConfig newConfig = new ConsumerGroupConfig.Builder(config.getGroupId(), config.getTopicName())
                .minConsumers(config.getMinConsumers())
                .maxConsumers(config.getMaxConsumers())
                .processingTimeout(config.getProcessingTimeout())
                .processingStrategy(strategy)
                .kafkaProperties(config.getKafkaProperties())
                .backlogThreshold(config.getBacklogThreshold())
                .build();

        // Update the message processor with the new strategy
        BacklogMetrics metrics = groupMetrics.get(groupId);
        MessageProcessor<K, V> processor = MessageProcessorFactory.createProcessor(
            record -> processRecord(record),
            strategy,
            metrics
        );
        messageProcessors.put(groupId, processor);
        groupConfigs.put(groupId, newConfig);

        return true;
    }

    @Override
    public Optional<BacklogMetrics> getMetrics(String groupId) {
        return Optional.ofNullable(groupMetrics.get(groupId));
    }

    @Override
    public Map<String, BacklogMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(groupMetrics);
    }

    @Override
    public Optional<ConsumerGroupConfig> getConsumerGroupConfig(String groupId) {
        return Optional.ofNullable(groupConfigs.get(groupId));
    }

    @Override
    public boolean isConsumerGroupActive(String groupId) {
        return groupConfigs.containsKey(groupId) && 
               activeConsumerCounts.containsKey(groupId) && 
               activeConsumerCounts.get(groupId).get() > 0;
    }

    @Override
    public int getCurrentConsumerCount(String groupId) {
        AtomicInteger count = activeConsumerCounts.get(groupId);
        return count != null ? count.get() : 0;
    }

    @SuppressWarnings("unchecked")
    private <K, V> void startNewConsumer(String groupId) {
        ConsumerGroupConfig config = groupConfigs.get(groupId);
        if (config == null) {
            return;
        }

        ExecutorService executor = groupExecutors.get(groupId);
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.submit(() -> {
            try (KafkaConsumer<K, V> consumer = (KafkaConsumer<K, V>) consumerFactory.createConsumer(groupId)) {
                consumer.subscribe(Collections.singletonList(config.getTopicName()));
                activeConsumerCounts.get(groupId).incrementAndGet();
                MessageProcessor<K, V> processor = (MessageProcessor<K, V>) messageProcessors.get(groupId);

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        ConsumerRecords<K, V> records = consumer.poll(POLL_TIMEOUT);
                        if (!records.isEmpty()) {
                            processor.beforeBatch();
                            int successCount = 0;
                            int failureCount = 0;

                            for (ConsumerRecord<K, V> record : records) {
                                if (processor.processMessage(record)) {
                                    successCount++;
                                } else {
                                    failureCount++;
                                }
                            }

                            processor.afterBatch(successCount, failureCount);
                            consumer.commitSync();
                        }
                    } catch (Exception e) {
                        log.error("Error in consumer loop for group {}", groupId, e);
                    }
                }
            } catch (Exception e) {
                log.error("Error creating consumer for group {}", groupId, e);
            } finally {
                activeConsumerCounts.get(groupId).decrementAndGet();
            }
        });
    }

    private <K, V> boolean processRecord(ConsumerRecord<K, V> record) {
        // This is a placeholder for actual message processing logic
        // In a real application, this would be implemented by the user of this library
        log.debug("Processing message: {}", record);
        return true;
    }
} 