package gov.noaa.nws.ncep.edex.common.metparameters;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
import si.uom.quantity.Density;
import tech.units.indriya.unit.ProductUnit;

/**
 * Maps to the GEMPAK parameter DDEN
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

/**
 *
 * Maps to the GEMPAK parameter DDEN
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/13/2016   R27046     SRussell    Added a second version of derive()
 * 
 * </pre>
 * 
 * @author ?
 * @version 1.0
 */

public class DryAirDensity extends AbstractMetParameter<Density> {

    public DryAirDensity() {
        super(new ProductUnit<Density>(
                SI.KILOGRAM.divide(SI.METRE.pow(3))));
    }

    @DeriveMethod
    AbstractMetParameter<Density> derive(PressureLevel p, AirTemperature t)
            throws InvalidValueException, NullPointerException {
        Amount val = PRLibrary.prDden(p, t);
        setValue(val);
        return this;
    }

    @DeriveMethod
    AbstractMetParameter<Density> derive(SurfacePressure p, AirTemperature t)
            throws InvalidValueException, NullPointerException {
        Amount val = PRLibrary.prDden(p, t);
        setValue(val);
        return this;
    }

}
