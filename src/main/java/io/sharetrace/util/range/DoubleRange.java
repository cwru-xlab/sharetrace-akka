package io.sharetrace.util.range;

import java.math.BigDecimal;
import java.util.Iterator;
import javax.annotation.Nonnull;

public final class DoubleRange implements Range<Double> {

  private final Range<Double> doubles;

  private DoubleRange(Range<BigDecimal> bigDecimals) {
    doubles = bigDecimals.map(DoubleRange::toDoubleExact);
  }

  public static Range<Double> of(double start, double stop, double step) {
    BigDecimal first = BigDecimal.valueOf(start);
    BigDecimal last = BigDecimal.valueOf(stop);
    BigDecimal delta = BigDecimal.valueOf(step);
    return new DoubleRange(BigDecimalRange.of(first, last, delta));
  }

  public static Range<Double> of(double start, double stop) {
    return of(start, stop, 1d);
  }

  public static Range<Double> of(double stop) {
    return of(0d, stop, 1d);
  }

  public static Range<Double> single(double value) {
    return of(value, value + 1d, 1d);
  }

  private static double toDoubleExact(BigDecimal value) {
    double v = value.doubleValue();
    if (v == Double.NEGATIVE_INFINITY || v == Double.POSITIVE_INFINITY) {
      throw new ArithmeticException("double overflow");
    }
    return v;
  }

  @Override
  @Nonnull
  public Iterator<Double> iterator() {
    return doubles.iterator();
  }
}
