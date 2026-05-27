package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Time;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to GEMPAK parameters STIM
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class ReportTimeInHourMins
        extends AbstractMetParameter<Time>
        implements ISerializableObject {

    private static final long serialVersionUID = -838483615539473683L;

    public ReportTimeInHourMins() {
        super(SI.SECOND);
    }

}
