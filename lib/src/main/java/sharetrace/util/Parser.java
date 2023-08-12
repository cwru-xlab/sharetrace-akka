package sharetrace.util;

@FunctionalInterface
public interface Parser<I, O> {

  O parse(I input);
}
