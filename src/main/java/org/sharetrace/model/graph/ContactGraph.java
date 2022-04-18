package org.sharetrace.model.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

  private final Collection<Integer> nodes;
  private final Collection<List<Integer>> edges;

  private ContactGraph(Graph<Integer, Edge<Integer>> graph) {
    this.nodes = mapNodes(graph.vertexSet());
    this.edges = mapEdges(graph.edgeSet());
  }

  public static ContactGraph create(GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    Graph<Integer, Edge<Integer>> graph = newGraph();
    generator.generateGraph(graph);
    return new ContactGraph(graph);
  }

  private static Collection<Integer> mapNodes(Collection<Integer> nodes) {
    return Collections.unmodifiableCollection(new IntArrayList(nodes));
  }

  private static Collection<List<Integer>> mapEdges(Collection<Edge<Integer>> edges) {
    return edges.stream()
        .map(edge -> List.of(edge.source(), edge.target()))
        .collect(Collectors.toUnmodifiableList());
  }

  private static Graph<Integer, Edge<Integer>> newGraph() {
    return new FastutilMapGraph<>(new NodeIdFactory(), Edge::new, DefaultGraphType.simple());
  }

  @Override
  public Collection<Integer> nodes() {
    return nodes;
  }

  @Override
  public Collection<List<Integer>> edges() {
    return edges;
  }

  private static final class NodeIdFactory implements Supplier<Integer> {

    private int id = 0;

    @Override
    public Integer get() {
      return id++;
    }
  }
}
