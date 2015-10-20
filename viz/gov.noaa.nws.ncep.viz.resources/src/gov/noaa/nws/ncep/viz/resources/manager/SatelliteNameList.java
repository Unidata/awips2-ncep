package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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