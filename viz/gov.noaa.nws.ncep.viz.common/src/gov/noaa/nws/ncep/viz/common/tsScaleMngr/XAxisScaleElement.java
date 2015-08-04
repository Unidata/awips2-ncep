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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * Implementation of XAxisScaleElement class
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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "xAxisScaleElement")
public class XAxisScaleElement {
    @XmlAttribute(name = "durationHour")
    private int duration;

    @XmlAttribute(name = "labelInterval")
    private int labelInterval;

    @XmlAttribute(name = "majorTickInterval")
    private int majorTickInterval;

    @XmlAttribute(name = "minorTickInterval")
    private int minorTickInterval;

    @XmlAttribute(name = "labelFormat")
    private String labelFormat;

    public XAxisScaleElement() {
        duration = 0;
        labelInterval = 0;
        majorTickInterval = 0;
        minorTickInterval = 0;
        labelFormat = "HHmm";
    }

    public XAxisScaleElement(int pName, int cType, int val1, int val2,
            String labelFormat) {
        this.duration = pName;
        this.labelInterval = cType;
        this.majorTickInterval = val1;
        this.minorTickInterval = val2;
        this.labelFormat = labelFormat;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getLabelInterval() {
        return labelInterval;
    }

    public void setLabelInterval(int labelInterval) {
        this.labelInterval = labelInterval;
    }

    public int getMajorTickInterval() {
        return majorTickInterval;
    }

    public void setMajorTickInterval(int majorTickInterval) {
        this.majorTickInterval = majorTickInterval;
    }

    public int getMinorTickInterval() {
        return minorTickInterval;
    }

    public void setMinorTickInterval(int minorTickInterval) {
        this.minorTickInterval = minorTickInterval;
    }

    public String getLabelFormat() {
        return labelFormat;
    }

    public void setLabelFormat(String labelFormat) {
        this.labelFormat = labelFormat;
    }

    @Override
    public String toString() {
        return duration + " " + labelInterval + " " + majorTickInterval + " "
                + minorTickInterval + " " + labelFormat;
    }
}
