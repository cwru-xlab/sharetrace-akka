package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.InstantSource;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@FunctionalInterface
public interface TimeFactory {

  static TimeFactory from(InstantSource instantSource) {
    return instantSource::millis;
  }

  @JsonIgnore
  long getTime();
}
