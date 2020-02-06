package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to either of the GEMPAK parameters PKWK or PKWS depending on the unit
 * used to measure the wind speed
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class Highest1MinMeanWindSpeedInPastHour
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {

    private static final long serialVersionUID = -4291223174405597214L;

    public Highest1MinMeanWindSpeedInPastHour() {
        super(SI.METRE_PER_SECOND);
    }

}
