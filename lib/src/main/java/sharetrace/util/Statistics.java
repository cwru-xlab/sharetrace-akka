package sharetrace.util;

import java.util.Collection;
import java.util.stream.DoubleStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import sharetrace.Buildable;

@Buildable
public record Statistics(
    double mean,
    double variance,
    double max,
    double min,
    double median,
    double lowerWhisker,
    double lowerQuartile,
    double interQuartileRange,
    double upperQuartile,
    double upperWhisker,
    long count,
    double sum,
    double[] outliers) {

  public static Statistics of(Collection<? extends Number> values) {
    DescriptiveStatistics stats = stats(values);
    var lowerQuartile = stats.getPercentile(25);
    var upperQuartile = stats.getPercentile(75);
    var interQuartileRange = upperQuartile - lowerQuartile;
    var lowerWhisker = lowerQuartile - 1.5 * interQuartileRange;
    var upperWhisker = upperQuartile + 1.5 * interQuartileRange;
    return StatisticsBuilder.create()
        .mean(stats.getMean())
        .variance(stats.getVariance())
        .max(stats.getMax())
        .min(stats.getMin())
        .lowerWhisker(lowerWhisker)
        .lowerQuartile(lowerQuartile)
        .median(stats.getPercentile(50))
        .upperQuartile(upperQuartile)
        .upperWhisker(upperWhisker)
        .interQuartileRange(interQuartileRange)
        .count(stats.getN())
        .sum(stats.getSum())
        .outliers(outliers(stats, lowerWhisker, upperWhisker))
        .build();
  }

  private static DescriptiveStatistics stats(Collection<? extends Number> values) {
    var stats = new DescriptiveStatistics();
    values.stream().mapToDouble(Number::doubleValue).forEach(stats::addValue);
    return stats;
  }

  private static double[] outliers(DescriptiveStatistics stats, double lower, double upper) {
    return DoubleStream.of(stats.getValues()).filter(v -> v < lower || v > upper).toArray();
  }
}
