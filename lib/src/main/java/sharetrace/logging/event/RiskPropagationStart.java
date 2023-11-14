package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record RiskPropagationStart(Timestamp timestamp) implements RiskPropagationEvent {}
