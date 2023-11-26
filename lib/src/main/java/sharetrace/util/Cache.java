package sharetrace.util;

import java.util.Optional;
import sharetrace.model.Timestamp;

public interface Cache<V> extends Iterable<V> {

  Optional<V> max();

  Optional<V> max(Timestamp atMost);

  Cache<V> refresh();

  void add(V value);
}
