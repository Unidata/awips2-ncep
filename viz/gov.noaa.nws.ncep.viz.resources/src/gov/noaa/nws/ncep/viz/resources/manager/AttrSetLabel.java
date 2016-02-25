package gov.noaa.nws.ncep.viz.resources.manager;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class defines the structure of the <attrSetLabel> xml tag.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 * 08/25/2015   R8824       A. Su       Created.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 */

@XmlRootElement(name = "attrSetLabel")
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
@XmlAccessorType(XmlAccessType.FIELD)
public class AttrSetLabel {

    private String alias = null;

    private String name = null;

    /**
     * The empty constructor.
     */
    public AttrSetLabel() {
    }

    /**
     * Get the alias of the label.
     * 
     * @return the alias of the label.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Set the alias of the label.
     * 
     * @param alias
     *            the alias of the label.
     */
    void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Get the name of the label.
     * 
     * @return the name of the label.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the label.
     * 
     * @param name
     *            the name of the label.
     */
    public void setName(String name) {
        this.name = name;
    }
}
