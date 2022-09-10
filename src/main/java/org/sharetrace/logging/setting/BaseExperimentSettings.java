package org.sharetrace.logging.setting;

import org.immutables.value.Value;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.model.CacheParams;
import org.sharetrace.model.MsgParams;
import org.sharetrace.model.UserParams;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParams userParams();

  MsgParams msgParams();

  CacheParams<RiskScoreMsg> cacheParams();

  long seed();

  String stateId();

  String graphType();
}
