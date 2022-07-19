package org.sharetrace.data.factory;

import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@FunctionalInterface
public interface GraphGeneratorFactory {

  GraphGenerator<Integer, DefaultEdge, ?> getGraphGenerator(int nNodes);
}
