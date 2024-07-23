package sharetrace.app;

import sharetrace.model.Context;
import sharetrace.model.Parameters;

public interface Runner {

  void run(Parameters parameters, Context context);
}
