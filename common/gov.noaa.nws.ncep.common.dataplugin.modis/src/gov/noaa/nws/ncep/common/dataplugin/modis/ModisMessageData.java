package gov.noaa.nws.ncep.common.dataplugin.modis;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * MODIS Message Data object
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation
 * *
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
@DynamicSerialize
public class ModisMessageData {

    @DynamicSerializeElement
    private Object rawData;

    @DynamicSerializeElement
    private Object latitudes;

    @DynamicSerializeElement
    private Object longitudes;

    @DynamicSerializeElement
    private float scale;

    @DynamicSerializeElement
    private float offset;

    @DynamicSerializeElement
    private float[] missingValues;

    @DynamicSerializeElement
    private String unitString;

    /**
     * @return the rawData
     */
    public Object getRawData() {
        return rawData;
    }

    /**
     * @param rawData
     *            the rawData to set
     */
    public void setRawData(Object rawData) {
        this.rawData = rawData;
    }

    public Object getLatitudes() {
        return latitudes;
    }

    public void setLatitudes(Object latitudes) {
        this.latitudes = latitudes;
    }

    public Object getLongitudes() {
        return longitudes;
    }

    public void setLongitudes(Object longitudes) {
        this.longitudes = longitudes;
    }

    /**
     * @return the scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * @param scale
     *            the scale to set
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * @return the offset
     */
    public float getOffset() {
        return offset;
    }

    /**
     * @param offset
     *            the offset to set
     */
    public void setOffset(float offset) {
        this.offset = offset;
    }

    /**
     * @return the missingValue
     */
    public float[] getMissingValues() {
        return missingValues;
    }

    /**
     * @param missingValue
     *            the missingValue to set
     */
    public void setMissingValues(float[] missingValues) {
        this.missingValues = missingValues;
    }

    /**
     * @return the unitString
     */
    public String getUnitString() {
        return unitString;
    }

    /**
     * @param unitString
     *            the unitString to set
     */
    public void setUnitString(String unitString) {
        this.unitString = unitString;
    }

}
