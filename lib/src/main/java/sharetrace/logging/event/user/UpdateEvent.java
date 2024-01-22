package sharetrace.logging.event.user;

import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    int self, RiskScoreMessage previous, RiskScoreMessage current, long timestamp)
    implements UserEvent {}
