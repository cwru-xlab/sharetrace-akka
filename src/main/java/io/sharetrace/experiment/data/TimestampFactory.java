package io.sharetrace.experiment.data;

import java.time.Instant;

@FunctionalInterface
public interface TimestampFactory {

  Instant getTimestamp();
}
