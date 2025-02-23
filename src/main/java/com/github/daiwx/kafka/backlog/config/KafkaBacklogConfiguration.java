package com.github.daiwx.kafka.backlog.config;

import com.github.daiwx.kafka.backlog.core.MessageProcessor;
import com.github.daiwx.kafka.backlog.core.MessageProcessorFactory;
import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ProcessingStrategy;
import com.github.daiwx.kafka.backlog.model.TimeBasedProcessingStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Configuration class for Kafka backlog handling components.
 */
@Configuration
public class KafkaBacklogConfiguration {

    private final KafkaBacklogProperties properties;

    public KafkaBacklogConfiguration(KafkaBacklogProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public <K, V> ProcessingStrategy<K, V> defaultProcessingStrategy() {
        return new TimeBasedProcessingStrategy<>(
            properties.isProcessOldMessagesFirst(),
            properties.getProcessingTimeout().toMillis()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public <K, V> Function<ConsumerRecord<K, V>, Boolean> defaultMessageHandler() {
        return record -> {
            // Default implementation just logs the message
            // Users should override this bean with their own implementation
            return true;
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public <K, V> MessageProcessor<K, V> defaultMessageProcessor(
            Function<ConsumerRecord<K, V>, Boolean> messageHandler,
            ProcessingStrategy<K, V> processingStrategy,
            String groupId,
            String topic) {
        
        BacklogMetrics metrics = new BacklogMetrics(topic, groupId);
        return MessageProcessorFactory.createProcessor(
            messageHandler,
            processingStrategy,
            metrics,
            properties.getMaxRetries(),
            retryCount -> properties.getRetryDelay().toMillis() * (long) Math.pow(2, retryCount)
        );
    }
} 