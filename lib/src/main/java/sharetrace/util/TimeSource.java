package sharetrace.util;

import java.time.InstantSource;
import sharetrace.model.Timestamp;

@FunctionalInterface
public interface TimeSource {

  static TimeSource from(InstantSource instantSource) {
    return new AdaptedTimeSource(instantSource);
  }

  Timestamp timestamp();
}
