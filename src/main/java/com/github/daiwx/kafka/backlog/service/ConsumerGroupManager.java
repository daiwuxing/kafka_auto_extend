package com.github.daiwx.kafka.backlog.service;

import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ConsumerGroupConfig;
import com.github.daiwx.kafka.backlog.model.ProcessingStrategy;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing Kafka consumer groups dynamically.
 */
public interface ConsumerGroupManager {

    /**
     * Starts a new consumer group with the given configuration.
     *
     * @param config The configuration for the consumer group
     * @return true if the consumer group was started successfully
     */
    boolean startConsumerGroup(ConsumerGroupConfig config);

    /**
     * Stops a consumer group.
     *
     * @param groupId The ID of the consumer group to stop
     * @return true if the consumer group was stopped successfully
     */
    boolean stopConsumerGroup(String groupId);

    /**
     * Adjusts the number of consumers in a group.
     *
     * @param groupId The ID of the consumer group
     * @param targetCount The desired number of consumers
     * @return true if the adjustment was successful
     */
    boolean adjustConsumerCount(String groupId, int targetCount);

    /**
     * Updates the processing strategy for a consumer group.
     *
     * @param groupId The ID of the consumer group
     * @param strategy The new processing strategy to use
     * @param <K> The type of key in the Kafka message
     * @param <V> The type of value in the Kafka message
     * @return true if the strategy was updated successfully
     */
    <K, V> boolean updateProcessingStrategy(String groupId, ProcessingStrategy<K, V> strategy);

    /**
     * Gets the current metrics for a consumer group.
     *
     * @param groupId The ID of the consumer group
     * @return The metrics for the consumer group, if it exists
     */
    Optional<BacklogMetrics> getMetrics(String groupId);

    /**
     * Gets the metrics for all managed consumer groups.
     *
     * @return A map of group IDs to their metrics
     */
    Map<String, BacklogMetrics> getAllMetrics();

    /**
     * Gets the current configuration for a consumer group.
     *
     * @param groupId The ID of the consumer group
     * @return The configuration for the consumer group, if it exists
     */
    Optional<ConsumerGroupConfig> getConsumerGroupConfig(String groupId);

    /**
     * Checks if a consumer group is currently active.
     *
     * @param groupId The ID of the consumer group
     * @return true if the consumer group is active
     */
    boolean isConsumerGroupActive(String groupId);

    /**
     * Gets the current number of consumers in a group.
     *
     * @param groupId The ID of the consumer group
     * @return The number of active consumers in the group, or 0 if the group doesn't exist
     */
    int getCurrentConsumerCount(String groupId);
} 