package sharetrace.cache;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;
import sharetrace.util.Instants;

public final class StandardCache<K, V extends Expirable & Comparable<? super V>>
    implements Cache<K, V> {

  private final Map<K, V> cache;
  private final InstantSource timeSource;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;

  private Instant min;

  public StandardCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.comparator = Comparator.naturalOrder();
    this.merger = BinaryOperator.maxBy(comparator);
    this.cache = new Object2ObjectOpenHashMap<>();
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
  public void put(K key, V value) {
    cache.merge(key, value, merger);
    updateMin(value);
  }

  @Override
  public StandardCache<K, V> refresh() {
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
    min = Instants.min(min, value.expiresAt());
  }

  private void updateMin() {
    min = values().stream().map(Expirable::expiresAt).min(Instant::compareTo).orElse(Instant.MAX);
  }

  private Collection<V> values() {
    return cache.values();
  }
}
