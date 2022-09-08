package org.sharetrace.util;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.doubles.AbstractDoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import java.math.BigDecimal;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRange extends AbstractDoubleCollection {

  public static Range single(int value) {
    return Range.builder().start(value).stop(value + 1).build();
  }

  public static Range single(int value, double scale) {
    return Range.builder().start(value).stop(value + 1).scale(scale).build();
  }

  public static Range of(int stop) {
    return Range.builder().stop(stop).build();
  }

  @Override
  public DoubleIterator iterator() {
    return new DoubleIterator() {

      private final BigDecimal scale = BigDecimal.valueOf(scale());
      private int next = start();

      @Override
      public boolean hasNext() {
        return step() > 0 ? next < stop() : next > stop();
      }

      @Override
      public double nextDouble() {
        double nxt = BigDecimal.valueOf(next).multiply(scale).doubleValue();
        next += step();
        return nxt;
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("start", start())
        .add("stop", stop())
        .add("step", step())
        .add("scale", scale())
        .toString();
  }

  @Override
  @Value.Derived
  public int size() {
    double range = stop() - start();
    double steps = Math.abs(range / step());
    return Math.toIntExact((long) Math.ceil(steps));
  }

  @Value.Check
  protected void check() {
    if (Checks.isNot(step(), 0, "step") > 0) {
      Checks.isAtLeast(stop(), start(), "stop");
    } else {
      Checks.isAtLeast(start(), stop(), "start");
    }
    Checks.isGreaterThan(scale(), 0d, "scale");
  }
}
