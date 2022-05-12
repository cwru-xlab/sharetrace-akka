package org.sharetrace.logging.settings;

import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.message.NodeParameters;
import org.sharetrace.util.CacheParameters;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  NodeParameters nodeParameters();

  CacheParameters cacheParameters();

  long seed();

  int nIterations();

  int iteration();

  GraphType graphType();
}
