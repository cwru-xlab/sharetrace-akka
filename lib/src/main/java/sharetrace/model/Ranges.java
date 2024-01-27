package sharetrace.model;

import com.google.common.collect.Range;

public final class Ranges {

  private Ranges() {}

  public static <T extends Comparable<T>> void check(String name, T value, Range<T> range) {
    if (!range.contains(value)) {
      throw new IllegalArgumentException("%s must be in %s; got %s".formatted(name, range, value));
    }
  }
}
