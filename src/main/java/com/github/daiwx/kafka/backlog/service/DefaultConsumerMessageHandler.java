package com.github.daiwx.kafka.backlog.service;

import com.github.daiwx.kafka.backlog.core.MessageProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Default implementation of ConsumerMessageHandler that uses a MessageProcessor for processing messages.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
@Component
public class DefaultConsumerMessageHandler<K, V> implements ConsumerMessageHandler<K, V> {
    private static final Logger log = LoggerFactory.getLogger(DefaultConsumerMessageHandler.class);

    private final MessageProcessor<K, V> messageProcessor;

    public DefaultConsumerMessageHandler(MessageProcessor<K, V> messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    @Override
    public boolean handleMessages(KafkaConsumer<K, V> consumer, Duration timeout) {
        try {
            ConsumerRecords<K, V> records = consumer.poll(timeout);
            if (records.isEmpty()) {
                return true;
            }

            messageProcessor.beforeBatch();
            int successCount = 0;
            int failureCount = 0;

            for (ConsumerRecord<K, V> record : records) {
                try {
                    if (messageProcessor.processMessage(record)) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing record: {}", record, e);
                    failureCount++;
                    if (!messageProcessor.handleFailure(record, e)) {
                        // If failure handling was not successful, consider stopping the batch
                        break;
                    }
                }
            }

            messageProcessor.afterBatch(successCount, failureCount);

            try {
                consumer.commitSync(timeout);
                return true;
            } catch (Exception e) {
                log.error("Error committing offsets", e);
                return false;
            }
        } catch (Exception e) {
            log.error("Error polling messages", e);
            return false;
        }
    }
} 