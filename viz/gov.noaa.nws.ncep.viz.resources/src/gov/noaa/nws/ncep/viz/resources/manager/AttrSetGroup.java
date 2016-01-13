package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.localization.LocalizationFile;

/**
 * This class defines the structure of the <AttrSetGroup> xml tag.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 * 08/31/2015   R8824       A. Su       Changed the <attrSetNames> tag to <attrSetLabels>.
 * 
 * </pre>
 * 
 * @author ?
 * @version 1
 */

@XmlRootElement(name = "AttributeSetGroup")
@XmlAccessorType(XmlAccessType.NONE)
public class AttrSetGroup {

    public static final String PGEN = "PGEN";

    // This class would be called AttrSetGroupName except that this was the name
    // of the old member for just the group name and the set method needs to
    // stay the same so that the existing jaxb files are still compatible.
    public static class RscAndGroupName implements Comparable<RscAndGroupName> {
        private String resource;

        private String groupName;

        public RscAndGroupName() {
            this("", "");
        }

        public RscAndGroupName(String rsc, String grp) {
            resource = rsc;
            groupName = grp;
        }

        public String getResource() {
            return resource;
        }

        public String getGroupName() {
            return groupName;
        }

        public Boolean isPGEN() {
            return getGroupName().equals(PGEN);
        }

        @Override
        public String toString() {
            return (isPGEN() ? PGEN : resource + "-" + groupName);
        }

        @Override
        public int compareTo(RscAndGroupName rscNGrp) {
            return toString().compareTo(rscNGrp.toString());
        }
    }

    private final AttrSetLabelsManager manager = AttrSetLabelsManager
            .getInstance();

    // All GRID resource attributes are specified in this directory.
    private final String gridData = "ModelFcstGridContours";

    private RscAndGroupName rscAndGroupName;

    private ArrayList<String> attrSetNames;

    @XmlElement(name = "attrSetLabels")
    private AttrSetLabels attrSetLabels;

    private LocalizationFile lclFile;

    private boolean isModified;

    public AttrSetGroup() {
        attrSetNames = new ArrayList<String>();
        attrSetLabels = new AttrSetLabels();
        rscAndGroupName = new RscAndGroupName();

        isModified = false;
    }

    public AttrSetGroup(AttrSetGroup group) {
        attrSetNames = new ArrayList<String>(group.getAttrSetNames());
        attrSetLabels = new AttrSetLabels();
        for (String asName : attrSetNames) {
            attrSetLabels.addLabel(asName);
        }
        rscAndGroupName = new RscAndGroupName(group.getResource(),
                group.getAttrSetGroupName());

        isModified = false;
    }

    public boolean isModified() {
        return isModified;
    }

    public String getResource() {
        return rscAndGroupName.getResource();
    }

    @XmlElement
    public void setResource(String resource) {
        rscAndGroupName = new RscAndGroupName(resource,
                rscAndGroupName.getGroupName());
    }

    public RscAndGroupName getRscAndGroupName() {
        return rscAndGroupName;
    }

    public String getAttrSetGroupName() {
        return rscAndGroupName.getGroupName();
    }

    @XmlElement
    public void setAttrSetGroupName(String group) {
        rscAndGroupName = new RscAndGroupName(rscAndGroupName.getResource(),
                group);
    }

    /**
     * This method retrieves a list of attribute names. If the resources are
     * GRID data, retrieve and save their aliases as well.
     * 
     * @return a String list of attribute names.
     */
    public List<String> getAttrSetNames() {

        AttrSetLabels labels = getAttrSetLabels();
        attrSetNames = new ArrayList<String>();

        if (labels == null) {
            attrSetNames.add("default");
        } else {
            ArrayList<AttrSetLabel> labelList = labels.getLabelList();
            if (labelList != null) {
                for (AttrSetLabel label : labelList) {
                    String name = label.getName();
                    attrSetNames.add(label.getName());

                    // Retrieve and store the alias of a GRID resource.
                    if (getLocalizationFile().getName().contains(gridData)) {
                        String type = getResource();
                        String group = getAttrSetGroupName();
                        String alias = label.getAlias();
                        manager.setAlias(type, group, name, alias);
                    }
                }
            }
        }

        return attrSetNames;
    }

    /**
     * This method sets a list array of attribute set names and creates their
     * corresponding labels to be saved into its xml file.
     * 
     * @param attrSetNames
     *            a list array of attribute set names.
     */
    public void setAttrSetNames(ArrayList<String> attrSetNames) {
        if (attrSetNames != null) {
            attrSetLabels = new AttrSetLabels();
            for (String asName : attrSetNames) {
                attrSetLabels.addLabel(asName);
            }
        }
        this.attrSetNames = attrSetNames;
    }

    /**
     * This method adds a name to the list of attribute set names.
     * 
     * @param asName
     *            a name to be added
     * @return true if the name is not in the list of attribute set names; false
     *         otherwise.
     */
    public boolean addAttrSetName(String asName) {
        if (!attrSetNames.contains(asName)) {
            attrSetLabels.addLabel(asName);
            attrSetNames.add(asName);

            return true;
        }
        return false;
    }

    public AttrSetLabels getAttrSetLabels() {
        return attrSetLabels;
    }

    public void setAttrSetLabels(AttrSetLabels labels) {
        this.attrSetLabels = labels;
    }

    public boolean removeAttrSet(String asName) {
        if (attrSetLabels != null) {
            attrSetLabels.removeLabel(asName);
        }
        return attrSetNames.remove(asName);
    }

    public void removeAllAttrSets() {
        attrSetLabels = null;
        attrSetNames.clear();
        return;
    }

    public LocalizationFile getLocalizationFile() {
        return lclFile;
    }

    public void setLocalizationFile(LocalizationFile lclFile) {
        this.lclFile = lclFile;
    }

}
