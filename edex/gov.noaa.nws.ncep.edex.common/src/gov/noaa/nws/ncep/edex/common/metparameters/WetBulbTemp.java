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

/**
 * 
 * Maps to any of the GEMPAK parameters TMWK/TMWC or TMWF depending on the units
 * in which the wet bulb temperature needs to be derived.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 07/20/2016   R15950      J.Huber     Added derive methods to use dewpoint
 *                                      instead of mixing ratio since mixing
 *                                      ratio is itself a derived parameter.
 * </pre>
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class WetBulbTemp extends AbstractMetParameter<Temperature> {

    public WetBulbTemp() {
        super(SI.KELVIN);
    }

    @DeriveMethod
    AbstractMetParameter<Temperature> derive(AirTemperature t, MixingRatio m, PressureLevel p)
            throws InvalidValueException, NullPointerException {

        if (t.hasValidValue() && m.hasValidValue() && p.hasValidValue()) {
            Amount val = PRLibrary.prTmwb(t, m, p);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }

    @DeriveMethod
    AbstractMetParameter<Temperature> derive(AirTemperature t, SurfaceMixingRatio m,
            SurfacePressure p) throws InvalidValueException,
            NullPointerException {

        if (t.hasValidValue() && m.hasValidValue() && p.hasValidValue()) {
            Amount val = PRLibrary.prTmwb(t, m, p);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }

    @DeriveMethod
    AbstractMetParameter<Temperature> derive(AirTemperature t, DewPointTemp d,
            SurfacePressure p) throws InvalidValueException,
            NullPointerException {

        if (t.hasValidValue() && d.hasValidValue() && p.hasValidValue()) {
            /* Derive mixing ratio prior to calculating wet bulb temperature */
            Amount mixingRatio = PRLibrary.prMixr(d, p);
            Amount val = PRLibrary.prTmwb(t, mixingRatio, p);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }

    @DeriveMethod
    AbstractMetParameter<Temperature> derive(AirTemperature t, DewPointTemp d,
            PressureLevel pl) throws InvalidValueException,
            NullPointerException {

        if (t.hasValidValue() && d.hasValidValue() && pl.hasValidValue()) {
            /* Derive mixing ratio prior to calculating wet bulb temperature */
            Amount mixingRatio = PRLibrary.prMixr(d, pl);
            Amount val = PRLibrary.prTmwb(t, mixingRatio, pl);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }
}
