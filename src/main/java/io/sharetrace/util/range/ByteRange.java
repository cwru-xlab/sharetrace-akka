package io.sharetrace.util.range;

import java.util.Iterator;
import javax.annotation.Nonnull;

public final class ByteRange implements Range<Byte> {

  private final Range<Byte> bytes;

  private ByteRange(Range<Long> longs) {
    this.bytes = longs.map(ByteRange::toByteExact);
  }

  public static Range<Byte> of(long start, long stop, long step) {
    return new ByteRange(LongRange.of(start, stop, step));
  }

  public static Range<Byte> of(long start, long stop) {
    return new ByteRange(LongRange.of(start, stop));
  }

  public static Range<Byte> of(long stop) {
    return new ByteRange(LongRange.of(stop));
  }

  public static Range<Byte> single(long value) {
    return new ByteRange(LongRange.single(value));
  }

  private static byte toByteExact(long value) {
    if ((byte) value != value) {
      throw new ArithmeticException("byte overflow");
    }
    return (byte) value;
  }

  @Override
  @Nonnull
  public Iterator<Byte> iterator() {
    return bytes.iterator();
  }
}
