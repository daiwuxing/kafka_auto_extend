package com.github.daiwx.kafka.backlog.service;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;

/**
 * Interface for handling Kafka consumer messages.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
public interface ConsumerMessageHandler<K, V> {

    /**
     * Handles messages from a Kafka consumer.
     *
     * @param consumer The Kafka consumer to poll messages from
     * @param timeout The maximum time to wait for messages
     * @return true if messages were processed successfully
     */
    boolean handleMessages(KafkaConsumer<K, V> consumer, Duration timeout);
} 