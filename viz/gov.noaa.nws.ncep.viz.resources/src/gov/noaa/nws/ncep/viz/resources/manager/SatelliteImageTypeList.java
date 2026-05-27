package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

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