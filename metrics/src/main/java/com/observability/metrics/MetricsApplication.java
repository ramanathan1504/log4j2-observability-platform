package com.observability.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Metrics Monitoring System - Project 5
 *
 * <p>Collects, aggregates, and exports OS/Application metrics - Time-series storage via circular
 * buffers - Sliding window aggregation for p50, p90, p99 - Linux /proc file metrics integration -
 * Prometheus-compatible /metrics endpoint - Grafana dashboard integration
 *
 * @version 1.0.0-SNAPSHOT
 */
@SpringBootApplication
public class MetricsApplication {

  private static final Logger logger = LogManager.getLogger(MetricsApplication.class);

  public static void main(String[] args) {
    runScaffoldPreview();

    logger.info("========================================");
    logger.info("Metrics Monitoring System");
    logger.info("Version: 1.0.0-SNAPSHOT");
    logger.info("Project 5 - Week 9");
    logger.info("========================================");

    SpringApplication.run(MetricsApplication.class, args);

    logger.info("✅ Metrics Service Started Successfully");
    logger.info("📊 Endpoints:");
    logger.info("   - GET  /health              : Service health check");
    logger.info("   - GET  /metrics             : Prometheus metrics (scraped every 15s)");
    logger.info("   - GET  /metrics/aggregator  : Aggregated metrics as JSON");
    logger.info("   - GET  /metrics/system      : OS metrics (CPU, memory, I/O)");
    logger.info("   - POST /metrics/custom      : Custom metric submission");
  }

  private static void runScaffoldPreview() {
    CircularDoubleBuffer latencyBuffer = new CircularDoubleBuffer(10);
    latencyBuffer.add(11.0);
    latencyBuffer.add(18.0);
    latencyBuffer.add(7.0);
    latencyBuffer.add(25.0);
    latencyBuffer.add(13.0);

    SlidingPercentileCalculator calculator = new SlidingPercentileCalculator();
    double[] snapshot = latencyBuffer.snapshot();
    logger.info(
        "Scaffold preview p50={} p90={} p99={}",
        calculator.percentile(snapshot, 50),
        calculator.percentile(snapshot, 90),
        calculator.percentile(snapshot, 99));
  }
}
