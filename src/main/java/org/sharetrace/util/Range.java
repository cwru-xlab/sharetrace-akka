package org.sharetrace.util;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.function.DoubleFunction;
import java.util.function.LongFunction;

public interface Range<T extends Number> extends Iterable<T> {

  static Range<Integer> ofInts(int start, int stop, int step) {
    return narrow(ofLongs(start, stop, step), v -> (int) v);
  }

  static Range<Integer> ofInts(int start, int stop) {
    return ofInts(start, stop, 1);
  }

  static Range<Integer> ofInts(int stop) {
    return ofInts(0, stop);
  }

  static Range<Integer> ofInt(int value) {
    return ofInts(value, value + 1);
  }

  static Range<Long> ofLongs(long start, long stop, long step) {
    return () ->
        new Iterator<>() {
          private long value = start - step;

          @Override
          public boolean hasNext() {
            return step > 0 ? value < stop : value > stop;
          }

          @Override
          public Long next() {
            return value += step;
          }
        };
  }

  static Range<Long> ofLongs(long start, long stop) {
    return ofLongs(start, stop, 1L);
  }

  static Range<Long> ofLongs(long stop) {
    return ofLongs(0L, stop);
  }

  static Range<Long> ofLong(long value) {
    return ofLongs(value, value + 1L);
  }

  static Range<Short> ofShorts(short start, short stop, short step) {
    return narrow(ofLongs(start, stop, step), v -> (short) v);
  }

  static Range<Short> ofShorts(short start, short stop) {
    return ofShorts(start, stop, (short) 1);
  }

  static Range<Short> ofShorts(short stop) {
    return ofShorts((short) 0, stop);
  }

  static Range<Short> ofShort(short value) {
    return ofShorts(value, (short) (value + 1));
  }

  static Range<Double> ofDoubles(double start, double stop, double step) {
    BigDecimal first = BigDecimal.valueOf(start);
    BigDecimal last = BigDecimal.valueOf(stop);
    BigDecimal delta = BigDecimal.valueOf(step);
    boolean ascending = delta.compareTo(BigDecimal.ZERO) > 0;
    return () ->
        new Iterator<>() {
          private BigDecimal value = first.subtract(delta);

          @Override
          public boolean hasNext() {
            return ascending ? value.compareTo(last) < 0 : value.compareTo(last) > 0;
          }

          @Override
          public Double next() {
            return (value = value.add(delta)).doubleValue();
          }
        };
  }

  static Range<Double> ofDoubles(double start, double stop) {
    return ofDoubles(start, stop, 1d);
  }

  static Range<Double> ofDoubles(double stop) {
    return ofDoubles(0d, stop);
  }

  static Range<Double> ofDouble(double value) {
    return ofDoubles(value, value + 1d);
  }

  static Range<Float> ofFloats(float start, float stop, float step) {
    return narrow(ofDoubles(start, stop, step), v -> (float) v);
  }

  static Range<Float> ofFloats(float start, float stop) {
    return ofFloats(start, stop, 1f);
  }

  static Range<Float> ofFloats(float stop) {
    return ofFloats(0f, stop);
  }

  static Range<Float> ofFloat(float value) {
    return ofFloats(value, value + 1f);
  }

  private static <T extends Number> Range<T> narrow(Range<Double> range, DoubleFunction<T> cast) {
    return () ->
        new Iterator<>() {
          private final Iterator<Double> iterator = range.iterator();

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

  private static <T extends Number> Range<T> narrow(Range<Long> range, LongFunction<T> cast) {
    return () ->
        new Iterator<>() {
          private final Iterator<Long> iterator = range.iterator();

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
