package com.github.daiwx.kafka.backlog.core;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Interface for processing Kafka messages with backlog handling capabilities.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
public interface MessageProcessor<K, V> {

    /**
     * Process a single message.
     *
     * @param record The Kafka consumer record to process
     * @return true if the message was processed successfully
     */
    boolean processMessage(ConsumerRecord<K, V> record);

    /**
     * Called before processing a batch of messages.
     */
    default void beforeBatch() {
        // Default implementation does nothing
    }

    /**
     * Called after processing a batch of messages.
     *
     * @param successCount Number of successfully processed messages
     * @param failureCount Number of failed messages
     */
    default void afterBatch(int successCount, int failureCount) {
        // Default implementation does nothing
    }

    /**
     * Handle a failed message.
     *
     * @param record The failed record
     * @param exception The exception that caused the failure
     * @return true if the failure was handled successfully
     */
    default boolean handleFailure(ConsumerRecord<K, V> record, Exception exception) {
        return false; // Default implementation marks message as failed
    }

    /**
     * Gets the maximum number of retries for failed messages.
     *
     * @return The maximum number of retries
     */
    default int getMaxRetries() {
        return 3; // Default to 3 retries
    }

    /**
     * Gets the delay in milliseconds before retrying a failed message.
     *
     * @param retryCount The current retry count
     * @return The delay in milliseconds
     */
    default long getRetryDelayMs(int retryCount) {
        return (long) Math.pow(2, retryCount) * 1000; // Exponential backoff
    }
} 