package org.sharetrace.model.message;

import org.sharetrace.RiskPropagation;

/** A signal that is sent to an instance of {@link RiskPropagation} to invoke it. */
public enum Run implements RiskPropMessage {
  INSTANCE
}
