package com.observability.apigateway;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ApiGatewayApplication {

  public static void main(String[] args) {
    GatewayRouter router =
        new GatewayRouter(
            Map.of(
                "notification",
                List.of(
                    new BackendInstance("notification-1", "http://notification-1:8080"),
                    new BackendInstance("notification-2", "http://notification-2:8080"))));

    SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker(2, 5_000);
    long now = Instant.now().toEpochMilli();

    System.out.println("api-gateway scaffold is running.");
    System.out.println("Route 1 -> " + router.route("notification"));
    System.out.println("Route 2 -> " + router.route("notification"));
    System.out.println("Route 3 -> " + router.route("notification"));

    circuitBreaker.onFailure(now);
    circuitBreaker.onFailure(now);
    System.out.println("Circuit state after failures: " + circuitBreaker.state(now));
    System.out.println("Allow request while open? " + circuitBreaker.allowRequest(now));
  }
}
