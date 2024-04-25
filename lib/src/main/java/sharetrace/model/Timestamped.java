package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Timestamped {

  @JsonProperty("t")
  long timestamp();
}
