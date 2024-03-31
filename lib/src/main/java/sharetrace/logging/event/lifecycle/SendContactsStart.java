package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SCS")
public record SendContactsStart(long timestamp) implements LifecycleEvent {}
