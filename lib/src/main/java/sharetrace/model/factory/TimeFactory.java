package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface TimeFactory {

  @JsonIgnore
  long getTime();
}
