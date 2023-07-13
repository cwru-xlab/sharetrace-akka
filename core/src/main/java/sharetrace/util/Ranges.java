package sharetrace.util;

public final class Ranges {

  private Ranges() {}

  public static double normalized(double value, double min, double max) {
    return finiteDouble(value - min) / finiteDouble(max - min);
  }

  public static double finiteDouble(double value) {
    return boundedDouble(value, -Double.MAX_VALUE, Double.MAX_VALUE);
  }

  public static float finiteFloat(double value) {
    return boundedFloat(value, -Float.MAX_VALUE, Float.MAX_VALUE);
  }

  public static float boundedFloat(double value, float min, float max) {
    return (float) boundedDouble(value, min, max);
  }

  public static double boundedDouble(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
