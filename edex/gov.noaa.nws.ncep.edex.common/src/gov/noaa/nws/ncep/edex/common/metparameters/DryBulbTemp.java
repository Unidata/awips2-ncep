package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class DryBulbTemp
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    private static final long serialVersionUID = 3026150096446118901L;

    public DryBulbTemp() {
        super(SI.KELVIN);
    }

}
