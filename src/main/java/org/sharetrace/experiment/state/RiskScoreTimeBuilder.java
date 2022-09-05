package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.data.factory.PdfFactory;

public interface RiskScoreTimeBuilder extends ContactTimeBuilder {

  ContactTimeBuilder riskScoreTimePdfFactory(PdfFactory pdfFactory);

  ContactTimeBuilder riskScoreTimePdfFactory(Function<PdfFactoryContext, PdfFactory> factory);
}
