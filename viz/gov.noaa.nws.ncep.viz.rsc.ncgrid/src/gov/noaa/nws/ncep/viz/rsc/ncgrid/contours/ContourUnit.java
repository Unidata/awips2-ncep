package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import java.util.Arrays;
import java.util.Map;

import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;

import tec.uom.se.AbstractUnit;

/**
 * ContourUnit
 * 
 * custom Units used for converting intervals from ContourSupport, (when
 * rendering image contour fills).
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/16/2015   R13016     mkean       Initial creation.
 * 04/15/2019   7596       lsingh      Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author mkean
 * @version 1.0
 */

public class ContourUnit<Q extends Quantity<Q>> extends AbstractUnit<Q> {

    private final AbstractUnit<Q> stdUnit;

    private final double[] pixelValues;

    private final double[] stdValues;

    @SuppressWarnings("unchecked")
    public ContourUnit(AbstractUnit<Q> dispUnit, double[] pixelValues,
            double[] dispValues) {
        super();
        if (pixelValues.length != dispValues.length) {
            throw new IllegalArgumentException(
                    "pixelValues and stdValues arrays must be of equal length");
        }
        this.pixelValues = pixelValues;

        if (!dispUnit.getSystemConverter().isLinear()) {
            stdUnit = dispUnit;
        } else {
            this.stdUnit = (AbstractUnit<Q>) dispUnit.getSystemUnit();
        }

        UnitConverter toStd = dispUnit.getConverterTo(stdUnit);

        this.stdValues = new double[dispValues.length];
        for (int i = 0; i < dispValues.length; i++) {
            stdValues[i] = toStd.convert(dispValues[i]);
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public Unit<Q> toSystemUnit() {
        return (Unit<Q>) stdUnit.getSystemUnit();
    }

    @Override
    public UnitConverter getSystemConverter() {
        if (!stdUnit.getSystemConverter().isLinear()) {
            return stdUnit.getSystemConverter().concatenate(
                    new ContourUnitConverter(pixelValues, stdValues));
        } else {
            return new ContourUnitConverter(pixelValues, stdValues);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(pixelValues);
        result = prime * result + ((stdUnit == null) ? 0 : stdUnit.hashCode());
        result = prime * result + Arrays.hashCode(stdValues);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ContourUnit<Q> other = (ContourUnit<Q>) obj;
        if (!Arrays.equals(pixelValues, other.pixelValues))
            return false;
        if (stdUnit == null) {
            if (other.stdUnit != null)
                return false;
        } else if (!stdUnit.equals(other.stdUnit))
            return false;
        if (!Arrays.equals(stdValues, other.stdValues))
            return false;
        return true;
    }

    @Override
    public Map<? extends Unit<?>, Integer> getBaseUnits() {
        return stdUnit.getBaseUnits();
    }

    @Override
    public Dimension getDimension() {
        return stdUnit.getDimension();
    }
}
