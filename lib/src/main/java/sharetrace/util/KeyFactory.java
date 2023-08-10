package sharetrace.util;

import java.util.concurrent.atomic.AtomicLong;

@FunctionalInterface
public interface KeyFactory {

  static KeyFactory autoIncrementing() {
    var key = new AtomicLong(0);
    return () -> String.valueOf(key.getAndIncrement());
  }

  String getKey();
}
