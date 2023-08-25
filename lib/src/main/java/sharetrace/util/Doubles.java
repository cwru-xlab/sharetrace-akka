package sharetrace.util;

public final class Doubles {

  private Doubles() {}

  public static double normalized(double value, double min, double max) {
    return finite(value - min) / finite(max - min);
  }

  public static double finite(double value) {
    return bounded(value, -Double.MAX_VALUE, Double.MAX_VALUE);
  }

  @SuppressWarnings("UnstableApiUsage")
  public static double bounded(double value, double min, double max) {
    return com.google.common.primitives.Doubles.constrainToRange(value, min, max);
  }
}
