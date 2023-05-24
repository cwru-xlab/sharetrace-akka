package io.sharetrace.experiment.state;

import io.sharetrace.experiment.data.FileDataset;
import io.sharetrace.experiment.data.SampledDataset;
import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.experiment.data.factory.GraphGeneratorBuilder;
import io.sharetrace.experiment.data.factory.GraphGeneratorFactory;
import io.sharetrace.experiment.data.factory.ScoreFactory;
import io.sharetrace.experiment.data.sampler.RiskScoreSampler;
import io.sharetrace.experiment.data.sampler.Sampler;
import io.sharetrace.experiment.data.sampler.TimeSampler;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.Identifiers;
import io.sharetrace.util.cache.CacheParameters;
import io.sharetrace.util.cache.DefaultCacheMergeStrategy;
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
import io.sharetrace.util.logging.metric.MessagePassingRuntime;
import io.sharetrace.util.logging.metric.RiskPropagationRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendRiskScoresRuntime;
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
  private static final Instant INSTANT = CLOCK.instant();
  private static final Duration TIME_TO_LIVE = Duration.ofDays(14L);
  private static final long SEED = Identifiers.ofLong();

  private Defaults() {}

  public static Clock clock() {
    return CLOCK;
  }

  public static CacheParameters<RiskScoreMessage> cacheParameters() {
    return CacheParameters.<RiskScoreMessage>builder()
        .interval(Duration.ofDays(1L))
        .intervals((int) (2 * TIME_TO_LIVE.toDays()))
        .lookAhead(1)
        .refreshPeriod(Duration.ofHours(1L))
        .mergeStrategy(new DefaultCacheMergeStrategy<>(userParameters()))
        .clock(CLOCK)
        .build();
  }

  public static Context context() {
    return Context.builder()
        .clock(CLOCK)
        .refTime(INSTANT)
        .seed(SEED)
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
            SendRiskScoresRuntime.class,
            SendContactsRuntime.class,
            RiskPropagationRuntime.class,
            MessagePassingRuntime.class,
            // Settings
            ExperimentSettings.class)
        .build();
  }

  public static RandomGenerator rng(long seed) {
    return new Well1024a(seed);
  }

  public static Sampler<RiskScore> scoreSampler(DatasetContext context) {
    return RiskScoreSampler.builder()
        .values(context.scoreValues())
        .timeSampler(scoreTimeSampler(context))
        .build();
  }

  public static Sampler<Instant> scoreTimeSampler(DatasetContext context) {
    return TimeSampler.builder()
        .lookBacks(context.scoreTimes())
        .maxLookBack(context.userParameters().scoreTimeToLive())
        .refTime(context.refTime())
        .build();
  }

  public static Sampler<Instant> contactTimeSampler(DatasetContext context) {
    return TimeSampler.builder()
        .lookBacks(context.contactTimes())
        .maxLookBack(context.userParameters().contactTimeToLive())
        .refTime(context.refTime())
        .build();
  }

  public static UserParameters userParameters() {
    return UserParameters.builder()
        .transmissionRate(0.8f)
        .sendCoefficient(1f)
        .scoreTimeToLive(TIME_TO_LIVE)
        .contactTimeToLive(TIME_TO_LIVE)
        .tolerance(0.01f)
        .timeBuffer(Duration.ofDays(2L))
        .idleTimeout(Duration.ofSeconds(5L))
        .build();
  }

  public static FileDataset fileDataset(DatasetContext context, Path path) {
    return FileDataset.builder()
        .delimiter(WHITESPACE_DELIMITER)
        .path(path)
        .refTime(context.refTime())
        .scoreFactory(scoreFactory(context))
        .build();
  }

  public static ScoreFactory scoreFactory(DatasetContext context) {
    return ScoreFactory.from(scoreSampler(context)::sample);
  }

  public static SampledDataset sampledDataset(DatasetContext context, int users) {
    return SampledDataset.builder()
        .scoreFactory(scoreFactory(context))
        .contactTimeFactory(contactTimeFactory(context))
        .graphGeneratorFactory(graphGeneratorFactory(context))
        .users(users)
        .build();
  }

  public static ContactTimeFactory contactTimeFactory(DatasetContext context) {
    return ContactTimeFactory.from(contactTimeSampler(context)::sample);
  }

  public static GraphGeneratorFactory graphGeneratorFactory(DatasetContext context) {
    return vertices ->
        GraphGeneratorBuilder.create(context.graphType(), vertices, context.seed())
            .numEdges(vertices * 2)
            .degree(4)
            .numNearestNeighbors(2)
            .numInitialNodes(2)
            .numNewEdges(2)
            .rewiringProbability(0.3)
            .build();
  }
}
