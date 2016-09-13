package gov.noaa.nws.ncep.common.dataplugin.pgen.response;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * A data object that holds fields required for activity map for a single entry
 * in the PGEN plugin table.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/06/2016   R9714       byin       Initial creation
 * 
 * </pre>
 * 
 * @author byin
 * @version 1.0
 */
@DynamicSerialize
public class ActivityMapData {

    @DynamicSerializeElement
    private String refTime;

    @DynamicSerializeElement
    private String activityType;

    @DynamicSerializeElement
    private String activitySubtype;

    @DynamicSerializeElement
    private String activityLabel;

    @DynamicSerializeElement
    private String activityName;

    @DynamicSerializeElement
    private String dataURI;

    /**
     * Constructor.
     */
    public ActivityMapData() {
        super();
    }

    /**
     * @return the refTime
     */
    public String getRefTime() {
        return refTime;
    }

    /**
     * @param refTime
     *            the refTime to set
     */
    public void setRefTime(String refTime) {
        this.refTime = refTime;
    }

    /**
     * @return the activityType
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * @param activityType
     *            the activityType to set
     */
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    /**
     * @return the activitySubtype
     */
    public String getActivitySubtype() {
        return activitySubtype;
    }

    /**
     * @param activitySubtype
     *            the activitySubtype to set
     */
    public void setActivitySubtype(String activitySubtype) {
        this.activitySubtype = activitySubtype;
    }

    /**
     * @return the activityLabel
     */
    public String getActivityLabel() {
        return activityLabel;
    }

    /**
     * @param activityLabel
     *            the activityLabel to set
     */
    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName
     *            the activityName to set
     */
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    /**
     * @return the dataURI
     */
    public String getDataURI() {
        return dataURI;
    }

    /**
     * @param dataURI
     *            the dataURI to set
     */
    public void setDataURI(String dataURI) {
        this.dataURI = dataURI;
    }

}
