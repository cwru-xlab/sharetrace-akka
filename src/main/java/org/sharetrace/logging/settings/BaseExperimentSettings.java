package org.sharetrace.logging.settings;

import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParameters userParameters();

  CacheParameters cacheParameters();

  long seed();

  int iteration();

  GraphType graphType();
}
