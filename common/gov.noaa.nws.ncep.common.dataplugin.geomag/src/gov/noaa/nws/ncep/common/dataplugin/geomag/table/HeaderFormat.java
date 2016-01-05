package gov.noaa.nws.ncep.common.dataplugin.geomag.table;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * HeaderFormat containing the regular expression (read from geoMagStations.xml
 * file) for the header
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer       Description
 * ------------ ---------- -----------    --------------------------
 * 03/29/2013   975        sgurung         Initial Creation
 * 10/07/2015   R11429     sgurung,jtravis Added functionality to return the groups in a HashMap
 * </pre>
 * 
 * @author sgurung
 * @version 1
 */
@XmlRootElement(name = "headerFormat")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class HeaderFormat {

    @XmlElement(name = "regex")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String pattern;

    @XmlElement(name = "group")
    protected Group[] group;

    public HeaderFormat() {

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
    public HashMap<String, Group> getHeaderGroups() {

        HashMap<String, Group> headerGroupMap = new HashMap<String, Group>();

        for (Group group : this.group) {
            headerGroupMap.put(group.getName(), group);
        }

        return headerGroupMap;
    }

}
