package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Expirable {

  @JsonIgnore
  long expiryTime();

  default boolean isExpired(long currentTime) {
    return expiryTime() < currentTime;
  }
}
