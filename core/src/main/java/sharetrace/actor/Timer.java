package sharetrace.actor;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.time.StopWatch;

final class Timer<K> extends StopWatch {

  private final Map<K, Long> runtimes = Maps.newHashMap();

  public void time(Runnable runnable, K key) {
    time(Executors.callable(runnable), key);
  }

  public <R> R time(Callable<R> callable, K key) {
    long start = getNanoTime();
    R result = call(callable);
    long stop = getNanoTime();
    runtimes.put(key, stop - start);
    return result;
  }

  public Duration duration(K key) {
    return Duration.ofNanos(runtimes.getOrDefault(key, getNanoTime()));
  }

  private <R> R call(Callable<R> callable) {
    try {
      return callable.call();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}