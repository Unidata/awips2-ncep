package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import tec.uom.se.AbstractUnit;

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
