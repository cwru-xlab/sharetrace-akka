package io.sharetrace.graph;

import io.sharetrace.model.Identifiable;
import org.jgrapht.Graph;

public interface TemporalNetwork<V> extends Graph<V, TemporalEdge>, Identifiable {

  String type();
}
