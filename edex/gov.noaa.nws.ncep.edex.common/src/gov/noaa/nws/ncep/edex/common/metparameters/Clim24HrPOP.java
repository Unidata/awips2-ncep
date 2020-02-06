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
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tec.uom.se.AbstractUnit;

/**
 * Maps to the GEMPAK parameter PP2C
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class Clim24HrPOP
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -3752589406220123063L;

    public Clim24HrPOP() {
        super(AbstractUnit.ONE);
    }

    @Override
    public String getParameterDescription() {
        return "Climatological 24 Hour Probability of Precipitation.";
    }

    @DeriveMethod
    public Clim24HrPOP derive(StationID stid)
            throws InvalidValueException, NullPointerException {
        return this;
    }
}