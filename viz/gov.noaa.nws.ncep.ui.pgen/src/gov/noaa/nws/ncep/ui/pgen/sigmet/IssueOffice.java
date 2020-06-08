package gov.noaa.nws.ncep.ui.pgen.sigmet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * Implementation of IssueOffice xml element
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 4,  2020 79256      ksunil      Series IDs now configurable
 * 
 * </pre>
 *
 * @author ksunil
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IssueOffice")
public class IssueOffice {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "seriesIDs")
    private String seriesIDs;

    public IssueOffice() {

    }

    public IssueOffice(String pName, String pSeriesIDs) {
        name = pName;
        seriesIDs = pSeriesIDs;
    }

    public String getName() {
        return name;
    }

    public String getSeriesIDs() {
        return seriesIDs;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeriesIDs(String seriesIDs) {
        this.seriesIDs = seriesIDs;
    }

    @Override
    public String toString() {
        return "IssueOffice [name=" + name + ", seriesIDs=" + seriesIDs + "]";
    }

}
