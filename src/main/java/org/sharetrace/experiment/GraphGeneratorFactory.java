package org.sharetrace.experiment;

import java.util.Random;
import org.immutables.builder.Builder;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;

class GraphGeneratorFactory {

  private GraphGeneratorFactory() {}

  @Builder.Factory
  public static <V, E> GraphGenerator<V, E, ?> graphGenerator(
      @Builder.Parameter GraphType graphType, int nNodes, int nEdges, Random random) {
    switch (graphType) {
      case GEOMETRIC -> {
        return new GnmRandomGraphGenerator<>(nNodes,nEdges, random, false, false);
      }
      case BARABASI_ALBERT -> {
        return new BarabasiAlbertGraphGenerator<>(2, 2, nNodes, random);
      }
      default -> {
        return new ScaleFreeGraphGenerator<>(nNodes, random);
      }
    }
  }
}
