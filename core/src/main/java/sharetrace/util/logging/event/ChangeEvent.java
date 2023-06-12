package sharetrace.util.logging.event;

import sharetrace.model.message.RiskScoreMessage;

interface ChangeEvent extends EventRecord {

  RiskScoreMessage previous();

  RiskScoreMessage current();
}
