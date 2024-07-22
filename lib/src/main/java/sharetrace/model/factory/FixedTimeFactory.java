package sharetrace.model.factory;

import java.time.Instant;

@SuppressWarnings("ClassCanBeRecord")
public final class FixedTimeFactory implements TimeFactory {

  private final long time;

  public FixedTimeFactory(long time) {
    this.time = time;
  }

  public static FixedTimeFactory from(Instant instant) {
    return new FixedTimeFactory(instant.toEpochMilli());
  }

  @Override
  public long getTime() {
    return time;
  }

  @Override
  public String type() {
    return "Fixed";
  }
}
