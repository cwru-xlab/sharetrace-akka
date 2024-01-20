package sharetrace.logging.event.user;

import sharetrace.model.message.RiskScoreMessage;

public interface MessageEvent extends UserEvent {

  RiskScoreMessage message();

  int contact();
}
