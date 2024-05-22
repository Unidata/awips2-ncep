package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tech.units.indriya.AbstractUnit;

/**
 * Maps to the Bufrmos parameter QPF24hr_bestCat
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class QuantPrecipFcstBestCat24Hr
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    public QuantPrecipFcstBestCat24Hr() {
        super(AbstractUnit.ONE);
    }

}
