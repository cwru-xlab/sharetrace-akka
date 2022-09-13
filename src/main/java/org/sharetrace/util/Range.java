package org.sharetrace.util;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.function.Function;

public interface Range<T extends Number> extends Iterable<T> {

  static Range<Integer> ofInts(long start, long stop, long step) {
    return map(ofLongs(start, stop, step), Math::toIntExact);
  }

  static Range<Integer> ofInts(long start, long stop) {
    return ofInts(start, stop, 1L);
  }

  static Range<Integer> ofInts(long stop) {
    return ofInts(0L, stop);
  }

  static Range<Integer> ofInt(long value) {
    return ofInts(value, value + 1L);
  }

  static Range<Long> ofLongs(long start, long stop, long step) {
    long first = Math.subtractExact(start, step);
    boolean ascending = step > 0;
    return () ->
        new Iterator<>() {
          private long value = first;

          @Override
          public boolean hasNext() {
            return ascending ? value < stop : value > stop;
          }

          @Override
          public Long next() {
            return value = Math.addExact(value, step);
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

  static Range<Short> ofShorts(long start, long stop, long step) {
    return map(ofLongs(start, stop, step), Range::toShortExact);
  }

  static Range<Short> ofShorts(long start, long stop) {
    return ofShorts(start, stop, 1L);
  }

  static Range<Short> ofShorts(long stop) {
    return ofShorts(0L, stop);
  }

  static Range<Short> ofShort(long value) {
    return ofShorts(value, value + 1L);
  }

  static Range<Byte> ofBytes(long start, long stop, long step) {
    return map(ofLongs(start, stop, step), Range::toByteExact);
  }

  static Range<Byte> ofBytes(long start, long stop) {
    return ofBytes(start, stop, 1L);
  }

  static Range<Byte> ofBytes(long stop) {
    return ofBytes(0L, stop);
  }

  static Range<Byte> ofByte(long value) {
    return ofBytes(value, value + 1L);
  }

  static Range<Double> ofDoubles(double start, double stop, double step) {
    BigDecimal last = BigDecimal.valueOf(stop);
    BigDecimal delta = BigDecimal.valueOf(step);
    BigDecimal first = BigDecimal.valueOf(start).subtract(delta);
    boolean ascending = delta.compareTo(BigDecimal.ZERO) > 0;
    return () ->
        new Iterator<>() {
          private BigDecimal value = first;

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

  static Range<Float> ofFloats(double start, double stop, double step) {
    return map(ofDoubles(start, stop, step), Range::toFloatExact);
  }

  static Range<Float> ofFloats(double start, double stop) {
    return ofFloats(start, stop, 1d);
  }

  static Range<Float> ofFloats(double stop) {
    return ofFloats(0d, stop);
  }

  static Range<Float> ofFloat(double value) {
    return ofFloats(value, value + 1d);
  }

  private static float toFloatExact(double value) {
    if ((float) value != value) {
      throw new ArithmeticException("float overflow");
    }
    return (float) value;
  }

  private static short toShortExact(long value) {
    if ((short) value != value) {
      throw new ArithmeticException("short overflow");
    }
    return (short) value;
  }

  private static byte toByteExact(long value) {
    if ((byte) value != value) {
      throw new ArithmeticException("byte overflow");
    }
    return (byte) value;
  }

  private static <T extends Number, R extends Number> Range<R> map(
      Range<T> range, Function<T, R> cast) {
    return () ->
        new Iterator<>() {
          private final Iterator<T> iterator = range.iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public R next() {
            return cast.apply(iterator.next());
          }
        };
  }
}
