package com.github.daiwx.kafka.backlog.actuator;

import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.service.ConsumerGroupManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics contributor for exposing Kafka backlog metrics to Actuator.
 */
@Component
public class KafkaBacklogMetricsContributor implements MeterBinder {

    private final ConsumerGroupManager consumerGroupManager;
    private final Map<String, AtomicLong> backlogGauges;

    public KafkaBacklogMetricsContributor(ConsumerGroupManager consumerGroupManager) {
        this.consumerGroupManager = consumerGroupManager;
        this.backlogGauges = new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Register metrics for each consumer group
        consumerGroupManager.getAllMetrics().forEach((groupId, metrics) -> {
            Tags tags = Tags.of(
                Tag.of("group", groupId),
                Tag.of("topic", metrics.getTopicName())
            );

            // Current backlog size
            Gauge.builder("kafka.backlog.size", metrics,
                    this::calculateBacklog)
                    .tags(tags)
                    .description("Current number of messages in backlog")
                    .register(registry);

            // Processing rate
            Gauge.builder("kafka.backlog.processing.rate", metrics,
                    BacklogMetrics::getProcessedMessages)
                    .tags(tags)
                    .description("Number of messages processed")
                    .register(registry);

            // Average processing time
            Gauge.builder("kafka.backlog.processing.time.avg", metrics,
                    BacklogMetrics::getAverageProcessingTime)
                    .tags(tags)
                    .description("Average message processing time in milliseconds")
                    .register(registry);

            // Active consumers
            Gauge.builder("kafka.backlog.consumers.active", groupId,
                    consumerGroupManager::getCurrentConsumerCount)
                    .tags(tags)
                    .description("Number of active consumers in the group")
                    .register(registry);

            // Message age (time since oldest unprocessed message)
            Gauge.builder("kafka.backlog.message.age.max", metrics,
                    this::calculateMaxMessageAge)
                    .tags(tags)
                    .description("Age of oldest unprocessed message in milliseconds")
                    .register(registry);
        });
    }

    private double calculateBacklog(BacklogMetrics metrics) {
        return metrics.getTotalMessages() - metrics.getProcessedMessages();
    }

    private double calculateMaxMessageAge(BacklogMetrics metrics) {
        long oldestTimestamp = metrics.getOldestMessageTimestamp();
        if (oldestTimestamp <= 0) {
            return 0;
        }
        return System.currentTimeMillis() - oldestTimestamp;
    }
} 