public enum ProcessingStrategy {
    PROCESS_ALL,      // Process all messages
    PROCESS_RECENT,   // Process only recent messages (configurable timeframe)
    IGNORE           // Ignore backlog messages
} 