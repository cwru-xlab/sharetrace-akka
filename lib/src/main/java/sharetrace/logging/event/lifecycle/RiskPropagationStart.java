package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("RPS")
public record RiskPropagationStart(long timestamp) implements LifecycleEvent {}
