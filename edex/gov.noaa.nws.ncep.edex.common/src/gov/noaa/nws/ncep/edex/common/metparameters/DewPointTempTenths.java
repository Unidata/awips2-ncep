package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * 
 * Used to hold value of temperature (In Celsius) from the remarks section of
 * observation. This has the temperature to a tenth of a degree. A new parameter
 * was created for AWIPS2 TMPT
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 5/11/2016     R6154       J.Huber     Initial creation
 * </pre>
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class DewPointTempTenths
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    @DynamicSerializeElement
    private static final long serialVersionUID = 8432204748729755161L;

    public DewPointTempTenths() {
        super(SI.KELVIN);
    }

    @DeriveMethod
    public DewPointTempTenths derive(MixingRatio m, PressureLevel p)
            throws InvalidValueException, NullPointerException {

        if (m.hasValidValue() && p.hasValidValue()) {
            Amount theDewpointTemperatureAmount = PRLibrary.prDwpt(m, p);
            this.setValue(theDewpointTemperatureAmount);
        } else
            setValueToMissing();
        return this;
    }

    @DeriveMethod
    public DewPointTempTenths derive(SurfaceMixingRatio m, SurfacePressure p)
            throws InvalidValueException, NullPointerException {

        if (m.hasValidValue() && p.hasValidValue()) {
            Amount theDewpointTemperatureAmount = PRLibrary.prDwpt(m, p);
            this.setValue(theDewpointTemperatureAmount);
        } else
            setValueToMissing();
        return this;
    }

    @DeriveMethod
    public DewPointTempTenths derive(AirTemperature t, DewPointDepression dp)
            throws InvalidValueException, NullPointerException {

        if (t.hasValidValue() && dp.hasValidValue()) {
            Amount theDewpointTemperatureAmount = PRLibrary.prDwdp(t, dp);
            this.setValue(theDewpointTemperatureAmount);
        } else
            setValueToMissing();

        return this;
    }

    @DeriveMethod
    public DewPointTempTenths derive(AirTemperature t, RelativeHumidity rh)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && rh.hasValidValue()) {
            Amount dewpointAmount = PRLibrary.prRhdp(t, rh);
            this.setValue(dewpointAmount);
        } else
            setValueToMissing();
        return this;
    }

}
