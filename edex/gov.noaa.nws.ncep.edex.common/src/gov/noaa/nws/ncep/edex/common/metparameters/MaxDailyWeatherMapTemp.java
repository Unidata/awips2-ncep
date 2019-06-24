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
 * Maps to the GEMPAK parameter DMAX
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class MaxDailyWeatherMapTemp
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    private static final long serialVersionUID = 65484313692581090L;

    public MaxDailyWeatherMapTemp() throws Exception {
        super(SI.KELVIN);
    }

    @DeriveMethod
    AbstractMetParameter derive(Max6HrTemp t00x, Max6HrTemp t06x,
            MaxMidnightTemp tdxc)
            throws InvalidValueException, NullPointerException {
        Amount val = PRLibrary.prDmax(t00x, t06x, tdxc);
        setValue(val);
        return this;
    }

}
