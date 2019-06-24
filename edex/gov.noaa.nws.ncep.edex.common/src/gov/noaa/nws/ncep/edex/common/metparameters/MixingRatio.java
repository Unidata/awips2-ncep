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
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tec.uom.se.AbstractUnit;

/**
 * Maps to the GEMPAK parameter MIXR
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public final class MixingRatio
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -2756891504664905612L;

    public MixingRatio() throws Exception {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    public MixingRatio derive(DewPointTemp d, PressureLevel p)
            throws InvalidValueException, NullPointerException {
        if (d.hasValidValue() && p.hasValidValue()) {
            Amount mixingRatio = PRLibrary.prMixr(d, p);
            this.setValue(mixingRatio);
        } else
            setValueToMissing();
        return this;
    }
}
