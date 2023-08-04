package sharetrace.analysis.model;

import java.util.Map;
import sharetrace.Buildable;

@Buildable
@SuppressWarnings("SpellCheckingInspection")
public record ReachabilityResult(
    Map<Integer, Integer> messageReachabilities,
    Map<Integer, Integer> sourceCounts,
    Map<Integer, Integer> influenceCounts,
    double reachabilityRatio) {}
