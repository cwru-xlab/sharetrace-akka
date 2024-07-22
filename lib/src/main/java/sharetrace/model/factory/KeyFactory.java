package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface KeyFactory {

  @JsonIgnore
  String getKey();

  @JsonProperty
  String type();
}
