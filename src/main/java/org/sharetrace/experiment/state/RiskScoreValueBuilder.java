package org.sharetrace.experiment.state;

import java.util.function.Function;
import org.sharetrace.data.factory.PdfFactory;

public interface RiskScoreValueBuilder extends RiskScoreTimeBuilder {

  RiskScoreTimeBuilder riskScoreValuePdfFactory(PdfFactory pdfFactory);

  RiskScoreTimeBuilder riskScoreValuePdfFactory(Function<PdfFactoryContext, PdfFactory> factory);
}
