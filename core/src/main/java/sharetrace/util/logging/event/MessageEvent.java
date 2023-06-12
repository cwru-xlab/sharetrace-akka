package sharetrace.util.logging.event;

import sharetrace.model.message.RiskScoreMessage;

interface MessageEvent extends EventRecord {

  RiskScoreMessage message();

  String contact();
}
