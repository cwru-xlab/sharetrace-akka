package sharetrace.analysis.handler;

import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;

public interface EventHandler {

  void onNext(EventRecord record, Context context);

  void onComplete(Results results, Context context);
}
