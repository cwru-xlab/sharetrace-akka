package io.sharetrace.data;

import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.LoggableRef;

public interface Dataset extends Identifiable, LoggableRef {

  Dataset withScoreFactory(RiskScoreFactory scoreFactory);

  Dataset withNewContactNetwork();

  RiskScoreFactory scoreFactory();

  ContactNetwork contactNetwork();
}
