package io.sharetrace.util.logging.event;

import io.sharetrace.model.message.RiskScoreMsg;

interface MessageEvent extends LoggableEvent {

  RiskScoreMsg message();
}
