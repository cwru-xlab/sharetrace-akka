package org.sharetrace.data;

import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;

public interface Dataset extends RiskScoreFactory, ContactTimeFactory {

  ContactNetwork contactNetwork();
}
