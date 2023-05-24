package io.sharetrace.util.logging.event;

import io.sharetrace.model.message.RiskScoreMessage;

interface ChangeEvent extends LoggableEvent {

  RiskScoreMessage previous();

  RiskScoreMessage current();
}
