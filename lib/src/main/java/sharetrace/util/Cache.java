package sharetrace.util;

import java.util.Optional;

public interface Cache<V> extends Iterable<V> {

  Optional<V> max();

  Optional<V> max(long atMost);

  Cache<V> refresh();

  void add(V value);
}
