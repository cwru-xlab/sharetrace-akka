package org.sharetrace.message;

import org.sharetrace.actors.RiskPropagation;

/**
 * A message that invokes instance of an algorithm.
 *
 * @see RiskPropagation
 */
public enum RunMsg implements AlgorithmMsg {
  INSTANCE
}
