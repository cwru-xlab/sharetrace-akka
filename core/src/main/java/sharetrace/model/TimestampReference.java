package sharetrace.model;

import java.time.Instant;

@FunctionalInterface
public interface TimestampReference {

  Instant referenceTimestamp();
}
