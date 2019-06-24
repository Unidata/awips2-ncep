package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Time;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to either the GEMPAK parameter PKWK or PKWS
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class PeakWindSpeedTime
        extends AbstractMetParameter<Time>
        implements ISerializableObject {

    private static final long serialVersionUID = 3843485044342999552L;

    public PeakWindSpeedTime() {
        super(SI.SECOND);
    }
}
