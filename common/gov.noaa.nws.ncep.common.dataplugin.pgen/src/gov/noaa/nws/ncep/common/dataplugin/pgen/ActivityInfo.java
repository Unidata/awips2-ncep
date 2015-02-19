package gov.noaa.nws.ncep.common.dataplugin.pgen;

import java.util.Calendar;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Information used to identify and describe a specific PGEN Activity
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 22, 2013            sgilbert     Initial creation
 * Jan 25, 2014            jwu          Set default in accordance with those in "Product"
 * 
 * </pre>
 * 
 * @author sgilbert
 * @version 1.0
 */
@DynamicSerialize
public class ActivityInfo implements IServerRequest {

    @DynamicSerializeElement
    private String activityName = "Default";

    @DynamicSerializeElement
    private String activityType = "Default";

    @DynamicSerializeElement
    private String activitySubtype = "";

    @DynamicSerializeElement
    private String activityLabel = "Default";

    @DynamicSerializeElement
    private String site = "";

    @DynamicSerializeElement
    private String desk = "";

    @DynamicSerializeElement
    private String forecaster = "";

    @DynamicSerializeElement
    private Calendar refTime = Calendar.getInstance();

    @DynamicSerializeElement
    private String mode = "OPERATIONAL";

    @DynamicSerializeElement
    private String status = "Unknown";

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivitySubtype() {
        return activitySubtype;
    }

    public void setActivitySubtype(String activitySubtype) {
        this.activitySubtype = activitySubtype;
    }

    public String getActivityLabel() {
        return activityLabel;
    }

    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Calendar getRefTime() {
        return refTime;
    }

    public void setRefTime(Calendar refTime) {
        this.refTime = refTime;
    }

    public String getDesk() {
        return desk;
    }

    public void setDesk(String desk) {
        this.desk = desk;
    }

    public String getForecaster() {
        return forecaster;
    }

    public void setForecaster(String forecaster) {
        this.forecaster = forecaster;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
