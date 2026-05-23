package com.observability.metrics;

import java.util.Arrays;

public class SlidingPercentileCalculator {

  public double percentile(double[] values, int percentile) {
    if (values.length == 0) {
      return 0.0;
    }
    if (percentile < 0 || percentile > 100) {
      throw new IllegalArgumentException("percentile must be between 0 and 100");
    }

    double[] copy = Arrays.copyOf(values, values.length);
    Arrays.sort(copy);

    if (copy.length == 1) {
      return copy[0];
    }

    double rank = (percentile / 100.0) * (copy.length - 1);
    int lower = (int) Math.floor(rank);
    int upper = (int) Math.ceil(rank);

    if (lower == upper) {
      return copy[lower];
    }

    double weight = rank - lower;
    return copy[lower] + weight * (copy[upper] - copy[lower]);
  }
}
