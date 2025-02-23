package com.github.daiwx.kafka.backlog.test;

import com.github.daiwx.kafka.backlog.model.BacklogMetrics;
import com.github.daiwx.kafka.backlog.model.ConsumerGroupConfig;
import com.github.daiwx.kafka.backlog.service.ConsumerGroupManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {TestConfig.class})
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = {"${test.topic}"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
    "test.topic=test-topic",
    "kafka.backlog.enabled=true",
    "kafka.backlog.max-backlog-threshold=1000",
    "kafka.backlog.processing-timeout=5s",
    "kafka.backlog.min-consumers=1",
    "kafka.backlog.max-consumers=3",
    "kafka.backlog.monitoring-interval=1s"
})
class KafkaDynamicConsumerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerGroupManager consumerGroupManager;

    private static final String TEST_TOPIC = "test-topic";
    private static final String TEST_GROUP = "test-group";

    private ConsumerGroupConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new ConsumerGroupConfig.Builder(TEST_GROUP, TEST_TOPIC)
            .minConsumers(1)
            .maxConsumers(3)
            .processingTimeout(Duration.ofSeconds(5))
            .backlogThreshold(1000)
            .build();
    }

    @Test
    void contextLoads() {
        assertThat(kafkaTemplate).isNotNull();
        assertThat(consumerGroupManager).isNotNull();
    }

    @Test
    void whenNewConsumerCreated_thenConsumerIsRegistered() {
        // Start a consumer group
        boolean started = consumerGroupManager.startConsumerGroup(testConfig);
        assertThat(started).isTrue();

        // Verify consumer count
        await().atMost(5, TimeUnit.SECONDS)
               .untilAsserted(() -> 
                   assertThat(consumerGroupManager.getCurrentConsumerCount(TEST_GROUP))
                   .isEqualTo(1));
    }

    @Test
    void whenConsumerRemoved_thenConsumerIsDeregistered() {
        // Start and then stop a consumer group
        consumerGroupManager.startConsumerGroup(testConfig);
        boolean stopped = consumerGroupManager.stopConsumerGroup(TEST_GROUP);
        
        assertThat(stopped).isTrue();
        assertThat(consumerGroupManager.getCurrentConsumerCount(TEST_GROUP))
            .isZero();
    }

    @Test
    void whenMessageReceived_thenProcessedCorrectly() {
        // Start consumer group
        consumerGroupManager.startConsumerGroup(testConfig);

        // Send test message
        kafkaTemplate.send(TEST_TOPIC, "test-key", "test-message");

        // Verify message processing
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Optional<BacklogMetrics> metrics = consumerGroupManager.getMetrics(TEST_GROUP);
                   assertThat(metrics).isPresent();
                   assertThat(metrics.get().getProcessedMessages()).isEqualTo(1);
               });
    }
} 