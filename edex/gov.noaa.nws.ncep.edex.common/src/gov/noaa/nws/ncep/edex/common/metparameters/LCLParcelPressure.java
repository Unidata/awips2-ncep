package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
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
 * Maps to the GEMPAK parameter PLCL
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class LCLParcelPressure
        extends AbstractMetParameter<Pressure>
        implements ISerializableObject {

    private static final long serialVersionUID = -1959728817791370835L;

    public LCLParcelPressure() {
        super(SI.PASCAL);
    }

    @DeriveMethod
    public LCLParcelPressure derive(AirTemperature t, PressureLevel p,
            LCLParcelTemperature parcelTemp)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && p.hasValidValue()
                && parcelTemp.hasValidValue()) {
            Amount val = PRLibrary.prPlcl(t, p, parcelTemp);
            setValue(val);
        } else
            setValueToMissing();
        return this;
    }

}
