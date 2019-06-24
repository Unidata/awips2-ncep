package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import si.uom.SI;

/**
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 09/29/2011              qzhou       Added STDZ format
 * 09/10/2016   R4151      jeff beck   Changed/added algorithm for STDZ to reference implementation.
 *                                     Now uses pressure and previously did not.
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
public class HeightAboveSeaLevel
        extends AbstractMetParameter<Length> {

    @DynamicSerializeElement
    private static final long serialVersionUID = -3831219033024527337L;

    private PressureLevel pressureLevel;

    // Value needed to convert to decameters in STDZ algorithm
    private static final int DECAMETER_DIVISOR = 10;

    // Value used to MOD in STDZ algorithm
    private static final int MOD_DIVISOR = 1000;

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

        int height = super.getValueAs(SI.METRE).intValue();

        if (pressureLevel == null) {

            // can't calculate STDZ
            return null;
        }

        int pressureLevelValue = pressureLevel.getValue().intValue();
        return calculateSTDZ(pressureLevelValue, height);
    }

    /**
     * 
     * Compute standard height using STDZ algorithm
     * 
     * This function computes a standard height used on upper-air charts. For
     * data below 500mb, the standard height is the last three digits of the
     * height. For data at and above 500mb, the height is the last three digits
     * of the height in decameters.
     * 
     * @param pressureLevelValue
     *            pressure level in mb
     * @param height
     *            height above sea level in meters
     * @return the standard height in meters
     */
    private String calculateSTDZ(int pressureLevelValue, int height) {

        String stdz = "";

        if (pressureLevelValue <= 500) {

            // Data at and above 500mb
            stdz = Integer.toString((height / DECAMETER_DIVISOR) % MOD_DIVISOR);
        }

        else if (pressureLevelValue > 500) {

            // Data below 500mb
            stdz = Integer.toString(height % MOD_DIVISOR);
        }

        return stdz;
    }

    public PressureLevel getPressureLevel() {
        return this.pressureLevel;
    }

    public void setPressureLevel(PressureLevel pressureLevel) {
        this.pressureLevel = pressureLevel;
    }

    public HeightAboveSeaLevel() throws Exception {
        super(SI.METRE);
    }

}
