package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SSE")
public record SendRiskScoresEnd(long timestamp) implements LifecycleEvent {}
