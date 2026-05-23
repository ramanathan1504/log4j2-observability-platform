package com.observability.metrics;

import java.util.Arrays;

public class CircularDoubleBuffer {

  private final double[] values;
  private int cursor;
  private int size;

  public CircularDoubleBuffer(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.values = new double[capacity];
  }

  public synchronized void add(double value) {
    values[cursor] = value;
    cursor = (cursor + 1) % values.length;
    if (size < values.length) {
      size++;
    }
  }

  public synchronized double[] snapshot() {
    double[] snapshot = new double[size];
    for (int i = 0; i < size; i++) {
      int index = (cursor - size + i + values.length) % values.length;
      snapshot[i] = values[index];
    }
    return snapshot;
  }

  public synchronized int size() {
    return size;
  }

  public int capacity() {
    return values.length;
  }

  @Override
  public synchronized String toString() {
    return Arrays.toString(snapshot());
  }
}
