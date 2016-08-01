package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import java.util.Arrays;

import javax.measure.converter.ConversionException;
import javax.measure.converter.UnitConverter;

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
 * 
 * </pre>
 * 
 * @author mkean
 * @version 1.0
 */

public class ContourUnitConverter extends UnitConverter {

    private static final long serialVersionUID = 1L;

    private double[] xVals;

    private double[] yVals;

    public ContourUnitConverter(double[] xVals, double[] yVals) {
        this.xVals = xVals;
        this.yVals = yVals;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.measure.converter.UnitConverter#convert(double)
     */
    @Override
    public double convert(double x) throws ConversionException {
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

    /*
     * (non-Javadoc)
     * 
     * @see javax.measure.converter.UnitConverter#inverse()
     */
    @Override
    public UnitConverter inverse() {
        return new ContourUnitConverter(yVals, xVals);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.measure.converter.UnitConverter#isLinear()
     */
    @Override
    public boolean isLinear() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.measure.converter.UnitConverter#concatenate(javax.measure.converter
     * .UnitConverter)
     */
    @Override
    public UnitConverter concatenate(UnitConverter converter) {
        // TODO Auto-generated method stub
        UnitConverter result = super.concatenate(converter);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(xVals);
        result = prime * result + Arrays.hashCode(yVals);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ContourUnitConverter other = (ContourUnitConverter) obj;
        if (!Arrays.equals(xVals, other.xVals))
            return false;
        if (!Arrays.equals(yVals, other.yVals))
            return false;
        return true;
    }
}
