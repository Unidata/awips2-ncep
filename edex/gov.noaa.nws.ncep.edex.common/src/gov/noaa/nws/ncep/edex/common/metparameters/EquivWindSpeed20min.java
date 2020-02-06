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
 * Maps to the GEMPAK parameter SP20
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class EquivWindSpeed20min
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {

    private static final long serialVersionUID = -8503686810160432294L;

    public EquivWindSpeed20min() {
        super(SI.METRE_PER_SECOND);
    }

}
