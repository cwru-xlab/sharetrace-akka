package sharetrace.config;

import com.typesafe.config.Config;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.List;
import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.graph.ContactNetworkFactory;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.util.Context;
import sharetrace.util.DistributedRandom;

@Buildable
public record Parsed(
    Config config,
    ConfigParser<DistributedRandom> randomParser,
    ConfigParser<RiskScoreFactory> scoreFactoryParser,
    ConfigParser<ContactNetworkFactory> networkFactoryParser) {

  public static Parsed of(Parameters parameters, Context context) {
    var randomParser = new DistributedRandomParser(context.randomGenerator());
    var timeFactoryParser = new TimeFactoryParser(context, randomParser);
    return ParsedBuilder.create()
        .config(context.config())
        .randomParser(randomParser)
        .scoreFactoryParser(new RiskScoreFactoryParser(parameters, randomParser, timeFactoryParser))
        .networkFactoryParser(new ContactNetworkFactoryParser(context, timeFactoryParser))
        .build();
  }

  public int iterations() {
    return config.getInt("iterations");
  }

  public int networks() {
    return config.getInt("networks");
  }

  public List<Float> transmissionRates() {
    return toFloats(config.getDoubleList("transmission-rates"));
  }

  public List<Float> sendCoefficients() {
    return toFloats(config.getDoubleList("send-coefficients"));
  }

  public RiskScoreFactory scoreFactory() {
    return scoreFactoryParser.parse(config.getConfig("score-factory"));
  }

  public ContactNetwork network() {
    return networkFactory().getContactNetwork();
  }

  public ContactNetworkFactory networkFactory() {
    return networkFactoryParser.parse(config.getConfig("network-factory"));
  }

  public List<ContactNetworkFactory> networkFactories() {
    return config.getConfigList("network-factories").stream()
        .map(networkFactoryParser::parse)
        .toList();
  }

  public List<DistributedRandom> randoms() {
    return config.getConfigList("distributions").stream().map(randomParser::parse).toList();
  }

  private List<Float> toFloats(List<Double> doubles) {
    var floats = new FloatArrayList(doubles.size());
    for (var d : doubles) {
      floats.add((float) d.doubleValue());
    }
    return floats;
  }
}
