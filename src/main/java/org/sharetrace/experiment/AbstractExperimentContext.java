package org.sharetrace.experiment;

import java.time.Clock;
import org.sharetrace.model.LoggableRef;
import org.sharetrace.model.TimeRef;

public interface AbstractExperimentContext extends TimeRef, LoggableRef {

  Clock clock();

  long seed();
}
