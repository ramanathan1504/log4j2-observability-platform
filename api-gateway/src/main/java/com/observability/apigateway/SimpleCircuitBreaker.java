package com.observability.apigateway;

public class SimpleCircuitBreaker {

  private final int failureThreshold;
  private final long openStateMillis;
  private int failureCount;
  private long openedAt;

  public SimpleCircuitBreaker(int failureThreshold, long openStateMillis) {
    this.failureThreshold = failureThreshold;
    this.openStateMillis = openStateMillis;
  }

  public synchronized boolean allowRequest(long nowMillis) {
    if (openedAt == 0L) {
      return true;
    }
    if (nowMillis - openedAt >= openStateMillis) {
      openedAt = 0L;
      failureCount = 0;
      return true;
    }
    return false;
  }

  public synchronized void onSuccess() {
    failureCount = 0;
    openedAt = 0L;
  }

  public synchronized void onFailure(long nowMillis) {
    failureCount++;
    if (failureCount >= failureThreshold) {
      openedAt = nowMillis;
    }
  }

  public synchronized String state(long nowMillis) {
    return allowRequest(nowMillis) ? "CLOSED" : "OPEN";
  }
}
