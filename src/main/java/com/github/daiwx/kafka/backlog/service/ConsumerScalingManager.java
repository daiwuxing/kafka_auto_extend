package com.github.daiwx.kafka.backlog.service;

import com.github.daiwx.kafka.backlog.config.KafkaBacklogProperties;
import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ConsumerGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Manages dynamic scaling of Kafka consumers based on backlog metrics.
 */
@Component
public class ConsumerScalingManager {
    private static final Logger log = LoggerFactory.getLogger(ConsumerScalingManager.class);

    private final ConsumerGroupManager consumerGroupManager;
    private final KafkaBacklogProperties properties;

    public ConsumerScalingManager(ConsumerGroupManager consumerGroupManager, KafkaBacklogProperties properties) {
        this.consumerGroupManager = consumerGroupManager;
        this.properties = properties;
    }

    /**
     * Periodically checks and adjusts consumer counts based on backlog metrics.
     */
    @Scheduled(fixedDelayString = "${kafka.backlog.monitoring-interval:30000}")
    public void adjustConsumerCounts() {
        if (!properties.isEnabled()) {
            return;
        }

        Map<String, BacklogMetrics> allMetrics = consumerGroupManager.getAllMetrics();
        
        for (Map.Entry<String, BacklogMetrics> entry : allMetrics.entrySet()) {
            String groupId = entry.getKey();
            BacklogMetrics metrics = entry.getValue();
            
            Optional<ConsumerGroupConfig> configOpt = consumerGroupManager.getConsumerGroupConfig(groupId);
            if (!configOpt.isPresent()) {
                continue;
            }
            
            ConsumerGroupConfig config = configOpt.get();
            int currentConsumers = consumerGroupManager.getCurrentConsumerCount(groupId);
            int targetConsumers = calculateTargetConsumers(metrics, config, currentConsumers);

            if (targetConsumers != currentConsumers) {
                log.info("Adjusting consumer count for group {}: {} -> {}", 
                    groupId, currentConsumers, targetConsumers);
                consumerGroupManager.adjustConsumerCount(groupId, targetConsumers);
            }
        }
    }

    private int calculateTargetConsumers(BacklogMetrics metrics, ConsumerGroupConfig config, int currentConsumers) {
        long backlog = metrics.getTotalMessages() - metrics.getProcessedMessages();
        long avgProcessingTime = metrics.getAverageProcessingTime();
        
        // If backlog is above threshold, consider scaling up
        if (backlog > properties.getMaxBacklogThreshold()) {
            // Calculate how many additional consumers we need based on backlog size
            int backlogFactor = (int) (backlog / properties.getMaxBacklogThreshold());
            int targetConsumers = Math.min(
                currentConsumers + backlogFactor,
                config.getMaxConsumers()
            );
            return Math.max(targetConsumers, config.getMinConsumers());
        }
        
        // If processing time is high, maintain current consumers
        if (avgProcessingTime > properties.getProcessingTimeout().toMillis()) {
            return currentConsumers;
        }
        
        // If backlog is low and processing time is acceptable, consider scaling down
        if (backlog < properties.getMaxBacklogThreshold() / 2) {
            return Math.max(
                currentConsumers - 1,
                config.getMinConsumers()
            );
        }
        
        return currentConsumers;
    }
} 