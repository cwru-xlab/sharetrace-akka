package sharetrace.app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import sharetrace.config.ContextParser;
import sharetrace.config.InstanceFactory;
import sharetrace.config.ParametersParser;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

public record Main() {

  public static void main(String[] args) {
    var config = ConfigFactory.load().getConfig("sharetrace");
    var runner = getRunner(config);
    var parameters = getParameters(config);
    var context = getContext(config);
    runner.run(parameters, context);
  }

  private static Runner getRunner(Config config) {
    var classPath = config.getString("runner.type");
    return InstanceFactory.getInstance(classPath);
  }

  private static Parameters getParameters(Config config) {
    var parameters = config.getConfig("parameters");
    return new ParametersParser().parse(parameters);
  }

  private static Context getContext(Config config) {
    var runner = config.getConfig("runner");
    var context = config.getConfig("context");
    return new ContextParser(runner).parse(context);
  }
}
