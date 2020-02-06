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
import tec.uom.se.AbstractUnit;

/**
 * Maps to the GEMPAK parameter FZRN
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class FcstFZRainAccumulationToWatchThresh extends
        AbstractMetParameter<Dimensionless> implements ISerializableObject {

    /**
     * 
     */
    private static final long serialVersionUID = -5030331180297757822L;

    public FcstFZRainAccumulationToWatchThresh() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    AbstractMetParameter<Dimensionless> derive(
            FcstFZRainAccumulationIn12Hours fz12, FZRainWatchThresh fzrt) {
        if (fz12.hasValidValue() && fzrt.hasValidValue()) {
            Amount val = new Amount(fz12.doubleValue() / fzrt.doubleValue(),
                    AbstractUnit.ONE);
            setValue(val);
        } else
            setValueToMissing();
        return this;
    }
}
