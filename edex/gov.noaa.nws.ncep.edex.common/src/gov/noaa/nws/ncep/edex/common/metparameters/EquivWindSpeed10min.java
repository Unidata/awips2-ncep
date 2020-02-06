/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter SP10
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class EquivWindSpeed10min
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {

    private static final long serialVersionUID = 1212777270519641600L;

    public EquivWindSpeed10min() {
        super(SI.METRE_PER_SECOND); // Velocity
    }

}
