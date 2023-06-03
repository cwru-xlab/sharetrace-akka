package io.sharetrace.util.logging.event;

import io.sharetrace.model.message.RiskScoreMessage;

interface ChangeEvent extends EventRecord {

  RiskScoreMessage previous();

  RiskScoreMessage current();
}
