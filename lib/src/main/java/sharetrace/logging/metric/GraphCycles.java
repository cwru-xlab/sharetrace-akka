package sharetrace.logging.metric;

import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;

public record GraphCycles(long girth, long triangles) implements MetricRecord {

  public static GraphCycles of(Graph<?, ?> graph) {
    return new GraphCycles(GraphMetrics.getGirth(graph), GraphMetrics.getNumberOfTriangles(graph));
  }
}
