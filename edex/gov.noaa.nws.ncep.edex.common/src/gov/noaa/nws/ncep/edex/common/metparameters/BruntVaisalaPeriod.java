package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Time;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class BruntVaisalaPeriod
        extends AbstractMetParameter<Time>
        implements ISerializableObject {

    private static final long serialVersionUID = -8554062919999416630L;

    public BruntVaisalaPeriod() {
        super(SI.SECOND);
    }
}
