package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class defines the structure of the <attrSetLabels> xml tag and its
 * access methods. This xml tag replaces the original <attrSetNames> xml tag.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 * 10/19/2015    R8824      A. Su        Created.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 */

@XmlRootElement(name = "attrSetLabels")
@XmlAccessorType(XmlAccessType.NONE)
public class AttrSetLabels {

    /**
     * An <attrSetLabels> tag is composed of zero or one or many <attrSetLabel>
     * tags.
     */
    @XmlElement(name = "attrSetLabel")
    private ArrayList<AttrSetLabel> labelList = new ArrayList<AttrSetLabel>();

    /**
     * The empty constructor.
     */
    public AttrSetLabels() {
    }

    /**
     * This method accesses the contents of <attrSetLabels>.
     * 
     * @return an array list of <attrSetLabel> tags.
     */
    public ArrayList<AttrSetLabel> getLabelList() {
        return labelList;
    }

    /**
     * Add a new label to the list of <AttrSetLabels>.
     * 
     * @param asName
     *            the name of label
     * @return false if the name of label exists in the list of <AttrSetLabels>;
     *         true for otherwise.
     */
    public boolean addLabel(String asName) {

        for (AttrSetLabel label : labelList) {
            if (label.getName().equals(asName)) {
                return false;
            }
        }
        AttrSetLabel label = new AttrSetLabel();
        label.setName(asName);
        labelList.add(label);
        return true;
    }

    /**
     * Remove a label based on its name from the list of <attrSwtLabels>.
     * 
     * @param asName
     *            the name of label.
     * @return true if the name of label exists in the list of <AttrSetLabels>;
     *         false for otherwise.
     */
    public boolean removeLabel(String asName) {

        for (AttrSetLabel label : labelList) {
            if (label.getName().equals(asName)) {
                labelList.remove(label);
                return true;
            }
        }
        return false;
    }
}
