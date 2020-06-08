package gov.noaa.nws.ncep.ui.pgen.sigmet;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Implementation of IntlSigmetIssueList
 *
 * <pre>
 *
 *  SOFTWARE HISTORY
 *  Date           Ticket#     Engineer    Description
 *  ------------   ----------  ----------- --------------------------
 *  Jun 4,  2020   79256       ksunil      New. Series IDs now configurable
 *
 * </pre>
 *
 * @author ksunil
 */
@XmlRootElement(name = "IntlSigmetIssueList")
@XmlAccessorType(XmlAccessType.NONE)
public class IntlSigmetIssueList {

    @XmlElements({
            @XmlElement(name = "IssueOffice", type = IssueOffice.class) })
    List<IssueOffice> offices;

    public List<IssueOffice> getOffices() {
        return offices;
    }

    public void setOffices(List<IssueOffice> offices) {
        this.offices = offices;
    }

    /**
     * Constructor
     */
    public IntlSigmetIssueList() {
        this.offices = new ArrayList<>();
    }

    public String getSeriedIDs(String officeName) {
        for (IssueOffice iOffice : getOffices()) {
            if (officeName.equals(iOffice.getName())) {
                return iOffice.getSeriesIDs();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return "IntlSigmetOffice [offices=" + offices + "]";
    }
}
