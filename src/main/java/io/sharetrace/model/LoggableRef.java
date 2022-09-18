package io.sharetrace.model;

import io.sharetrace.logging.Loggable;
import java.util.Set;

@FunctionalInterface
public interface LoggableRef {

  Set<Class<? extends Loggable>> loggable();
}
