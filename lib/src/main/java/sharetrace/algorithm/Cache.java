package sharetrace.algorithm;

import java.util.Optional;

interface Cache<V> extends Iterable<V> {

  Optional<V> max();

  Optional<V> max(long atMost);

  Cache<V> refresh();

  void add(V value);
}
