package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
import tec.uom.se.AbstractUnit;

/**
 * Maps to "pressChange3Hour" field of the HDF5 table/decoder
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 17, 2014 TTR 923    S. Russell   TTR 923: added derive() to get PTSY needed to get the sign
 * 
 * </pre>
 * 
 * @author unknown
 * @version 2.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class PressChange3Hr
        extends AbstractMetParameter<Pressure>
        implements ISerializableObject {

    private static final long serialVersionUID = 4636092028758506639L;

    public PressChange3Hr() {
        super(SI.PASCAL);
    }

    /*
     * Derive the pressure value using P03C along with PTSY to calculate the
     * positive or negative sign.
     */
    @DeriveMethod
    public PressChange3Hr derive(PressChange3HrAbsVal p,
            PressureTendencySymbol ptsy)
            throws InvalidValueException, NullPointerException {

        if (p.hasValidValue() && ptsy.hasValidValue()) {
            Number n = (Number) new Integer(ptsy.getStringValue());
            Amount ptsyAmount = new Amount(n, AbstractUnit.ONE);
            Amount theP03CAmount = PRLibrary.prP03CAbsVal(p, ptsyAmount);
            this.setAssociatedMetParam(copyDerivedPTSY(ptsy));
            this.setValue(theP03CAmount);
        } else {
            this.setValueToMissing();
        }

        return this;
    }

    private PressureTendencySymbol copyDerivedPTSY(
            PressureTendencySymbol ptsy) {

        PressureTendencySymbol cptsy = new PressureTendencySymbol();

        cptsy.setDataTime(ptsy.getDataTime());
        cptsy.setStringValue(ptsy.getStringValue());
        cptsy.setUnit(ptsy.getUnit());
        cptsy.setValidTime(ptsy.getValidTime());

        return cptsy;

    }

}
