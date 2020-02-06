package gov.noaa.nws.ncep.viz.rsc.satellite.units;

import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.commons.lang.builder.HashCodeBuilder;

import tec.uom.se.AbstractConverter;

/**
 * Converts a temperature value in Kelvin to a pixel value from 0 to 255 using
 * NMAP's equation
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/25                    ghull     Initial creation
 * 06/07         #          archana   Updated the convert() method to 
 *                                    match legacy imttob.f   
 * Apr 29, 2019  7596       lsingh    Updated units framework to JSR-363.
 * </pre>
 * 
 * @author ghull
 */
public class NcIRTempToPixelConverter extends AbstractConverter {

	private static final long serialVersionUID = 1L;

	@Override
	public double convert(double aTemperature) {
		double result = Double.NaN;

//		if (aTemperature < 238.15) {
//			result = 418.15 - aTemperature;
//		} else {
//			result = 656.3 - (2.0 * aTemperature);
//		}
//
//		if (result < 0) {
//			result = 0.0;
//		} else if (result > 255) {
//			result = 255.0;
//		}

		if ( aTemperature < 163 || aTemperature > 330)
			return result;
		else if ( aTemperature <= 242 ){
			result = 418 - aTemperature; 
		}else
		     result = 2 * ( 330 - aTemperature );
		return result;
	}

	@Override
	public boolean equals(Object aConverter) {
		return (aConverter instanceof NcIRTempToPixelConverter);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public AbstractConverter inverse() {
		return new NcIRPixelToTempConverter();
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
