package sharetrace.app;

import sharetrace.model.Parameters;
import sharetrace.util.Context;

@FunctionalInterface
public interface Runner {

  void run(Parameters parameters, Context context);
}
