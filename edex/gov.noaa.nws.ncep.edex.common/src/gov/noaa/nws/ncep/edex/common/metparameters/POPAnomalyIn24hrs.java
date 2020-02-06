/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import tec.uom.se.AbstractUnit;
import tec.uom.se.unit.Units;

/**
 * Maps to the GEMPAK parameter PP2A
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class POPAnomalyIn24hrs
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = 8690261205610416205L;

    public POPAnomalyIn24hrs() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
    public POPAnomalyIn24hrs derive(Clim24HrPOP clim24hrpop,
            POP24Hrs pop24hrs) {
        if (pop24hrs == null || clim24hrpop == null
                || (!pop24hrs.hasValidValue())
                || (!clim24hrpop.hasValidValue())) {
            setUnit(Units.PERCENT);
            return this;
        }

        double pp2c = clim24hrpop.getValueAs("%").doubleValue();
        double pp24 = pop24hrs.getValueAs("%").doubleValue();
        double pp2a = pp24 - pp2c;
        setValueAs(pp2a, "%");

        return this;
    }

}
