# Kafka Auto Extend

A Spring Boot application for automatic Kafka consumer group management and scaling.

## Overview

This project provides functionality for automatically managing and scaling Kafka consumer groups based on various metrics such as consumer lag and processing delay.

## Features

- Automatic consumer group scaling
- Lag-based monitoring and adjustment
- Configurable thresholds and strategies
- Spring Boot integration

## Requirements

- Java 17 or higher
- Spring Boot 3.1.0
- Apache Kafka

## Configuration

Configuration can be done through `application.yml`:

```yaml
kafka:
  dynamic:
    consumer:
      enabled: true
      max-lag-threshold: 10000
      max-delay-ms: 30000
      backlog-process-strategy: PROCESS_ALL
```

## Building

```bash
mvn clean install
```

## License

This project is licensed under the MIT License.