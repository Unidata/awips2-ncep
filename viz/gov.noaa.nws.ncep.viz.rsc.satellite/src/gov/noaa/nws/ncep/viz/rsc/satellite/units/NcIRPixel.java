package gov.noaa.nws.ncep.viz.rsc.satellite.units;

import java.util.Map;

import javax.measure.Dimension;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Temperature;

import org.apache.commons.lang.builder.HashCodeBuilder;

import si.uom.SI;
import tec.uom.se.AbstractUnit;
import tec.uom.se.quantity.QuantityDimension;

/**
 * Represents a pixel value on a satellite IR image.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/25/10                 ghull      Initial creation
 * Apr 29, 2019  7596       lsingh     Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author ghull
 */
public class NcIRPixel extends AbstractUnit<Temperature> {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object anObject) {
        return (anObject instanceof NcIRPixel);
    }

    @Override
    public Unit<Temperature> toSystemUnit() {
        return SI.KELVIN;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public UnitConverter getSystemConverter() {
        return new NcIRPixelToTempConverter();
    }

    @Override
    public Map<? extends Unit<?>, Integer> getBaseUnits() {
        return null;
    }

    @Override
    public Dimension getDimension() {
        return QuantityDimension.TEMPERATURE;
    }

}
