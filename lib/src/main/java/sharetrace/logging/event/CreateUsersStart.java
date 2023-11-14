package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record CreateUsersStart(Timestamp timestamp) implements RiskPropagationEvent {}
