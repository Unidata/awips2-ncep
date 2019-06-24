package gov.noaa.nws.ncep.viz.rsc.satellite.units;


import java.util.Map;

import javax.measure.Dimension;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Dimensionless;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.raytheon.uf.common.dataplugin.satellite.units.generic.GenericFromPixelConverter;

import tec.uom.se.AbstractUnit;
import tec.uom.se.quantity.QuantityDimension;

/**
 * Used to represent the McIdas BRIT Calibration type.  Its the same unit as 
 * com.raytheon.uf.common.dataplugin.satellite.units.generic.GenericPixel.  Even though this Unit
 * is exactly the same as GenericPixel, it was created so that valid pixel value range could be 
 * recognized as 0 - 255.  GenericPixel values are treated as signed values that range from
 * -128 to 127 by the com.raytheon.viz.satellite.rsc.SatFileBasedTileSet.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         sgilbert    Initial creation
 * Apr 15, 2019 7596       lsingh      Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 *
 * @author sgilbert
 */

public class McidasBritPixel extends AbstractUnit<Dimensionless> {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean equals(Object anObject) {
		return (anObject instanceof McidasBritPixel);
	}

	@Override
	public Unit<Dimensionless> toSystemUnit() {
		return AbstractUnit.ONE;
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public UnitConverter getSystemConverter() {
		return new GenericFromPixelConverter();
	}

    @Override
    public Map<? extends Unit<?>, Integer> getBaseUnits() {
        return null;
    }

    @Override
    public Dimension getDimension() {
        return QuantityDimension.NONE;
    }
}
