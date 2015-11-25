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
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 *  08/17/15      R7656      RCR        Created.
 *  10/15/2015    R7190     R. Reynolds  Added support for Mcidas
 * 
 * </pre>
 * 
 * @author RCR
 * @version 1
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SatelliteAreaList {

    @XmlElement(name = "satelliteArea")
    private ArrayList<SatelliteArea> areaList = null;

    public SatelliteAreaList() {
    }

    public ArrayList<SatelliteArea> getAreaList() {
        return areaList;
    }
}