package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter PMPH
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class PeakWindSpeed
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {
    private static final long serialVersionUID = 1574677024600899549L;

    public PeakWindSpeed() {
        super(SI.METRE_PER_SECOND);
    }
}
