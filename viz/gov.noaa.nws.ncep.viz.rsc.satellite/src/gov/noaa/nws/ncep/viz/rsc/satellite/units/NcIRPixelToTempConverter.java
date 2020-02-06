package gov.noaa.nws.ncep.viz.rsc.satellite.units;

import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.commons.lang.builder.HashCodeBuilder;

import tec.uom.se.AbstractConverter;

/**
 * Converts a pixel value from 0-255 into a temperature in Kelvin using NMAP's
 * equation.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/25/10                ghull       Initial creation
 * Apr 29, 2019  7596      lsingh      Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author gull
 */
public class NcIRPixelToTempConverter extends AbstractConverter {

	private static final long serialVersionUID = 1L;

	@Override
	public double convert(double aPixel) {
		double result = 0.0;

		if (aPixel >= 176) {
			result = 418 - aPixel;
		} else {
			result = 330 - (aPixel / 2.0);
		}

		return result;
	}

	@Override
	public boolean equals(Object aConverter) {
		return (aConverter instanceof NcIRPixelToTempConverter);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public AbstractConverter inverse() {
		return null; // new NcIRTempToPixelConverter();
	}

	@Override
	public boolean isLinear() {
		return false;
	}

    @Override
    public BigDecimal convert(BigDecimal value, MathContext ctx)
            throws ArithmeticException {
        throw new UnsupportedOperationException();
    }

}
