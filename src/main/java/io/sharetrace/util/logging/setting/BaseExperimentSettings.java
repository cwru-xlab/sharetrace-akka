package io.sharetrace.util.logging.setting;

import io.sharetrace.model.UserParameters;
import io.sharetrace.util.cache.CacheParameters;
import org.immutables.value.Value;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParameters userParameters();

  CacheParameters<?> cacheParameters();

  long seed();

  String stateId();

  String networkId();

  String datasetId();

  String graphType();
}
