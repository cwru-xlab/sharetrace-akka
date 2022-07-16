package org.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import java.util.Collection;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseDescriptiveStats {

  public static DescriptiveStats of(Collection<? extends Number> values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    values.stream().mapToDouble(Number::doubleValue).forEach(statistics::addValue);
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
    double[] values = statistics().getValues();
    FloatCollection outliers = new FloatArrayList(values.length);
    for (double value : values) {
      if (value < lowerWhisker() || value > upperWhisker()) {
        outliers.add((float) value);
      }
    }
    return outliers.toFloatArray();
  }
}
