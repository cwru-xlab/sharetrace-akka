package io.sharetrace.actor;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;

final class Timer<K> extends StopWatch {

  private final Map<K, Long> runtimes = new Object2LongOpenHashMap<>();

  public void time(Runnable task, K key) {
    time(Executors.callable(task), key);
  }

  public <R> R time(Callable<R> task, K key) {
    try {
      long start = getNanoTime();
      R result = task.call();
      long stop = getNanoTime();
      runtimes.put(key, stop - start);
      return result;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public long millis(K key) {
    return millis(nanos(key));
  }

  public static long millis(long nanos) {
    return TimeUnit.NANOSECONDS.toMillis(nanos);
  }

  public long nanos(K key) {
    return runtimes.computeIfAbsent(key, x -> getNanoTime());
  }
}
