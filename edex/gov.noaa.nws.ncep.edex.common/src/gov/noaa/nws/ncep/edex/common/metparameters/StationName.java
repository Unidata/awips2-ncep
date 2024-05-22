package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tech.units.indriya.AbstractUnit;

/**
 * Maps to the GEMPAK parameter STNM
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StationName
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -5094080499590194267L;

    // StationName is a String and so the Amount value does not apply
    public StationName() {
        super(AbstractUnit.ONE);
        setValueIsString();
    }

    // NOT Implemented. Just enough to execute and return something
    @DeriveMethod
    public AbstractMetParameter<Dimensionless> getStationNameFromID(
            StationID stnId)
            throws InvalidValueException, NullPointerException {
        this.setStringValue(stnId.getStringValue());
        return this;
    }

}
