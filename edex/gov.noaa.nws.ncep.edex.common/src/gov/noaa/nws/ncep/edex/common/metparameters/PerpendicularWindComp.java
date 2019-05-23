package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Angle;

import com.raytheon.uf.common.serialization.ISerializableObject;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

public class PerpendicularWindComp
        extends AbstractMetParameter<Angle>
        implements ISerializableObject {

    private static final long serialVersionUID = -166841346863071797L;

    public PerpendicularWindComp() {
        super(SI.RADIAN);
    }

    @DeriveMethod
    AbstractMetParameter<Angle> derive(WindDirection wd,
            WindSpeed ws, WindCompDirection d)
            throws InvalidValueException, NullPointerException {
        Amount windDrct = PRLibrary.prWnml(wd, ws, d);
        this.setValue(windDrct);
        return this;
    }
}
