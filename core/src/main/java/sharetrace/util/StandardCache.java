package sharetrace.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import org.immutables.builder.Builder;
import sharetrace.model.Expirable;

@SuppressWarnings("NullableProblems")
public final class StandardCache<K, V extends Expirable> implements Iterable<V> {

  private final Clock clock;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private final Map<K, V> cache;

  private Instant min;

  @Builder.Constructor
  StandardCache(Clock clock, Comparator<? super V> comparator, BinaryOperator<V> merger) {
    this.clock = clock;
    this.comparator = comparator;
    this.merger = merger;
    this.cache = Maps.newHashMap();
    updateMin();
  }

  public Optional<V> max() {
    return values().stream().max(comparator);
  }

  public Optional<V> max(Instant atMost) {
    return values().stream().filter(value -> !value.timestamp().isAfter(atMost)).max(comparator);
  }

  public void put(K key, V value) {
    cache.merge(key, value, merger);
    updateMin(value);
  }

  public StandardCache<K, V> refresh() {
    if (min.isBefore(clock.instant())) {
      values().removeIf(value -> value.isExpired(clock));
      updateMin();
    }
    return this;
  }

  @Override
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(values().iterator());
  }

  private void updateMin(V value) {
    min = min.isBefore(value.expiresAt()) ? min : value.expiresAt();
  }

  private void updateMin() {
    min = values().stream().map(Expirable::expiresAt).min(Instant::compareTo).orElse(Instant.MAX);
  }

  private Collection<V> values() {
    return cache.values();
  }
}
