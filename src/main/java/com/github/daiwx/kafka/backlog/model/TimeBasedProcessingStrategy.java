package com.github.daiwx.kafka.backlog.model;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * A processing strategy that prioritizes messages based on their timestamps.
 * Can be configured to process either oldest or newest messages first.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
public class TimeBasedProcessingStrategy<K, V> implements ProcessingStrategy<K, V> {

    private final boolean processOldestFirst;
    private final long processingThresholdMs;

    /**
     * Creates a new TimeBasedProcessingStrategy.
     *
     * @param processOldestFirst If true, older messages get higher priority
     * @param processingThresholdMs Messages older than this threshold (in milliseconds) will be processed immediately
     */
    public TimeBasedProcessingStrategy(boolean processOldestFirst, long processingThresholdMs) {
        this.processOldestFirst = processOldestFirst;
        this.processingThresholdMs = processingThresholdMs;
    }

    @Override
    public int calculatePriority(ConsumerRecord<K, V> record) {
        long currentTime = System.currentTimeMillis();
        long messageAge = currentTime - record.timestamp();
        
        // Convert age to a priority score
        // If processOldestFirst is true, older messages get higher priority
        // If false, newer messages get higher priority
        if (processOldestFirst) {
            return (int) (messageAge / 1000); // Convert to seconds for a reasonable priority range
        } else {
            return (int) ((currentTime - messageAge) / 1000);
        }
    }

    @Override
    public boolean shouldProcessImmediately(ConsumerRecord<K, V> record) {
        long messageAge = System.currentTimeMillis() - record.timestamp();
        return messageAge >= processingThresholdMs;
    }

    @Override
    public String getStrategyName() {
        return processOldestFirst ? "TimeBasedOldestFirst" : "TimeBasedNewestFirst";
    }

    /**
     * @return true if this strategy processes oldest messages first
     */
    public boolean isProcessOldestFirst() {
        return processOldestFirst;
    }

    /**
     * @return the processing threshold in milliseconds
     */
    public long getProcessingThresholdMs() {
        return processingThresholdMs;
    }
} 