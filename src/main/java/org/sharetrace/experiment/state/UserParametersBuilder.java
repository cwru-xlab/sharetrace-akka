package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.message.UserParameters;

public interface UserParametersBuilder extends FinalBuilder {

  FinalBuilder userParameters(UserParameters parameters);

  FinalBuilder userParameters(Function<UserParametersContext, UserParameters> factory);
}
