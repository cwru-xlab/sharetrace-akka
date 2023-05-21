package io.sharetrace.experiment.state;

import io.sharetrace.model.TimeRef;
import io.sharetrace.util.logging.Loggable;
import java.time.Clock;
import java.util.Set;

interface AbstractContext extends TimeRef {

  Clock clock();

  long seed();

  Set<Class<? extends Loggable>> loggable();
}
