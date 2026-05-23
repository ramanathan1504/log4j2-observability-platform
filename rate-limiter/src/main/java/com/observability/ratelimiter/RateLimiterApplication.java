package com.observability.ratelimiter;

import java.time.Clock;

public class RateLimiterApplication {

  public static void main(String[] args) {
    SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 10_000, Clock.systemUTC());
    String user = "user-42";

    for (int i = 1; i <= 5; i++) {
      boolean allowed = limiter.allow(user);
      System.out.println("Request " + i + " for " + user + " -> " + (allowed ? "ALLOW" : "BLOCK"));
    }

    System.out.println("Current in-window request count: " + limiter.currentLoad(user));
  }
}
