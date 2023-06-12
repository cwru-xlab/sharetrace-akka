package sharetrace;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.BinomialDistribution;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Statistics;

public class Sandbox {

  public static void main(String[] args) {

    DistributedRandom random = DistributedRandom.from(new BinomialDistribution(100, 0.5));
    Statistics statistics =
        IntStream.range(0, 10000)
            .mapToDouble(x -> random.nextDouble())
            .boxed()
            .collect(Collectors.collectingAndThen(Collectors.toList(), Statistics::of));
    System.out.println(statistics);
  }
}
