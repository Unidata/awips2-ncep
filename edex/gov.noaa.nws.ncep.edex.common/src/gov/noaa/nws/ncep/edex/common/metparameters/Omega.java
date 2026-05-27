/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInPressureWithTime;

/**
 * Maps to the GEMPAK parameter OMEG
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class Omega extends AbstractMetParameter<RateOfChangeInPressureWithTime>
        implements ISerializableObject {

    @DynamicSerializeElement
    private static final long serialVersionUID = -264798634764916223L;

    public Omega() throws Exception {
        super(RateOfChangeInPressureWithTime.UNIT);
    }
}
