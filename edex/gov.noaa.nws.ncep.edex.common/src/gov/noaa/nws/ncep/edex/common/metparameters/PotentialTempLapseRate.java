package gov.noaa.nws.ncep.edex.common.metparameters;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInTemperatureWithHeight;

/**
 * Maps to the GEMPAK parameter STAB
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class PotentialTempLapseRate extends
        AbstractMetParameter<RateOfChangeInTemperatureWithHeight> implements

        ISerializableObject {

    private static final long serialVersionUID = 2377541620177003256L;

    public PotentialTempLapseRate() {
        super(RateOfChangeInTemperatureWithHeight.UNIT);
    }

}