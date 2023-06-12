package sharetrace.experiment;

public interface Experiment<K> {

  void run(ExperimentState<K> state);
}
