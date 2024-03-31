package sharetrace.logging;

import sharetrace.Buildable;
import sharetrace.model.Context;
import sharetrace.model.Parameters;
import sharetrace.model.factory.ContactNetworkFactory;
import sharetrace.model.factory.KeyFactory;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.graph.ContactNetwork;

@Buildable
public record ExecutionProperties(
    Context context,
    Parameters parameters,
    RiskScoreFactory scoreFactory,
    ContactNetworkFactory networkFactory,
    ContactNetwork network,
    KeyFactory keyFactory)
    implements LogRecord {}
