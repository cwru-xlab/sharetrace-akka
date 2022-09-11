package org.sharetrace.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoublePredicate;
import it.unimi.dsi.fastutil.doubles.DoubleUnaryOperator;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import it.unimi.dsi.fastutil.ints.IntUnaryOperator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public final class Ranges {

  public static DoubleRange ofDoubles(double start, double stop, double step) {
    DoublePredicate hasNext = v -> (step > 0d) ? v < stop : v > stop;
    DoubleUnaryOperator next = v -> v + step;
    return DoubleStream.iterate(start, hasNext, next)::iterator;
  }

  public static DoubleRange ofDoubles(double stop) {
    return ofDoubles(0d, stop, 1d);
  }

  public static DoubleRange ofDouble(double value) {
    return DoubleStream.of(value)::iterator;
  }

  public static FloatRange ofFloats(float start, float stop, float step) {
    Iterator<Double> iterator = ofDoubles(start, stop, step).iterator();
    return DoubleStream.generate(iterator::next).mapToObj(v -> (float) v)::iterator;
  }

  public static FloatRange ofFloat(float value) {
    return ofFloats(value, value + 1f, 1f);
  }

  public static FloatRange ofFloats(float stop) {
    return ofFloats(0f, stop, 1f);
  }

  public static IntRange ofInts(int start, int stop, int step) {
    IntPredicate hasNext = v -> (step > 0) ? v < stop : v > stop;
    IntUnaryOperator next = v -> v + step;
    return IntStream.iterate(start, hasNext, next)::iterator;
  }

  public static IntRange ofInts(int stop) {
    return IntStream.range(0, stop)::iterator;
  }

  public static IntRange ofInt(int value) {
    return IntStream.of(value)::iterator;
  }

  public interface Range<T extends Number> extends Iterable<T> {

    List<T> toList();
  }

  public interface DoubleRange extends Range<Double> {

    default List<Double> toList() {
      return new DoubleArrayList(iterator());
    }
  }

  public interface IntRange extends Range<Integer> {

    default List<Integer> toList() {
      return new IntArrayList(iterator());
    }
  }

  public interface FloatRange extends Range<Float> {

    default List<Float> toList() {
      return new FloatArrayList(iterator());
    }
  }
}
