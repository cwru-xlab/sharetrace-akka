package io.sharetrace.data.factory;

import io.sharetrace.experiment.GraphType;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.immutables.builder.Builder;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.RandomRegularGraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultEdge;

final class GraphGenerators {

  private static final String NUM_EDGES = "numEdges";
  private static final String DEGREE = "degree";
  private static final String NUM_INITIAL_NODES = "numInitialNodes";
  private static final String NUM_NEW_EDGES = "numNewEdges";
  private static final String NUM_NEAREST_NEIGHBORS = "numNearestNeighbors";
  private static final String REWIRING_PROBABILITY = "rewiringProbability";

  private GraphGenerators() {}

  @Builder.Factory
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static GraphGenerator<Integer, DefaultEdge, Integer> graphGenerator(
      @Builder.Parameter GraphType graphType,
      @Builder.Parameter int numNodes,
      @Builder.Parameter long seed,
      OptionalInt numInitialNodes,
      OptionalInt numNewEdges,
      OptionalInt numEdges,
      OptionalInt degree,
      OptionalInt numNearestNeighbors,
      OptionalDouble rewiringProbability) {
    switch (graphType) {
        // Random
      case GNM_RANDOM:
        return new GnmRandomGraphGenerator<>(
            numNodes, getOrThrow(numEdges, NUM_EDGES, GraphType.GNM_RANDOM), seed, false, false);
      case RANDOM_REGULAR:
        return new RandomRegularGraphGenerator<>(
            numNodes, getOrThrow(degree, DEGREE, GraphType.RANDOM_REGULAR), seed);
        // Non-random
      case BARABASI_ALBERT:
        return new BarabasiAlbertGraphGenerator<>(
            getOrThrow(numInitialNodes, NUM_INITIAL_NODES, GraphType.BARABASI_ALBERT),
            getOrThrow(numNewEdges, NUM_NEW_EDGES, GraphType.BARABASI_ALBERT),
            numNodes,
            seed);
      case WATTS_STROGATZ:
        return new WattsStrogatzGraphGenerator<>(
            numNodes,
            getOrThrow(numNearestNeighbors, NUM_NEAREST_NEIGHBORS, GraphType.WATTS_STROGATZ),
            getOrThrow(rewiringProbability, REWIRING_PROBABILITY, GraphType.WATTS_STROGATZ),
            seed);
      case SCALE_FREE:
        return new ScaleFreeGraphGenerator<>(numNodes, seed);
      default:
        throw generatorCreationFailed(graphType);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static int getOrThrow(OptionalInt optional, String name, GraphType graphType) {
    return optional.orElseThrow(() -> missingParam(name, graphType));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static double getOrThrow(OptionalDouble optional, String name, GraphType graphType) {
    return optional.orElseThrow(() -> missingParam(name, graphType));
  }

  private static RuntimeException generatorCreationFailed(GraphType graphType) {
    return new IllegalArgumentException("Failed to create graph generator for " + graphType);
  }

  private static RuntimeException missingParam(String name, GraphType graphType) {
    return new IllegalArgumentException(
        "Missing parameter '" + name + "' for " + graphType + " graph");
  }
}
