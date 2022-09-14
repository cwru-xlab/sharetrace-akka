package org.sharetrace.util.range;

import java.math.BigDecimal;
import java.util.Iterator;
import javax.annotation.Nonnull;

public final class BigDecimalRange implements Range<BigDecimal> {

  private final BigDecimal start;
  private final BigDecimal stop;
  private final BigDecimal step;
  private final boolean ascending;

  private BigDecimalRange(BigDecimal first, BigDecimal last, BigDecimal delta) {
    start = first;
    stop = last;
    step = delta;
    ascending = delta.compareTo(BigDecimal.ZERO) > 0;
  }

  public static Range<BigDecimal> of(BigDecimal start, BigDecimal stop, BigDecimal step) {
    return new BigDecimalRange(start, stop, step);
  }

  public static Range<BigDecimal> of(BigDecimal start, BigDecimal stop) {
    return new BigDecimalRange(start, stop, BigDecimal.ONE);
  }

  public static Range<BigDecimal> of(BigDecimal stop) {
    return new BigDecimalRange(BigDecimal.ZERO, stop, BigDecimal.ONE);
  }

  public static Range<BigDecimal> single(BigDecimal value) {
    return new BigDecimalRange(value, value.add(BigDecimal.ONE), BigDecimal.ONE);
  }

  @Override
  @Nonnull
  public Iterator<BigDecimal> iterator() {
    return new Iterator<>() {
      private BigDecimal next = start;

      @Override
      public boolean hasNext() {
        return ascending ? next.compareTo(stop) < 0 : next.compareTo(stop) > 0;
      }

      @Override
      public BigDecimal next() {
        BigDecimal value = next;
        next = next.add(step);
        return value;
      }
    };
  }
}
