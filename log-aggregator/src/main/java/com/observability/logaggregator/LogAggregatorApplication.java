package com.observability.logaggregator;

import java.time.Instant;
import java.util.List;

public class LogAggregatorApplication {

  public static void main(String[] args) {
    long now = Instant.now().toEpochMilli();
    SlidingWindowErrorRateCalculator calculator = new SlidingWindowErrorRateCalculator(60_000);

    List<LogEvent> sampleBatch =
        List.of(
            new LogEvent(now - 45_000, "api-gateway", LogLevel.INFO, "request accepted"),
            new LogEvent(now - 30_000, "notification", LogLevel.ERROR, "provider timeout"),
            new LogEvent(now - 20_000, "rate-limiter", LogLevel.WARN, "burst near limit"),
            new LogEvent(now - 10_000, "api-gateway", LogLevel.ERROR, "backend unavailable"),
            new LogEvent(now - 2_000, "metrics", LogLevel.INFO, "scrape completed"));

    for (LogEvent event : sampleBatch) {
      calculator.add(event);
    }

    System.out.println("log-aggregator scaffold is running.");
    System.out.println("Window total events: " + calculator.totalCount(now));
    System.out.println("Window error events: " + calculator.errorCount(now));
    System.out.printf("Window error rate: %.2f%%%n", calculator.errorRatePercent(now));
  }
}
