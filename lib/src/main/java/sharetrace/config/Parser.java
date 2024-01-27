package sharetrace.config;

@FunctionalInterface
public interface Parser<I, O> {

  O parse(I input);
}
