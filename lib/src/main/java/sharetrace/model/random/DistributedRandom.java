package sharetrace.model.random;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@FunctionalInterface
public interface DistributedRandom {

  double nextDouble();

  default long nextLong(double bound) {
    return Math.round(nextDouble() * bound);
  }
}
