package io.sharetrace.util.logging.event;

import io.sharetrace.util.logging.Loggable;
import java.time.Instant;

public interface LoggableEvent extends Loggable {

  String KEY = "event";

  String self();

  Instant timestamp();
}
