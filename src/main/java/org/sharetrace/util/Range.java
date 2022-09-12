package org.sharetrace.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public interface Range<T extends Number> extends Iterable<T> {

  static OfInt of(int start, int stop, int step) {
    IntPredicate hasNext = v -> (step > 0) ? v < stop : v > stop;
    IntUnaryOperator next = v -> v + step;
    return IntStream.iterate(start, hasNext, next)::iterator;
  }

  static OfInt of(int stop) {
    return of(0, stop, 1);
  }

  static OfInt single(int value) {
    return of(value, value + 1, 1);
  }

  static OfDouble of(double start, double stop, double step) {
    DoublePredicate hasNext = v -> (step > 0d) ? v < stop : v > stop;
    DoubleUnaryOperator next = v -> v + step;
    return DoubleStream.iterate(start, hasNext, next)::iterator;
  }

  static OfDouble of(double stop) {
    return of(0d, stop, 1d);
  }

  static OfDouble single(double value) {
    return of(value, value + 1d, 1d);
  }

  static OfFloat of(float start, float stop, float step) {
    Iterator<Double> iterator = Range.of((double) start, stop, step).iterator();
    return DoubleStream.generate(iterator::next).mapToObj(v -> (float) v)::iterator;
  }

  static OfFloat of(float stop) {
    return of(0f, stop, 1f);
  }

  static OfFloat single(float value) {
    return of(value, value + 1f, 1f);
  }

  List<T> toList();

  interface OfInt extends Range<Integer> {

    @Override
    default List<Integer> toList() {
      return new IntArrayList(iterator());
    }
  }

  interface OfDouble extends Range<Double> {

    @Override
    default List<Double> toList() {
      return new DoubleArrayList(iterator());
    }
  }

  interface OfFloat extends Range<Float> {

    @Override
    default List<Float> toList() {
      return new FloatArrayList(iterator());
    }
  }
}
