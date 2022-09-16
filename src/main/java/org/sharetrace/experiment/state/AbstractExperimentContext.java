package org.sharetrace.experiment.state;

import java.time.Clock;
import org.sharetrace.model.LoggableRef;
import org.sharetrace.model.TimeRef;

interface AbstractExperimentContext extends TimeRef, LoggableRef {

  Clock clock();

  long seed();
}
