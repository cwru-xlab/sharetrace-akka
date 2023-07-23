package sharetrace.model.message;

public record IdleTimeout(int key) implements UserMessage, MonitorMessage {}
