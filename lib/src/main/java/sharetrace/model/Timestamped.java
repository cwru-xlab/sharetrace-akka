package sharetrace.model;

import java.time.Instant;

public interface Timestamped {

  Instant MIN_TIME = Instant.EPOCH;

  Instant timestamp();
}
