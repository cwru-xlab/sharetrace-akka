package org.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.util.function.BinaryOperator;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
interface BaseCacheParams<T> {

  int numIntervals();

  int numLookAhead();

  Duration interval();

  Duration refreshPeriod();

  @JsonIgnore
  BinaryOperator<T> mergeStrategy();
}
