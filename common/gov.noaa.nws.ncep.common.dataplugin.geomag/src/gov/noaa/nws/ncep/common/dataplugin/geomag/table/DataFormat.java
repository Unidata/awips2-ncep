package gov.noaa.nws.ncep.common.dataplugin.geomag.table;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * DataFormat containing the regular expression (read from geoMagStations.xml
 * file) for the data
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer       Description
 * -----------  ----------  ----------      --------------------------
 * 10/07/2015   R11429      sgurung,jtravis added functionality to return the groups in a HashMap
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1
 */
@XmlRootElement(name = "dataFormat")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class DataFormat {

    @XmlAttribute
    protected boolean conversionRequired;

    @XmlElement(name = "regex")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String pattern;

    @XmlElement(name = "group")
    protected Group[] group;

    public DataFormat() {

    }

    /**
     * @return String
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return boolean
     */
    public boolean getConversionRequired() {
        return conversionRequired;
    }

    /**
     * @param conversionRequired
     */
    public void setConversionRequired(boolean conversionRequired) {
        this.conversionRequired = conversionRequired;
    }

    /**
     * @return Group[]
     */
    public Group[] getGroup() {
        return group;
    }

    /**
     * @param group
     */
    public void setGroup(Group[] group) {
        this.group = group;
    }

    /**
     * @return HashMap<String, Group>
     */
    public HashMap<String, Group> getDataGroups() {

        HashMap<String, Group> dataGroupMap = new HashMap<String, Group>();

        for (Group group : this.group) {
            dataGroupMap.put(group.getName(), group);
        }

        return dataGroupMap;
    }
}
