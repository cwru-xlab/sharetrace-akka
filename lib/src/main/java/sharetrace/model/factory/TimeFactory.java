package sharetrace.model.factory;

import java.time.Instant;

@FunctionalInterface
public interface TimeFactory {

  Instant getTime();
}
