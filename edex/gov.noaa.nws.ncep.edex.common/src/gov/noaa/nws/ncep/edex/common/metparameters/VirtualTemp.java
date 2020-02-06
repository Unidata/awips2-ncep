package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

/**
 *
 * Maps to any of the GEMPAK parameters TVRK/TVRC or TVRF depending on the units
 * in which the virtual temperature is to be computed.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/13/2016   R27046     SRussell    Uncommented out second version of
 *                                     computeVirtualTemp()
 * </pre>
 * 
 * @author ?
 * @version 1.0
 */

public class VirtualTemp extends AbstractMetParameter<Temperature> {

    public VirtualTemp() {
        super(SI.KELVIN);
    }

    // TODO test this to see if the name of the derive method can be anything
    // other than 'derive'
    @DeriveMethod
    public VirtualTemp computeVirtualTemp(AirTemperature t, DewPointTemp d,
            PressureLevel p)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && d.hasValidValue() && p.hasValidValue()) {
            Amount virtualTempAmount = PRLibrary.prTvrk(t, d, p);
            setValue(virtualTempAmount);
        } else
            setValueToMissing();

        return this;
    }

    @DeriveMethod
    public VirtualTemp computeVirtualTemp(AirTemperature t, DewPointTemp d,
            SurfacePressure p)
            throws InvalidValueException, NullPointerException {
        if (t.hasValidValue() && d.hasValidValue() && p.hasValidValue()) {
            Amount virtualTempAmount = PRLibrary.prTvrk(t, d, p);
            setValue(virtualTempAmount);
        } else
            setValueToMissing();

        return this;
    }

}
