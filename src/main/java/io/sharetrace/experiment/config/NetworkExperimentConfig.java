package io.sharetrace.experiment.config;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import org.immutables.value.Value;

abstract class NetworkExperimentConfig extends ExperimentConfig {

    public static final int MIN_NETWORKS = 1;

    private static final Range<Integer> NETWORKS_RANGE = Range.atLeast(MIN_NETWORKS);

    @Value.Check
    protected void check() {
        super.check();
        Checks.inRange(numNetworks(), NETWORKS_RANGE, "numNetworks");
    }

    @Value.Default
    public int numNetworks() {
        return MIN_NETWORKS;
    }
}
