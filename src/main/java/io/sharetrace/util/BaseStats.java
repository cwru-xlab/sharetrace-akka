package io.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseStats {

  public static Stats of(Collection<? extends Number> values) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    values.stream().mapToDouble(Number::doubleValue).forEach(statistics::addValue);
    return Stats.builder().statistics(statistics).build();
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
    return (float) statistics().getPercentile(50d);
  }

  @Value.Derived
  public float lowerWhisker() {
    return (float) (lowerQuartile() - 1.5 * interQuartileRange());
  }

  @Value.Derived
  public float lowerQuartile() {
    return (float) statistics().getPercentile(25d);
  }

  @Value.Derived
  public float interQuartileRange() {
    return upperQuartile() - lowerQuartile();
  }

  @Value.Derived
  public float upperQuartile() {
    return (float) statistics().getPercentile(75d);
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
  public List<Float> outliers() {
    return Arrays.stream(statistics().getValues())
        .filter(v -> v < lowerWhisker() || v > upperWhisker())
        .mapToObj(v -> (float) v)
        .collect(FloatArrayList::new, List::add, List::addAll);
  }
}
