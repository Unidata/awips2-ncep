package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter VAPS
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class SatVaporPressure
        extends AbstractMetParameter<Pressure>
        implements ISerializableObject {

    private static final long serialVersionUID = -6945752189065771295L;

    public SatVaporPressure() {
        super(SI.PASCAL);
    }

    @DeriveMethod
    public SatVaporPressure derive(AirTemperature t)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue()) {
            Amount vaporPresAmount = PRLibrary.prVapr(t);
            this.setValue(vaporPresAmount);
        } else
            setValueToMissing();
        return this;
    }
}