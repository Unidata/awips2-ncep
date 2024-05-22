package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class ConvectivePrecip
        extends AbstractMetParameter<Length>
        implements ISerializableObject {

    private static final long serialVersionUID = -6389631280178712154L;

    public ConvectivePrecip() {
        super(SI.METRE);
    }

}
