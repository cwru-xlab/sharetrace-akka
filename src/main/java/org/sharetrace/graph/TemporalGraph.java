package org.sharetrace.graph;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface TemporalGraph {

  IntStream nodes();

  Stream<List<Integer>> edges();

  long nNodes();

  long nEdges();
}
