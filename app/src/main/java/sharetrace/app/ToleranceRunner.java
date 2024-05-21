package sharetrace.app;

import sharetrace.config.AppConfig;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public class ToleranceRunner extends AbstractParameterRunner<Double> {

  @Override
  protected Iterable<Double> parameterValues(AppConfig config) {
    return config.getTolerances();
  }

  @Override
  protected Parameters updateParameters(Parameters parameters, Double value) {
    return ParametersBuilder.from(parameters).withTolerance(value);
  }
}
