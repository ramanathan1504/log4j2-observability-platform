package com.observability.notification;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationDispatcher {

  private final int maxAttempts;
  private final Queue<NotificationEvent> deadLetterQueue = new ArrayDeque<>();
  private final Map<String, ExecutorService> executorsByChannel =
      Map.of(
          "EMAIL", Executors.newFixedThreadPool(2),
          "SMS", Executors.newFixedThreadPool(2),
          "PUSH", Executors.newFixedThreadPool(2));

  public NotificationDispatcher(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public void dispatch(NotificationEvent event) {
    ExecutorService executor =
        executorsByChannel.getOrDefault(event.channel(), executorsByChannel.get("PUSH"));
    executor.submit(() -> sendWithRetry(event));
  }

  public int deadLetterSize() {
    synchronized (deadLetterQueue) {
      return deadLetterQueue.size();
    }
  }

  public void shutdownGracefully() throws InterruptedException {
    for (ExecutorService executor : executorsByChannel.values()) {
      executor.shutdown();
      executor.awaitTermination(2, TimeUnit.SECONDS);
    }
  }

  private void sendWithRetry(NotificationEvent event) {
    NotificationEvent current = event;
    while (current.attempt() <= maxAttempts) {
      if (send(current)) {
        System.out.println(
            "Delivered: "
                + current.id()
                + " via "
                + current.channel()
                + " attempt="
                + current.attempt());
        return;
      }
      current = current.nextAttempt();
    }
    synchronized (deadLetterQueue) {
      deadLetterQueue.add(current);
    }
    System.out.println("DLQ: " + event.id() + " after " + maxAttempts + " attempts");
  }

  private boolean send(NotificationEvent event) {
    int hash = Math.abs((event.id() + ":" + event.channel() + ":" + event.attempt()).hashCode());
    return (hash % 4) != 0;
  }
}
