package org.sharetrace.logging.settings;

import org.immutables.value.Value;
import org.sharetrace.message.MessageParameters;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;

@Value.Immutable
interface BaseExperimentSettings extends LoggableSetting {

  UserParameters userParameters();

  MessageParameters messageParameters();

  CacheParameters<RiskScoreMessage> cacheParameters();

  long seed();

  String stateId();

  String graphType();
}
