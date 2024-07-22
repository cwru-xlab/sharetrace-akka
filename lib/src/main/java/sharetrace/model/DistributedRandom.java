package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface DistributedRandom {

  @JsonIgnore
  double nextDouble();

  default long nextLong(double bound) {
    return Math.round(nextDouble() * bound);
  }

  @JsonProperty
  String type();
}
