package org.sharetrace.data;

import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.util.LoggableRef;

public interface Dataset extends RiskScoreFactory, LoggableRef {

  ContactNetwork contactNetwork();
}
