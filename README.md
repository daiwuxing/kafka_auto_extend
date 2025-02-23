# Kafka Backlog Spring Boot Starter

A Spring Boot Starter that provides automatic handling of Kafka message backlogs by dynamically adjusting consumer counts and implementing message processing strategies.

## Features

- Automatic scaling of Kafka consumers based on backlog size
- Configurable processing strategies for handling message backlogs
- Support for prioritizing old or new messages
- Built-in retry mechanism with exponential backoff
- Metrics collection for monitoring backlog status
- Easy integration with Spring Boot applications

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.daiwx</groupId>
    <artifactId>kafka-backlog-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Configure the backlog handling behavior in your `application.properties` or `application.yml`:

```yaml
kafka:
  backlog:
    enabled: true
    max-backlog-threshold: 10000
    processing-timeout: 5m
    min-consumers: 1
    max-consumers: 10
    monitoring-interval: 30s
    process-old-messages-first: false
    max-retries: 3
    retry-delay: 1s
    use-exponential-backoff: true
```

## Usage

### Basic Usage

1. Add the starter dependency to your project
2. Configure the properties as needed
3. Implement your message processing logic

Example:

```java
@Service
public class MyMessageHandler implements Function<ConsumerRecord<String, String>, Boolean> {
    
    @Override
    public Boolean apply(ConsumerRecord<String, String> record) {
        // Your message processing logic here
        return true; // Return true if processing was successful
    }
}

@Configuration
public class KafkaConfig {
    
    @Bean
    public Function<ConsumerRecord<String, String>, Boolean> messageHandler(MyMessageHandler handler) {
        return handler;
    }
}
```

### Custom Processing Strategy

You can implement your own processing strategy:

```java
@Service
public class CustomProcessingStrategy implements ProcessingStrategy<String, String> {
    
    @Override
    public int calculatePriority(ConsumerRecord<String, String> record) {
        // Your priority calculation logic
        return 0;
    }

    @Override
    public boolean shouldProcessImmediately(ConsumerRecord<String, String> record) {
        // Your processing decision logic
        return true;
    }

    @Override
    public String getStrategyName() {
        return "CustomStrategy";
    }
}
```

### Monitoring

The starter exposes metrics through Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: always
```

## Properties

| Property | Description | Default |
|----------|-------------|---------|
| `kafka.backlog.enabled` | Enable/disable backlog handling | `true` |
| `kafka.backlog.max-backlog-threshold` | Maximum messages in backlog before scaling | `10000` |
| `kafka.backlog.processing-timeout` | Maximum time to process messages | `5m` |
| `kafka.backlog.min-consumers` | Minimum number of consumers | `1` |
| `kafka.backlog.max-consumers` | Maximum number of consumers | `10` |
| `kafka.backlog.monitoring-interval` | Interval to check backlog status | `30s` |
| `kafka.backlog.process-old-messages-first` | Process older messages first | `false` |
| `kafka.backlog.max-retries` | Maximum retry attempts | `3` |
| `kafka.backlog.retry-delay` | Base delay between retries | `1s` |
| `kafka.backlog.use-exponential-backoff` | Use exponential backoff for retries | `true` |

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.