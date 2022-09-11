package org.sharetrace.util;

import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Ranges {

  public static Iterable<Double> ofDoubles(double start, double stop, double step) {
    return iter(DoubleStream.iterate(start, v -> step > 0d ? v < stop : v > stop, v -> v + step));
  }

  public static Iterable<Double> ofDoubles(double stop) {
    return ofDoubles(0d, stop, 1d);
  }

  public static Iterable<Double> ofDouble(double value) {
    return ofDoubles(value, value + 1d, 1d);
  }

  public static Iterable<Float> ofFloats(double start, double stop, double step) {
    Iterable<Double> iterable = ofDoubles(start, stop, step);
    Stream<Double> stream = StreamSupport.stream(iterable.spliterator(), false);
    return iter(stream.mapToDouble(v -> v).mapToObj(v -> (float) v));
  }

  public static Iterable<Float> ofFloat(float value) {
    return ofFloats(value, value + 1f, 1f);
  }

  public static Iterable<Float> ofFloats(float stop) {
    return ofFloats(0f, stop, 1f);
  }

  public static Iterable<Integer> ofInts(int start, int stop, int step) {
    return iter(IntStream.iterate(start, v -> step > 0 ? v < stop : v > stop, v -> v + step));
  }

  public static Iterable<Integer> ofInts(int stop) {
    return iter(IntStream.range(0, stop));
  }

  public static Iterable<Integer> ofInt(int value) {
    return iter(IntStream.of(value));
  }

  private static <S extends BaseStream<T, S>, T> Iterable<T> iter(BaseStream<T, S> stream) {
    return stream::iterator;
  }
}
