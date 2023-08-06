package sharetrace.logging.event;

import sharetrace.model.message.RiskScoreMessage;

public interface MessageEvent extends UserEvent {

  RiskScoreMessage message();

  int contact();
}
