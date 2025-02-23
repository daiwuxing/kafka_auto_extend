package com.example.kafka.dynamic.config;
import com.example.kafka.dynamic.model.ProcessingStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.dynamic.consumer")
public class KafkaDynamicConsumerProperties {
    private boolean enabled = false;
    private long maxLagThreshold = 10000;
    private long maxDelayMs = 30000;
    private ProcessingStrategy backlogProcessStrategy = ProcessingStrategy.PROCESS_ALL;
    private int maxConsumerGroups = 3;
    private int consumerThreadPoolSize = 10;

    // Getters, Setters, and Builder pattern...
} 