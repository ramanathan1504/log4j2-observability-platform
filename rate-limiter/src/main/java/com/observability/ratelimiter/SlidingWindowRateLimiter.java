package com.observability.ratelimiter;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowRateLimiter {

  private final int maxRequests;
  private final long windowMillis;
  private final Clock clock;
  private final Map<String, Deque<Long>> requestsByUser = new ConcurrentHashMap<>();

  public SlidingWindowRateLimiter(int maxRequests, long windowMillis, Clock clock) {
    this.maxRequests = maxRequests;
    this.windowMillis = windowMillis;
    this.clock = clock;
  }

  public boolean allow(String userId) {
    long now = clock.millis();
    Deque<Long> bucket = requestsByUser.computeIfAbsent(userId, key -> new ArrayDeque<>());
    synchronized (bucket) {
      evictExpired(bucket, now);
      if (bucket.size() >= maxRequests) {
        return false;
      }
      bucket.addLast(now);
      return true;
    }
  }

  public int currentLoad(String userId) {
    long now = clock.millis();
    Deque<Long> bucket = requestsByUser.computeIfAbsent(userId, key -> new ArrayDeque<>());
    synchronized (bucket) {
      evictExpired(bucket, now);
      return bucket.size();
    }
  }

  private void evictExpired(Deque<Long> bucket, long now) {
    long cutoff = now - windowMillis;
    while (!bucket.isEmpty() && bucket.peekFirst() < cutoff) {
      bucket.removeFirst();
    }
  }
}
