package sharetrace.logging.metric;

import org.jgrapht.Graph;

public record GraphSize(int nodes, int edges) implements Metric {

  public static GraphSize of(Graph<?, ?> graph) {
    return new GraphSize(graph.vertexSet().size(), graph.edgeSet().size());
  }
}
