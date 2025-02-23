package com.github.daiwx.kafka.backlog.actuator;

import com.github.daiwx.kafka.backlog.config.KafkaBacklogProperties;
import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.service.ConsumerGroupManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for monitoring Kafka backlog status.
 */
@Component
public class KafkaBacklogHealthIndicator implements HealthIndicator {

    private final ConsumerGroupManager consumerGroupManager;
    private final KafkaBacklogProperties properties;

    public KafkaBacklogHealthIndicator(ConsumerGroupManager consumerGroupManager, KafkaBacklogProperties properties) {
        this.consumerGroupManager = consumerGroupManager;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up()
                    .withDetail("status", "Backlog handling is disabled")
                    .build();
        }

        Map<String, BacklogMetrics> allMetrics = consumerGroupManager.getAllMetrics();
        Map<String, Object> details = new HashMap<>();
        boolean hasHighBacklog = false;

        for (Map.Entry<String, BacklogMetrics> entry : allMetrics.entrySet()) {
            String groupId = entry.getKey();
            BacklogMetrics metrics = entry.getValue();
            
            Map<String, Object> groupDetails = new HashMap<>();
            groupDetails.put("totalMessages", metrics.getTotalMessages());
            groupDetails.put("processedMessages", metrics.getProcessedMessages());
            groupDetails.put("averageProcessingTime", metrics.getAverageProcessingTime());
            groupDetails.put("oldestMessageTimestamp", metrics.getOldestMessageTimestamp());
            groupDetails.put("activeConsumers", consumerGroupManager.getCurrentConsumerCount(groupId));
            
            long backlog = metrics.getTotalMessages() - metrics.getProcessedMessages();
            groupDetails.put("currentBacklog", backlog);
            
            if (backlog > properties.getMaxBacklogThreshold()) {
                hasHighBacklog = true;
            }
            
            details.put(groupId, groupDetails);
        }

        Health.Builder healthBuilder = hasHighBacklog ? Health.down() : Health.up();
        return healthBuilder
                .withDetail("groups", details)
                .withDetail("maxBacklogThreshold", properties.getMaxBacklogThreshold())
                .build();
    }
} 