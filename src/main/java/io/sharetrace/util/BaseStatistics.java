package io.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseStatistics {

  public static Statistics of(Collection<? extends Number> values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    values.stream().mapToDouble(Number::doubleValue).forEach(statistics::addValue);
    return Statistics.builder().statistics(statistics).build();
  }

  @JsonIgnore
  protected abstract DescriptiveStatistics statistics();

  @Value.Derived
  public double mean() {
    return statistics().getMean();
  }

  @Value.Derived
  public double variance() {
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
    return (upperQuartile() + 1.5 * interQuartileRange());
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
  public List<Double> outliers() {
    return DoubleStream.of(statistics().getValues())
        .filter(v -> v < lowerWhisker() || v > upperWhisker())
        .boxed()
        .collect(Collecting.toUnmodifiableDoubleList());
  }
}
