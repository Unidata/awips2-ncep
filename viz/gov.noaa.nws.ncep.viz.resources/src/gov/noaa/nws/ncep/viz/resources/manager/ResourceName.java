package gov.noaa.nws.ncep.viz.resources.manager;

import java.io.File;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;

/**
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------- ----------- -------------------------
 *  09/13/10      #307        Greg Hull   add cycleTime.
 *  09/16/10      #307        Greg Hull   add forecast flag
 *  10/14/10      #277        M. Li       add ensemble category
 *  10/20/10                  Xilin Guo   Rename getCycleTimeStringFromDataTime to getTimeStringFromDataTime
 *  02/16/11      #408        Greg Hull   add 'backup' categories for obs/fcst surface/uair
 *  01/09/11      #561        Greg Hull   generated equals()
 *  09/13/12      #860        Greg Hull   trim()
 *  11/19/12      #630        Greg Hull   getAbbrName() (for satellite-area names
 *  02/10/13      #972        Greg Hull   ResourceCategory class
 *  02/16/16      R15244      bkowal      Cleanup unused imports.
 *  11/04/2016    R23113      Bugenhagen  Updated toString to return time including milliseconds.
 *  03/27/2017    R28354      S. Russell  Updated setCycleTimeFromString(),
 *                                        added getOrigTimeStr(),
 *                                        added getOrigTimeStrLgth()
 * 
 *
 * </pre>
 * 
 * @author
 * @version 1
 */

public class ResourceName {

    public static class ResourceNameAdapter
            extends XmlAdapter<String, ResourceName> {

        @Override
        public String marshal(ResourceName rscName) throws Exception {
            return (rscName != null ? rscName.toString() : "");
        }

        @Override
        public ResourceName unmarshal(String rscNameStr) throws Exception {
            return new ResourceName(rscNameStr);
        }
    }

    public static final String GENERATED_TYPE_DELIMETER = ":";

    public static final String DFLT_ATTR_SET_NAME = "default";

    private ResourceCategory rscCategory = ResourceCategory.NullCategory;

    private String rscType;

    private String rscGroup; // this could be either a subType or attrSetGroup

    private String rscAttrSetName;

    // for forecast resources. If this is not null then this is a forecast
    // resource.
    private DataTime cycleTime;

    // The original time string parsed out of the original resource name
    // passed into the constructor
    private String originalTimeString = null;

    private int originalTimeStringLength = 0;

    // This may be used to set the cycle time
    public static final String LATEST_CYCLE_TIME = "LATEST";

    private static final DataTime LATEST_DATA_TIME = new DataTime(new Date(0));

    public ResourceName() {
        rscCategory = ResourceCategory.NullCategory;
        rscType = "";
        rscGroup = "";
        rscAttrSetName = "";
        cycleTime = null;
    }

    // parse the full resource name
    public ResourceName(String rName) {
        if (rName == null || rName.isEmpty())
            return;

        setFullResourceName(rName);
    }

    public ResourceName(ResourceCategory cat, String type, String attrSet) {
        this(cat.getCategoryName(), type, attrSet);
    }

    public ResourceName(String cat, String type, String attrSet) {

        setFullResourceName(cat + File.separator + type + File.separator
                + (attrSet == null ? DFLT_ATTR_SET_NAME : attrSet));
    }

    public ResourceName(ResourceName rn) {
        super();
        if (rn == null) {
            return;
        }

        rscCategory = rn.getRscCategory();
        rscType = rn.getRscType();
        rscGroup = rn.getRscGroup();
        rscAttrSetName = rn.getRscAttrSetName();
        if (rn.getCycleTime() == null) {
            cycleTime = null;
        } else if (rn.getCycleTime() == LATEST_DATA_TIME) {
            cycleTime = LATEST_DATA_TIME;
        } else {
            cycleTime = new DataTime(rn.getCycleTime().getRefTime());
        }
    }

    public ResourceName(String cat, String type, String group, String attrSet) {

        setFullResourceName(cat.trim() + File.separator + type.trim()
                + File.separator + group.trim() + File.separator
                + (attrSet == null ? DFLT_ATTR_SET_NAME : attrSet.trim()));
    }

    public void setFullResourceName(String rscName) {
        String[] parts = rscName.split(File.separator);
        if (parts == null || parts.length < 3) {
            System.out.println("Unrecognized Resource Name : " + rscName);
            return;
        }

        rscCategory = ResourceCategory.getCategory(parts[0].trim());
        rscType = parts[1].trim();
        rscAttrSetName = parts[parts.length - 1].trim();

        // forecast resources have the cycle time after the attrSet without a
        // separator
        //
        int parenIndx = rscAttrSetName.indexOf('(');

        if (parenIndx != -1) {
            String cycleTimeStr = rscAttrSetName.substring(parenIndx + 1,
                    rscAttrSetName.indexOf(')'));

            this.originalTimeString = cycleTimeStr;

            if (cycleTimeStr != null) {
                this.originalTimeStringLength = cycleTimeStr.length();
            }

            setCycleTimeFromString(cycleTimeStr);

            rscAttrSetName = rscAttrSetName.substring(0, parenIndx);
            rscAttrSetName = rscAttrSetName.trim();
        } else {
            cycleTime = null;
        }

        if (parts.length > 3) {
            rscGroup = parts[2].trim();
        } else {
            rscGroup = "";
        }

    }

    public boolean isValid() {
        try {
            return ResourceDefnsMngr.getInstance().isResourceNameValid(this);
        } catch (VizException vizex) {
            return false;
        }
    }

    public ResourceCategory getRscCategory() {
        return (rscCategory == null ? ResourceCategory.NullCategory
                : rscCategory);
    }

