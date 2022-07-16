package org.sharetrace.util;

import static org.sharetrace.util.Preconditions.checkIsAtLeast;
import static org.sharetrace.util.Preconditions.checkIsNonzero;
import static org.sharetrace.util.Preconditions.checkIsPositive;
import java.math.BigDecimal;
import java.util.AbstractCollection;
import java.util.Iterator;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRange extends AbstractCollection<Double> {

  public static Range single(int value) {
    return Range.builder().start(value).stop(value + 1).build();
  }

  public static Range single(int value, double scale) {
    return Range.builder().start(value).stop(value + 1).scale(scale).build();
  }

  public static Range of(int start, int stop, int step) {
    return Range.builder().start(start).stop(stop).step(step).build();
  }

  public static Range of(int stop) {
    return Range.builder().stop(stop).build();
  }

  public static Range of(int start, int stop) {
    return Range.builder().start(start).stop(stop).build();
  }

  @Override
  public Iterator<Double> iterator() {
    return new Iterator<>() {

      private final BigDecimal scale = BigDecimal.valueOf(scale());
      private int next = start();

      @Override
      public boolean hasNext() {
        return step() > 0 ? next < stop() : next > stop();
      }

      @Override
      public Double next() {
        double nxt = computeNext();
        next += step();
        return nxt;
      }

      private double computeNext() {
        return BigDecimal.valueOf(next).multiply(scale).doubleValue();
      }
    };
  }

  @Value.Parameter(order = 4)
  @Value.Default
  public double scale() {
    return 1d;
  }

  @Value.Parameter(order = 1)
  @Value.Default
  public int start() {
    return 0;
  }

  @Value.Parameter(order = 3)
  @Value.Default
  public int step() {
    return 1;
  }

  @Value.Parameter(order = 2)
  public abstract int stop();

  @Override
  @Value.Derived
  public int size() {
    double steps = Math.abs(1d * (stop() - start()) / step());
    return Math.toIntExact((long) Math.ceil(steps));
  }

  @Override
  public String toString() {
    return "Range{start="
        + start()
        + ", stop="
        + stop()
        + ", step="
        + step()
        + ", scale="
        + scale()
        + "}";
  }

  @Value.Check
  protected void check() {
    if (checkIsNonzero(step(), "step") > 0) {
      checkIsAtLeast(stop(), start(), "stop");
    } else {
      checkIsAtLeast(start(), stop(), "start");
    }
    checkIsPositive(scale(), "scale");
  }
}
