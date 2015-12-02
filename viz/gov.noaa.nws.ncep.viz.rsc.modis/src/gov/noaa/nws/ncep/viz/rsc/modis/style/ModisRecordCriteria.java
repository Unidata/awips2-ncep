package gov.noaa.nws.ncep.viz.rsc.modis.style;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;

import com.raytheon.uf.common.style.MatchCriteria;
import com.raytheon.uf.common.style.StyleException;

/**
 * Match criteria for a populated {@link ModisRecord} object
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

public class ModisRecordCriteria extends MatchCriteria {

    private String parameter;

    /**
     * Constructor that exists to keep the StyleManager's JAXBManager happy.
     * JAXB will throw an error when introspecting this class if there's not a
     * no-arg constructor.
     */
    protected ModisRecordCriteria() {
    }

    public ModisRecordCriteria(ModisRecord record) {
        this.parameter = record.getParameter();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.style.MatchCriteria#matches(com.raytheon.uf.
     * common.style.MatchCriteria)
     */
    @Override
    public int matches(MatchCriteria aCriteria) throws StyleException {
        int rval = -1;
        if (aCriteria instanceof ModisDataMatchCriteria) {
            return matches(this, (ModisDataMatchCriteria) aCriteria);
        } else if (aCriteria instanceof ModisRecordCriteria) {
            rval = 0;
            ModisRecordCriteria criteria = (ModisRecordCriteria) aCriteria;
            if (parameter != null && parameter.equals(criteria.parameter)) {
                rval |= (1 << 3);
            }
        }
        return rval;
    }

    /**
     * Matches a data record criteria to a Modis style rule criteria.
     * 
     * @param recordCriteria
     * @param matchCriteria
     * @return
     */
    public static int matches(ModisRecordCriteria recordCriteria,
            ModisDataMatchCriteria matchCriteria) {
        int rval = 0;
        // Check parameter
        if (matchCriteria.getParameter() != null
                && matchCriteria.getParameter()
                        .equals(recordCriteria.parameter)) {
            rval |= (1 << 3);
        } else if (matchCriteria.getParameter() != null) {
            --rval;
        }
        return rval;
    }
}
