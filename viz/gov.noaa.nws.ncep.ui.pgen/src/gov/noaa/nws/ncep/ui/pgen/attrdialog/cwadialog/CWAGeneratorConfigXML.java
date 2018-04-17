/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import gov.noaa.nws.ncep.edex.common.stationTables.Station;

/**
 * This class for the CWA generator configuration.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#  Engineer    Description
 * ----------- -------- ----------- --------------------------
 * 12/02/2016  17469    wkwock      Initial creation
 * 
 * </pre>
 * 
 * @author wkwock
 */

@XmlRootElement(name = "CWAGeneratorConfig")
@XmlAccessorType(XmlAccessType.NONE)
public class CWAGeneratorConfigXML {

    /** is this operational mode */
    @XmlElement(name = "isOperational")
    private boolean isOperational;

    /** is text product editable */
    @XmlElement(name = "isEditable")
    private boolean isEditable;

    /** CWSU ID */
    @XmlElement(name = "cwsuId")
    private String cwsuId;

    /** KCWSU ID */
    @XmlElement(name = "kcwsuId")
    private String kcwsuId;

    /** AWIPS node */
    @XmlElement(name = "awipsNode")
    private String awipsNode;

    /** place that volcano ash would reach */
    @XmlElement(name = "reach")
    private ArrayList<String> reach;

    /** volcano filter list */
    @XmlElement(name = "include")
    private ArrayList<String> include;

    /** volcano station list */
    @XmlElements({ @XmlElement(name = "station", type = Station.class) })
    private ArrayList<Station> stations;

    /**
     * Is this operational mode
     * 
     * @return true for operational and false for practice mode.
     */
    public boolean isOperational() {
        return isOperational;
    }

    /**
     * set operation mode
     * 
     * @param isOperational
     */
    public void setOperational(boolean isOperational) {
        this.isOperational = isOperational;
    }

    /**
     * Is product text editable
     * 
     * @return true for editable and false for not editable
     */
    public boolean isEditable() {
        return isEditable;
    }

    /**
     * set editable
     * 
     * @param isEditable
     */
    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    /**
     * get CWSU ID
     * 
     * @return CWSU ID
     */
    public String getCwsuId() {
        return cwsuId;
    }

    /**
     * Set CWSU ID
     * 
     * @param cwsuId
     */
    public void setCwsuId(String cwsuId) {
        this.cwsuId = cwsuId;
    }

    /**
     * get the KCWSU ID
     * 
     * @return KCWSU ID
     */
    public String getKcwsuId() {
        return kcwsuId;
    }

    /**
     * Set the KCWSU ID
     * 
     * @param kcwsuId
     */
    public void setKcwsuId(String kcwsuId) {
        this.kcwsuId = kcwsuId;
    }

    /**
     * get AWIPS Node
     * 
     * @return AWIPS Node
     */
    public String getAwipsNode() {
        return awipsNode;
    }

    /**
     * Set AWIPS Node
     * 
     * @param awipsNode
     */
    public void setAwipsNode(String awipsNode) {
        this.awipsNode = awipsNode;
    }

    /**
     * get the reaches
     * 
     * @return list of reach
     */
    public ArrayList<String> getReach() {
        return reach;
    }

    /**
     * set the reaches
     * 
     * @param reach
     */
    public void setReach(ArrayList<String> reach) {
        this.reach = reach;
    }

    /**
     * get includes
     * 
     * @return
     */
    public ArrayList<String> getInclude() {
        return include;
    }

    /**
     * Set includes
     * 
     * @param include
     */
    public void setInclude(ArrayList<String> include) {
        this.include = include;
    }

    /**
     * get volcano stations
     * 
     * @return
     */
    public List<Station> getStations() {
        return stations;
    }

    /**
     * set volcano stations
     * 
     * @param stations
     */
    public void setStations(ArrayList<Station> stations) {
        this.stations = stations;
    }

    /**
     * filter volcano stations
     */
    public void filterVolcanoStations() {
        Iterator<Station> iter = stations.iterator();

        while (iter.hasNext()) {
            Station station = iter.next();
            boolean discard = true;
            for (String filter : include) {
                if (station.getLocation().contains(filter)) {
                    discard = false;
                    break;
                }
            }
            if (discard) {
                iter.remove();
            }
        }
    }
}
