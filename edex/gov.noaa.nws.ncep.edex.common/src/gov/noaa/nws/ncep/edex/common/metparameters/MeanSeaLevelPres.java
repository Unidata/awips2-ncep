package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.units.UnitConv;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
import tec.uom.se.unit.MetricPrefix;

/**
 * <pre>
 * 
 *  Maps to the GEMPAK parameter PMSL
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 07/20/2016    R15950     J. Huber    Code cleanup and fixed incorrect RMSL/SMSL 
 *                                      format strings
 * 
 * 
 * </pre>
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class MeanSeaLevelPres
        extends AbstractMetParameter<Pressure> {

    private final static String PMSL = "PMSL";

    private final static String RMSL = "RMSL";

    private final static String SMSL = "SMSL";

    private final static String MILLIBARS = "mb";

    public MeanSeaLevelPres() {
        super(SI.PASCAL);
    }

    @DeriveMethod
    public MeanSeaLevelPres derive(PressureLevel prs, AirTemperature t,
            DewPointTemp dpt, HeightAboveSeaLevel hght)
            throws InvalidValueException, NullPointerException {
        if (prs.hasValidValue() && t.hasValidValue() && dpt.hasValidValue()
                && hght.hasValidValue()) {
            Amount pmsl = PRLibrary.prPmsl(prs, t, dpt, hght);
            if (pmsl.hasValidValue()) {
                setValue(pmsl);
            }
        } else {
            setValueToMissing();
        }

        return this;
    }

    @Override
    public String getFormattedString(String formatStr) {

        double presValInMb = getValue().doubleValue();

        /*
         * Since all three pressures are suppose to be in millibars we convert
         * value to millibars first if it comes in as something else.
         */
        if ((!this.getUnit().toString()
                .equals(MetricPrefix.HECTO(SI.PASCAL).toString())
                && !this.getUnit().toString().equals(MILLIBARS))) {
            presValInMb = UnitConv.getConverterToUnchecked(this.getUnit(), NcUnits.MILLIBAR)
                    .convert(presValInMb);
        }

        /*
         * There is currently a bug in the METAR Decoder which decodes values in
         * hPa but labels the data as in Pa. Multiply the value by 100 to adjust
         * the value to what it should be
         */
        if (presValInMb < 100) {
            presValInMb *= 100;
        }

        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {
            return super.getFormattedString(formatStr);

        } else if ((formatStr.equals(RMSL)) || (formatStr.equals(SMSL))) {

            /*
             * RMSL: tens, ones, tenths place of pressure not 0-padded SMSL:
             * same as RMSL but 0-padded.
             * 
             * Example pressures: 1007.6
             * 
             * RMSL: 76 SMSL: 076
             * 
             * 983.6 RMSL and SMSL: 836
             */
            double temp = presValInMb * 10;
            double abbrevPressVal = temp % 1000;
            abbrevPressVal = Math.abs(abbrevPressVal);
            Integer abbrevpressValAsInt = new Integer(
                    (int) Math.round(abbrevPressVal));
            String abbrevPressureString = abbrevpressValAsInt.toString();

            /*
             * Create zero-padding for SMSL
             */
            if (formatStr.equals(SMSL) && abbrevPressureString.length() < 3) {

                abbrevPressureString = String.format("%03d",
                        Integer.parseInt(abbrevPressureString));

            }
            return abbrevPressureString;

        } else if (formatStr.equals(PMSL)) {
            /*
             * This is the same as format %4.0f
             */
            int t = (int) Math.round(presValInMb);
            return String.valueOf(t);
        } else {
            return super.getFormattedString(formatStr);
        }
    }

}
