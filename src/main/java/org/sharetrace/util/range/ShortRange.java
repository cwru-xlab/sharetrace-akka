package org.sharetrace.util.range;

import java.util.Iterator;
import javax.annotation.Nonnull;

public final class ShortRange implements Range<Short> {

  private final Range<Short> shorts;

  private ShortRange(Range<Long> longs) {
    shorts = longs.map(ShortRange::toShortExact);
  }

  public static ShortRange of(long start, long stop, long step) {
    return new ShortRange(LongRange.of(start, stop, step));
  }

  public static ShortRange of(long start, long stop) {
    return new ShortRange(LongRange.of(start, stop));
  }

  public static ShortRange of(long stop) {
    return new ShortRange(LongRange.of(stop));
  }

  public static ShortRange single(long value) {
    return new ShortRange(LongRange.single(value));
  }

  private static short toShortExact(long value) {
    if ((short) value != value) {
      throw new ArithmeticException("short overflow");
    }
    return (short) value;
  }

  @Override
  @Nonnull
  public Iterator<Short> iterator() {
    return shorts.iterator();
  }
}
