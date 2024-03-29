package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Angle;
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
 * Maps to the GEMPAK parameter STMD
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StormMotionDirection extends AbstractMetParameter<Angle>
        implements ISerializableObject {

    /**
     * 
     */
    private static final long serialVersionUID = 7198383505591927665L;

    public StormMotionDirection() {
        super(SI.RADIAN);
    }

    @DeriveMethod
    AbstractMetParameter<Angle> derive(EstStormDirectionUComp u,
            EstStormDirectionVComp v)
            throws InvalidValueException, NullPointerException {
        if (u.hasValidValue() && v.hasValidValue()) {
            Amount val = PRLibrary.prDrct(u, v);
            setValue(val);
        } else
            setValueToMissing();
        return this;
    }
}