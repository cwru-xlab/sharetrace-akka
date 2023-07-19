package sharetrace.model;

import java.time.Clock;
import java.time.Duration;

public interface Expirable extends Timestamped {

  Duration expiry();

  default boolean isAlive(Clock clock) {
    return !isExpired(clock);
  }

  default boolean isExpired(Clock clock) {
    return Duration.between(timestamp(), clock.instant()).compareTo(expiry()) > 0;
  }
}
