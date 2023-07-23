package sharetrace.cache;

import java.time.Instant;
import java.util.Optional;

public interface Cache<K, V> extends Iterable<V> {

  Optional<V> max();

  Optional<V> max(Instant atMost);

  Cache<K, V> refresh();

  void put(K key, V value);

  default void add(V value) {
    put(null, value);
  }
}
