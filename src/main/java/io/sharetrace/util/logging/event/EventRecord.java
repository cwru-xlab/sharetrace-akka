package io.sharetrace.util.logging.event;

import io.sharetrace.util.logging.LogRecord;
import java.time.Instant;

public interface EventRecord extends LogRecord {

  String KEY = "event";

  String self();

  Instant timestamp();
}
