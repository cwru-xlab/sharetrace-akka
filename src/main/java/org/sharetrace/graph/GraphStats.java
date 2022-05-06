package org.sharetrace.graph;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.alg.scoring.Coreness;
import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.shortestpath.GraphMeasurer;

public final class GraphStats<V, E> {

  private static final double NOT_COMPUTED = -1d;
  private final Graph<V, E> graph;
  private final ShortestPathAlgorithm<V, ?> shortestPath;
  private GraphMeasurer<V, ?> measurer;
  private double[] harmonicCentralities;
  private double[] katzCentralities;
  private double[] eigenvectorCentralities;
  private double[] localClusteringCoefficients;
  private double globalClusteringCoefficient = NOT_COMPUTED;
  private long nTriangles = (long) NOT_COMPUTED;
  private int center = (int) NOT_COMPUTED;
  private int periphery = (int) NOT_COMPUTED;
  private int girth = (int) NOT_COMPUTED;
  private int degeneracy = (int) NOT_COMPUTED;

  private GraphStats(Graph<V, E> graph) {
    this.graph = graph;
    this.shortestPath = new FloydWarshallShortestPaths<>(graph);
  }

  public static <V, E> GraphStats<V, E> of(Graph<V, E> graph) {
    return new GraphStats<>(graph);
  }

  public int girth() {
    if (girth == NOT_COMPUTED) {
      girth = GraphMetrics.getGirth(graph);
    }
    return girth;
  }

  public long nTriangles() {
    if (nTriangles == NOT_COMPUTED) {
      nTriangles = GraphMetrics.getNumberOfTriangles(graph);
    }
    return nTriangles;
  }

  public long nNodes() {
    return graph.iterables().vertexCount();
  }

  public long nEdges() {
    return graph.iterables().edgeCount();
  }

  public long radius() {
    return (long) getMeasurer().getRadius();
  }

  private GraphMeasurer<V, ?> getMeasurer() {
    if (measurer == null) {
      measurer = new GraphMeasurer<>(graph);
    }
    return measurer;
  }

  public long diameter() {
    return (long) getMeasurer().getDiameter();
  }

  public int periphery() {
    if (periphery == NOT_COMPUTED) {
      periphery = getMeasurer().getGraphPeriphery().size();
    }
    return periphery;
  }

  public int center() {
    if (center == NOT_COMPUTED) {
      center = getMeasurer().getGraphCenter().size();
    }
    return center;
  }

  public double[] harmonicCentralities() {
    if (harmonicCentralities == null) {
      harmonicCentralities =
          toDoubleArray(new HarmonicCentrality<>(graph, shortestPath).getScores());
    }
    return harmonicCentralities;
  }

  private static double[] toDoubleArray(Map<?, Double> map) {
    return map.values().stream().mapToDouble(Number::doubleValue).toArray();
  }

  public double globalClusteringCoefficient() {
    if (globalClusteringCoefficient == NOT_COMPUTED) {
      globalClusteringCoefficient =
          new ClusteringCoefficient<>(graph).getGlobalClusteringCoefficient();
    }
    return globalClusteringCoefficient;
  }

  public double[] localClusteringCoefficients() {
    if (localClusteringCoefficients == null) {
      localClusteringCoefficients = toDoubleArray(new ClusteringCoefficient<>(graph).getScores());
    }
    return localClusteringCoefficients;
  }

  public double[] katzCentralities() {
    if (katzCentralities == null) {
      katzCentralities = toDoubleArray(new KatzCentrality<>(graph).getScores());
    }
    return katzCentralities;
  }

  public double[] eigenvectorCentralities() {
    if (eigenvectorCentralities == null) {
      eigenvectorCentralities = toDoubleArray(new EigenvectorCentrality<>(graph).getScores());
    }
    return eigenvectorCentralities;
  }

  public int degeneracy() {
    if (degeneracy == NOT_COMPUTED) {
      degeneracy = new Coreness<>(graph).getDegeneracy();
    }
    return degeneracy;
  }

  // Copied from JGraphT's implementation, but reuses a ShortestPathAlgorithm instance.
  private static final class HarmonicCentrality<V, E> extends ClosenessCentrality<V, E> {

    private final ShortestPathAlgorithm<V, ?> shortestPathAlgorithm;

    public HarmonicCentrality(
        Graph<V, E> graph, ShortestPathAlgorithm<V, ?> shortestPathAlgorithm) {
      super(graph);
      this.shortestPathAlgorithm = shortestPathAlgorithm;
    }

    @Override
    protected void compute() {
      // Modified from original to use fastutil Map.
      scores = new Object2DoubleOpenHashMap<>();
      int n = graph.vertexSet().size();
      for (V v : graph.vertexSet()) {
        double sum = 0d;
        SingleSourcePaths<V, ?> paths = shortestPathAlgorithm.getPaths(v);
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
