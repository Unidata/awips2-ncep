package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.units.UnitConv;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import si.uom.NonSI;
import si.uom.SI;

/**
 * Maps to either of the GEMPAK parameters ALTI or ALTM depending on the unit
 * used to measure the sea level pressure
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class SeaLevelPressure extends AbstractMetParameter<Pressure> 
        implements ISerializableObject {

    private static final long serialVersionUID = -1025585414782928040L;

    public SeaLevelPressure() throws Exception {
        super(SI.PASCAL);
    }

    @Override
    public String getFormattedString(String formatStr) {

        if (formatStr == null || formatStr.isEmpty()
                || formatStr.startsWith("%")) {
            return super.getFormattedString(formatStr);
        }

        else if ((formatStr.compareToIgnoreCase("RSLT") == 0)
                || (formatStr.compareToIgnoreCase("SALT") == 0)) {
            double newPresValInMb = Double.NaN;
            if ((this.getUnit().toString().compareTo("mb") != 0)) {
                double oldPresVal = getValue().doubleValue();
                newPresValInMb = UnitConv.getConverterToUnchecked(this.getUnit(),
                        NcUnits.MILLIBAR).convert(oldPresVal);
            }

            double abbrevPressVal = (newPresValInMb % 100) * 10;
            abbrevPressVal = Math.round((abbrevPressVal * 100) / 100);
            Integer abbrevpressValAsInt = new Integer((int) abbrevPressVal);
            String abbrevPressureString = abbrevpressValAsInt.toString();
            return abbrevPressureString;
        }

        else if ((formatStr.compareToIgnoreCase("RSLI") == 0)
                || (formatStr.compareToIgnoreCase("SALI") == 0)) {

            double newPresValInMb = Double.NaN;
            double oldPresVal = getValue().doubleValue();
            if ((this.getUnit().toString().compareTo("inHg") != 0)) {
                newPresValInMb = UnitConv.getConverterToUnchecked(this.getUnit(),
                        NonSI.INCH_OF_MERCURY)
                        .convert(oldPresVal);
            } else {
                newPresValInMb = oldPresVal;
            }

            double abbrevPressVal = (newPresValInMb % 10) * 100;
            abbrevPressVal = Math.round((abbrevPressVal * 100) / 100);
            Integer abbrevpressValAsInt = new Integer((int) abbrevPressVal);
            String abbrevPressureString = abbrevpressValAsInt.toString();
            abbrevPressureString = String.format("%03d",
                    Integer.parseInt(abbrevPressureString));

            return abbrevPressureString;
        }

        else
            return super.getFormattedString(formatStr);
    }

}
