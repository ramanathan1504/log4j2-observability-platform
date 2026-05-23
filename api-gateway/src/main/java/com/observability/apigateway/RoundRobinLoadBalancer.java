package com.observability.apigateway;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer {

  private final AtomicInteger cursor = new AtomicInteger(0);

  public BackendInstance next(List<BackendInstance> backends) {
    if (backends.isEmpty()) {
      throw new IllegalArgumentException("At least one backend is required");
    }
    int index = Math.floorMod(cursor.getAndIncrement(), backends.size());
    return backends.get(index);
  }
}
