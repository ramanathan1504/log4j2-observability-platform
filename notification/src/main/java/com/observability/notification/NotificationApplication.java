package com.observability.notification;

import java.util.List;

public class NotificationApplication {

  public static void main(String[] args) {
    NotificationDispatcher dispatcher = new NotificationDispatcher(3);
    List<NotificationEvent> events =
        List.of(
            new NotificationEvent("n-1", "EMAIL", "Welcome", 1),
            new NotificationEvent("n-2", "SMS", "OTP", 1),
            new NotificationEvent("n-3", "PUSH", "Campaign", 1),
            new NotificationEvent("n-4", "EMAIL", "Invoice", 1));

    for (NotificationEvent event : events) {
      dispatcher.dispatch(event);
    }

    try {
      dispatcher.shutdownGracefully();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }

    System.out.println("notification scaffold is running.");
    System.out.println("DLQ size: " + dispatcher.deadLetterSize());
  }
}
