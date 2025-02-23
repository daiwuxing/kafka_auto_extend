package com.github.daiwx.kafka.backlog.config;

import com.github.daiwx.kafka.backlog.service.ConsumerGroupManager;
import com.github.daiwx.kafka.backlog.service.DefaultConsumerGroupManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Auto-configuration for Kafka backlog handling.
 */
@Configuration
@AutoConfiguration
@EnableConfigurationProperties(KafkaBacklogProperties.class)
@ConditionalOnProperty(prefix = "kafka.backlog", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaBacklogAutoConfiguration {

    private final KafkaBacklogProperties properties;

    public KafkaBacklogAutoConfiguration(KafkaBacklogProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerGroupManager consumerGroupManager(ConsumerFactory<?, ?> consumerFactory) {
        return new DefaultConsumerGroupManager(consumerFactory);
    }
} 