package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.immutables.value.Value;

public interface Expirable extends Timestamped {

  Duration expiry();

  @JsonIgnore
  @Value.Default
  default Instant expiresAt() {
    return timestamp().plus(expiry());
  }

  default boolean isAlive(Clock clock) {
    return !isExpired(clock);
  }

  default boolean isExpired(Clock clock) {
    return expiresAt().isBefore(clock.instant());
  }
}
