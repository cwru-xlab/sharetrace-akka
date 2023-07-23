package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.InstantSource;

public interface Expirable extends Timestamped {

  @JsonIgnore
  Instant expiresAt();

  default boolean isAlive(InstantSource source) {
    return !isExpired(source);
  }

  default boolean isExpired(InstantSource source) {
    return expiresAt().isBefore(source.instant());
  }
}
