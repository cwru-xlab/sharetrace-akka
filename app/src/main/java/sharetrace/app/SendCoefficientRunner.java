package sharetrace.app;

import sharetrace.config.AppConfig;
import sharetrace.model.Parameters;
import sharetrace.model.ParametersBuilder;

public final class SendCoefficientRunner extends AbstractParameterRunner<Double> {

  @Override
  protected Iterable<Double> parameterValues(AppConfig config) {
    return config.getSendCoefficients();
  }

  @Override
  protected Parameters updateParameters(Parameters parameters, Double value) {
    return ParametersBuilder.from(parameters).withSendCoefficient(value);
  }
}
