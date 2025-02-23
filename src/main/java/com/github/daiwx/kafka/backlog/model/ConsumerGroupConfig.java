package com.github.daiwx.kafka.backlog.model;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for a Kafka consumer group.
 */
public class ConsumerGroupConfig {
    private final String groupId;
    private final String topicName;
    private final int minConsumers;
    private final int maxConsumers;
    private final Duration processingTimeout;
    private final ProcessingStrategy<?, ?> processingStrategy;
    private final Properties kafkaProperties;
    private final int backlogThreshold;

    private ConsumerGroupConfig(Builder builder) {
        this.groupId = builder.groupId;
        this.topicName = builder.topicName;
        this.minConsumers = builder.minConsumers;
        this.maxConsumers = builder.maxConsumers;
        this.processingTimeout = builder.processingTimeout;
        this.processingStrategy = builder.processingStrategy;
        this.kafkaProperties = builder.kafkaProperties;
        this.backlogThreshold = builder.backlogThreshold;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTopicName() {
        return topicName;
    }

    public int getMinConsumers() {
        return minConsumers;
    }

    public int getMaxConsumers() {
        return maxConsumers;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public ProcessingStrategy<?, ?> getProcessingStrategy() {
        return processingStrategy;
    }

    public Properties getKafkaProperties() {
        return kafkaProperties;
    }

    public int getBacklogThreshold() {
        return backlogThreshold;
    }

    /**
     * Builder for ConsumerGroupConfig.
     */
    public static class Builder {
        private String groupId;
        private String topicName;
        private int minConsumers = 1;
        private int maxConsumers = 10;
        private Duration processingTimeout = Duration.ofMinutes(5);
        private ProcessingStrategy<?, ?> processingStrategy;
        private Properties kafkaProperties = new Properties();
        private int backlogThreshold = 10000;

        public Builder(String groupId, String topicName) {
            this.groupId = groupId;
            this.topicName = topicName;
        }

        public Builder minConsumers(int minConsumers) {
            this.minConsumers = minConsumers;
            return this;
        }

        public Builder maxConsumers(int maxConsumers) {
            this.maxConsumers = maxConsumers;
            return this;
        }

        public Builder processingTimeout(Duration processingTimeout) {
            this.processingTimeout = processingTimeout;
            return this;
        }

        public Builder processingStrategy(ProcessingStrategy<?, ?> processingStrategy) {
            this.processingStrategy = processingStrategy;
            return this;
        }

        public Builder kafkaProperties(Properties kafkaProperties) {
            this.kafkaProperties = kafkaProperties;
            return this;
        }

        public Builder kafkaProperties(Map<String, Object> properties) {
            Properties props = new Properties();
            props.putAll(properties);
            this.kafkaProperties = props;
            return this;
        }

        public Builder backlogThreshold(int backlogThreshold) {
            this.backlogThreshold = backlogThreshold;
            return this;
        }

        public ConsumerGroupConfig build() {
            if (processingStrategy == null) {
                processingStrategy = new TimeBasedProcessingStrategy<>(true, processingTimeout.toMillis());
            }
            return new ConsumerGroupConfig(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ConsumerGroupConfig{groupId='%s', topic='%s', min=%d, max=%d, timeout=%s, strategy='%s', threshold=%d}",
            groupId, topicName, minConsumers, maxConsumers, processingTimeout,
            processingStrategy.getStrategyName(), backlogThreshold
        );
    }
} 