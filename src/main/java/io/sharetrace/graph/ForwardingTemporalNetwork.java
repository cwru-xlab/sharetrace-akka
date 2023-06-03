package io.sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.GraphDelegator;

final class ForwardingTemporalNetwork<V> extends GraphDelegator<V, TemporalEdge>
    implements TemporalNetwork<V> {

  private final String id;
  private final String type;

  public ForwardingTemporalNetwork(Graph<V, TemporalEdge> delegate, String id, String type) {
    super(delegate);
    this.id = id;
    this.type = type;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public String type() {
    return type;
  }
}
