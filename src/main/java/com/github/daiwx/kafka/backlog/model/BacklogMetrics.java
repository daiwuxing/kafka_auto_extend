package com.github.daiwx.kafka.backlog.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for tracking Kafka message backlog statistics.
 */
public class BacklogMetrics {
    private final String topicName;
    private final String consumerGroup;
    private final AtomicLong totalMessages;
    private final AtomicLong processedMessages;
    private final AtomicLong backlogCount;
    private final AtomicLong oldestMessageTimestamp;
    private final AtomicLong averageProcessingTime;
    private volatile Instant lastUpdated;

    public BacklogMetrics(String topicName, String consumerGroup) {
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
        this.totalMessages = new AtomicLong(0);
        this.processedMessages = new AtomicLong(0);
        this.backlogCount = new AtomicLong(0);
        this.oldestMessageTimestamp = new AtomicLong(System.currentTimeMillis());
        this.averageProcessingTime = new AtomicLong(0);
        this.lastUpdated = Instant.now();
    }

    public void incrementTotalMessages() {
        totalMessages.incrementAndGet();
        updateLastUpdated();
    }

    public void incrementProcessedMessages() {
        processedMessages.incrementAndGet();
        updateBacklogCount();
        updateLastUpdated();
    }

    public void updateOldestMessageTimestamp(long timestamp) {
        oldestMessageTimestamp.set(Math.min(oldestMessageTimestamp.get(), timestamp));
        updateLastUpdated();
    }

    public void updateAverageProcessingTime(long processingTime) {
        long currentAvg = averageProcessingTime.get();
        long processedCount = processedMessages.get();
        
        if (processedCount == 0) {
            averageProcessingTime.set(processingTime);
        } else {
            long newAvg = (currentAvg * processedCount + processingTime) / (processedCount + 1);
            averageProcessingTime.set(newAvg);
        }
        updateLastUpdated();
    }

    private void updateBacklogCount() {
        backlogCount.set(Math.max(0, totalMessages.get() - processedMessages.get()));
    }

    private void updateLastUpdated() {
        this.lastUpdated = Instant.now();
    }

    // Getters

    public String getTopicName() {
        return topicName;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public long getTotalMessages() {
        return totalMessages.get();
    }

    public long getProcessedMessages() {
        return processedMessages.get();
    }

    public long getBacklogCount() {
        return backlogCount.get();
    }

    public long getOldestMessageTimestamp() {
        return oldestMessageTimestamp.get();
    }

    public long getAverageProcessingTime() {
        return averageProcessingTime.get();
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return String.format(
            "BacklogMetrics{topic='%s', group='%s', total=%d, processed=%d, backlog=%d, oldestMsg=%d, avgProcessingTime=%d, lastUpdated=%s}",
            topicName, consumerGroup, totalMessages.get(), processedMessages.get(), backlogCount.get(),
            oldestMessageTimestamp.get(), averageProcessingTime.get(), lastUpdated
        );
    }
} 