package org.sharetrace.model.graph;

import java.util.List;
import java.util.stream.Stream;

public interface TemporalGraph<T> {

  Stream<T> nodes();

  Stream<List<T>> edges();

  long nNodes();

  long nEdges();
}
