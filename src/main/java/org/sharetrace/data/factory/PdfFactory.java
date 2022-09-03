package org.sharetrace.data.factory;

import org.apache.commons.math3.distribution.RealDistribution;

@FunctionalInterface
public interface PdfFactory {

  RealDistribution getPdf(long seed);
}
