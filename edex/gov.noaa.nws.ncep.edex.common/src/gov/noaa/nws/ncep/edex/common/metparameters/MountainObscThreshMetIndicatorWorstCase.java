package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tech.units.indriya.AbstractUnit;

/**
 * Maps to the GEMPAK parameter WMOB
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class MountainObscThreshMetIndicatorWorstCase
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -7188732050781835300L;

    public MountainObscThreshMetIndicatorWorstCase() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    AbstractMetParameter<Dimensionless> derive(CeilingFromSeaLevelWorstCase wcms,
            MountainObscThresh motv)
            throws InvalidValueException, NullPointerException {
        if (wcms.hasValidValue() && motv.hasValidValue()) {
            Amount val = PRLibrary.prMobs(wcms, motv);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }
}
