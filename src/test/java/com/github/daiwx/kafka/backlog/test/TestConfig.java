package com.github.daiwx.kafka.backlog.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

/**
 * Test configuration for Kafka components.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestConfig {

    @Bean
    public Map<String, Object> producerConfigs(EmbeddedKafkaBroker embeddedKafkaBroker) {
        return KafkaTestUtils.producerProps(embeddedKafkaBroker);
    }

    @Bean
    public DefaultKafkaProducerFactory<String, String> producerFactory(
            EmbeddedKafkaBroker embeddedKafkaBroker,
            Map<String, Object> producerConfigs) {
        return new DefaultKafkaProducerFactory<>(producerConfigs);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            DefaultKafkaProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
} 