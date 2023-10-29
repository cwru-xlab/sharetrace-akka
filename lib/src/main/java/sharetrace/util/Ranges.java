package sharetrace.util;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;

public final class Ranges {

  private Ranges() {}

  public static <T extends Comparable<T>> void check(String name, T value, Range<T> range) {
    if (!range.contains(value)) {
      throw new IllegalArgumentException("%s must be in %s; got %s".formatted(name, range, value));
    }
  }

  public static double normalized(double value, double min, double max) {
    return finite(value - min) / finite(max - min);
  }

  public static double finite(double value) {
    return bounded(value, -Double.MAX_VALUE, Double.MAX_VALUE);
  }

  @SuppressWarnings("UnstableApiUsage")
  public static double bounded(double value, double min, double max) {
    return Doubles.constrainToRange(value, min, max);
  }
}
