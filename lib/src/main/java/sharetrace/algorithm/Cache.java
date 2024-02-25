package sharetrace.algorithm;

import java.time.InstantSource;
import java.util.Collection;
import java.util.Iterator;
import sharetrace.model.Expirable;

abstract class Cache<V extends Expirable & Comparable<? super V>> implements Iterable<V> {

  private final InstantSource timeSource;

  private long minExpiryTime;

  public Cache(InstantSource timeSource) {
    this.timeSource = timeSource;
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
    var currentTime = timeSource.millis();
    if (minExpiryTime < currentTime) {
      values().removeIf(value -> value.isExpired(currentTime));
      minExpiryTime =
          values().stream().map(Expirable::expiryTime).min(Long::compare).orElse(Long.MAX_VALUE);
    }
  }

  protected abstract Collection<V> values();
}
