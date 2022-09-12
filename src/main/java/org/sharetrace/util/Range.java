package org.sharetrace.util;

import java.math.BigDecimal;
import java.util.Iterator;

public interface Range<T extends Number> extends Iterable<T> {

  static Range<Integer> of(int start, int stop, int step) {
    return () ->
        new Iterator<>() {
          private final Iterator<Double> iterator = of((double) start, stop, step).iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Integer next() {
            return iterator.next().intValue();
          }
        };
  }

  static Range<Integer> of(int stop) {
    return of(0, stop, 1);
  }

  static Range<Integer> single(int value) {
    return of(value, value + 1, 1);
  }

  static Range<Double> of(double start, double stop, double step) {
    return () ->
        new Iterator<>() {
          private final BigDecimal delta = BigDecimal.valueOf(step);
          private final boolean ascending = delta.compareTo(BigDecimal.ZERO) >= 0;
          private final BigDecimal last = BigDecimal.valueOf(stop);
          private BigDecimal curr = BigDecimal.valueOf(start);

          @Override
          public boolean hasNext() {
            return ascending ? curr.compareTo(last) < 0 : curr.compareTo(last) > 0;
          }

          @Override
          public Double next() {
            double next = curr.doubleValue();
            curr = curr.add(delta);
            return next;
          }
        };
  }

  static Range<Double> of(double stop) {
    return of(0d, stop, 1d);
  }

  static Range<Double> single(double value) {
    return of(value, value + 1d, 1d);
  }
}
