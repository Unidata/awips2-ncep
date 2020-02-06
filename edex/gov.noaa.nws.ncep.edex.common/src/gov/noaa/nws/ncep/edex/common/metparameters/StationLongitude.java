package gov.noaa.nws.ncep.edex.common.metparameters;

import java.io.Serializable;

import javax.measure.quantity.Angle;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter SLON
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StationLongitude
        extends AbstractMetParameter<Angle>
        implements Serializable {

    private static final long serialVersionUID = -6858325572623824100L;

    public StationLongitude() throws Exception {
        super(SI.RADIAN);
    }
}
