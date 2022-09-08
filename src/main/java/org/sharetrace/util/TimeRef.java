package org.sharetrace.util;

import java.time.Instant;

@FunctionalInterface
public interface TimeRef {

  Instant refTime();
}
