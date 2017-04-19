package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;

/**
 * <pre>
 * 
 *  Maps to the GEMPAK parameters SKYC, TCLD
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Unknown      Unknown     Unknown     Created.
 * 01/31/2017   R28380      S.Russell   Added getFormattedString()
 * 
 * 
 * </pre>
 */

public class SkyCoverage extends AbstractMetParameter implements Dimensionless {

    public SkyCoverage() {
        super(UNIT);
        setValueIsString();
    }

    @Override
    public String getFormattedString(String formatStr) {

        String returnValue = this.getValueString();

        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {
            return super.getFormattedString(formatStr);
        } else if (formatStr.equals("TCLD")) {
            if (returnValue.equalsIgnoreCase("FEW")) {
                returnValue = "-SCT";
            }
        }

        return returnValue;

    }
}
