package io.sharetrace.util.logging.setting;

import io.sharetrace.model.UserParams;
import io.sharetrace.model.message.RiskScoreMsg;
import io.sharetrace.util.cache.CacheParams;
import org.immutables.value.Value;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParams userParams();

  CacheParams<RiskScoreMsg> cacheParams();

  long seed();

  String stateId();

  String networkId();

  String graphType();

  String datasetId();
}
