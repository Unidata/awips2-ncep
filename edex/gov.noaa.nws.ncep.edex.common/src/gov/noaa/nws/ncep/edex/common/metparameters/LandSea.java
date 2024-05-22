/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tech.units.indriya.AbstractUnit;

/**
 * Maps to the modelsounding parameter landSea
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class LandSea
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = 6559113832610632797L;

    public LandSea() {
        super(AbstractUnit.ONE);
    }
}