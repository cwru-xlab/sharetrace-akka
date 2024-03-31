package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@SuppressWarnings("unused")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@FunctionalInterface
public interface DistributedRandom {

  double nextDouble();

  default double nextDouble(double bound) {
    return nextDouble() * bound;
  }

  default double nextDouble(double origin, double bound) {
    return nextDouble(bound - origin) + origin;
  }

  default long nextLong(double bound) {
    return Math.round(nextDouble(bound));
  }
}
