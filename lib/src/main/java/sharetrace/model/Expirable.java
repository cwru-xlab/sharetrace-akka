package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.InstantSource;
import java.util.Comparator;

public interface Expirable extends Timestamped {

  Comparator<Expirable> COMPARATOR =
      Comparator.comparingLong(Expirable::timestamp).thenComparingLong(Expirable::expiryTime);

  static int compare(Expirable left, Expirable right) {
    return COMPARATOR.compare(left, right);
  }

  @JsonIgnore
  long expiryTime();

  default boolean isExpired(long currentTime) {
    return expiryTime() < currentTime;
  }

  default boolean isExpired(InstantSource timeSource) {
    return isExpired(timeSource.millis());
  }
}
