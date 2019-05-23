package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tec.uom.se.AbstractUnit;

/**
 * 
 * <pre>
 * 
 * P03D
 * 
 * Produces a combination of the pressure value + the code used to refer to 
 * the PresstureTendencySymbol
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/10/2017   R27759     S. Russell   Initial Creation
 * 
 * </pre>
 * 
 * @author unknown
 * @version 2.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class PressureTendencyAndChangeGroup
        extends AbstractMetParameter<Dimensionless> {

    public PressureTendencyAndChangeGroup() {
        super(AbstractUnit.ONE);
    }

    /**
     * Combine the value for pressure with the code used to refer to the
     * PressureTendencySymbol
     * 
     * @param p
     * @param ptsy
     * @return
     * @throws InvalidValueException
     * @throws NullPointerException
     */
    @DeriveMethod
    public PressureTendencyAndChangeGroup derive(PressChange3HrAbsVal p,
            PressureTendencySymbol ptsy)
            throws InvalidValueException, NullPointerException {

        setValueIsString();
        setValueToMissing();

        if (p.hasValidValue() && ptsy.hasValidValue()) {

            String sPTSYCode = ptsy.getStringValue();
            Integer iP = p.getValue().intValue();
            String sP = String.format("%03d", iP);
            String sCombinedValues = sPTSYCode + sP;

            this.setStringValue(sCombinedValues);

        }
        return this;
    }

}
