package sharetrace.logging.event;

import sharetrace.model.Timestamp;

public record SendRiskScoresEnd(Timestamp timestamp) implements RiskPropagationEvent {}
