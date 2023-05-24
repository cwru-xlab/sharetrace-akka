package io.sharetrace.experiment.state;

import io.sharetrace.model.UserParameters;

public interface DatasetContext extends UserParametersContext {

  UserParameters userParameters();
}
