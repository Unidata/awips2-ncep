package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter DPRN
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class DPRN extends AbstractMetParameter<Length>
        implements ISerializableObject {

    private static final long serialVersionUID = 4784319145861079670L;

    public DPRN() {
        super(SI.METRE);
    }

    @DeriveMethod
    AbstractMetParameter derive(Precip24Hr pt, MaxPrecipPR6X mp) {
        if (pt.hasValidValue() && mp.hasValidValue()) {
            setValue(pt.doubleValue() > mp.doubleValue() ? pt : mp);
        } else
            setValueToMissing();

        return this;
    }
}
