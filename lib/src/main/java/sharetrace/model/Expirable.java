package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import sharetrace.model.factory.TimeFactory;

public interface Expirable {

  @JsonIgnore
  long expiryTime();

  default boolean isExpired(long currentTime) {
    return expiryTime() < currentTime;
  }

  default boolean isExpired(TimeFactory timeFactory) {
    return isExpired(timeFactory.getTime());
  }
}
