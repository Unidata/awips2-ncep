package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to GEMPAK parameter SHPK
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class Avg3HrShipSpeed extends AbstractMetParameter<Speed>
        implements ISerializableObject {

    /**
    * 
    */
    private static final long serialVersionUID = 6360391790792140109L;

    public Avg3HrShipSpeed() {
        super(SI.METRE_PER_SECOND);
    }

    @Override
    public String getParameterDescription() {
        return "Average Ship Speed for the last 3 Hours";
    }
}
