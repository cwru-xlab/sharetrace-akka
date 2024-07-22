package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
public final class AutoIncrementingKeyFactory implements KeyFactory {

  private final long initialValue;
  private final AtomicLong value;

  public AutoIncrementingKeyFactory(long initialValue) {
    this.initialValue = initialValue;
    this.value = new AtomicLong(initialValue);
  }

  @Override
  public String getKey() {
    return String.valueOf(value.getAndIncrement());
  }

  @Override
  public String type() {
    return "AutoIncrementing";
  }

  @JsonProperty
  public long initialValue() {
    return initialValue;
  }
}
