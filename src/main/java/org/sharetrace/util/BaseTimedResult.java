package org.sharetrace.util;

import java.time.Duration;
import java.util.Arrays;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimedResult {

  public static TimedResult time(Runnable runnable, int nRepeats) {
    double[] runtimes = collectRuntimes(runnable, nRepeats);
    StatisticalSummary summary = summarize(runtimes);
    return fromSummary(summary);
  }

  private static double[] collectRuntimes(Runnable runnable, int nRepeats) {
    long start, stop;
    double[] runtimes = new double[nRepeats];
    for (int i = 0; i < nRepeats; i++) {
      start = System.nanoTime();
      runnable.run();
      stop = System.nanoTime();
      runtimes[i] = stop - start;
    }
    return runtimes;
  }

  private static StatisticalSummary summarize(double[] values) {
    SummaryStatistics summary = new SummaryStatistics();
    Arrays.stream(values).forEach(summary::addValue);
    return summary;
  }

  private static TimedResult fromSummary(StatisticalSummary summary) {
    return TimedResult.builder()
        .max(fromNanos(summary.getMax()))
        .min(fromNanos(summary.getMin()))
        .mean(fromNanos(summary.getMean()))
        .stdDev(fromNanos(summary.getStandardDeviation()))
        .build();
  }

  private static Duration fromNanos(double nanos) {
    return Duration.ofNanos(Math.round(nanos));
  }

  public abstract Duration max();

  public abstract Duration min();

  public abstract Duration mean();

  public abstract Duration stdDev();
}
