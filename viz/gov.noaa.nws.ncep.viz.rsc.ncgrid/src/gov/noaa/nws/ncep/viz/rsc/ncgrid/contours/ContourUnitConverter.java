package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import javax.measure.UnitConverter;

import tec.uom.se.AbstractConverter;

/**
 * ContourUnitConverter
 * 
 * provides process to convert intervals from ContourSupport, when rendering
 * image contour fills.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/16/2015   R13016     mkean       Initial creation.
 * Apr 29, 2019 7596       lsingh      Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author mkean
 */

public class ContourUnitConverter extends AbstractConverter {

    private static final long serialVersionUID = 1L;

    private double[] xVals;

    private double[] yVals;

    public ContourUnitConverter(double[] xVals, double[] yVals) {
        this.xVals = xVals;
        this.yVals = yVals;
    }

    @Override
    public double convert(double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }
        if (Double.isInfinite(x)) {
            return x;
        }
        double increment = (yVals[yVals.length - 1] - yVals[0])
                / ((double) yVals.length - 1);
        int index = (int) Math.round((x - yVals[0]) / increment);

        return yVals[index];
    }

    @Override
    public AbstractConverter inverse() {
        return new ContourUnitConverter(yVals, xVals);
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    @Override
    public UnitConverter concatenate(UnitConverter converter) {
        // TODO Auto-generated method stub
        UnitConverter result = super.concatenate(converter);
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Float.floatToIntBits((float)convert(1.0));
        result = prime * result + Arrays.hashCode(xVals);
        result = prime * result + Arrays.hashCode(yVals);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        final ContourUnitConverter other = (ContourUnitConverter) obj;
        if (!Arrays.equals(xVals, other.xVals))
            return false;
        if (!Arrays.equals(yVals, other.yVals))
            return false;
        return true;
    }

    @Override
    public BigDecimal convert(BigDecimal value, MathContext ctx)
            throws ArithmeticException {
        throw new UnsupportedOperationException();
    }
}
