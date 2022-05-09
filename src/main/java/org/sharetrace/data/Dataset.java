package org.sharetrace.data;

import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.ScoreFactory;
import org.sharetrace.graph.TemporalGraph;

public interface Dataset extends ScoreFactory, ContactTimeFactory {

  TemporalGraph graph();
}
