package com.github.daiwx.kafka.backlog.core;

import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ProcessingStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.function.Function;

/**
 * Factory for creating message processors with backlog handling capabilities.
 */
public class MessageProcessorFactory {

    /**
     * Creates a new message processor with backlog handling capabilities.
     *
     * @param processingFunction The function that processes each message
     * @param processingStrategy The strategy for handling message backlogs
     * @param metrics The metrics collector for tracking backlog statistics
     * @param <K> The type of key in the Kafka message
     * @param <V> The type of value in the Kafka message
     * @return A new message processor
     */
    public static <K, V> MessageProcessor<K, V> createProcessor(
            Function<ConsumerRecord<K, V>, Boolean> processingFunction,
            ProcessingStrategy<K, V> processingStrategy,
            BacklogMetrics metrics) {
        
        SimpleMessageProcessor<K, V> simpleProcessor = new SimpleMessageProcessor<>(processingFunction);
        return new BacklogAwareMessageProcessor<>(simpleProcessor, processingStrategy, metrics);
    }

    /**
     * Creates a new message processor with backlog handling capabilities and custom retry settings.
     *
     * @param processingFunction The function that processes each message
     * @param processingStrategy The strategy for handling message backlogs
     * @param metrics The metrics collector for tracking backlog statistics
     * @param maxRetries Maximum number of retries for failed messages
     * @param retryDelayFunction Function to calculate delay before retry based on retry count
     * @param <K> The type of key in the Kafka message
     * @param <V> The type of value in the Kafka message
     * @return A new message processor
     */
    public static <K, V> MessageProcessor<K, V> createProcessor(
            Function<ConsumerRecord<K, V>, Boolean> processingFunction,
            ProcessingStrategy<K, V> processingStrategy,
            BacklogMetrics metrics,
            int maxRetries,
            Function<Integer, Long> retryDelayFunction) {
        
        SimpleMessageProcessor<K, V> simpleProcessor = new SimpleMessageProcessor<>(
            processingFunction, maxRetries, retryDelayFunction);
        return new BacklogAwareMessageProcessor<>(simpleProcessor, processingStrategy, metrics);
    }
} 