package com.observability.jobscheduler;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class DistributedJobScheduler {

  private final Clock clock;
  private final PriorityQueue<ScheduledJob> queue = new PriorityQueue<>();
  private final Map<String, Long> workerHeartbeat = new HashMap<>();

  public DistributedJobScheduler(Clock clock) {
    this.clock = clock;
  }

  public synchronized void schedule(ScheduledJob job) {
    queue.add(job);
  }

  public synchronized List<ScheduledJob> pollDueJobs() {
    long now = clock.millis();
    List<ScheduledJob> due = new ArrayList<>();
    while (!queue.isEmpty() && queue.peek().executeAtMillis() <= now) {
      due.add(queue.remove());
    }
    return due;
  }

  public synchronized void heartbeat(String workerId) {
    workerHeartbeat.put(workerId, clock.millis());
  }

  public synchronized List<String> staleWorkers(long ttlMillis) {
    long cutoff = clock.millis() - ttlMillis;
    List<String> stale = new ArrayList<>();
    for (Map.Entry<String, Long> entry : workerHeartbeat.entrySet()) {
      if (entry.getValue() < cutoff) {
        stale.add(entry.getKey());
      }
    }
    return stale;
  }
}
