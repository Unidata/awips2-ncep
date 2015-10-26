package gov.noaa.nws.ncep.viz.resources.manager;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class declares the second level of SatelliteAreas.xml as <radarStation>/
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 *  08/17/15      R7656      RCR        Created.
 *  10/15/2015    R7190     R. Reynolds  Added support for Mcidas
 * 
 * </pre>
 * 
 * @author rcr
 * @version 1
 */

@XmlRootElement(name = "SatelliteArea")
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
@XmlAccessorType(XmlAccessType.FIELD)
public class SatelliteArea {

    private String alias = null;

    private String areaID = null;

    public SatelliteArea() {
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String al) {
        alias = al;
    }

    public String getAreaID() {
        return areaID;
    }

    public void setAreaID(String name) {
        areaID = name;
    }

}