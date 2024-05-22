package gov.noaa.nws.ncep.edex.common.metparameters;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInTemperatureWithPressure;

/**
 * Maps to the GEMPAK parameter STAP
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class StabilityWithRespectToPressure
        extends AbstractMetParameter<RateOfChangeInTemperatureWithPressure>
        implements ISerializableObject {

    private static final long serialVersionUID = 4499398695128635919L;

    public StabilityWithRespectToPressure() {
        super(RateOfChangeInTemperatureWithPressure.UNIT);
    }

}
