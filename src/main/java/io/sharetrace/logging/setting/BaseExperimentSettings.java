package io.sharetrace.logging.setting;

import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.CacheParams;
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
