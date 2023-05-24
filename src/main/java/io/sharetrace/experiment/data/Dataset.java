package io.sharetrace.experiment.data;

import io.sharetrace.experiment.data.factory.ScoreFactory;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.model.Identifiable;

public interface Dataset extends Identifiable {

  Dataset withScoreFactory(ScoreFactory scoreFactory);

  Dataset withNewContactNetwork();

  ScoreFactory scoreFactory();

  ContactNetwork contactNetwork();
}