    public void setRscCategory(String rscCatStr) {
        this.rscCategory = (rscCategory == null ? ResourceCategory.NullCategory
                : ResourceCategory.getCategory(rscCatStr));
    }

    public void setRscCategory(ResourceCategory rscCat) {
        this.rscCategory = (rscCategory == null ? ResourceCategory.NullCategory
                : rscCat);
    }

    public String getRscType() {
        return (rscType == null ? "" : rscType);
    }

    public void setRscType(String rscType) {
        this.rscType = (rscType == null ? "" : rscType.trim());
    }

    public String getRscGroup() {
        return (rscGroup == null ? "" : rscGroup);
    }

    public void setRscGroup(String rscGroup) {
        this.rscGroup = (rscGroup == null ? "" : rscGroup.trim());
    }

    public String getRscAttrSetName() {
        return (rscAttrSetName == null ? "" : rscAttrSetName);
    }

    public void setRscAttrSetName(String rscAttrSetName) {
        this.rscAttrSetName = (rscAttrSetName == null ? ""
                : rscAttrSetName.trim());
    }

    public DataTime getCycleTime() {
        return cycleTime;
    }

    public String getCycleTimeString() {
        if (cycleTime == null) {
            return null;
        }
        if (cycleTime == LATEST_DATA_TIME) {
            return LATEST_CYCLE_TIME;
        }
        return NmapCommon.getTimeStringFromDataTime(cycleTime, "_");
    }

    /**
     * Get the time string as it originally was when the resource name was
     * passed into the constructor
     * 
     * @return
     */
    public String getOrigTimeStr() {
        return this.originalTimeString;
    }

    public int getOrigTimeStrLgth() {
        return this.originalTimeStringLength;
    }

    public void setCycleTime(DataTime cycleTime) {
        this.cycleTime = cycleTime;
    }

    public void setCycleTimeLatest() {
        this.cycleTime = LATEST_DATA_TIME;
    }

    public boolean isLatestCycleTime() {
        return (cycleTime == LATEST_DATA_TIME);
    }

    public void setCycleTimeFromString(String cycTimeStr) {
        if (cycTimeStr == null || cycTimeStr.isEmpty()) {
            cycleTime = null;
            return;
        }
        if (cycTimeStr.equals(LATEST_CYCLE_TIME)) {
            cycleTime = LATEST_DATA_TIME;
            return;
        }
        // else this will be in the format as created from NmapCommon.
        cycleTime = NmapCommon
                .parseDataTimeToMillisFromCycleTimeString(cycTimeStr);
    }

    public boolean isForecastResource() {
        return (cycleTime != null);
    }

    public boolean isPgenResource() {
        return rscCategory == ResourceCategory.PGENRscCategory;
    }

    public boolean isOverlayResource() {
        return rscCategory == ResourceCategory.OverlayRscCategory;
    }

    public String toString() {
        if (rscCategory == null || rscCategory == ResourceCategory.NullCategory
                || rscType == null || rscType.isEmpty()
                || rscAttrSetName == null || rscAttrSetName.isEmpty()) {
            return "";
        } else {
            String str = rscCategory + File.separator + rscType + File.separator
                    + (!rscGroup.isEmpty() ? rscGroup + File.separator : "")
                    + rscAttrSetName;
            if (cycleTime == null) {
                return str;
            } else if (cycleTime == LATEST_DATA_TIME) {
                return str + '(' + LATEST_CYCLE_TIME + ')';
            } else {
                return str + '(' + NmapCommon.getTimeStringToMillisFromDataTime(
                        cycleTime, "_") + ')';
            }
        }
    }

    public String toStringNoSecsNoMillisecs() {
        if (rscCategory == null || rscCategory == ResourceCategory.NullCategory
                || rscType == null || rscType.isEmpty()
                || rscAttrSetName == null || rscAttrSetName.isEmpty()) {
            return "";
        } else {
            String str = rscCategory + File.separator + rscType + File.separator
                    + (!rscGroup.isEmpty() ? rscGroup + File.separator : "")
                    + rscAttrSetName;
            if (cycleTime == null) {
                return str;
            } else if (cycleTime == LATEST_DATA_TIME) {
                return str + '(' + LATEST_CYCLE_TIME + ')';
            } else {
                return str + '('
                        + NmapCommon.getTimeStringFromDataTime(cycleTime, "_")
                        + ')';
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((cycleTime == null) ? 0 : cycleTime.hashCode());
        result = prime * result
                + ((rscAttrSetName == null) ? 0 : rscAttrSetName.hashCode());
        result = prime * result
                + ((rscCategory == null) ? 0 : rscCategory.hashCode());
        result = prime * result
                + ((rscGroup == null) ? 0 : rscGroup.hashCode());
        result = prime * result + ((rscType == null) ? 0 : rscType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceName other = (ResourceName) obj;
        if (cycleTime == null) {
            if (other.cycleTime != null)
                return false;
        } else if (!cycleTime.equals(other.cycleTime))
            return false;
        if (rscAttrSetName == null) {
            if (other.rscAttrSetName != null)
                return false;
        } else if (!rscAttrSetName.equals(other.rscAttrSetName))
            return false;
        if (rscCategory == null) {
            if (other.rscCategory != null)
                return false;
        } else if (!rscCategory.equals(other.rscCategory))
            return false;
        if (rscGroup == null) {
            if (other.rscGroup != null)
                return false;
        } else if (!rscGroup.equals(other.rscGroup))
            return false;
        if (rscType == null) {
            if (other.rscType != null)
                return false;
        } else if (!rscType.equals(other.rscType))
            return false;
        return true;
    }

}
