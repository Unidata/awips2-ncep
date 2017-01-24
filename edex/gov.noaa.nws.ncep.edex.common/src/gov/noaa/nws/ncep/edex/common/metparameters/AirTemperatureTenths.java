package gov.noaa.nws.ncep.edex.common.metparameters;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.units.UnitAdapter;

/**
 * 
 * Used to hold value of temperature (In Celsius) from the remarks section of
 * observation. This has the temperature to a tenth of a degree. A new parameter
 * was created for AWIPS2 TMPT
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 5/11/2016     R6154       J.Huber     Initial creation
 * </pre>
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AirTemperatureTenths extends AbstractMetParameter implements
        javax.measure.quantity.Temperature {

    /**
	 * 
	 */
    @DynamicSerializeElement
    private static final long serialVersionUID = 6434922743958827566L;

    public AirTemperatureTenths() throws Exception {
        super(new UnitAdapter().marshal(UNIT));
    }

    @DeriveMethod
    public AirTemperatureTenths derive(PressureLevel p, PotentialTemp pt)
            throws InvalidValueException, NullPointerException {
        if (p.hasValidValue() && pt.hasValidValue()) {
            Amount tempAmount = PRLibrary.prTmpk(p, pt);
            setValue(tempAmount);
        } else
            setValueToMissing();

        return this;
    }
}
