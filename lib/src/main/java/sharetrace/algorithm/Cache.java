package sharetrace.algorithm;

import com.google.common.collect.Iterators;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;

abstract class Cache<V extends Expirable & Comparable<? super V>> implements Iterable<V> {

  protected final Comparator<V> comparator;
  protected final BinaryOperator<V> merger;
  protected final InstantSource timeSource;

  private long minExpiryTime;

  public Cache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.comparator = Comparator.naturalOrder();
    this.merger = BinaryOperator.maxBy(comparator);
    this.minExpiryTime = Long.MAX_VALUE;
  }

  public void add(V value) {
    doAdd(value);
    updateMinExpiryTime(value);
  }

  public Cache<V> refresh() {
    var currentTime = timeSource.millis();
    if (minExpiryTime < currentTime) {
      values().removeIf(value -> value.isExpired(currentTime));
      updateMinExpiryTime();
    }
    return this;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(values().iterator());
  }

  protected abstract void doAdd(V value);

  protected abstract Collection<V> values();

  private void updateMinExpiryTime(V value) {
    minExpiryTime = Math.min(minExpiryTime, value.expiryTime());
  }

  private void updateMinExpiryTime() {
    minExpiryTime =
        values().stream().map(Expirable::expiryTime).min(Long::compareTo).orElse(Long.MAX_VALUE);
  }
}
