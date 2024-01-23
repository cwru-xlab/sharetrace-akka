package sharetrace.model.message;

public record IdleTimeoutMessage(int id) implements UserMessage, MonitorMessage {}
