package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record RiskPropagationEnd(Timestamp timestamp) implements RiskPropagationEvent {}
