package org.sharetrace.util;

import java.util.Iterator;

public interface Range<T extends Number> extends Iterable<T> {

  static Range<Integer> of(int start, int stop, int step) {
    return () ->
        new Iterator<>() {
          private int curr = start;

          @Override
          public boolean hasNext() {
            return curr != stop;
          }

          @Override
          public Integer next() {
            int next = curr;
            curr += step;
            return next;
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
          private double curr = start;

          @Override
          public boolean hasNext() {
            return curr != stop;
          }

          @Override
          public Double next() {
            double next = curr;
            curr += step;
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

  static Range<Float> of(float start, float stop, float step) {
    return () ->
        new Iterator<>() {
          private float curr = start;

          @Override
          public boolean hasNext() {
            return curr != stop;
          }

          @Override
          public Float next() {
            float next = curr;
            curr += step;
            return next;
          }
        };
  }

  static Range<Float> of(float stop) {
    return of(0f, stop, 1f);
  }

  static Range<Float> single(float value) {
    return of(value, value + 1f, 1f);
  }
}
