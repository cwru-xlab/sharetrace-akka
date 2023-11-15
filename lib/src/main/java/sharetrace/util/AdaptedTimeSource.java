package sharetrace.util;

import java.time.InstantSource;
import sharetrace.model.Timestamp;

record AdaptedTimeSource(InstantSource delegate) implements TimeSource {

  @Override
  public Timestamp timestamp() {
    return Timestamp.from(delegate.instant());
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
