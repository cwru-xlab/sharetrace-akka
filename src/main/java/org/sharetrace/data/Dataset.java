package org.sharetrace.data;

import org.sharetrace.graph.TemporalGraph;

public interface Dataset extends ScoreFactory, ContactTimeFactory {

  TemporalGraph graph();
}
