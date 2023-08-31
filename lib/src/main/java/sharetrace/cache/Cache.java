package sharetrace.cache;

import java.time.Instant;
import java.util.Optional;

public interface Cache<V> extends Iterable<V> {

  Optional<V> max();

  Optional<V> max(Instant atMost);

  Cache<V> refresh();

  void add(V value);
}
