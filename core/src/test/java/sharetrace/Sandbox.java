package sharetrace;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import sharetrace.util.cache.CacheParameters;
import sharetrace.util.cache.IntervalCache;

public class Sandbox {

  public static void main(String[] args) throws IOException {

    IntervalCache<Integer> cache =
        IntervalCache.create(
            CacheParameters.<Integer>builder()
                .mergeStrategy((v1, v2) -> v2)
                .clock(Clock.systemUTC())
                .interval(Duration.ofSeconds(20))
                .forwardIntervals(3)
                .intervals(5)
                .refreshPeriod(Duration.ofSeconds(1))
                .build());
    cache.put(Instant.now(), 3);
    cache.put(Instant.now(), 2);
    System.out.println(cache.max(Instant.now()));
  }
}
