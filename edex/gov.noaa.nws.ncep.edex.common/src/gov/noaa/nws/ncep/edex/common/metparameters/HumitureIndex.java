package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tech.units.indriya.AbstractUnit;

/**
 * Maps to the GEMPAK parameter HMTR
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class HumitureIndex
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = 977661518465118018L;

    public HumitureIndex() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    public HumitureIndex derive(AirTemperature t, DewPointTemp dt)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && dt.hasValidValue()) {
            Amount hmtrAmount = PRLibrary.prHmtr(t, dt);
            setValue(hmtrAmount);
        } else
            setValueToMissing();
        return this;
    }

}
