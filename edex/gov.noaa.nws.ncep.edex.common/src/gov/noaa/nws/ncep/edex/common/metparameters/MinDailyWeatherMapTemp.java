package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter DMIN
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class MinDailyWeatherMapTemp
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    private static final long serialVersionUID = 2749065884345131481L;

    public MinDailyWeatherMapTemp() {
        super(SI.KELVIN);
    }

    @DeriveMethod
    AbstractMetParameter<Temperature> derive(Min6HrTemp t00x, Min6HrTemp t06x)
            throws InvalidValueException, NullPointerException {
        if (t00x.hasValidValue() && t06x.hasValidValue()) {
            Amount val = PRLibrary.prDmin(t00x, t06x);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }

}
