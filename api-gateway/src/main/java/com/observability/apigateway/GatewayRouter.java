package com.observability.apigateway;

import java.util.List;
import java.util.Map;

public class GatewayRouter {

  private final Map<String, List<BackendInstance>> routes;
  private final RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();

  public GatewayRouter(Map<String, List<BackendInstance>> routes) {
    this.routes = routes;
  }

  public BackendInstance route(String serviceName) {
    List<BackendInstance> backends = routes.getOrDefault(serviceName, List.of());
    if (backends.isEmpty()) {
      throw new IllegalArgumentException("No route configured for service: " + serviceName);
    }
    return loadBalancer.next(backends);
  }
}
