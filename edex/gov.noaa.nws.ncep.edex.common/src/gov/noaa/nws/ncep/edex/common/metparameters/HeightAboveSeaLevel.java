package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.units.UnitAdapter;

/**
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 09/29/2011              qzhou       Added STDZ format
 * 09/10/2016   R4151      Jeff Beck   Changed/added algorithm for STDZ to reference implementation.
 *                                     Now uses pressure and previously did not.
 * 04/11/2017   R28887     Jeff Beck   Corrected the calculataion of standard height in calculateSTDZ()
 *                                     so that there will always be 3 digits to display in upper air plots.
 * </pre> 
 * @author  qzhou
 * @version 1.0
 */

/**
 * Maps to either of the GEMPAK parameters HGHT or STDZ
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class HeightAboveSeaLevel extends AbstractMetParameter
        implements Length {

    @DynamicSerializeElement
    private static final long serialVersionUID = -3831219033024527337L;

    private PressureLevel pressureLevel;

    private static final int DECAMETER_DIVISOR = 10;

    /*
     * (non-Javadoc) Run the STDZ algorithm if conditions are met
     * 
     * @see gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter#
     * getFormattedString(java.lang.String)
     */
    @Override
    public String getFormattedString(String formatStr) {

        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {
            return super.getFormattedString(formatStr);

        } else if (!formatStr.equals("STDZ")) {
            return super.getFormattedString(formatStr);
        }

        int height = super.getValueAs(SI.METER).intValue();

        if (pressureLevel == null) {

            // can't calculate STDZ
            return null;
        }

        int pressureLevelValue = pressureLevel.getValue().intValue();
        return calculateSTDZ(pressureLevelValue, height);
    }

    /**
     * 
     * Compute standard height using PR_STDZ algorithm
     * 
     * This function computes a standard height used on upper-air charts.
     * 
     * For pressure levels below 500mb, the standard height is defined as the
     * last three digits of the height above sea level.
     * 
     * For pressure levels at and above 500mb, the standard height is defined as
     * the last three digits of the height expressed in decameters.
     * 
     * Special Case:
     * 
     * If we are at a very high pressure near the surface, the height may be
     * only 1 or 2 digits. In this case, we pad with zeros to get the standard
     * height (as observed in legacy).
     * 
     * 
     * @param pressureLevelValue
     *            pressure level in mb
     * 
     * @param height
     *            height above sea level in meters
     * 
     * @return the standard height in meters
     */
    private String calculateSTDZ(int pressureLevelValue, int height) {

        String stdz = "";
        int len = 0;

        if (pressureLevelValue <= 500) {
            stdz = Integer.toString(height / DECAMETER_DIVISOR);

        } else {
            stdz = Integer.toString(height);
        }

        len = stdz.length();

        if (len <= 3) {
            return (String.format("%03d", Integer.parseInt(stdz)));
        } else {
            return stdz.substring(len - 3, len);
        }
    }

    public PressureLevel getPressureLevel() {
        return this.pressureLevel;
    }

    public void setPressureLevel(PressureLevel pressureLevel) {
        this.pressureLevel = pressureLevel;
    }

    public HeightAboveSeaLevel() throws Exception {
        super(new UnitAdapter().marshal(UNIT));
    }

}
