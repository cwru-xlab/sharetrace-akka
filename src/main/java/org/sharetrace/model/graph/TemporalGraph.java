package org.sharetrace.model.graph;

import java.util.stream.Stream;

public interface TemporalGraph<N, E> {

  Stream<E> edgeStream();

  Stream<N> nodeStream();

  long nNodes();
}
