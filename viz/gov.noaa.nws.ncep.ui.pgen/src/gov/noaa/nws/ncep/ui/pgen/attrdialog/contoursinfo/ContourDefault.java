package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Default meta info for a contour parameter.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 11/20/2015   12829       J. Wu        Link Contour Parm with layer
 * 
 * </pre>
 * 
 * @author J. Wu
 * @version 1
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "default")
public class ContourDefault {

    @XmlAttribute
    private String level;

    @XmlAttribute
    private String fhrs;

    @XmlAttribute
    private String cint;

    public ContourDefault() {
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getFhrs() {
        return fhrs;
    }

    public void setFhrs(String fhrs) {
        this.fhrs = fhrs;
    }

    public String getCint() {
        return cint;
    }

    public void setCint(String cint) {
        this.cint = cint;
    }

}
