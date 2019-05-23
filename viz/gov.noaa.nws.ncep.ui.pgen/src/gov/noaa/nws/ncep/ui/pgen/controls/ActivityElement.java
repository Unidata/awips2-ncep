package gov.noaa.nws.ncep.ui.pgen.controls;

import java.util.Date;

/**
 *
 * Container for PGEN Activity information retrieved from the database
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 1, 2019  7752       tjensen     Extracted from RetrieveActivityDialog class
 *
 * </pre>
 *
 * @author tjensen
 */
public class ActivityElement implements Comparable {
    private final String site;

    private final String desk;

    private final String dataURI;

    private final String activityType;

    private final String activitySubtype;

    private String activityLabel;

    private final Date refTime;

    public ActivityElement(String site, String desk, String activityType,
            String activitySubtype, String activityLabel, String dataURI,
            Date refTime) {
        this.site = site;
        this.desk = desk;
        this.activityType = activityType;
        this.activitySubtype = activitySubtype;
        this.activityLabel = activityLabel;
        this.dataURI = dataURI;
        this.refTime = refTime;
    }

    public String getSite() {
        return site;
    }

    public String getDesk() {
        return desk;
    }

    public String getDataURI() {
        return dataURI;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getActivitySubtype() {
        return activitySubtype;
    }

    public String getActivityLabel() {
        return activityLabel;
    }

    public Date getRefTime() {
        return refTime;
    }

    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;

    }

    @Override
    public int compareTo(Object o) {
        ActivityElement other = (ActivityElement) o;
        return this.refTime.compareTo(other.getRefTime());

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((activityLabel == null) ? 0 : activityLabel.hashCode());
        result = prime * result
                + ((activitySubtype == null) ? 0 : activitySubtype.hashCode());
        result = prime * result
                + ((activityType == null) ? 0 : activityType.hashCode());
        result = prime * result + ((dataURI == null) ? 0 : dataURI.hashCode());
        result = prime * result + ((desk == null) ? 0 : desk.hashCode());
        result = prime * result + ((refTime == null) ? 0 : refTime.hashCode());
        result = prime * result + ((site == null) ? 0 : site.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ActivityElement other = (ActivityElement) obj;
        if (activityLabel == null) {
            if (other.activityLabel != null) {
                return false;
            }
        } else if (!activityLabel.equals(other.activityLabel)) {
            return false;
        }
        if (activitySubtype == null) {
            if (other.activitySubtype != null) {
                return false;
            }
        } else if (!activitySubtype.equals(other.activitySubtype)) {
            return false;
        }
        if (activityType == null) {
            if (other.activityType != null) {
                return false;
            }
        } else if (!activityType.equals(other.activityType)) {
            return false;
        }
        if (dataURI == null) {
            if (other.dataURI != null) {
                return false;
            }
        } else if (!dataURI.equals(other.dataURI)) {
            return false;
        }
        if (desk == null) {
            if (other.desk != null) {
                return false;
            }
        } else if (!desk.equals(other.desk)) {
            return false;
        }
        if (refTime == null) {
            if (other.refTime != null) {
                return false;
            }
        } else if (!refTime.equals(other.refTime)) {
            return false;
        }
        if (site == null) {
            if (other.site != null) {
                return false;
            }
        } else if (!site.equals(other.site)) {
            return false;
        }
        return true;
    }

}