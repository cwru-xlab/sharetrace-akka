package sharetrace.cache;

import java.util.Optional;
import sharetrace.model.Timestamp;
import sharetrace.util.TimeSource;

public abstract class Cache<V> implements Iterable<V> {

  protected final TimeSource timeSource;

  protected Cache(TimeSource timeSource) {
    this.timeSource = timeSource;
  }

  public abstract Optional<V> max();

  public abstract Optional<V> max(Timestamp atMost);

  public abstract Cache<V> refresh();

  public abstract void add(V value);
}
