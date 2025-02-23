package com.github.daiwx.kafka.backlog.core;

import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ProcessingStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A message processor that handles message backlogs using a processing strategy.
 *
 * @param <K> The type of key in the Kafka message
 * @param <V> The type of value in the Kafka message
 */
public class BacklogAwareMessageProcessor<K, V> implements MessageProcessor<K, V> {
    private static final Logger log = LoggerFactory.getLogger(BacklogAwareMessageProcessor.class);

    private final MessageProcessor<K, V> delegate;
    private final ProcessingStrategy<K, V> processingStrategy;
    private final BacklogMetrics metrics;
    private final DelayQueue<DelayedMessage<K, V>> delayedMessages;

    public BacklogAwareMessageProcessor(
            MessageProcessor<K, V> delegate,
            ProcessingStrategy<K, V> processingStrategy,
            BacklogMetrics metrics) {
        this.delegate = delegate;
        this.processingStrategy = processingStrategy;
        this.metrics = metrics;
        this.delayedMessages = new DelayQueue<>();
    }

    @Override
    public boolean processMessage(ConsumerRecord<K, V> record) {
        metrics.incrementTotalMessages();
        metrics.updateOldestMessageTimestamp(record.timestamp());

        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            if (processingStrategy.shouldProcessImmediately(record)) {
                success = processMessageImmediately(record);
            } else {
                delayMessage(record);
                success = true; // Message is successfully queued for later
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", record, e);
            success = handleFailure(record, e);
        } finally {
            if (success) {
                metrics.incrementProcessedMessages();
                metrics.updateAverageProcessingTime(System.currentTimeMillis() - startTime);
            }
        }

        return success;
    }

    @Override
    public void beforeBatch() {
        delegate.beforeBatch();
        processDelayedMessages();
    }

    @Override
    public void afterBatch(int successCount, int failureCount) {
        delegate.afterBatch(successCount, failureCount);
    }

    @Override
    public boolean handleFailure(ConsumerRecord<K, V> record, Exception exception) {
        return delegate.handleFailure(record, exception);
    }

    private boolean processMessageImmediately(ConsumerRecord<K, V> record) {
        return delegate.processMessage(record);
    }

    private void delayMessage(ConsumerRecord<K, V> record) {
        int priority = processingStrategy.calculatePriority(record);
        DelayedMessage<K, V> delayedMessage = new DelayedMessage<>(record, priority);
        delayedMessages.offer(delayedMessage);
        log.debug("Message delayed for later processing: {}", record);
    }

    private void processDelayedMessages() {
        DelayedMessage<K, V> delayed;
        while ((delayed = delayedMessages.poll()) != null) {
            processMessageImmediately(delayed.getRecord());
        }
    }

    /**
     * Wrapper class for delayed messages with priority.
     */
    private static class DelayedMessage<K, V> implements Delayed {
        private final ConsumerRecord<K, V> record;
        private final int priority;
        private final long creationTime;

        public DelayedMessage(ConsumerRecord<K, V> record, int priority) {
            this.record = record;
            this.priority = priority;
            this.creationTime = System.currentTimeMillis();
        }

        public ConsumerRecord<K, V> getRecord() {
            return record;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = priority - (System.currentTimeMillis() - creationTime);
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof DelayedMessage) {
                DelayedMessage<?, ?> that = (DelayedMessage<?, ?>) other;
                return Integer.compare(this.priority, that.priority);
            }
            return 0;
        }
    }
} 