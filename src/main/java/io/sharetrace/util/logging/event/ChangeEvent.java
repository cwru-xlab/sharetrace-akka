package io.sharetrace.util.logging.event;

import io.sharetrace.model.message.RiskScoreMsg;

interface ChangeEvent extends LoggableEvent {

  RiskScoreMsg previous();

  RiskScoreMsg current();
}
