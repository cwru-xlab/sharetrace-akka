package io.sharetrace.model.message;

import io.sharetrace.actor.RiskPropagation;

/**
 * A message that invokes instance of an algorithm.
 *
 * @see RiskPropagation
 */
public enum RunMsg implements AlgorithmMsg {
  INSTANCE
}
