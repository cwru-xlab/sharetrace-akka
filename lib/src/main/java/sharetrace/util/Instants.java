package sharetrace.util;

import java.time.Instant;

public final class Instants {

  private Instants() {}

  public static Instant max(Instant left, Instant right) {
    return left.isAfter(right) ? left : right;
  }

  public static Instant min(Instant left, Instant right) {
    return left.isBefore(right) ? left : right;
  }
}
