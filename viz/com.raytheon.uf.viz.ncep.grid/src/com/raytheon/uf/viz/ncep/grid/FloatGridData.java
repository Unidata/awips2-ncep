package com.raytheon.uf.viz.ncep.grid;

import java.nio.FloatBuffer;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Holds scaler and vector model grids and dimension information. An attempt is
 * made to store the grids in direct memory to save heap space.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer        Description
 * ------------ ----------  -----------     --------------------------
 * Apr 06, 2010 164         M. Li          Modified from NcFloatDataRecord
 * Jun 15, 2010 164         mgamazaychikov Cleaned up, improved reduce method,
 *                                         implemented cloneInternal, getSizeInBytes
 * Apr 25, 2016 R17741      S. Gilbert     Modified from NcFloatDataRecord
 * Sep 05, 2018 54480       mapeters       Support DynamicSerialize
 * Nov 27, 2018 54476       tjensen        Removed allocation of direct memory
 * </pre>
 *
 * @author mli
 */
@DynamicSerialize
public class FloatGridData {

    @DynamicSerializeElement
    private FloatBuffer xdata;

    @DynamicSerializeElement
    private FloatBuffer ydata;

    @DynamicSerializeElement
    private boolean vector = false;

    @DynamicSerializeElement
    private int dimension;

    @DynamicSerializeElement
    private long[] sizes;

    public FloatGridData() {

    }

    /**
     * @return the reference to scaler grid or u-component grid, if vector is
     *         true
     */
    public FloatBuffer getXdata() {
        return xdata;
    }

    /**
     * @param xdata
     *            the xdata to set
     */
    public void setXdata(FloatBuffer xdata) {
        this.xdata = xdata;
    }

    /**
     * @param aFloatData
     *            saves a float grid array into a FloatBuffer
     */
    public void setXdata(float[] aFloatData) {
        setXdata(toFloatBuffer(aFloatData));
    }

    /**
     * @return the reference to the v-component grid, if vector is true
     */
    public FloatBuffer getYdata() {
        return ydata;
    }

    /**
     * @param ydata
     *            the ydata to set
     */
    public void setYdata(FloatBuffer ydata) {
        this.ydata = ydata;
    }

    /**
     * @param aFloatData
     *            saves the v-component float grid array into a FloatBuffer
     */
    public void setYdata(float[] aFloatData) {
        setYdata(toFloatBuffer(aFloatData));
    }

    /**
     * @return the dimension of the grid data
     */
    public int getDimension() {
        return dimension;
    }

    /**
     *
     * @param dimension
     *            of grid data (usually 2)
     */
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    /**
     *
     * @return Number of grid points in each dimension
     */
    public long[] getSizes() {
        return sizes;
    }

    /**
     *
     * @param ls
     *            Number of grid points in each dimension
     */
    public void setSizes(long[] ls) {
        this.sizes = ls;
    }

    /**
     *
     * @return the scare grid (or u-component grid) as a float array
     */
    public float[] getXdataAsArray() {

        if (xdata.hasArray()) {
            return xdata.array();
        }

        float[] array = new float[xdata.capacity()];
        xdata.position(0);
        xdata.get(array);
        return array;
    }

    /**
     * Converts a float array to a {@link FloatBuffer}
     *
     * @param aFloatData
     *            the float array to convert
     * @return the converted {@link FloatBuffer}
     */
    private FloatBuffer toFloatBuffer(float[] aFloatData) {
        return FloatBuffer.wrap(aFloatData);
    }

    public boolean validateDataSet() {

        long size = 1;

        for (int i = 0; i < this.dimension; i++) {
            size *= this.sizes[i];
        }

        if (size == this.xdata.capacity()) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @return true if, u-component and v-component grids are stored otherwise,
     *         1 scaler grid is stored
     */
    public boolean isVector() {
        return vector;
    }

    /**
     * Are grids vector grids?
     *
     * @param vector
     */
    public void setVector(boolean vector) {
        this.vector = vector;
    }

}