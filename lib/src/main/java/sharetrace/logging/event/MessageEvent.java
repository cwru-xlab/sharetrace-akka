package sharetrace.logging.event;

import sharetrace.model.message.RiskScoreMessage;

public interface MessageEvent extends Event {

  RiskScoreMessage message();

  int contact();
}
