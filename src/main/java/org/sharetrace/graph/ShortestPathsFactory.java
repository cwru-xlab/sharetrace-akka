package org.sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

@FunctionalInterface
public interface ShortestPathsFactory<V, E> {

  ShortestPathAlgorithm<V, E> newShortestPaths(Graph<V, E> graph);
}
