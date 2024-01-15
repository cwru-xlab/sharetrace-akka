package sharetrace.logging.event;

public record SendRiskScoresEnd(long timestamp) implements RiskPropagationEvent {}
