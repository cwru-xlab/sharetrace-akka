package sharetrace.app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import sharetrace.config.ClassFactory;
import sharetrace.config.ContextParser;
import sharetrace.config.ParametersParser;
import sharetrace.model.Context;
import sharetrace.model.Parameters;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    var config = getConfig();
    var runner = getRunner(config);
    var parameters = getParameters(config);
    var context = getContext(config);
    runner.run(parameters, context);
  }

  private static Config getConfig() {
    return ConfigFactory.load().getConfig("sharetrace");
  }

  private static Runner getRunner(Config config) {
    return ClassFactory.getInstance(Runner.class, config.getString("runner.type"));
  }

  private static Parameters getParameters(Config config) {
    return new ParametersParser().parse(config.getConfig("parameters"));
  }

  private static Context getContext(Config config) {
    return new ContextParser(config.getConfig("runner")).parse(config.getConfig("context"));
  }
}
