package com.github.daiwx.kafka.backlog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for Kafka backlog handling.
 */
@ConfigurationProperties(prefix = "kafka.backlog")
@Validated
public class KafkaBacklogProperties {

    /**
     * Whether to enable the Kafka backlog handling feature.
     */
    private boolean enabled = true;

    /**
     * Maximum number of messages that can be in backlog before triggering scaling.
     */
    @Min(1)
    private int maxBacklogThreshold = 10000;

    /**
     * Maximum time to process backlog messages before considering it as delayed.
     */
    @NotNull
    private Duration processingTimeout = Duration.ofMinutes(5);

    /**
     * Minimum number of consumers in a consumer group.
     */
    @Min(1)
    private int minConsumers = 1;

    /**
     * Maximum number of consumers in a consumer group.
     */
    @Min(1)
    private int maxConsumers = 10;

    /**
     * Interval to check for backlog status.
     */
    @NotNull
    private Duration monitoringInterval = Duration.ofSeconds(30);

    /**
     * Whether to process old messages first when backlog is detected.
     */
    private boolean processOldMessagesFirst = false;

    /**
     * Maximum number of retries for failed messages.
     */
    @Min(0)
    private int maxRetries = 3;

    /**
     * Base delay before retrying failed messages.
     */
    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    /**
     * Whether to use exponential backoff for retries.
     */
    private boolean useExponentialBackoff = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxBacklogThreshold() {
        return maxBacklogThreshold;
    }

    public void setMaxBacklogThreshold(int maxBacklogThreshold) {
        this.maxBacklogThreshold = maxBacklogThreshold;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
    }

    public int getMinConsumers() {
        return minConsumers;
    }

    public void setMinConsumers(int minConsumers) {
        this.minConsumers = minConsumers;
    }

    public int getMaxConsumers() {
        return maxConsumers;
    }

    public void setMaxConsumers(int maxConsumers) {
        this.maxConsumers = maxConsumers;
    }

    public Duration getMonitoringInterval() {
        return monitoringInterval;
    }

    public void setMonitoringInterval(Duration monitoringInterval) {
        this.monitoringInterval = monitoringInterval;
    }

    public boolean isProcessOldMessagesFirst() {
        return processOldMessagesFirst;
    }

    public void setProcessOldMessagesFirst(boolean processOldMessagesFirst) {
        this.processOldMessagesFirst = processOldMessagesFirst;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isUseExponentialBackoff() {
        return useExponentialBackoff;
    }

    public void setUseExponentialBackoff(boolean useExponentialBackoff) {
        this.useExponentialBackoff = useExponentialBackoff;
    }
} 