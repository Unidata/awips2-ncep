package gov.noaa.nws.ncep.viz.resources.manager;

import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorOrder;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class declares the second level of SatelliteNames.xml as <radarStation>/
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 * 08/17/2015      R7656      RCR        Created.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 */

@XmlRootElement(name = "SatelliteName")
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
@XmlAccessorType(XmlAccessType.FIELD)
public class SatelliteName {

    private String alias = null;

    // private String nameID = null;

    private String ID = null;

    public SatelliteName() {
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String al) {
        alias = al;
    }

    public String getID() {
        return ID;
    }

    public void setID(String id) {
        ID = id;
    }

}