package sharetrace.app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import sharetrace.config.ContextParser;
import sharetrace.config.InstanceParser;
import sharetrace.config.ParametersParser;
import sharetrace.model.Parameters;
import sharetrace.util.Context;

public record Main() {

  public static void main(String[] args) {
    var config = config();
    runner(config).run(parameters(config), context(config));
  }

  private static Config config() {
    return ConfigFactory.load().getConfig("sharetrace");
  }

  private static Runner runner(Config config) {
    return new InstanceParser<Runner>("runner.type").parse(config);
  }

  private static Parameters parameters(Config config) {
    return new ParametersParser().parse(config.getConfig("parameters"));
  }

  private static Context context(Config config) {
    return new ContextParser(config.getConfig("runner")).parse(config.getConfig("context"));
  }
}
