package org.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseDescriptiveStats {

  public static DescriptiveStats of(Collection<Number> values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    values.stream().mapToDouble(Number::floatValue).forEach(statistics::addValue);
    return DescriptiveStats.builder().statistics(statistics).build();
  }

  public static DescriptiveStats of(float[] values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    for (float value : values) {
      statistics.addValue(value);
    }
    return DescriptiveStats.builder().statistics(statistics).build();
  }

  @Value.Derived
  public float mean() {
    return (float) statistics().getMean();
  }

  @JsonIgnore
  protected abstract DescriptiveStatistics statistics();

  @Value.Derived
  public float sampleVariance() {
    return (float) statistics().getVariance();
  }

  @Value.Derived
  public float max() {
    return (float) statistics().getMax();
  }

  @Value.Derived
  public float min() {
    return (float) statistics().getMin();
  }

  @Value.Derived
  public float median() {
    return (float) statistics().getPercentile(50);
  }

  @Value.Derived
  public float lowerWhisker() {
    return (float) (lowerQuartile() - 1.5 * interQuartileRange());
  }

  @Value.Derived
  public float lowerQuartile() {
    return (float) statistics().getPercentile(25);
  }

  @Value.Derived
  public float interQuartileRange() {
    return upperQuartile() - lowerQuartile();
  }

  @Value.Derived
  public float upperQuartile() {
    return (float) statistics().getPercentile(75);
  }

  @Value.Derived
  public float upperWhisker() {
    return (float) (upperQuartile() + 1.5 * interQuartileRange());
  }

  @Value.Derived
  public long size() {
    return statistics().getN();
  }

  @Value.Derived
  public float sum() {
    return (float) statistics().getSum();
  }

  @Value.Derived
  public float[] outliers() {
    List<Float> outliers = new ArrayList<>();
    for (double value : statistics().getValues()) {
      if (value < lowerWhisker() || value > upperWhisker()) {
        outliers.add((float) value);
      }
    }
    float[] unwrapped = new float[outliers.size()];
    for (int i = 0; i < outliers.size(); i++) {
      unwrapped[i] = outliers.get(i);
    }
    return unwrapped;
  }
}
