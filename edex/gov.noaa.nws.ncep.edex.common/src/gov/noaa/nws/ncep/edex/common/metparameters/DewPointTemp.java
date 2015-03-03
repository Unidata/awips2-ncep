package gov.noaa.nws.ncep.edex.common.metparameters;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
/**
 * Maps to any of the GEMPAK parameters DWPC/ DWPK/DWPF depending on the unit
 * used to measure the dewpoint.
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class DewPointTemp extends AbstractMetParameter implements javax.measure.quantity.Temperature, ISerializableObject {

    /**
	 * 
	 */
    @DynamicSerializeElement
    private static final long serialVersionUID = 8432204748729755161L;

    public DewPointTemp() {
        super(UNIT);
    }

    @DeriveMethod
    public DewPointTemp derive(MixingRatio m, PressureLevel p) throws InvalidValueException, NullPointerException {

        if (m.hasValidValue() && p.hasValidValue()) {
            Amount theDewpointTemperatureAmount = PRLibrary.prDwpt(m, p);
            this.setValue(theDewpointTemperatureAmount);
        } else
            setValueToMissing();
        return this;
    }

    @DeriveMethod
    public DewPointTemp derive(SurfaceMixingRatio m, SurfacePressure p) throws InvalidValueException, NullPointerException {

        if (m.hasValidValue() && p.hasValidValue()) {
            Amount theDewpointTemperatureAmount = PRLibrary.prDwpt(m, p);
            this.setValue(theDewpointTemperatureAmount);
        } else
            setValueToMissing();
        return this;
    }

    @DeriveMethod
    public DewPointTemp derive(AirTemperature t, DewPointDepression dp) throws InvalidValueException, NullPointerException {

        if (t.hasValidValue() && dp.hasValidValue()) {
            Amount theDewpointTemperatureAmount = PRLibrary.prDwdp(t, dp);
            this.setValue(theDewpointTemperatureAmount);
        } else
            setValueToMissing();

        return this;
    }

    @DeriveMethod
    public DewPointTemp derive(AirTemperature t, RelativeHumidity rh) throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && rh.hasValidValue()) {
            Amount dewpointAmount = PRLibrary.prRhdp(t, rh);
            this.setValue(dewpointAmount);
        } else
            setValueToMissing();
        return this;
    }

}
