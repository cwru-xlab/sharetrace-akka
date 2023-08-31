package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.InstantSource;

public interface Expirable {

  @JsonIgnore
  Instant expiresAt();

  default boolean isAlive(InstantSource timeSource) {
    return !isExpired(timeSource);
  }

  default boolean isExpired(InstantSource timeSource) {
    return expiresAt().isBefore(timeSource.instant());
  }
}
