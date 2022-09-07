package org.sharetrace.logging.settings;

import org.immutables.value.Value;
import org.sharetrace.message.MsgParams;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.message.UserParams;
import org.sharetrace.util.CacheParams;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParams userParams();

  MsgParams msgParams();

  CacheParams<RiskScoreMsg> cacheParams();

  long seed();

  String stateId();

  String graphType();
}
