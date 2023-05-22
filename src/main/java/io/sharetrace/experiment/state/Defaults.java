package io.sharetrace.experiment.state;

import io.sharetrace.experiment.data.FileDataset;
import io.sharetrace.experiment.data.SampledDataset;
import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.experiment.data.factory.GraphGeneratorBuilder;
import io.sharetrace.experiment.data.factory.GraphGeneratorFactory;
import io.sharetrace.experiment.data.factory.RiskScoreFactory;
import io.sharetrace.experiment.data.sampler.RiskScoreSampler;
import io.sharetrace.experiment.data.sampler.Sampler;
import io.sharetrace.experiment.data.sampler.TimeSampler;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParams;
import io.sharetrace.model.message.RiskScoreMsg;
import io.sharetrace.util.Uid;
import io.sharetrace.util.cache.CacheParams;
import io.sharetrace.util.logging.event.ContactEvent;
import io.sharetrace.util.logging.event.ContactsRefreshEvent;
import io.sharetrace.util.logging.event.CurrentRefreshEvent;
import io.sharetrace.util.logging.event.ReceiveEvent;
import io.sharetrace.util.logging.event.SendCachedEvent;
import io.sharetrace.util.logging.event.SendCurrentEvent;
import io.sharetrace.util.logging.event.UpdateEvent;
import io.sharetrace.util.logging.metric.CreateUsersRuntime;
import io.sharetrace.util.logging.metric.GraphCycles;
import io.sharetrace.util.logging.metric.GraphEccentricity;
import io.sharetrace.util.logging.metric.GraphScores;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.metric.GraphTopology;
import io.sharetrace.util.logging.metric.MsgPassingRuntime;
import io.sharetrace.util.logging.metric.RiskPropRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendScoresRuntime;
import io.sharetrace.util.logging.setting.ExperimentSettings;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well1024a;

public final class Defaults {

  private static final String WHITESPACE_DELIMITER = "\\s+";
  private static final Clock CLOCK = Clock.systemUTC();
  private static final Duration TTL = Duration.ofDays(14L);
  private static final UserParams USER_PARAMS = newUserParams();
  private static final CacheParams<RiskScoreMsg> CACHE_PARAMS = newCacheParams();
  private static final Context CONTEXT = newContext();

  private Defaults() {}

  public static Clock clock() {
    return CLOCK;
  }

  public static CacheParams<RiskScoreMsg> cacheParams() {
    return CACHE_PARAMS;
  }

  public static Context context() {
    return CONTEXT;
  }

  public static RandomGenerator rng() {
    return rng(CONTEXT.seed());
  }

  public static RandomGenerator rng(long seed) {
    return new Well1024a(seed);
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
        .maxLookBack(ctx.userParams().scoreTtl())
        .refTime(ctx.refTime())
        .build();
  }

  public static Sampler<Instant> contactTimeSampler(DatasetContext ctx) {
    return TimeSampler.builder()
        .lookBacks(ctx.contactTimes())
        .maxLookBack(ctx.userParams().contactTtl())
        .refTime(ctx.refTime())
        .build();
  }

  public static UserParams userParams() {
    return USER_PARAMS;
  }

  public static FileDataset fileDataset(DatasetContext ctx, Path path) {
    return FileDataset.builder()
        .delimiter(WHITESPACE_DELIMITER)
        .path(path)
        .refTime(ctx.refTime())
        .scoreFactory(scoreFactory(ctx))
        .build();
  }

  public static RiskScoreFactory scoreFactory(DatasetContext ctx) {
    return RiskScoreFactory.from(scoreSampler(ctx)::sample);
  }

  public static SampledDataset sampledDataset(DatasetContext ctx, int numNodes) {
    return SampledDataset.builder()
        .scoreFactory(scoreFactory(ctx))
        .contactTimeFactory(contactTimeFactory(ctx))
        .graphGeneratorFactory(graphGeneratorFactory(ctx))
        .numNodes(numNodes)
        .build();
  }

  public static ContactTimeFactory contactTimeFactory(DatasetContext ctx) {
    return ContactTimeFactory.from(contactTimeSampler(ctx)::sample);
  }

  public static GraphGeneratorFactory graphGeneratorFactory(DatasetContext ctx) {
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

  private static UserParams newUserParams() {
    return UserParams.builder()
        .transRate(0.8f)
        .sendCoeff(1f)
        .scoreTtl(TTL)
        .contactTtl(TTL)
        .tolerance(0.01f)
        .timeBuffer(Duration.ofDays(2L))
        .idleTimeout(Duration.ofSeconds(5L))
        .build();
  }

  private static CacheParams<RiskScoreMsg> newCacheParams() {
    return CacheParams.<RiskScoreMsg>builder()
        .interval(Duration.ofDays(1L))
        .numIntervals((int) (2 * TTL.toDays()))
        .numLookAhead(1)
        .refreshPeriod(Duration.ofHours(1L))
        .mergeStrategy(Defaults::cacheMerge)
        .clock(CLOCK)
        .build();
  }

  public static RiskScoreMsg cacheMerge(RiskScoreMsg oldMsg, RiskScoreMsg newMsg) {
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
    return Math.abs(msg1.score().value() - msg2.score().value()) < USER_PARAMS.tolerance();
  }

  private static Context newContext() {
    return Context.builder()
        .clock(CLOCK)
        .refTime(CLOCK.instant())
        .seed(Uid.ofInt())
        .addLoggable(
            // Events
            ContactEvent.class,
            ContactsRefreshEvent.class,
            CurrentRefreshEvent.class,
            ReceiveEvent.class,
            SendCachedEvent.class,
            SendCurrentEvent.class,
            UpdateEvent.class,
            // Graph metrics
            GraphCycles.class,
            GraphEccentricity.class,
            GraphScores.class,
            GraphSize.class,
            // Runtime metrics
            GraphTopology.class,
            CreateUsersRuntime.class,
            SendScoresRuntime.class,
            SendContactsRuntime.class,
            RiskPropRuntime.class,
            MsgPassingRuntime.class,
            // Settings
            ExperimentSettings.class)
        .build();
  }
}
