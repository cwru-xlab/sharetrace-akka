package org.sharetrace.experiment.state;

import com.google.common.math.DoubleMath;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.GraphGeneratorBuilder;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.data.sampler.RiskScoreSampler;
import org.sharetrace.data.sampler.Sampler;
import org.sharetrace.data.sampler.TimeSampler;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.model.CacheParams;
import org.sharetrace.model.MsgParams;
import org.sharetrace.model.RiskScore;
import org.sharetrace.model.UserParams;

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

  public static RiskScoreFactory scoreFactory(DatasetContext ctx) {
    return RiskScoreFactory.from(scoreSampler(ctx)::sample);
  }

  public static Sampler<RiskScore> scoreSampler(DatasetContext ctx) {
    return RiskScoreSampler.builder()
        .values(ctx.scoreValues())
        .timeSampler(scoreTimeSampler(ctx))
        .build();
  }

  public static Sampler<Instant> scoreTimeSampler(DatasetContext ctx) {
    return TimeSampler.builder()
        .lookBacks(ctx.scoreTimes())
        .maxLookBack(ctx.msgParams().scoreTtl())
        .refTime(ctx.refTime())
        .build();
  }

  public static ContactTimeFactory contactTimeFactory(DatasetContext ctx) {
    return ContactTimeFactory.from(contactTimeSampler(ctx)::sample);
  }

  public static Sampler<Instant> contactTimeSampler(DatasetContext ctx) {
    return TimeSampler.builder()
        .lookBacks(ctx.contactTimes())
        .maxLookBack(ctx.msgParams().contactTtl())
        .refTime(ctx.refTime())
        .build();
  }

  public static UserParams userParams(Dataset dataset) {
    return UserParams.builder()
        .refreshPeriod(Duration.ofHours(1L))
        .idleTimeout(idleTimeout(dataset))
        .build();
  }

  public static FileDataset fileDataset(DatasetContext ctx, Path path) {
    return FileDataset.builder()
        .delimiter(WHITESPACE_DELIMITER)
        .path(path)
        .addAllLoggable(ctx.loggable())
        .refTime(ctx.refTime())
        .scoreFactory(scoreFactory(ctx))
        .build();
  }

  public static SampledDataset sampledDataset(DatasetContext ctx, int numNodes) {
    return SampledDataset.builder()
        .addAllLoggable(ctx.loggable())
        .riskScoreFactory(scoreFactory(ctx))
        .contactTimeFactory(contactTimeFactory(ctx))
        .graphGeneratorFactory(defaultFactory(ctx))
        .numNodes(numNodes)
        .build();
  }

  private static Duration idleTimeout(Dataset dataset) {
    double numContacts = dataset.contactNetwork().contacts().size();
    double targetBase = Math.max(MIN_BASE, MAX_BASE - DECAY_RATE * numContacts);
    long timeout = (long) Math.ceil(Math.log(numContacts) / targetBase);
    return Duration.ofSeconds(timeout);
  }

  private static GraphGeneratorFactory defaultFactory(DatasetContext ctx) {
    return numNodes ->
        GraphGeneratorBuilder.create(ctx.graphType(), numNodes, ctx.seed())
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
        .transRate(0.8f)
        .sendCoeff(0.6f)
        .scoreTtl(DEFAULT_TTL)
        .contactTtl(DEFAULT_TTL)
        .tolerance(0.01f)
        .timeBuffer(Duration.ofDays(2L))
        .build();
  }
}
