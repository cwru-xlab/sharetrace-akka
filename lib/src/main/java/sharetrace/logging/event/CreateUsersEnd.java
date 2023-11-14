package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record CreateUsersEnd(Timestamp timestamp) implements RiskPropagationEvent {}
