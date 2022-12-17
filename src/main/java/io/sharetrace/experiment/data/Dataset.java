package io.sharetrace.experiment.data;

import io.sharetrace.experiment.data.factory.RiskScoreFactory;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.Identifiable;

public interface Dataset extends Identifiable {

    Dataset withScoreFactory(RiskScoreFactory scoreFactory);

    Dataset withNewContactNetwork();

    RiskScoreFactory scoreFactory();

    ContactNetwork contactNetwork();
}
