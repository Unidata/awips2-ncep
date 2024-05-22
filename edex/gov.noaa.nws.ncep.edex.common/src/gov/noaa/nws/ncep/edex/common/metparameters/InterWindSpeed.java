/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter ISPD
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class InterWindSpeed
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {
    /**
    * 
    */
    private static final long serialVersionUID = 4938488066790115956L;

    public InterWindSpeed() {
        super(SI.METRE_PER_SECOND); // Velocity
    }
}
