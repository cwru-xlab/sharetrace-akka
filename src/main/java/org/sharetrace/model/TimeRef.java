package org.sharetrace.model;

import java.time.Instant;

@FunctionalInterface
public interface TimeRef {

  Instant refTime();
}
