package sharetrace.util;

import com.google.common.collect.Range;

public final class Checks {

  private Checks() {}

  public static <T extends Comparable<T>> void checkRange(T value, Range<T> range, String name) {
    if (!range.contains(value)) {
      throw new IllegalArgumentException("%s must be in %s; got %s".formatted(name, range, value));
    }
  }
}
