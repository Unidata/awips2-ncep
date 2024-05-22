package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tech.units.indriya.AbstractUnit;

/*
 * Used for the PTND button, which combines P03C + PTSY
 * PressureChange3Hr + PressureTendencySymbol
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class PressureChange3HrAndTendency extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    @DynamicSerializeElement
    private static final long serialVersionUID = -6602297437762954327L;

    public PressureChange3HrAndTendency() {
        super(AbstractUnit.ONE);

    }

    @DeriveMethod
    public PressureChange3HrAndTendency derive(PressChange3HrAbsVal p,
            PressureTendencySymbol ptsy) throws InvalidValueException,
            NullPointerException {

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

    private PressureTendencySymbol copyDerivedPTSY(PressureTendencySymbol ptsy) {

        PressureTendencySymbol cptsy = new PressureTendencySymbol();

        cptsy.setDataTime(ptsy.getDataTime());
        cptsy.setStringValue(ptsy.getStringValue());
        cptsy.setUnit(ptsy.getUnit());
        cptsy.setValidTime(ptsy.getValidTime());

        return cptsy;

    }

}// end class PressureChange3HrAndTendency
