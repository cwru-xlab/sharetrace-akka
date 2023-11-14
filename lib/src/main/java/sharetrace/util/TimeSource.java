package sharetrace.util;

import java.time.InstantSource;
import sharetrace.model.Timestamp;

public interface TimeSource extends InstantSource {

  static TimeSource from(InstantSource instantSource) {
    return instantSource::instant;
  }

  default Timestamp timestamp() {
    return Timestamp.ofEpochMillis(millis());
  }
}
