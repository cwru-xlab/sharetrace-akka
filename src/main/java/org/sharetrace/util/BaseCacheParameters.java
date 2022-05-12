package org.sharetrace.util;

import java.time.Duration;
import org.immutables.value.Value;

@Value.Immutable
interface BaseCacheParameters {

  long nIntervals();

  long nLookAhead();

  Duration interval();

  Duration refreshRate();

  boolean prioritizeReads();
}
