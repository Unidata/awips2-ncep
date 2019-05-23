package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Energy;
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
 * Maps to the GEMPAK parameter LHVP
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class LatentHeatOfVapor
        extends AbstractMetParameter<Energy>
        implements ISerializableObject {

    private static final long serialVersionUID = -249479667379624821L;

    public LatentHeatOfVapor() {
        super(SI.JOULE);
    }

    @DeriveMethod
    public LatentHeatOfVapor derive(AirTemperature t)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue()) {
            Amount val = PRLibrary.prLhvp(t);
            setValue(val);
        } else
            setValueToMissing();
        return this;
    }

}
