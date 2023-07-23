package sharetrace.logging.metric;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import sharetrace.Buildable;

@Buildable
public record GraphEccentricity(long radius, long diameter, long center, long periphery)
    implements MetricRecord {

  public static GraphEccentricity of(Graph<?, ?> graph) {
    GraphMeasurer<?, ?> measurer = new GraphMeasurer<>(graph);
    return GraphEccentricityBuilder.create()
        .radius((long) measurer.getRadius())
        .diameter((long) measurer.getDiameter())
        .center(measurer.getGraphCenter().size())
        .periphery(measurer.getGraphPeriphery().size())
        .build();
  }
}
