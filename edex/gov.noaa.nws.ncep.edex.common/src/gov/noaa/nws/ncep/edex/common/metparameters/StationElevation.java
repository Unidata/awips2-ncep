package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.units.UnitAdapter;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

/**
 * Maps to the GEMPAK parameter SELV
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StationElevation extends AbstractMetParameter
        implements javax.measure.quantity.Length {

    public StationElevation() throws Exception {
        super(new UnitAdapter().marshal(UNIT));
    }

    // TODO : check that this is stationPressure is correct here
    @DeriveMethod
    AbstractMetParameter derive(SeaLevelPressure alti, SurfacePressure pres)
            throws InvalidValueException, NullPointerException {
        if (alti.hasValidValue() && pres.hasValidValue()) {
            Amount val = PRLibrary.prZalt(alti, pres);
            setValue(val);
        } else
            setValueToMissing();
        return this;
    }

    @Override
    public String getFormattedString(String formatStr) {

        Double dataInFeet = getValue().doubleValue();

        if (formatStr.equalsIgnoreCase("FELV")) {
            return convertToHundredsOfFeet(dataInFeet);
        } else {
            return super.getFormattedString(formatStr);
        }
    }

    /**
     * Convert from feet to hundreds-of-feet
     * 
     * @param feet
     * @return
     */
    public static String convertToHundredsOfFeet(Double feet) {
        double hundredsOfFeet = feet;
        String sHundredsOfFeet = null;

        hundredsOfFeet = hundredsOfFeet * 0.01;
        hundredsOfFeet = Math.round(hundredsOfFeet);
        sHundredsOfFeet = String.valueOf(hundredsOfFeet);

        return sHundredsOfFeet;

    }

}
