/**
* 
* gov.noaa.nws.ncep.ui.nctextui.dbutil.NctextStationInfo
* This class provides represents an nctext station
* 
* <pre>
* 
* SOFTWARE HISTORY
* 
* Date         Ticket#     Engineer    Description
* -------      -------     --------    -----------
* 12/12/2016   R25982      Jeff Beck   Add implements Comparable to enable Collections.sort()
*                                      Add compareTo()                                  
* </pre>
* 
* @author Chin Chen
* @version 1.0
*/
package gov.noaa.nws.ncep.ui.nctextui.dbutil;

public class NctextStationInfo implements Comparable<NctextStationInfo> {

    protected String productid; // WMO id

    protected String stnid;

    protected String stnname;

    protected String state;

    protected String country;

    protected double latitude;

    protected double longitude;

    protected Integer elevation;

    public String getProductid() {
        return productid;
    }

    public void setProductid(String productid) {
        this.productid = productid;
    }

    public String getStnid() {
        return stnid;
    }

    public void setStnid(String stnid) {
        this.stnid = stnid;
    }

    public String getStnname() {
        return stnname;
    }

    public void setStnname(String stnname) {
        this.stnname = stnname;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public Integer getElevation() {
        return elevation;
    }

    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }

    public int compareTo(NctextStationInfo other) {
        return this.getStnid().compareTo(other.getStnid());
    }

}
