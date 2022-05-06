package org.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseDescriptiveStats {

  public static DescriptiveStats of(Collection<Number> values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    values.stream().mapToDouble(Number::doubleValue).forEach(statistics::addValue);
    return DescriptiveStats.builder().statistics(statistics).build();
  }

  public static DescriptiveStats of(double[] values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    Arrays.stream(values).forEach(statistics::addValue);
    return DescriptiveStats.builder().statistics(statistics).build();
  }

  @Value.Derived
  public double mean() {
    return statistics().getMean();
  }

  @JsonIgnore
  protected abstract DescriptiveStatistics statistics();

  @Value.Derived
  public double sampleVariance() {
    return statistics().getVariance();
  }

  @Value.Derived
  public double max() {
    return statistics().getMax();
  }

  @Value.Derived
  public double min() {
    return statistics().getMin();
  }

  @Value.Derived
  public double median() {
    return statistics().getPercentile(50);
  }

  @Value.Derived
  public double lowerWhisker() {
    return lowerQuartile() - 1.5 * interQuartileRange();
  }

  @Value.Derived
  public double lowerQuartile() {
    return statistics().getPercentile(25);
  }

  @Value.Derived
  public double interQuartileRange() {
    return upperQuartile() - lowerQuartile();
  }

  @Value.Derived
  public double upperQuartile() {
    return statistics().getPercentile(75);
  }

  @Value.Derived
  public double upperWhisker() {
    return upperQuartile() + 1.5 * interQuartileRange();
  }

  @Value.Derived
  public long size() {
    return statistics().getN();
  }

  @Value.Derived
  public double sum() {
    return statistics().getSum();
  }

  @Value.Derived
  public double[] outliers() {
    return Arrays.stream(statistics().getValues())
        .filter(v -> v < lowerWhisker() || v > upperWhisker())
        .toArray();
  }
}
