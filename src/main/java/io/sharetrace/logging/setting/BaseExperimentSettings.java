package io.sharetrace.logging.setting;

import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.model.MsgParams;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.CacheParams;
import org.immutables.value.Value;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParams userParams();

  MsgParams msgParams();

  CacheParams<RiskScoreMsg> cacheParams();

  long seed();

  String sid();

  String graphType();
}
