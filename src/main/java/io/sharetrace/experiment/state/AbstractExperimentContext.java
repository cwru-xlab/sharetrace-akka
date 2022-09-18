package io.sharetrace.experiment.state;

import io.sharetrace.model.LoggableRef;
import io.sharetrace.model.TimeRef;
import java.time.Clock;

interface AbstractExperimentContext extends TimeRef, LoggableRef {

  Clock clock();

  long seed();
}
