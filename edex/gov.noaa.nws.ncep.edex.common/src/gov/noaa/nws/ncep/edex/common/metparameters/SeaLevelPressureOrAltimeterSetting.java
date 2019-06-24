package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
import tec.uom.se.unit.MetricPrefix;

/**
 * Used for PANY,RANY,or SANY. Return appropriately formatted PMSL value if
 * available, else return appropriately formatted ALTM value.
 * 
 * PANY is the raw number rounded up to be a whole number
 * 
 * RANY is a 3 digit representation of PANY. If it turns out to be two digits,
 * it is not padded with a leading zero
 * 
 * SANY is String representation of RANY, and it is padded with leading zeros if
 * needed.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/03/2017   R27759     SRussell     Initial creation
 *
 * </pre>
 *
 * @author Steve Russell
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class SeaLevelPressureOrAltimeterSetting
        extends AbstractMetParameter<Pressure> {

    private final static String MILLIBARS = "mb";

    public SeaLevelPressureOrAltimeterSetting() {
        super(SI.PASCAL);

        // Allow the derive() below to be called even if one of the arguments
        // has a missing value.
        this.overrideCallChildDeriveAnyway = true;
    }

    @DeriveMethod
    public SeaLevelPressureOrAltimeterSetting derive(MeanSeaLevelPres pmsl,
            SeaLevelPressure altm)
            throws InvalidValueException, NullPointerException {

        if (pmsl.hasValidValue()) {
            setValue(pmsl);
        } else if (altm.hasValidValue()) {
            setValue(altm);
        } else {
            setValueToMissing();
        }

        return this;
    }

    @Override
    public String getFormattedString(String formatStr) {

        // Pressure
        double presValInMb = getValue().doubleValue();

        /*
         * Since all three pressures are suppose to be in millibars we convert
         * value to millibars first if it comes in as something else.
         */
        if ((!this.getUnit().toString()
                .equals(MetricPrefix.HECTO(SI.PASCAL).toString())
                && !this.getUnit().toString().equals(MILLIBARS))) {
            presValInMb = this.getUnit()
                    .asType(javax.measure.quantity.Pressure.class)
                    .getConverterTo(NcUnits.MILLIBAR).convert(presValInMb);
        }

        // Format the resulting pressure value
        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {

            return super.getFormattedString(formatStr);

        } else if (formatStr.equalsIgnoreCase("PANY")) {
            // Round up to a whole Number
            int t = (int) Math.round(presValInMb);
            return String.valueOf(t);
        } else if (formatStr.equalsIgnoreCase("RANY")) {
            // Abbreviate the pressure into 3 digits, the tens, the ones,
            // and the tenths.
            Integer iRany = getAbbreviatedPressure(presValInMb);
            return iRany.toString();
        } else if (formatStr.equalsIgnoreCase("SANY")) {
            // Make the pressure abbreviation into a zero padded String
            Integer iSany = getAbbreviatedPressure(presValInMb);
            String sSany = String.format("%03d", iSany);
            return sSany;
        }

        else {
            return super.getFormattedString(formatStr);
        }
    }

    /**
     * Takes in a number representing pressure and abbreviate it to be 3 digits:
     * the tens, ones, and tenths of the pressure value.
     * 
     * Example 1007.6 becomes 76
     * 
     * @param double
     *            presValInMb - pressure value
     * @return
     */
    protected Integer getAbbreviatedPressure(double presValInMb) {
        double times10 = 0.0d;
        double abbrevPress = 0.0;
        Integer abbrevPressInt = null;

        times10 = presValInMb * 10;
        abbrevPress = times10 % 1000;
        abbrevPress = Math.abs(abbrevPress);
        abbrevPressInt = new Integer((int) Math.round(abbrevPress));

        return abbrevPressInt;

    }

}
