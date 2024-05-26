package sharetrace.algorithm;

import java.util.Collection;
import java.util.Iterator;
import sharetrace.model.Expirable;
import sharetrace.model.factory.TimeFactory;

abstract class ExpirableStore<V extends Expirable> implements Iterable<V> {

  private final TimeFactory timeFactory;

  private long minExpiryTime;

  public ExpirableStore(TimeFactory timeFactory) {
    this.timeFactory = timeFactory;
    this.minExpiryTime = Long.MAX_VALUE;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<V> iterator() {
    return values().iterator();
  }

  public void add(V value) {
    minExpiryTime = Math.min(minExpiryTime, value.expiryTime());
  }

  public void refresh() {
    var currentTime = timeFactory.getTime();
    if (minExpiryTime < currentTime) {
      values().removeIf(value -> value.isExpired(currentTime));
      minExpiryTime =
          values().stream().mapToLong(Expirable::expiryTime).min().orElse(Long.MAX_VALUE);
    }
  }

  protected abstract Collection<V> values();
}
