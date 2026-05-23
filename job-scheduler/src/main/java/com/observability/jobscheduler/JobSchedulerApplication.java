package com.observability.jobscheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class JobSchedulerApplication {

  public static void main(String[] args) {
    Clock clock = Clock.systemUTC();
    DistributedJobScheduler scheduler = new DistributedJobScheduler(clock);
    long now = Instant.now().toEpochMilli();

    scheduler.schedule(
        new ScheduledJob("job-1", now - 1_000, 5, () -> System.out.println("execute job-1")));
    scheduler.schedule(
        new ScheduledJob("job-2", now - 500, 10, () -> System.out.println("execute job-2")));
    scheduler.schedule(
        new ScheduledJob("job-3", now + 5_000, 1, () -> System.out.println("execute job-3")));

    scheduler.heartbeat("worker-a");
    scheduler.heartbeat("worker-b");

    List<ScheduledJob> due = scheduler.pollDueJobs();
    System.out.println("job-scheduler scaffold is running.");
    System.out.println("Due jobs now: " + due.size());
    for (ScheduledJob job : due) {
      System.out.println("Running " + job.id());
      job.task().run();
    }

    System.out.println("Stale workers: " + scheduler.staleWorkers(60_000));
  }
}
