package com.observability.notification;

public record NotificationEvent(String id, String channel, String payload, int attempt) {

  public NotificationEvent nextAttempt() {
    return new NotificationEvent(id, channel, payload, attempt + 1);
  }
}
