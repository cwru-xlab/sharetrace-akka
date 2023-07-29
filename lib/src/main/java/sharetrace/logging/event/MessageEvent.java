package sharetrace.logging.event;

import sharetrace.model.message.RiskScoreMessage;

public interface MessageEvent extends EventRecord {

  RiskScoreMessage message();

  String contact();
}
