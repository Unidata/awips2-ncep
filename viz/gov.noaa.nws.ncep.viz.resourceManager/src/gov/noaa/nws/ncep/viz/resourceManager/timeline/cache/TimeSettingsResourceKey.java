/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.resourceManager.timeline.cache;

import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;

/**
 * Lookup key used to retrieve dominant resource timeline settings. Utilizes
 * every field associated with a resource with the exception of attribute.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/12/2016   R15244     bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class TimeSettingsResourceKey {

    private final String rscCategory;

    private final String rscType;

    private final String rscGroup;

    private final DataTime cycleTime;

    public TimeSettingsResourceKey(final ResourceName resourceName) {
        this.rscCategory = resourceName.getRscCategory().getCategoryName();
        this.rscType = resourceName.getRscType();
        this.rscGroup = resourceName.getRscGroup();
        this.cycleTime = resourceName.getCycleTime();
    }

    /**
     * @return the rscCategory
     */
    public String getRscCategory() {
        return rscCategory;
    }

    /**
     * @return the rscType
     */
    public String getRscType() {
        return rscType;
    }

    /**
     * @return the rscGroup
     */
    public String getRscGroup() {
        return rscGroup;
    }

    /**
     * @return the cycleTime
     */
    public DataTime getCycleTime() {
        return cycleTime;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((cycleTime == null) ? 0 : cycleTime.hashCode());
        result = prime * result
                + ((rscCategory == null) ? 0 : rscCategory.hashCode());
        result = prime * result
                + ((rscGroup == null) ? 0 : rscGroup.hashCode());
        result = prime * result + ((rscType == null) ? 0 : rscType.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimeSettingsResourceKey other = (TimeSettingsResourceKey) obj;
        if (cycleTime == null) {
            if (other.cycleTime != null)
                return false;
        } else if (!cycleTime.equals(other.cycleTime))
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
                "TimeSettingsResourceKey [rscCategory=");
        sb.append(this.rscCategory).append(", rscType=");
        sb.append(this.rscType).append(", rscGroup=").append(this.rscGroup);
        sb.append(", cycleTime=");
        if (this.cycleTime != null) {
            sb.append(this.cycleTime.toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
