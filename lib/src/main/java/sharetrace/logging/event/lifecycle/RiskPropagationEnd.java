package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("RPE")
public record RiskPropagationEnd(long timestamp) implements LifecycleEvent {}
