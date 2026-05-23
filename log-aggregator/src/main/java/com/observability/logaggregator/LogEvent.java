package com.observability.logaggregator;

public record LogEvent(long epochMillis, String serviceName, LogLevel level, String message) {}
