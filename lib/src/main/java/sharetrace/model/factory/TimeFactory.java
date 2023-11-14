package sharetrace.model.factory;

import sharetrace.model.Timestamp;

@FunctionalInterface
public interface TimeFactory {

  Timestamp getTime();
}
