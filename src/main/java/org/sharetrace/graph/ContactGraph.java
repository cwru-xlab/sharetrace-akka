package org.sharetrace.graph;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.sharetrace.RiskPropagation;

/**
 * A simple graph in which a node represents a person and an edge between two nodes indicates that
 * the associated persons of the incident nodes came in contact. Nodes identifiers are zero-based
 * contiguous natural numbers. In an instance of {@link RiskPropagation}, the topology of this graph
 * is mapped to a collection {@link Node} actors.
 *
 * @see Node
 * @see Edge
 */
public class ContactGraph implements TemporalGraph<Integer> {

  private final Graph<Integer, Edge<Integer>> graph;

  private ContactGraph(Graph<Integer, Edge<Integer>> graph) {
    this.graph = graph;
  }

  public static ContactGraph create(GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    Graph<Integer, Edge<Integer>> graph = newGraph();
    generator.generateGraph(graph);
    return new ContactGraph(graph);
  }

  private static Graph<Integer, Edge<Integer>> newGraph() {
    return new FastutilMapGraph<>(new NodeIdFactory(), Edge::new, DefaultGraphType.simple());
  }

  @Override
  public Stream<Integer> nodes() {
    return graph.vertexSet().stream();
  }

  @Override
  public Stream<List<Integer>> edges() {
    return graph.edgeSet().stream().map(edge -> List.of(edge.source(), edge.target()));
  }

  @Override
  public long nNodes() {
    return graph.iterables().vertexCount();
  }

  @Override
  public long nEdges() {
    return graph.iterables().edgeCount();
  }

  private static final class NodeIdFactory implements Supplier<Integer> {

    private int id = 0;

    @Override
    public Integer get() {
      return id++;
    }
  }
}