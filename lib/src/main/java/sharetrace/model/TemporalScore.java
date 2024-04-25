package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface TemporalScore extends Expirable, Timestamped {

  @JsonProperty("v")
  double value();
}
