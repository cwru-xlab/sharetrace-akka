package org.sharetrace.experiment.state;

import com.google.common.math.DoubleMath;
import java.nio.file.Path;
import java.time.Duration;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.data.factory.GraphGeneratorBuilder;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.message.MsgParams;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.message.UserParams;
import org.sharetrace.util.CacheParams;

public final class Defaults {

  private static final String WHITESPACE_DELIMITER = "\\s+";
  private static final double MIN_BASE = Math.log(1.1);
  private static final double MAX_BASE = Math.log(10d);
  private static final double DECAY_RATE = 1.75E-7;
  private static final Duration DEFAULT_TTL = Duration.ofDays(14L);
  private static final MsgParams MSG_PARAMS = newMsgParams();
  private static final CacheParams<RiskScoreMsg> CACHE_PARAMS = newCacheParams();

  private Defaults() {}

  public static MsgParams msgParams() {
    return MSG_PARAMS;
  }

  public static CacheParams<RiskScoreMsg> cacheParams() {
    return CACHE_PARAMS;
  }

  public static UserParams userParams(Dataset dataset) {
    return UserParams.builder()
        .refreshPeriod(Duration.ofHours(1L))
        .idleTimeout(idleTimeout(dataset))
        .build();
  }

  private static Duration idleTimeout(Dataset dataset) {
    double numContacts = dataset.contactNetwork().contacts().size();
    double targetBase = Math.max(MIN_BASE, MAX_BASE - DECAY_RATE * numContacts);
    long timeout = (long) Math.ceil(Math.log(numContacts) / targetBase);
    return Duration.ofSeconds(timeout);
  }

  public static Dataset fileDataset(DatasetContext context, Path path) {
    return FileDataset.builder()
        .delimiter(WHITESPACE_DELIMITER)
        .path(path)
        .addAllLoggable(context.loggable())
        .refTime(context.refTime())
        .scoreFactory(context.scoreFactory())
        .build();
  }

  public static Dataset sampledDataset(DatasetContext context, int numNodes) {
    return SampledDataset.builder()
        .addAllLoggable(context.loggable())
        .riskScoreFactory(context.scoreFactory())
        .contactTimeFactory(context.contactTimeFactory())
        .graphGeneratorFactory(defaultFactory(context))
        .numNodes(numNodes)
        .build();
  }

  private static GraphGeneratorFactory defaultFactory(DatasetContext context) {
    return numNodes ->
        GraphGeneratorBuilder.create(context.graphType(), numNodes, context.seed())
            .numEdges(numNodes * 2)
            .degree(4)
            .numNearestNeighbors(2)
            .numInitialNodes(2)
            .numNewEdges(2)
            .rewiringProbability(0.3)
            .build();
  }

  private static CacheParams<RiskScoreMsg> newCacheParams() {
    return CacheParams.<RiskScoreMsg>builder()
        .interval(Duration.ofDays(1L))
        .numIntervals((int) (2 * DEFAULT_TTL.toDays()))
        .refreshPeriod(Duration.ofHours(1L))
        .mergeStrategy(Defaults::cacheMerge)
        .numLookAhead(1)
        .build();
  }

  private static RiskScoreMsg cacheMerge(RiskScoreMsg oldMsg, RiskScoreMsg newMsg) {
    // Simpler to check for higher value first.
    // Most will likely not be older, which avoids checking for approximate equality.
    return isHigher(newMsg, oldMsg) || (isOlder(newMsg, oldMsg) && isApproxEqual(newMsg, oldMsg))
        ? newMsg
        : oldMsg;
  }

  private static boolean isHigher(RiskScoreMsg msg1, RiskScoreMsg msg2) {
    return msg1.score().value() > msg2.score().value();
  }

  private static boolean isOlder(RiskScoreMsg msg1, RiskScoreMsg msg2) {
    return msg1.score().time().isBefore(msg2.score().time());
  }

  private static boolean isApproxEqual(RiskScoreMsg msg1, RiskScoreMsg msg2) {
    return DoubleMath.fuzzyEquals(
        msg1.score().value(), msg2.score().value(), MSG_PARAMS.tolerance());
  }

  private static MsgParams newMsgParams() {
    return MsgParams.builder()
        .transmissionRate(0.8f)
        .sendCoefficient(0.6f)
        .scoreTtl(DEFAULT_TTL)
        .contactTtl(DEFAULT_TTL)
        .tolerance(0.01f)
        .timeBuffer(Duration.ofDays(2L))
        .build();
  }
}
