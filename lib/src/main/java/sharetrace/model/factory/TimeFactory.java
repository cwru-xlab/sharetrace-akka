package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@FunctionalInterface
public interface TimeFactory {

  static TimeFactory system() {
    return System::currentTimeMillis;
  }

  static TimeFactory fixed(Instant instant) {
    var value = instant.toEpochMilli();
    return () -> value;
  }

  @JsonIgnore
  long getTime();
}
