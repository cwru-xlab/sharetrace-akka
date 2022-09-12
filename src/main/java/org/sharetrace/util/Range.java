package org.sharetrace.util;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.function.DoubleFunction;

public interface Range<T extends Number> extends Iterable<T> {

  static Range<Integer> of(int start, int stop, int step) {
    return cast(of((double) start, stop, step), d -> (int) d);
  }

  static Range<Integer> of(int stop) {
    return of(0, stop, 1);
  }

  static Range<Integer> single(int value) {
    return of(value, value + 1, 1);
  }

  static Range<Long> of(long start, long stop, long step) {
    return cast(of((double) start, stop, step), d -> (long) d);
  }

  static Range<Long> of(long stop) {
    return of(0L, stop, 1L);
  }

  static Range<Long> single(long value) {
    return of(value, value + 1L, 1L);
  }

  static Range<Short> of(short start, short stop, short step) {
    return cast(of((double) start, stop, step), d -> (short) d);
  }

  static Range<Short> of(short stop) {
    return of((short) 0, stop, (short) 1);
  }

  static Range<Short> single(short value) {
    return of(value, (short) (value + 1), (short) 1);
  }

  static Range<Double> of(double start, double stop, double step) {
    BigDecimal first = BigDecimal.valueOf(start);
    BigDecimal last = BigDecimal.valueOf(stop);
    BigDecimal delta = BigDecimal.valueOf(step);
    boolean ascending = delta.compareTo(BigDecimal.ZERO) >= 0;
    return () ->
        new Iterator<>() {
          private BigDecimal value = first;

          @Override
          public boolean hasNext() {
            return ascending ? value.compareTo(last) < 0 : value.compareTo(last) > 0;
          }

          @Override
          public Double next() {
            double next = value.doubleValue();
            value = value.add(delta);
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

  private static <T extends Number> Range<T> cast(Range<Double> doubles, DoubleFunction<T> cast) {
    return () ->
        new Iterator<>() {
          private final Iterator<Double> iterator = doubles.iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public T next() {
            return cast.apply(iterator.next());
          }
        };
  }
}
