package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import tech.units.indriya.AbstractUnit;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class SpecificHumidity
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    /**
     * 
     */
    @DynamicSerializeElement
    private static final long serialVersionUID = -4072992714364345396L;

    public SpecificHumidity() throws Exception {
        super(AbstractUnit.ONE);
    }

}
