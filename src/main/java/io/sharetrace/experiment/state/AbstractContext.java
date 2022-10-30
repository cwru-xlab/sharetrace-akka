package io.sharetrace.experiment.state;

import io.sharetrace.logging.Loggable;
import io.sharetrace.model.TimeRef;
import java.time.Clock;
import java.util.Set;

interface AbstractContext extends TimeRef {

  Clock clock();

  long seed();

  Set<Class<? extends Loggable>> loggable();
}
