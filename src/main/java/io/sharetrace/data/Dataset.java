package io.sharetrace.data;

import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.LoggableRef;

public interface Dataset extends RiskScoreFactory, ContactNetwork, LoggableRef {

  Dataset withScoreFactory(RiskScoreFactory scoreFactory);

  Dataset withNewContactNetwork();
}
