package io.sharetrace.util.logging.event;

import io.sharetrace.model.message.RiskScoreMessage;

interface MessageEvent extends LoggableEvent {

  RiskScoreMessage message();

  String contact();
}
