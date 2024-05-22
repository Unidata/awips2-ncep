package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter VSTM
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class EstStormDirectionVComp
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {

    private static final long serialVersionUID = -6158382200072441442L;

    public EstStormDirectionVComp() {
        super(SI.METRE_PER_SECOND);
    }

}
