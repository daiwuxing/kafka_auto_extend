package com.github.daiwx.kafka.backlog.service;

import com.github.daiwx.kafka.backlog.model.ConsumerGroupConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the lifecycle of Kafka consumers, including graceful shutdown and scaling operations.
 */
public class ConsumerLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(ConsumerLifecycleManager.class);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    private final ConsumerFactory<?, ?> consumerFactory;
    private final Map<String, ExecutorService> groupExecutors;
    private final Map<String, Set<KafkaConsumer<?, ?>>> activeConsumers;
    private final Map<String, AtomicBoolean> shutdownFlags;

    public ConsumerLifecycleManager(ConsumerFactory<?, ?> consumerFactory) {
        this.consumerFactory = consumerFactory;
        this.groupExecutors = new ConcurrentHashMap<>();
        this.activeConsumers = new ConcurrentHashMap<>();
        this.shutdownFlags = new ConcurrentHashMap<>();
    }

    /**
     * Initializes resources for a consumer group.
     *
     * @param config The consumer group configuration
     */
    public void initializeGroup(ConsumerGroupConfig config) {
        String groupId = config.getGroupId();
        groupExecutors.computeIfAbsent(groupId, k -> Executors.newFixedThreadPool(config.getMaxConsumers()));
        activeConsumers.computeIfAbsent(groupId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        shutdownFlags.computeIfAbsent(groupId, k -> new AtomicBoolean(false));
    }

    /**
     * Starts a new consumer in the specified group.
     *
     * @param config The consumer group configuration
     * @param messageHandler The handler for processing messages
     * @return true if the consumer was started successfully
     */
    public boolean startConsumer(ConsumerGroupConfig config, ConsumerMessageHandler<?, ?> messageHandler) {
        String groupId = config.getGroupId();
        ExecutorService executor = groupExecutors.get(groupId);
        Set<KafkaConsumer<?, ?>> consumers = activeConsumers.get(groupId);
        AtomicBoolean shutdownFlag = shutdownFlags.get(groupId);

        if (executor == null || consumers == null || shutdownFlag == null || executor.isShutdown()) {
            log.warn("Cannot start consumer for group {}: resources not initialized", groupId);
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            KafkaConsumer<Object, Object> consumer = (KafkaConsumer<Object, Object>) consumerFactory.createConsumer(groupId);
            consumers.add(consumer);

            executor.submit(() -> runConsumer(consumer, config, messageHandler, shutdownFlag));
            return true;
        } catch (Exception e) {
            log.error("Failed to start consumer for group {}", groupId, e);
            return false;
        }
    }

    /**
     * Gracefully shuts down a consumer group.
     *
     * @param groupId The ID of the consumer group to shut down
     * @return true if shutdown was successful
     */
    public boolean shutdownGroup(String groupId) {
        AtomicBoolean shutdownFlag = shutdownFlags.get(groupId);
        if (shutdownFlag != null) {
            shutdownFlag.set(true);
        }

        Set<KafkaConsumer<?, ?>> consumers = activeConsumers.get(groupId);
        ExecutorService executor = groupExecutors.get(groupId);

        if (consumers != null) {
            consumers.forEach(this::stopConsumerGracefully);
        }

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }

        activeConsumers.remove(groupId);
        groupExecutors.remove(groupId);
        shutdownFlags.remove(groupId);

        return true;
    }

    /**
     * Gets the current number of active consumers in a group.
     *
     * @param groupId The consumer group ID
     * @return The number of active consumers
     */
    public int getActiveConsumerCount(String groupId) {
        Set<KafkaConsumer<?, ?>> consumers = activeConsumers.get(groupId);
        return consumers != null ? consumers.size() : 0;
    }

    private void runConsumer(KafkaConsumer<Object, Object> consumer, 
                           ConsumerGroupConfig config,
                           ConsumerMessageHandler<?, ?> messageHandler,
                           AtomicBoolean shutdownFlag) {
        try {
            consumer.subscribe(Collections.singletonList(config.getTopicName()));
            
            while (!shutdownFlag.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    @SuppressWarnings("unchecked")
                    ConsumerMessageHandler<Object, Object> handler = 
                        (ConsumerMessageHandler<Object, Object>) messageHandler;
                    
                    handler.handleMessages(consumer, config.getProcessingTimeout());
                } catch (Exception e) {
                    log.error("Error in consumer loop for group {}", config.getGroupId(), e);
                }
            }
        } finally {
            stopConsumerGracefully(consumer);
            Set<KafkaConsumer<?, ?>> consumers = activeConsumers.get(config.getGroupId());
            if (consumers != null) {
                consumers.remove(consumer);
            }
        }
    }

    private void stopConsumerGracefully(KafkaConsumer<?, ?> consumer) {
        try {
            // Stop fetching new data
            consumer.wakeup();
            
            // Get current assignment
            Set<TopicPartition> assignment = consumer.assignment();
            if (!assignment.isEmpty()) {
                // Commit final offsets
                consumer.commitSync(Duration.ofSeconds(5));
            }
        } catch (Exception e) {
            log.warn("Error during graceful consumer shutdown", e);
        } finally {
            try {
                consumer.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.warn("Error closing consumer", e);
            }
        }
    }
} 