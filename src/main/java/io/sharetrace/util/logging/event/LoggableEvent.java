package io.sharetrace.util.logging.event;

import io.sharetrace.util.logging.Loggable;
import org.immutables.value.Value;

public interface LoggableEvent extends Loggable {

    String KEY = "event";

    @Value.Default
    default long timestamp() {
        return System.currentTimeMillis();
    }
}
