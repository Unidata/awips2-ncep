package gov.noaa.nws.ncep.viz.rsc.ncgrid;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Holds scaler and vector model grids and dimension information. An attempt is
 * made to store the grids in direct memory to save heap space.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    	Description
 * ------------ ----------  ----------- 	--------------------------
 * Apr 6, 2010  164          M. Li      	Modified from NcFloatDataRecord
 * Jun15, 2010	164			mgamazaychikov	Cleaned up, improved reduce method,
 * 											implemented cloneInternal, getSizeInBytes
 * Apr 25,2016  R17741       S. Gilbert     Modified from NcFloatDataRecord
 * </pre>
 * 
 * @author mli
 * @version 1
 */
public class FloatGridData {

    private FloatBuffer xdata;

    private FloatBuffer ydata;

    private boolean vector = false;

    private int dimension;

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
     *            saves a float grid array into a FloatBuffer
     */
    public void setXdata(float[] aFloatData) {
        this.xdata = toFloatBuffer(aFloatData);
    }

    /**
     * 
     * @return the reference to the v-component grid, if vector is true
     */
    public FloatBuffer getYdata() {
        return ydata;
    }

    /**
     * @param ydata
     *            saves the v-component float grid array into a FloatBuffer
     */
    public void setYdata(float[] aFloatData) {
        this.ydata = toFloatBuffer(aFloatData);
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

    /*
     * Converts a float array to a FloatBuffer
     */
    private FloatBuffer toFloatBuffer(float[] aFloatData) {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * aFloatData.length);
        FloatBuffer fb = bb.asFloatBuffer();
        fb.position(0);
        fb.put(aFloatData);
        return fb;
    }

    /*
     * (non-Javadoc)
     */
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
     * 
     * @return true if, u-componenet and v-component grids are stored otherwise,
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