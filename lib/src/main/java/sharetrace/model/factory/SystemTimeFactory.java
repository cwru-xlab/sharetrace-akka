package sharetrace.model.factory;

public final class SystemTimeFactory implements TimeFactory {

  @Override
  public long getTime() {
    return System.currentTimeMillis();
  }

  @Override
  public String type() {
    return "System";
  }
}
