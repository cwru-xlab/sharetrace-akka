package io.sharetrace.experiment.data.factory;

import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@FunctionalInterface
public interface GraphGeneratorFactory {

  GraphGenerator<Integer, DefaultEdge, Integer> graphGenerator(int numNodes);
}
