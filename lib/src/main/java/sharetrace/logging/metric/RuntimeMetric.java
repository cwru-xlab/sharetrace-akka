package sharetrace.logging.metric;

import java.time.Duration;

public interface RuntimeMetric extends Metric {

  Duration runtime();
}
