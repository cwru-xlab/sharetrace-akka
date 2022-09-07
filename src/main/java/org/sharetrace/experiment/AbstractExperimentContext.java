package org.sharetrace.experiment;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.sharetrace.logging.Loggable;

public interface AbstractExperimentContext {

  Instant refTime();

  Clock clock();

  long seed();

  Set<Class<? extends Loggable>> loggable();
}
