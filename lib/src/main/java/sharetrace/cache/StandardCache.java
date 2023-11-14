package sharetrace.cache;

import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;
import sharetrace.model.Timestamp;
import sharetrace.model.Timestamped;
import sharetrace.util.TimeSource;

public final class StandardCache<V extends Expirable & Timestamped & Comparable<? super V>>
    extends Cache<V> {

  private final Map<V, V> cache;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private Timestamp min;

  public StandardCache(TimeSource timeSource) {
    super(timeSource);
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
  public Optional<V> max(Timestamp atMost) {
    return values().stream().filter(value -> !value.timestamp().after(atMost)).max(comparator);
  }

  @Override
  public void add(V value) {
    cache.merge(value, value, merger);
    updateMin(value);
  }

  @Override
  public StandardCache<V> refresh() {
    var currentTime = timeSource.timestamp();
    if (min.before(currentTime)) {
      values().removeIf(value -> value.isExpired(currentTime));
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
    min = Timestamp.min(min, value.expiryTime());
  }

  private void updateMin() {
    min =
        values().stream()
            .map(Expirable::expiryTime)
            .min(Timestamp::compareTo)
            .orElse(Timestamp.MAX);
  }

  private Collection<V> values() {
    return cache.values();
  }
}
