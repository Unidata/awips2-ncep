/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tec.uom.se.AbstractUnit;

/**
 * Maps to the GEMPAK parameter MIXS
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public final class SatMixingRatio
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -6509723867277103993L;

    public SatMixingRatio() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    public SatMixingRatio derive(DewPointTemp d, PressureLevel p)
            throws InvalidValueException, NullPointerException {
        if (d.hasValidValue() && p.hasValidValue()) {
            Amount mixingRatio = PRLibrary.prMixr(d, p);
            setValue(mixingRatio);
        } else
            setValueToMissing();
        return this;
    }
}
