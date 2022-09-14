package org.sharetrace.util.range;

import com.google.common.primitives.Floats;
import java.math.BigDecimal;
import java.util.Iterator;
import javax.annotation.Nonnull;

public final class FloatRange implements Range<Float> {

  private final Range<Float> floats;

  public FloatRange(Range<BigDecimal> bigDecimals) {
    floats = bigDecimals.map(FloatRange::toFloatExact);
  }

  public static Range<Float> of(double start, double stop, double step) {
    BigDecimal delta = BigDecimal.valueOf(step);
    BigDecimal first = BigDecimal.valueOf(start);
    BigDecimal last = BigDecimal.valueOf(stop);
    return new FloatRange(BigDecimalRange.of(first, last, delta));
  }

  public static Range<Float> of(double start, double stop) {
    return of(start, stop, 1d);
  }

  public static Range<Float> of(double stop) {
    return of(0d, stop, 1d);
  }

  public static Range<Float> single(double value) {
    return of(value, value + 1d, 1d);
  }

  private static float toFloatExact(BigDecimal value) {
    float v = value.floatValue();
    if (!Floats.isFinite(v)) {
      throw new ArithmeticException("float overflow");
    }
    return v;
  }

  @Override
  @Nonnull
  public Iterator<Float> iterator() {
    return floats.iterator();
  }
}
