package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tech.units.indriya.AbstractUnit;

//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 

/**
 * Maps to the GEMPAK parameter RELH
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class RelativeHumidity
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    @DynamicSerializeElement
    private static final long serialVersionUID = 1580959009469861384L;

    public RelativeHumidity() throws Exception {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    public RelativeHumidity derive(AirTemperature t, DewPointTemp d)
            throws InvalidValueException, NullPointerException {

        if (t.hasValidValue() && d.hasValidValue()) {
            Amount theRelhAmount = PRLibrary.prRelh(t, d);
            this.setValue(theRelhAmount);
        } else
            this.setValueToMissing();

        return this;
    }
}
