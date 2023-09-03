package sharetrace.analysis.results;

public interface Results {

  Results put(String key, Object result);

  Results withScope(String scope);
}
