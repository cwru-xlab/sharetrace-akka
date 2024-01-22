package sharetrace.logging.event.user;

import sharetrace.model.message.RiskScoreMessage;

public record SendEvent(int self, int contact, RiskScoreMessage message, long timestamp)
    implements MessageEvent {}
