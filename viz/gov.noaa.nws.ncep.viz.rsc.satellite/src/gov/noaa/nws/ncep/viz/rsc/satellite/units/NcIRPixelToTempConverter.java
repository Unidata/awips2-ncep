package gov.noaa.nws.ncep.viz.rsc.satellite.units;

import javax.measure.UnitConverter;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import tech.units.indriya.function.AbstractConverter;

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
 * Aug 05, 2022  8905      lsingh      Updated units framework to 2.0.2.
 *                                     Renamed methods, and overrided additional methods.
 * 
 * </pre>
 * 
 * @author gull
 */
public class NcIRPixelToTempConverter extends AbstractConverter {

	private static final long serialVersionUID = 1L;

	@Override
	public Number convertWhenNotIdentity(Number pixel) {
		double result = 0.0;
		double aPixel = pixel.doubleValue();

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
	public AbstractConverter inverseWhenNotIdentity() {
		return null; // new NcIRTempToPixelConverter();
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
