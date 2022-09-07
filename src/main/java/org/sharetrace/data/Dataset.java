package org.sharetrace.data;

import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;

public interface Dataset extends RiskScoreFactory {

  ContactNetwork contactNetwork();
}
