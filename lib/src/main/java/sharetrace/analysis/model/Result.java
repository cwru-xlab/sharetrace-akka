package sharetrace.analysis.model;

public interface Result<T> {

  default String key() {
    return getClass().getSimpleName();
  }

  T value();
}
