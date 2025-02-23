package com.github.daiwx.kafka.backlog.model;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Strategy interface for defining how messages should be processed when backlog is detected.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
public interface ProcessingStrategy<K, V> {

    /**
     * Determines the priority of a message for processing.
     *
     * @param record The Kafka consumer record to evaluate
     * @return A priority score for the message (higher means higher priority)
     */
    int calculatePriority(ConsumerRecord<K, V> record);

    /**
     * Determines whether this message should be processed immediately or delayed.
     *
     * @param record The Kafka consumer record to evaluate
     * @return true if the message should be processed immediately, false if it should be delayed
     */
    boolean shouldProcessImmediately(ConsumerRecord<K, V> record);

    /**
     * Gets the name of this processing strategy.
     *
     * @return The name of the strategy
     */
    String getStrategyName();
} 