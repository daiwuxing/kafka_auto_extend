package com.example;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestConfig {

    @Bean
    public Map<String, Object> producerConfigs(EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> props = new HashMap<>(
            KafkaTestUtils.producerProps(embeddedKafkaBroker));
        return props;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            EmbeddedKafkaBroker embeddedKafkaBroker,
            Map<String, Object> producerConfigs) {
        return new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(embeddedKafkaBroker)
            ));
    }
} 