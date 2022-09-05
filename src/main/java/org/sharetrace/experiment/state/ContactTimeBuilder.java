package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.data.factory.PdfFactory;

public interface ContactTimeBuilder extends DatasetBuilder {

  DatasetBuilder contactTimePdfFactory(PdfFactory pdfFactory);

  DatasetBuilder contactTimePdfFactory(Function<PdfFactoryContext, PdfFactory> factory);
}
