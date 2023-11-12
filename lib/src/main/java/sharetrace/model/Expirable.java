package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public interface Expirable {

  @JsonIgnore
  Instant expiryTime();

  default boolean isAlive(Instant currentTime) {
    return !isExpired(currentTime);
  }

  default boolean isExpired(Instant currentTime) {
    return expiryTime().isBefore(currentTime);
  }
}
