package sharetrace.logging.event;

public record SendRiskScoresStart(long timestamp) implements RiskPropagationEvent {}
