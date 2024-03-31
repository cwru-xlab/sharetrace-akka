package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SSS")
public record SendRiskScoresStart(long timestamp) implements LifecycleEvent {}
