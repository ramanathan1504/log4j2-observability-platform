package com.observability.jobscheduler;

public record ScheduledJob(String id, long executeAtMillis, int priority, Runnable task)
    implements Comparable<ScheduledJob> {

  @Override
  public int compareTo(ScheduledJob other) {
    int byTime = Long.compare(this.executeAtMillis, other.executeAtMillis);
    if (byTime != 0) {
      return byTime;
    }
    return Integer.compare(other.priority, this.priority);
  }
}
