package sharetrace.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.time.StopWatch;

public final class Timer<K> extends StopWatch {

  private final Map<K, Long> runtimes = new HashMap<>();

  public void time(Runnable runnable, K key) {
    time(Executors.callable(runnable), key);
  }

  public <R> R time(Callable<R> callable, K key) {
    var start = getNanoTime();
    var result = call(callable);
    var stop = getNanoTime();
    runtimes.put(key, stop - start);
    return result;
  }

  public Duration duration(K key) {
    return Duration.ofNanos(runtimes.computeIfAbsent(key, x -> getNanoTime()));
  }

  private <R> R call(Callable<R> callable) {
    try {
      return callable.call();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
