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
 *  08/17/2015    R7656     RCR          Created.
 *  10/15/2015    R7190     R. Reynolds  Added support for Mcidas
 * 
 * </pre>
 * 
 * @author rcr
 * @version 1
 */

@XmlRootElement(name = "SatelliteImageType")
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
@XmlAccessorType(XmlAccessType.FIELD)
public class SatelliteImageType {

    private String satelliteId = null;

    private String imageTypeId = null;

    private String ASImageTypeId = null;

    public void setSatelliteId(String x) {
        satelliteId = x;
    }

    public String getSatelliteId() {
        return satelliteId;
    }

    public void setImageTypeId(String x) {
        imageTypeId = x;

    }

    public String getImageTypeId() {

        return imageTypeId;
    }

    public void setASImageTypeId(String x) {
        ASImageTypeId = x;
    }

    public String getASImageTypeId() {
        return ASImageTypeId;
    }

}