package org.sharetrace.experiment.state;

import com.google.common.math.DoubleMath;
import java.time.Duration;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sharetrace.data.Dataset;
import org.sharetrace.experiment.ExperimentContext;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.metrics.TopologyMetric;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.message.MessageParameters;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.CacheParameters;

public final class Defaults {

  private static final double MIN_BASE = Math.log(1.1);
  private static final double MAX_BASE = Math.log(10d);
  private static final double DECAY_RATE = 1.75E-7;
  private static final Duration DEFAULT_TTL = Duration.ofDays(14L);
  private static final CacheParameters<RiskScoreMessage> CACHE_PARAMETERS = newCacheParameters();
  private static final MessageParameters MESSAGE_PARAMETERS = newMessageParameters();
  private static final ExperimentContext CONTEXT = ExperimentContext.create();
  private static final ExperimentContext RUNTIME_CONTEXT = newRuntimeContext();
  private static final ExperimentContext PARAMETERS_CONTEXT = newParametersContext();

  private Defaults() {}

  public static CacheParameters<RiskScoreMessage> cacheParameters() {
    return CACHE_PARAMETERS;
  }

  public static UserParameters userParameters(Dataset dataset) {
    return UserParameters.builder()
        .refreshPeriod(Duration.ofHours(1L))
        .idleTimeout(idleTimeout(dataset))
        .build();
  }

  public static Duration idleTimeout(Dataset dataset) {
    double nContacts = dataset.getContactNetwork().numContacts();
    double targetBase = Math.max(MIN_BASE, MAX_BASE - DECAY_RATE * nContacts);
    long timeout = (long) Math.ceil(Math.log(nContacts) / targetBase);
    return Duration.ofSeconds(timeout);
  }

  public static ExperimentContext runtimeContext() {
    return RUNTIME_CONTEXT;
  }

  public static ExperimentContext parametersContext() {
    return PARAMETERS_CONTEXT;
  }

  private static ExperimentContext newParametersContext() {
    return CONTEXT.withLoggable(
        CONTEXT.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(TopologyMetric.class)))
            .collect(Collectors.toUnmodifiableSet()));
  }

  private static ExperimentContext newRuntimeContext() {
    return CONTEXT.withLoggable(
        Set.of(SizeMetrics.class, RuntimeMetric.class, ExperimentSettings.class));
  }

  private static CacheParameters<RiskScoreMessage> newCacheParameters() {
    return CacheParameters.<RiskScoreMessage>builder()
        .interval(Duration.ofDays(1L))
        .numIntervals((int) (2 * DEFAULT_TTL.toDays()))
        .refreshPeriod(Duration.ofHours(1L))
        .mergeStrategy(Defaults::cacheMerge)
        .numLookAhead(1)
        .build();
  }

  private static RiskScoreMessage cacheMerge(RiskScoreMessage oldMsg, RiskScoreMessage newMsg) {
    // Simpler to check for higher value first.
    // Most will likely not be older, which avoids checking for approximate equality.
    return isHigher(newMsg, oldMsg) || (isOlder(newMsg, oldMsg) && isApproxEqual(newMsg, oldMsg))
        ? newMsg
        : oldMsg;
  }

  private static boolean isHigher(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return msg1.score().value() > msg2.score().value();
  }

  private static boolean isOlder(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return msg1.score().timestamp().isBefore(msg2.score().timestamp());
  }

  private static boolean isApproxEqual(RiskScoreMessage msg1, RiskScoreMessage msg2) {
    return DoubleMath.fuzzyEquals(
        msg1.score().value(), msg2.score().value(), MESSAGE_PARAMETERS.scoreTolerance());
  }

  public static MessageParameters messageParameters() {
    return MESSAGE_PARAMETERS;
  }

  private static MessageParameters newMessageParameters() {
    return MessageParameters.builder()
        .transmissionRate(0.8f)
        .sendCoefficient(0.6f)
        .scoreTtl(DEFAULT_TTL)
        .contactTtl(DEFAULT_TTL)
        .scoreTolerance(0.01f)
        .timeBuffer(Duration.ofDays(2L))
        .build();
  }
}
