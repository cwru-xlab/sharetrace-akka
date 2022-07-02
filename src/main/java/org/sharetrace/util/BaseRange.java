package org.sharetrace.util;

import java.util.Iterator;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRange implements Iterable<Integer> {

  public static Range of(int start, int stop) {
    return Range.of(start, stop, 1);
  }

  public static Range of(int stop) {
    return Range.of(0, stop, 1);
  }

  public static Range single(int value) {
    return Range.of(value, value + 1, 1);
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<>() {
      private int next = start();

      @Override
      public boolean hasNext() {
        return next < stop();
      }

      @Override
      public Integer next() {
        int nxt = next;
        next += step();
        return nxt;
      }
    };
  }

  @Value.Parameter
  public abstract int start();

  @Value.Parameter
  public abstract int stop();

  @Value.Parameter
  public abstract int step();

  @Value.Check
  protected void check() {
    if (Preconditions.checkIsNonzero(step(), "step") > 0) {
      Preconditions.checkIsAtLeast(stop(), start(), "stop");
    } else {
      Preconditions.checkIsAtLeast(start(), stop(), "start");
    }
  }
}
