package org.sharetrace.util.range;

import java.util.Iterator;
import javax.annotation.Nonnull;

public final class LongRange implements Range<Long> {

  private final long start;
  private final long stop;
  private final long step;
  private final boolean ascending;

  private LongRange(long first, long last, long delta) {
    start = first;
    stop = last;
    step = delta;
    ascending = delta > 0;
  }

  public static Range<Long> of(long start, long stop, long step) {
    return new LongRange(start, stop, step);
  }

  public static Range<Long> of(long start, long stop) {
    return new LongRange(start, stop, 1L);
  }

  public static Range<Long> of(long stop) {
    return new LongRange(0L, stop, 1L);
  }

  public static Range<Long> single(long value) {
    return new LongRange(value, value + 1L, 1L);
  }

  @Override
  @Nonnull
  public Iterator<Long> iterator() {
    return new Iterator<>() {
      private long next = start;

      @Override
      public boolean hasNext() {
        return ascending ? next < stop : next > stop;
      }

      @Override
      public Long next() {
        long value = next;
        next = Math.addExact(next, step);
        return value;
      }
    };
  }
}
