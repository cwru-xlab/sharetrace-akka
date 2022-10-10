package io.sharetrace.actor;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;

final class Timer<T> extends StopWatch {

  private final Map<T, Long> runtimes = new Object2LongOpenHashMap<>();

  public void time(Runnable task, T metric) {
    time(Executors.callable(task), metric);
  }

  public <R> R time(Callable<R> task, T metric) {
    try {
      long start = getNanoTime();
      R result = task.call();
      long stop = getNanoTime();
      runtimes.put(metric, stop - start);
      return result;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public long milli(T metric) {
    return milli(nanos(metric));
  }

  public static long milli(long nanos) {
    return TimeUnit.NANOSECONDS.toMillis(nanos);
  }

  public long nanos(T metric) {
    return runtimes.computeIfAbsent(metric, x -> getNanoTime());
  }
}
