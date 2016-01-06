package gov.noaa.nws.ncep.edex.plugin.geomag;

/**
 * The Conditions class specifies the conditions that the data must match to
 * determine the appropriate business logic that is to be applied
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer       Description
 * -----------  ----------  ----------      --------------------------
 * 10/07/2015    R11429      sgurung,jtravis Initial creation
 * 
 * </pre>
 * 
 * @author jtravis
 * @version 1
 */

public class Conditions {

    /**
     * Check if any of one of component 1 through component 3 is missing and if
     * component 4 is missing.
     * 
     * @param data
     * @return boolean
     * 
     * @throws ComponentException
     */
    public static boolean isCondition1Valid(GeoMagData data,
            Double missingValueCode) throws ComponentException {

        boolean isConditionMet = false;

        if ((data.getComp1Val().doubleValue() == missingValueCode.doubleValue()
                || data.getComp2Val().doubleValue() == missingValueCode
                        .doubleValue() || data.getComp3Val().doubleValue() == missingValueCode
                .doubleValue())
                && (data.getComp4Val().doubleValue() == missingValueCode
                        .doubleValue())) {

            isConditionMet = true;
        }

        return isConditionMet;

    }

    /**
     * Check if component 1, 2, and 3 are not missing but component 4 is
     * missing.
     * 
     * @param data
     * @return boolean
     * 
     * @throws ComponentException
     */
    public static boolean isCondition2Valid(GeoMagData data,
            Double missingValueCode) throws ComponentException {

        boolean isConditionMet = false;

        if ((data.getComp1Val().doubleValue() != missingValueCode.doubleValue())
                && (data.getComp2Val().doubleValue() != missingValueCode
                        .doubleValue())
                && (data.getComp3Val().doubleValue() != missingValueCode
                        .doubleValue())
                && (data.getComp4Val() == missingValueCode.doubleValue())) {

            isConditionMet = true;

        }

        return isConditionMet;

    }

    /**
     * Check if component 1 or component 2 or component 3 are missing and that
     * component 4 is not missing.
     * 
     * @param data
     * @return boolean
     * 
     * @throws ComponentException
     */
    public static boolean isCondition3Valid(GeoMagData data,
            Double missingValueCode) throws ComponentException {

        boolean isConditionMet = false;

        if ((data.getComp1Val().doubleValue() == missingValueCode.doubleValue()
                || data.getComp2Val().doubleValue() == missingValueCode
                        .doubleValue() || data.getComp3Val().doubleValue() == missingValueCode
                .doubleValue())
                && (data.getComp4Val().doubleValue() != missingValueCode
                        .doubleValue())) {

            isConditionMet = true;

        }

        return isConditionMet;

    }

}