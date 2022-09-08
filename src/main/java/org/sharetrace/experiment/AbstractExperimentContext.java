package org.sharetrace.experiment;

import java.time.Clock;
import org.sharetrace.util.LoggableRef;
import org.sharetrace.util.TimeRef;

public interface AbstractExperimentContext extends TimeRef, LoggableRef {

  Clock clock();

  long seed();
}
