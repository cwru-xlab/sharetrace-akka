package sharetrace.cache;

import com.google.common.collect.Iterators;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;
import sharetrace.model.Timestamped;
import sharetrace.util.Instants;

public final class StandardCache<V extends Expirable & Timestamped & Comparable<? super V>>
    implements Cache<V> {

  private final Map<V, V> cache;
  private final InstantSource timeSource;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private Instant min;

  public StandardCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.comparator = Comparator.naturalOrder();
    this.merger = BinaryOperator.maxBy(comparator);
    this.cache = new HashMap<>();
    updateMin();
  }

  @Override
  public Optional<V> max() {
    return values().stream().max(comparator);
  }

  @Override
  public Optional<V> max(Instant atMost) {
    return values().stream().filter(value -> !value.timestamp().isAfter(atMost)).max(comparator);
  }

  @Override
  public void add(V value) {
    cache.merge(value, value, merger);
    updateMin(value);
  }

  @Override
  public StandardCache<V> refresh() {
    if (min.isBefore(timeSource.instant())) {
      values().removeIf(value -> value.isExpired(timeSource));
      updateMin();
    }
    return this;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(values().iterator());
  }

  private void updateMin(V value) {
    min = Instants.min(min, value.expiryTime());
  }

  private void updateMin() {
    min = values().stream().map(Expirable::expiryTime).min(Instant::compareTo).orElse(Instant.MAX);
  }

  private Collection<V> values() {
    return cache.values();
  }
}
