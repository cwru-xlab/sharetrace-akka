package sharetrace.config;

import com.typesafe.config.Config;
import java.nio.file.Path;
import sharetrace.graph.BarabasiAlbertContactNetworkFactoryBuilder;
import sharetrace.graph.FileContactNetworkFactoryBuilder;
import sharetrace.graph.GnmRandomContactNetworkFactoryBuilder;
import sharetrace.graph.RandomRegularContactNetworkFactoryBuilder;
import sharetrace.graph.ScaleFreeContactNetworkFactoryBuilder;
import sharetrace.graph.WattsStrogatzContactNetworkFactoryBuilder;
import sharetrace.model.Context;
import sharetrace.model.factory.CachedContactNetworkFactory;
import sharetrace.model.factory.ContactNetworkFactory;
import sharetrace.model.factory.TimeFactory;

public record ContactNetworkFactoryParser(
    Context context, ConfigParser<TimeFactory> timeFactoryParser)
    implements ConfigParser<ContactNetworkFactory> {

  @Override
  public ContactNetworkFactory parse(Config config) {
    return decorated(baseFactory(config), config);
  }

  private ContactNetworkFactory baseFactory(Config config) {
    var type = config.getString("type");
    return switch (type) {
      case ("gnm-random") -> gnmRandom(config);
      case ("random-regular") -> randomRegular(config);
      case ("barabasi-albert") -> barabasiAlbert(config);
      case ("watts-strogatz") -> wattsStrogatz(config);
      case ("scale-free") -> scaleFree(config);
      case ("file") -> file(config);
      default -> throw new IllegalArgumentException(type);
    };
  }

  private ContactNetworkFactory decorated(ContactNetworkFactory factory, Config config) {
    return config.getBoolean("cached") ? new CachedContactNetworkFactory(factory) : factory;
  }

  private ContactNetworkFactory gnmRandom(Config config) {
    return GnmRandomContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .edges(config.getInt("edges"))
        .loops(false)
        .multipleEdges(false)
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory randomRegular(Config config) {
    return RandomRegularContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .degree(config.getInt("degree"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory barabasiAlbert(Config config) {
    return BarabasiAlbertContactNetworkFactoryBuilder.create()
        .initialNodes(config.getInt("initial-nodes"))
        .newEdges(config.getInt("new-edges"))
        .nodes(config.getInt("nodes"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory wattsStrogatz(Config config) {
    return WattsStrogatzContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .nearestNeighbors(config.getInt("nearest-neighbors"))
        .rewiringProbability(config.getDouble("rewiring-probability"))
        .addInsteadOfRewire(false)
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory scaleFree(Config config) {
    return ScaleFreeContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory file(Config config) {
    return FileContactNetworkFactoryBuilder.create()
        .path(Path.of(config.getString("path")))
        .delimiter(config.getString("delimiter"))
        .referenceTime(context.referenceTime())
        .build();
  }

  private TimeFactory timeFactory(Config config) {
    return timeFactoryParser.parse(config.getConfig("time-factory"));
  }
}
