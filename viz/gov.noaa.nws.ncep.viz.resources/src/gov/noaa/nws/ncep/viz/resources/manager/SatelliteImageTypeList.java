package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class declares the top level of SatelliteArea.xml as
 * <SatelliteAreaList>.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Descriptio
 * ------------ ----------  ----------- --------------------------
 * 
 *  08/17/15      R7656      RCR        Created.
 * 
 * </pre>
 * 
 * @author RCR
 * @version 1
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SatelliteImageTypeList {

    @XmlElement(name = "satelliteImageType")
    private ArrayList<SatelliteImageType> imageTypeList = null;

    public SatelliteImageTypeList() {
    }

    public ArrayList<SatelliteImageType> getImageTypeList() {
        return imageTypeList;
    }
}