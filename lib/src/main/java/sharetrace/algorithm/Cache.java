package sharetrace.algorithm;

import com.google.common.collect.Iterators;
import java.time.InstantSource;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;

abstract class Cache<V extends Expirable & Comparable<? super V>> extends AbstractCollection<V> {

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

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(values().iterator());
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

  public Cache<V> refresh() {
    var currentTime = timeSource.millis();
    if (minExpiryTime < currentTime) {
      removeIf(value -> value.isExpired(currentTime));
      minExpiryTime = stream().map(Expirable::expiryTime).min(Long::compare).orElse(Long.MAX_VALUE);
    }
    return this;
  }

  protected abstract Collection<V> values();
}
