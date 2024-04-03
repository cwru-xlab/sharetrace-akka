package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Comparator;
import sharetrace.model.factory.TimeFactory;

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

  default boolean isExpired(TimeFactory timeFactory) {
    return isExpired(timeFactory.getTime());
  }
}
