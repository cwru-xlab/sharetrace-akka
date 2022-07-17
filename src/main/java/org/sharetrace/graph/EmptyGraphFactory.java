package org.sharetrace.graph;

import org.jgrapht.Graph;

@FunctionalInterface
public interface EmptyGraphFactory<V, E> {

  Graph<V, E> newEmptyGraph();
}
