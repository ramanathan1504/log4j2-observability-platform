package com.observability.logaggregator;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowErrorRateCalculator {

  private final long windowMillis;
  private final Deque<Long> allEvents = new ArrayDeque<>();
  private final Deque<Long> errorEvents = new ArrayDeque<>();

  public SlidingWindowErrorRateCalculator(long windowMillis) {
    this.windowMillis = windowMillis;
  }

  public synchronized void add(LogEvent event) {
    long now = event.epochMillis();
    allEvents.addLast(now);
    if (event.level() == LogLevel.ERROR) {
      errorEvents.addLast(now);
    }
    evictExpired(now);
  }

  public synchronized double errorRatePercent(long now) {
    evictExpired(now);
    if (allEvents.isEmpty()) {
      return 0.0;
    }
    return (errorEvents.size() * 100.0) / allEvents.size();
  }

  public synchronized int totalCount(long now) {
    evictExpired(now);
    return allEvents.size();
  }

  public synchronized int errorCount(long now) {
    evictExpired(now);
    return errorEvents.size();
  }

  private void evictExpired(long now) {
    long cutoff = now - windowMillis;
    while (!allEvents.isEmpty() && allEvents.peekFirst() < cutoff) {
      allEvents.removeFirst();
    }
    while (!errorEvents.isEmpty() && errorEvents.peekFirst() < cutoff) {
      errorEvents.removeFirst();
    }
  }
}
