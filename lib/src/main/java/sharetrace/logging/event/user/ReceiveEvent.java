package sharetrace.logging.event.user;

import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(int self, int contact, RiskScoreMessage message, long timestamp)
    implements MessageEvent {

  public ReceiveEvent(int self, RiskScoreMessage message, long timestamp) {
    this(self, message.sender(), message, timestamp);
  }
}
