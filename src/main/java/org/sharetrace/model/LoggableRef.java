package org.sharetrace.model;

import java.util.Set;
import org.sharetrace.logging.Loggable;

@FunctionalInterface
public interface LoggableRef {

  Set<Class<? extends Loggable>> loggable();
}
