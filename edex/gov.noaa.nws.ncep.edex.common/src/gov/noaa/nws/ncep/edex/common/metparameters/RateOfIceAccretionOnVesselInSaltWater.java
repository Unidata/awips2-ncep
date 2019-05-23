package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Speed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter IGRO
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class RateOfIceAccretionOnVesselInSaltWater
        extends AbstractMetParameter<Speed>
        implements ISerializableObject {

    private static final long serialVersionUID = 4592641986498306172L;

    public RateOfIceAccretionOnVesselInSaltWater() {
        super(SI.METRE_PER_SECOND);
    }

    @DeriveMethod
    AbstractMetParameter<Speed> derive(AirTemperature airTemp,
            SeaSurfaceTemp seaTemp, WindSpeed ws)
            throws InvalidValueException, NullPointerException {
        if (airTemp.hasValidValue() && seaTemp.hasValidValue()
                && ws.hasValidValue()) {
            Amount val = PRLibrary.prIgro(airTemp, seaTemp, ws);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }

}
