package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class declares the top level of localRadarStations.xml as
 * <localRadarStationList>.
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

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class LocalRadarStationList {

    @XmlElement(name = "radarStation")
    private ArrayList<LocalRadarStation> stationList = null;

    public LocalRadarStationList() {
    }

    public ArrayList<LocalRadarStation> getStationList() {
        return stationList;
    }
}