package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Comparator;

public interface Expirable extends Timestamped {

  Comparator<Expirable> COMPARATOR =
      Comparator.comparingLong(Expirable::timestamp).thenComparingLong(Expirable::expiryTime);

  static int compare(Expirable left, Expirable right) {
    return COMPARATOR.compare(left, right);
  }

  @JsonIgnore
  long expiryTime();

  default boolean isAlive(long currentTime) {
    return !isExpired(currentTime);
  }

  default boolean isExpired(long currentTime) {
    return expiryTime() < currentTime;
  }
}
