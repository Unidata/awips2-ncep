package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Angle;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter SLAT
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StationLatitude
        extends AbstractMetParameter<Angle>
        implements ISerializableObject {

    private static final long serialVersionUID = -7368446769312114604L;

    public StationLatitude() throws Exception {
        super(SI.RADIAN);
    }

}
