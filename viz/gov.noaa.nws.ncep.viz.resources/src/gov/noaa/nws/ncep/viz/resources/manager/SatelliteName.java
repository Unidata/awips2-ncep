package gov.noaa.nws.ncep.viz.resources.manager;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

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