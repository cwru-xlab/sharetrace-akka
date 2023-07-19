package sharetrace.model;

import java.time.Instant;

@FunctionalInterface
public interface Timestamped {

  Instant timestamp();
}
