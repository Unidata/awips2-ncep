package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class declares the top level of SatelliteName.xml as
 * <SatelliteNameList>.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 *  08/17/2015    R7656      RCR        Created.
 * 
 * </pre>
 * 
 * @author RCR
 * @version 1
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SatelliteNameList {

    @XmlElement(name = "satelliteName")
    private ArrayList<SatelliteName> nameList = null;

    public SatelliteNameList() {
    }

    public ArrayList<SatelliteName> getNameList() {
        return nameList;
    }
}