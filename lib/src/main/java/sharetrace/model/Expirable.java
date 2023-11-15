package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Expirable {

  @JsonIgnore
  Timestamp expiryTime();

  default boolean isAlive(Timestamp currentTime) {
    return !isExpired(currentTime);
  }

  default boolean isExpired(Timestamp currentTime) {
    return expiryTime().isBefore(currentTime);
  }
}
