package com.github.daiwx.kafka.backlog.core;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * A simple implementation of MessageProcessor that delegates actual processing to a function.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
public class SimpleMessageProcessor<K, V> implements MessageProcessor<K, V> {
    private static final Logger log = LoggerFactory.getLogger(SimpleMessageProcessor.class);

    private final Function<ConsumerRecord<K, V>, Boolean> processingFunction;
    private final int maxRetries;
    private final Function<Integer, Long> retryDelayFunction;

    /**
     * Creates a new SimpleMessageProcessor with default retry settings.
     *
     * @param processingFunction The function that processes each message
     */
    public SimpleMessageProcessor(Function<ConsumerRecord<K, V>, Boolean> processingFunction) {
        this(processingFunction, 3, retryCount -> (long) Math.pow(2, retryCount) * 1000);
    }

    /**
     * Creates a new SimpleMessageProcessor with custom retry settings.
     *
     * @param processingFunction The function that processes each message
     * @param maxRetries Maximum number of retries for failed messages
     * @param retryDelayFunction Function to calculate delay before retry based on retry count
     */
    public SimpleMessageProcessor(
            Function<ConsumerRecord<K, V>, Boolean> processingFunction,
            int maxRetries,
            Function<Integer, Long> retryDelayFunction) {
        this.processingFunction = processingFunction;
        this.maxRetries = maxRetries;
        this.retryDelayFunction = retryDelayFunction;
    }

    @Override
    public boolean processMessage(ConsumerRecord<K, V> record) {
        try {
            return processingFunction.apply(record);
        } catch (Exception e) {
            log.error("Error processing message: {}", record, e);
            return false;
        }
    }

    @Override
    public void beforeBatch() {
        // No special handling needed
    }

    @Override
    public void afterBatch(int successCount, int failureCount) {
        log.info("Batch processing completed. Success: {}, Failures: {}", successCount, failureCount);
    }

    @Override
    public boolean handleFailure(ConsumerRecord<K, V> record, Exception exception) {
        log.error("Message processing failed: {}", record, exception);
        return false;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public long getRetryDelayMs(int retryCount) {
        return retryDelayFunction.apply(retryCount);
    }
} 