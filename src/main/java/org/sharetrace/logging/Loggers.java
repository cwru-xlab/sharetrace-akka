package org.sharetrace.logging;

public final class Loggers {

  private static final String METRICS = "MetricLogger";
  private static final String EVENTS = "EventLogger";

  private Loggers() {}

  public static String events() {
    return EVENTS;
  }

  public static String metrics() {
    return METRICS;
  }
}
