package org.sharetrace.util;

import org.immutables.value.Value;

import java.time.Duration;

@Value.Immutable
interface BaseCacheParameters {

  long nIntervals();

  long nLookAhead();

  Duration interval();

  Duration refreshRate();

  boolean prioritizeReads();
}
