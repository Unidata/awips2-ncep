/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter THTA
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public final class PotentialTemp
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    private static final long serialVersionUID = -273204188466291660L;

    public PotentialTemp() throws Exception {
        super(SI.KELVIN);
    }

    @DeriveMethod
    public PotentialTemp derive(AirTemperature t, PressureLevel p)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && p.hasValidValue()) {
            Amount thePotentialTempAmount = PRLibrary.prThta(t, p);
            this.setValue(thePotentialTempAmount);
        } else
            setValueToMissing();
        return this;
    }

}
