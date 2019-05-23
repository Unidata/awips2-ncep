package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tec.uom.se.AbstractUnit;

/**
 * Maps to the GEMPAK parameter FOSB
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class FosbergFireWxIndex extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    /**
     * 
     */
    private static final long serialVersionUID = 5452338178762123762L;

    public FosbergFireWxIndex() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    AbstractMetParameter<Dimensionless> derive(AirTemperature t,
            RelativeHumidity r, WindSpeed w)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && r.hasValidValue() && w.hasValidValue()) {
            Amount val = PRLibrary.prFosb(t, r, w);
            setValue(val);
        } else {
            setValueToMissing();
        }
        return this;
    }
}
