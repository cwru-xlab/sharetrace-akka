package sharetrace.logging.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SCE")
public record SendContactsEnd(long timestamp) implements LifecycleEvent {}
