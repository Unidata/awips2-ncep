package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter SELV
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StationElevation
        extends AbstractMetParameter<Length> {

    public StationElevation() throws Exception {
        super(SI.METRE);
    }

    // TODO : check that this is stationPressure is correct here
    @DeriveMethod
    AbstractMetParameter<Length> derive(
            SeaLevelPressure alti, SurfacePressure pres)
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
