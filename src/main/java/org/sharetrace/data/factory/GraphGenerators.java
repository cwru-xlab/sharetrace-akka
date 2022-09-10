package org.sharetrace.data.factory;

import java.util.Optional;
import org.immutables.builder.Builder;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.RandomRegularGraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.sharetrace.experiment.GraphType;

final class GraphGenerators {

  private GraphGenerators() {}

  @Builder.Factory
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static GraphGenerator<Integer, DefaultEdge, Integer> graphGenerator(
      @Builder.Parameter GraphType graphType,
      @Builder.Parameter int numNodes,
      @Builder.Parameter long seed,
      Optional<Integer> numInitialNodes,
      Optional<Integer> numNewEdges,
      Optional<Integer> numEdges,
      Optional<Integer> degree,
      Optional<Integer> numNearestNeighbors,
      Optional<Double> rewiringProbability) {
    switch (graphType) {
        // Random
      case GNM_RANDOM:
        return new GnmRandomGraphGenerator<>(
            numNodes, getOrThrow(numEdges, "numEdges", GraphType.GNM_RANDOM), seed, false, false);
      case RANDOM_REGULAR:
        return new RandomRegularGraphGenerator<>(
            numNodes, getOrThrow(degree, "degree", GraphType.RANDOM_REGULAR), seed);
        // Non-random
      case BARABASI_ALBERT:
        return new BarabasiAlbertGraphGenerator<>(
            getOrThrow(numInitialNodes, "numInitialNodes", GraphType.BARABASI_ALBERT),
            getOrThrow(numNewEdges, "numNewEdges", GraphType.BARABASI_ALBERT),
            numNodes,
            seed);
      case WATTS_STROGATZ:
        return new WattsStrogatzGraphGenerator<>(
            numNodes,
            getOrThrow(numNearestNeighbors, "numNearestNeighbors", GraphType.WATTS_STROGATZ),
            getOrThrow(rewiringProbability, "rewiringProbability", GraphType.WATTS_STROGATZ),
            seed);
      case SCALE_FREE:
        return new ScaleFreeGraphGenerator<>(numNodes, seed);
      default:
        throw generatorCreationFailed(graphType);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static <T> T getOrThrow(Optional<T> optional, String name, GraphType graphType) {
    return optional.orElseThrow(() -> missingParam(name, graphType));
  }

  private static RuntimeException missingParam(String name, GraphType graphType) {
    return new IllegalArgumentException("Missing parameter " + name + " for graph " + graphType);
  }

  private static RuntimeException generatorCreationFailed(GraphType graphType) {
    return new IllegalArgumentException("Failed to create graph generator for " + graphType);
  }
}
