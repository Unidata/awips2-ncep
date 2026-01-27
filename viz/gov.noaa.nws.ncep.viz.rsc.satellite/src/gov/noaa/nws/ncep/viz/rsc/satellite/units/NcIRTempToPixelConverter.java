package gov.noaa.nws.ncep.viz.rsc.satellite.units;

import javax.measure.UnitConverter;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import tech.units.indriya.function.AbstractConverter;

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
 * Aug 05, 2022  8905       lsingh    Updated units framework to 2.0.2.
 *                                    Renamed methods, and overrided additional methods.
 * </pre>
 * 
 * @author ghull
 */
public class NcIRTempToPixelConverter extends AbstractConverter {

	private static final long serialVersionUID = 1L;

	@Override
	public Number convertWhenNotIdentity(Number temperature) {
		double result = Double.NaN;
        double aTemperature = temperature.doubleValue();

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
	public AbstractConverter inverseWhenNotIdentity() {
		return new NcIRPixelToTempConverter();
	}

	@Override
	public boolean isLinear() {
		return false;
	}
	
    @Override
    public boolean isIdentity() {
        return false;
    }

    @Override
    public int compareTo(UnitConverter o) {
     // This method hasn't been implemented yet since it's unused
        return 0;
    }

    @Override
    protected String transformationLiteral() {
     // This method hasn't been implemented yet since it's unused
        return null;
    }

    @Override
    protected boolean canReduceWith(AbstractConverter that) {
     // This method hasn't been implemented yet since it's unused
        return false;
    }

}
