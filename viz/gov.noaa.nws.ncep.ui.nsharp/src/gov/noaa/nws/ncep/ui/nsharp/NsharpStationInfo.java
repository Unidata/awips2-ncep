/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo
 * 
 * This java class defines NSHARP NsharpStationInfo functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer   Description
 * ------------- ---------- ---------- -----------
 * 03/26/2010    229        Chin Chen  Initial coding
 * 12/16/2010    362        Chin Chen  add support of BUFRUA observed sounding and PFC (NAM and GFS) model sounding data
 * 12/17/2014    Task#5694  Chin Chen  initialize stnDisplayInfo and stnId
 * Aug 05, 2015  4486       rjpeter    Changed Timestamp to Date.
 * 01/15/2019    7697       bsteffen   Add locationDetails to hold ACARS aircraft info.
 *
 * </pre>
 * 
 * @author Chin Chen
 */
package gov.noaa.nws.ncep.ui.nsharp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NsharpStationInfo {
    public class timeLineSpecific {
        private Date timeLine;

        private String displayInfo;

        public Date getTimeLine() {
            return timeLine;
        }

        public void setTimeLine(Date timeLine) {
            this.timeLine = timeLine;
        }

        public String getDisplayInfo() {
            return displayInfo;
        }

        public void setDisplayInfo(String displayInfo) {
            this.displayInfo = displayInfo;
        }

    }

    protected List<timeLineSpecific> timeLineSpList = new ArrayList<>();

    protected double latitude;

    protected double longitude;

    /* uair (same as Synoptictime in uair) */
    protected Date reftime;

    /*
     * rangestart used by model sounding e.g. PFC sounding, or uair (same as
     * Synoptictime in uair)
     */
    protected Date rangestarttime;

    protected String stnDisplayInfo = "N/A";

    protected String sndType = "N/A";

    protected String stnId = "N/A";

    private String locationDetails;

    public String getStnId() {
        return stnId;
    }

    public void setStnId(String stnId) {
        this.stnId = stnId;
    }

    public List<timeLineSpecific> getTimeLineSpList() {
        return timeLineSpList;
    }

    public void setTimeLineSpList(List<timeLineSpecific> timeLineSpList) {
        this.timeLineSpList = timeLineSpList;
    }

    public void addToTimeLineSpList(timeLineSpecific timeLineSpInfo,
            int index) {
        this.timeLineSpList.add(index, timeLineSpInfo);
    }

    public void addToTimeLineSpList(timeLineSpecific timeLineSpInfo) {
        this.timeLineSpList.add(timeLineSpInfo);
    }

    public String getSndType() {
        return sndType;
    }

    public void setSndType(String sndType) {
        this.sndType = sndType;
    }

    public Date getRangestarttime() {
        return rangestarttime;
    }

    public void setRangestarttime(Date rangestarttime) {
        this.rangestarttime = rangestarttime;
    }

    public Date getReftime() {
        return reftime;
    }

    public void setReftime(Date reftime) {
        this.reftime = reftime;
    }

    public String getStnDisplayInfo() {
        return stnDisplayInfo;
    }

    public void setStnDisplayInfo(String stnDisplayInfo) {
        this.stnDisplayInfo = stnDisplayInfo;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLocationDetails() {
        return locationDetails;
    }

    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }

    /**
     * Get a user friendly summary of the location. This is a formatted lat,lon
     * and the locationDetails if any are provided.
     * 
     * @return location display information
     */
    public String getLocationDisplayInfo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("%1.2f,%1.2f", latitude, longitude));
        if (locationDetails != null) {
            info.append(' ');
            info.append(locationDetails);
        }
        return info.toString();
    }

}
