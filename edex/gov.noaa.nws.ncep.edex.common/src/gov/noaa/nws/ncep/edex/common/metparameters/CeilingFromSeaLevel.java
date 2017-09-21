package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

/**
 * Maps to the GEMPAK parameter CMSL
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class CeilingFromSeaLevel extends AbstractMetParameter
        implements Length {

    public CeilingFromSeaLevel() {
        super(UNIT);
    }

    @Override
    public String getParameterDescription() {
        return "The Ceiling as measured from sea level.";
    }

    @DeriveMethod // TODO cross check the validity of this equation
    AbstractMetParameter derive(CeilingFromSurface c, StationElevation se)
            throws InvalidValueException, NullPointerException {
        if (c.hasValidValue() && se.hasValidValue()) {
            Amount val = PRLibrary.prCmsl(c, se);
            setValue(val);
        } else
            setValueToMissing();

        return this;
    }
}
