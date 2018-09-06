package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class GempakProcessingConfig {

    @XmlElement
    private boolean runAsSubprocess;

    @XmlElement
    private int subprocessLimit;

    /**
     * @return the runAsSubprocess
     */
    public boolean isRunAsSubprocess() {
        return runAsSubprocess;
    }

    /**
     * @param runAsSubprocess
     *            the runAsSubprocess to set
     */
    public void setRunAsSubprocess(boolean runAsSubprocess) {
        this.runAsSubprocess = runAsSubprocess;
    }

    /**
     * @return the subprocessLimit
     */
    public int getSubprocessLimit() {
        return subprocessLimit;
    }

    /**
     * @param subprocessLimit
     *            the subprocessLimit to set
     */
    public void setSubprocessLimit(int subprocessLimit) {
        this.subprocessLimit = subprocessLimit;
    }
}
