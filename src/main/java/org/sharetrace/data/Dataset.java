package org.sharetrace.data;

import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.model.LoggableRef;

public interface Dataset extends RiskScoreFactory, ContactNetwork, LoggableRef {

  Dataset withScoreFactory(RiskScoreFactory scoreFactory);
}
