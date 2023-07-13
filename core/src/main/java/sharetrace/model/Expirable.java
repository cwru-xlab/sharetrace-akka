package sharetrace.model;

import java.time.Clock;
import java.time.Duration;

public interface Expirable extends Timestamped {

  Duration expiry();

  default boolean isAlive(Clock clock) {
    return !isExpired(clock);
  }

  default boolean isExpired(Clock clock) {
    Duration elapsed = Duration.between(timestamp(), clock.instant());
    return elapsed.compareTo(expiry()) > 0;
  }
}
