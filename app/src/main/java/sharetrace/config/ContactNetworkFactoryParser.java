package sharetrace.config;

import com.typesafe.config.Config;
import java.nio.file.Path;
import sharetrace.graph.BarabasiAlbertContactNetworkFactoryBuilder;
import sharetrace.graph.ContactNetworkFactory;
import sharetrace.graph.FileContactNetworkFactoryBuilder;
import sharetrace.graph.GnmRandomContactNetworkFactoryBuilder;
import sharetrace.graph.RandomRegularContactNetworkFactoryBuilder;
import sharetrace.graph.ScaleFreeContactNetworkFactoryBuilder;
import sharetrace.graph.WattsStrogatzContactNetworkFactoryBuilder;
import sharetrace.model.Context;
import sharetrace.model.factory.TimeFactory;

public record ContactNetworkFactoryParser(
    Context context, ConfigParser<TimeFactory> timeFactoryParser)
    implements ConfigParser<ContactNetworkFactory> {

  @Override
  public ContactNetworkFactory parse(Config config) {
    var type = config.getString("type");
    return switch (type) {
      case ("gnm-random") -> gnmRandomFactory(config);
      case ("random-regular") -> randomRegularFactory(config);
      case ("barabasi-albert") -> barabasiAlbertFactory(config);
      case ("watts-strogatz") -> wattsStrogatzFactory(config);
      case ("scale-free") -> scaleFreeFactory(config);
      case ("file") -> fileFactory(config);
      default -> throw new IllegalArgumentException(type);
    };
  }

  private ContactNetworkFactory gnmRandomFactory(Config config) {
    return GnmRandomContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .edges(config.getInt("edges"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory randomRegularFactory(Config config) {
    return RandomRegularContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .degree(config.getInt("degree"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory barabasiAlbertFactory(Config config) {
    return BarabasiAlbertContactNetworkFactoryBuilder.create()
        .initialNodes(config.getInt("initial-nodes"))
        .newEdges(config.getInt("new-edges"))
        .nodes(config.getInt("nodes"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory wattsStrogatzFactory(Config config) {
    return WattsStrogatzContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .nearestNeighbors(config.getInt("nearest-neighbors"))
        .rewiringProbability(config.getDouble("rewiring-probability"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory scaleFreeFactory(Config config) {
    return ScaleFreeContactNetworkFactoryBuilder.create()
        .nodes(config.getInt("nodes"))
        .timeFactory(timeFactory(config))
        .randomGenerator(context.randomGenerator())
        .build();
  }

  private ContactNetworkFactory fileFactory(Config config) {
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
