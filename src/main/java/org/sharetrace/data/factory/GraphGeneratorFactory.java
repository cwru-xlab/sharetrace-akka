package org.sharetrace.data.factory;

import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.graph.Edge;

@FunctionalInterface
public interface GraphGeneratorFactory {

  GraphGenerator<Integer, Edge<Integer>, ?> getGraphGenerator(int nNodes);
}
