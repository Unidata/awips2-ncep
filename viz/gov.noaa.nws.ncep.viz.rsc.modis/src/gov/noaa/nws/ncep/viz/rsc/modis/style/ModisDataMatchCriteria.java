package gov.noaa.nws.ncep.viz.rsc.modis.style;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.style.MatchCriteria;
import com.raytheon.uf.common.style.StyleException;

/**
 * Style match criteria for MODIS data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- -------------------------- 
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "modisDataMatches")
public class ModisDataMatchCriteria extends MatchCriteria {

    @XmlElement
    private String parameter;

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.style.MatchCriteria#matches(com.raytheon
     * .uf.common.style.MatchCriteria)
     */
    @Override
    public int matches(MatchCriteria aCriteria) throws StyleException {
        int rval = -1;
        if (aCriteria instanceof ModisRecordCriteria) {
            return ModisRecordCriteria.matches((ModisRecordCriteria) aCriteria,
                    this);
        } else if (aCriteria instanceof ModisDataMatchCriteria) {
            rval = 0;
            ModisDataMatchCriteria criteria = (ModisDataMatchCriteria) aCriteria;
            if (parameter != null && parameter.equals(criteria.parameter)) {
                rval |= (1 << 3);
            }
        }
        return rval;
    }

    /**
     * @return the parameter
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * @param parameter
     *            the parameter to set
     */
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

}
