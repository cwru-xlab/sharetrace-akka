package io.sharetrace.logging.setting;

import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.model.CacheParams;
import io.sharetrace.model.MsgParams;
import io.sharetrace.model.UserParams;
import org.immutables.value.Value;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParams userParams();

  MsgParams msgParams();

  CacheParams<RiskScoreMsg> cacheParams();

  long seed();

  String stateId();

  String graphType();
}
