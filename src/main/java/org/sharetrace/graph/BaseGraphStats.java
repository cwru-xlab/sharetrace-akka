package org.sharetrace.graph;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.sharetrace.logging.metrics.CycleMetrics;
import org.sharetrace.logging.metrics.EccentricityMetrics;
import org.sharetrace.logging.metrics.ScoringMetrics;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.util.DescriptiveStats;

@Value.Immutable
abstract class BaseGraphStats<V, E> {

  private static List<Float> getScores(VertexScoringAlgorithm<?, Double> algorithm) {
    Collection<Double> scores = algorithm.getScores().values();
    return scores.stream()
        .map(Double::floatValue)
        .collect(() -> new FloatArrayList(scores.size()), List::add, List::addAll);
  }

  public SizeMetrics sizeMetrics() {
    return SizeMetrics.builder().numNodes(numNodes()).numEdges(numEdges()).build();
  }

  @Value.Lazy
  public int numNodes() {
    return graph().vertexSet().size();
  }

  @Value.Lazy
  public int numEdges() {
    return graph().edgeSet().size();
  }

  public CycleMetrics cycleMetrics() {
    return CycleMetrics.builder().numTriangles(numTriangles()).girth(girth()).build();
  }

  @Value.Lazy
  public long numTriangles() {
    return GraphMetrics.getNumberOfTriangles(graph());
  }

  @Value.Lazy
  public int girth() {
    return GraphMetrics.getGirth(graph());
  }

  public EccentricityMetrics eccentricityMetrics() {
    return EccentricityMetrics.builder()
        .radius(radius())
        .diameter(diameter())
        .center(center())
        .periphery(periphery())
        .build();
  }

  public ScoringMetrics scoringMetrics() {
    return ScoringMetrics.builder()
        .degeneracy(degeneracy())
        .globalClusteringCoefficient(globalClusteringCoefficient())
        .localClusteringCoefficient(DescriptiveStats.of(localClusteringCoefficients()))
        .harmonicCentrality(DescriptiveStats.of(harmonicCentralities()))
        .katzCentrality(DescriptiveStats.of(katzCentralities()))
        .eigenvectorCentrality(DescriptiveStats.of(eigenvectorCentralities()))
        .build();
  }

  @Value.Lazy
  public int degeneracy() {
    return new Coreness<>(graph()).getDegeneracy();
  }

  @Value.Lazy
  public float globalClusteringCoefficient() {
    return (float) new ClusteringCoefficient<>(graph()).getGlobalClusteringCoefficient();
  }

  @Value.Lazy
  public List<Float> localClusteringCoefficients() {
    return getScores(new ClusteringCoefficient<>(graph()));
  }

  @Value.Lazy
  public List<Float> harmonicCentralities() {
    return getScores(new HarmonicCentrality<>(graph(), shortestPath()));
  }

  @Value.Lazy
  public List<Float> katzCentralities() {
    return getScores(new KatzCentrality<>(graph()));
  }

  @Value.Lazy
  public List<Float> eigenvectorCentralities() {
    return getScores(new EigenvectorCentrality<>(graph()));
  }

  @Value.Lazy
  public int radius() {
    return (int) graphMeasurer().getRadius();
  }

  @Value.Lazy
  public int diameter() {
    return (int) graphMeasurer().getDiameter();
  }

  @Value.Lazy
  public int periphery() {
    return graphMeasurer().getGraphPeriphery().size();
  }

  @Value.Lazy
  public int center() {
    return graphMeasurer().getGraphCenter().size();
  }

  @Value.Parameter
  protected abstract Graph<V, E> graph();

  @Value.Lazy
  protected ShortestPathAlgorithm<V, E> shortestPath() {
    return new FloydWarshallShortestPaths<>(graph());
  }

  @Value.Lazy
  protected GraphMeasurer<V, E> graphMeasurer() {
    return new GraphMeasurer<>(graph(), shortestPath());
  }

  // Copied from JGraphT's implementation, but reuses a ShortestPathAlgorithm instance.
  private static final class HarmonicCentrality<V, E> extends ClosenessCentrality<V, E> {

    private final ShortestPathAlgorithm<V, E> shortestPaths;

    public HarmonicCentrality(Graph<V, E> graph, ShortestPathAlgorithm<V, E> shortestPaths) {
      super(graph);
      this.shortestPaths = shortestPaths;
    }

    @Override
    protected void compute() {
      // Modified from original to use fastutil Map.
      scores = new Object2DoubleOpenHashMap<>();
      int n = graph.vertexSet().size();
      SingleSourcePaths<V, E> paths;
      double sum;
      for (V v : graph.vertexSet()) {
        sum = 0d;
        paths = shortestPaths.getPaths(v);
        for (V u : graph.vertexSet()) {
          if (!u.equals(v)) {
            sum += 1.0 / paths.getWeight(u);
          }
        }
        if (normalize && n > 1) {
          scores.put(v, sum / (n - 1));
        } else {
          scores.put(v, sum);
        }
      }
    }
  }
}
