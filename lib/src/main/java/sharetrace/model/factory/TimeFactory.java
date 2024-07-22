package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface TimeFactory {

  @JsonIgnore
  long getTime();

  @JsonProperty
  String type();
}
