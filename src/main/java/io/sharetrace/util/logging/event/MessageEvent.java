package io.sharetrace.util.logging.event;

import io.sharetrace.model.message.RiskScoreMessage;

interface MessageEvent extends EventRecord {

  RiskScoreMessage message();

  String contact();
}
