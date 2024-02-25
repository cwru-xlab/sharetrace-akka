package sharetrace.algorithm;

import java.time.InstantSource;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import sharetrace.model.Expirable;

abstract class Cache<V extends Expirable & Comparable<? super V>> extends AbstractCollection<V> {

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

  @Override
  public int size() {
    return values().size();
  }

  @Override
  public boolean add(V value) {
    minExpiryTime = Math.min(minExpiryTime, value.expiryTime());
    return true;
  }

  public void refresh() {
    var currentTime = timeSource.millis();
    if (minExpiryTime < currentTime) {
      removeIf(value -> value.isExpired(currentTime));
      minExpiryTime = stream().map(Expirable::expiryTime).min(Long::compare).orElse(Long.MAX_VALUE);
    }
  }

  protected abstract Collection<V> values();
}
