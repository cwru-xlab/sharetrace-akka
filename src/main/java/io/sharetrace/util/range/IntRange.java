package io.sharetrace.util.range;

import java.util.Iterator;
import javax.annotation.Nonnull;

public final class IntRange implements Range<Integer> {

  private final Range<Integer> ints;

  private IntRange(Range<Long> longs) {
    ints = longs.map(Math::toIntExact);
  }

  public static Range<Integer> of(long start, long stop, long step) {
    return new IntRange(LongRange.of(start, stop, step));
  }

  public static Range<Integer> of(long start, long stop) {
    return new IntRange(LongRange.of(start, stop));
  }

  public static Range<Integer> of(long stop) {
    return new IntRange(LongRange.of(stop));
  }

  public static Range<Integer> single(long value) {
    return new IntRange(LongRange.single(value));
  }

  @Override
  @Nonnull
  public Iterator<Integer> iterator() {
    return ints.iterator();
  }
}
