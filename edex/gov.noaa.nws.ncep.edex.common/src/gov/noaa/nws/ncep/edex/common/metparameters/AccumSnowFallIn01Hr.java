package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * 
 * Maps to the modelsounding parameter snowWater
 *
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AccumSnowFallIn01Hr
        extends AbstractMetParameter<Length>
        implements ISerializableObject {

    private static final long serialVersionUID = -6822860327143776139L;

    public AccumSnowFallIn01Hr() {
        super(SI.METRE);
    }
}
