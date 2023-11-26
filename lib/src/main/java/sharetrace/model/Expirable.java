package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Comparator;

public interface Expirable extends Timestamped {

  Comparator<Expirable> COMPARATOR =
      Comparator.comparing(Expirable::timestamp).thenComparing(Expirable::expiryTime);

  static int compare(Expirable left, Expirable right) {
    return COMPARATOR.compare(left, right);
  }

  @JsonIgnore
  Timestamp expiryTime();

  default boolean isAlive(Timestamp currentTime) {

    return !isExpired(currentTime);
  }

  default boolean isExpired(Timestamp currentTime) {
    return expiryTime().isBefore(currentTime);
  }
}
