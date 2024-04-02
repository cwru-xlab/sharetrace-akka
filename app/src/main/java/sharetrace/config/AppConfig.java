package sharetrace.config;

import com.typesafe.config.Config;
import java.util.List;
import sharetrace.Buildable;
import sharetrace.model.Context;
import sharetrace.model.Parameters;
import sharetrace.model.factory.ContactNetworkFactory;
import sharetrace.model.factory.KeyFactory;
import sharetrace.model.factory.RiskScoreFactory;

@Buildable
public record AppConfig(
    Config config,
    ConfigParser<RiskScoreFactory> scoreFactoryParser,
    ConfigParser<ContactNetworkFactory> networkFactoryParser,
    ConfigParser<KeyFactory> keyFactoryParser) {

  public static AppConfig of(Parameters parameters, Context context) {
    var randomParser = new DistributedRandomParser(context.randomGenerator());
    var timeFactoryParser = new TimeFactoryParser(context, randomParser);
    return AppConfigBuilder.create()
        .config(context.config())
        .scoreFactoryParser(new RiskScoreFactoryParser(parameters, randomParser, timeFactoryParser))
        .networkFactoryParser(new ContactNetworkFactoryParser(context, timeFactoryParser))
        .keyFactoryParser(new KeyFactoryParser())
        .build();
  }

  public int getIterations() {
    return config.getInt("iterations");
  }

  public int getRepeats() {
    return config.getInt("repeats");
  }

  public List<Double> getSendCoefficients() {
    return config.getDoubleList("send-coefficients");
  }

  public RiskScoreFactory getScoreFactory() {
    return scoreFactoryParser.parse(config.getConfig("score-factory"));
  }

  public ContactNetworkFactory getNetworkFactory() {
    return networkFactoryParser.parse(config.getConfig("network-factory"));
  }

  public KeyFactory getKeyFactory() {
    return keyFactoryParser.parse(config.getConfig("key-factory"));
  }
}
