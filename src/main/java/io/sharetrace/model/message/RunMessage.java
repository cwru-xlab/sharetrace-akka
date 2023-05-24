package io.sharetrace.model.message;

import io.sharetrace.actor.RiskPropagation;

/**
 * A message that invokes instance of an algorithm.
 *
 * @see RiskPropagation
 */
public enum RunMessage implements AlgorithmMessage {
  INSTANCE
}
