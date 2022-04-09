package org.sharetrace.experiment;

import java.util.Random;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;

public class GraphGeneratorFactory {

  private GraphGeneratorFactory() {}

  public static <V, E> GraphGenerator<V, E, ?> create(GraphType graphType, Random random) {
    switch (graphType) {
      case GEOMETRIC -> {
        return new GnmRandomGraphGenerator<>(10000,50000, random, false, false);
      }
      case BARABASI_ALBERT -> {
        return new BarabasiAlbertGraphGenerator<>(1, 2, 10000, random);
      }
      default -> {
        return new ScaleFreeGraphGenerator<>(10000, random);
      }
    }
  }
}
