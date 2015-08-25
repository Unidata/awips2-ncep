/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.common.tsScaleMngr;

import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.serialization.ISerializableObject;

/**
 * Implementation of a GraphAttributes
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 15, 2014   R4875       sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

@XmlRootElement(name = "xAxisScale")
@XmlAccessorType(XmlAccessType.NONE)
public class XAxisScale extends AbstractXAxisScale implements
        ISerializableObject {

    @XmlElements({ @XmlElement(name = "xAxisScaleElement", type = XAxisScaleElement.class) })
    protected List<XAxisScaleElement> xAxisScaleElements;

    @XmlAttribute
    protected String name;

    // This is only set if created from a saved file as opposed
    // to being edited.
    protected LocalizationFile lFile;

    /**
     * Constructor used by JiBX
     */
    public XAxisScale() {
        this.xAxisScaleElements = new ArrayList<XAxisScaleElement>();
    }

    public XAxisScale(XAxisScale sc) {
        xAxisScaleElements = new ArrayList<XAxisScaleElement>();
        name = new String(sc.name);

        for (XAxisScaleElement xsc : sc.getXAxisScaleElements()) {
            XAxisScaleElement newXsc = new XAxisScaleElement();
            newXsc.setDuration(xsc.getDuration());
            newXsc.setLabelInterval(xsc.getLabelInterval());
            newXsc.setMajorTickInterval(xsc.getMajorTickInterval());
            newXsc.setMinorTickInterval(xsc.getMinorTickInterval());
            newXsc.setLabelFormat(xsc.getLabelFormat());
            xAxisScaleElements.add(newXsc);
        }

        lFile = sc.lFile;
    }

    public String createLocalizationFilename() {
        return NcPathConstants.XAXIS_SCALE_DIR + File.separator + name + ".xml";
    }

    public int[] getDurations() {
        int condNum = xAxisScaleElements.size();
        int[] duration = new int[condNum];
        for (int i = 0; i < condNum; ++i) {
            duration[i] = xAxisScaleElements.get(i).getDuration();
        }
        return duration;
    }

    public int[] getLabelIntervals() {
        int condNum = xAxisScaleElements.size();
        int[] labelInterval = new int[condNum];
        for (int i = 0; i < condNum; ++i) {
            labelInterval[i] = xAxisScaleElements.get(i).getLabelInterval();
        }
        return labelInterval;
    }

    public int[] getMajorTickIntervals() {
        int condNum = xAxisScaleElements.size();
        int[] majorTickInterval = new int[condNum];
        for (int i = 0; i < condNum; ++i) {
            majorTickInterval[i] = xAxisScaleElements.get(i)
                    .getMajorTickInterval();
        }
        return majorTickInterval;
    }

    public int[] getMinorTickIntervals() {
        int condNum = xAxisScaleElements.size();
        int[] minorTickInterval = new int[condNum];
        for (int i = 0; i < condNum; ++i) {
            minorTickInterval[i] = xAxisScaleElements.get(i)
                    .getMinorTickInterval();
        }
        return minorTickInterval;
    }

    public String[] getLabelFormats() {
        int condNum = xAxisScaleElements.size();
        String[] labelFormat = new String[condNum];
        for (int i = 0; i < condNum; ++i) {
            labelFormat[i] = xAxisScaleElements.get(i).getLabelFormat();
        }
        return labelFormat;
    }

    public int getSize() {
        return xAxisScaleElements.size();
    }

    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer xscStr = new StringBuffer(getName());

        xscStr.append(" {" + getxAxisScaleAsString() + "}");

        return xscStr.toString().trim();
    }

    public String getxAxisScaleAsString() {
        StringBuffer xscStr = new StringBuffer();

        for (XAxisScaleElement xsc : getXAxisScaleElements()) {
            xscStr.append((xscStr.length() != 0 ? " AND " : "")
                    + xsc.toString());
        }
        return xscStr.toString().trim();
    }

    public void setName(String aName) {
        name = aName;
    }

    public LocalizationFile getLocalizationFile() {
        return lFile;
    }

    public void setLocalizationFile(LocalizationFile lFile) {
        this.lFile = lFile;
    }

    /**
     * Gets the value of the xAxisScaleElements property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the xAxisScaleElements property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getxAxisScaleElements().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XAxisScaleElement }
     * 
     * 
     */
    public List<XAxisScaleElement> getXAxisScaleElements() {
        if (xAxisScaleElements == null) {
            xAxisScaleElements = new ArrayList<XAxisScaleElement>();
        }
        return this.xAxisScaleElements;
    }

    @Override
    public XAxisScale clone() {
        return new XAxisScale(this);
    }

    public XAxisScaleElement getXAxisScaleElement(int index) {
        if (xAxisScaleElements != null) {
            return xAxisScaleElements.get(index);
        }
        return null;
    }

    public HashMap<Integer, XAxisScaleElement> getxAxisScaleMap() {

        HashMap<Integer, XAxisScaleElement> xAxisScaleMap = new HashMap<Integer, XAxisScaleElement>();

        if (xAxisScaleElements != null) {
            for (XAxisScaleElement xsce : xAxisScaleElements) {
                xAxisScaleMap.put(xsce.getDuration(), xsce);
            }
        }

        return xAxisScaleMap;
    }

    public List<XAxisScaleElement> getxAxisScaleElements() {
        if (xAxisScaleElements == null) {
            xAxisScaleElements = new ArrayList<XAxisScaleElement>();
        }
        return xAxisScaleElements;
    }

    public void setxAxisScaleElements(List<XAxisScaleElement> xAxisScaleElements) {
        this.xAxisScaleElements = xAxisScaleElements;
    }

}
