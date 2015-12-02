package gov.noaa.nws.ncep.viz.resources.manager;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class declares the second level of localRadarStations.xml as
 * <radarStation>/
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 *  05/14/15      R7656      A. Su        Created.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 */

@XmlRootElement(name = "radarStation")
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalRadarStation {

    private String alias = null;

    private String stationID = null;

    public LocalRadarStation() {
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String al) {
        alias = al;
    }

    public String getStationID() {
        return stationID;
    }

    public void setStationID(String name) {
        stationID = name;
    }

}