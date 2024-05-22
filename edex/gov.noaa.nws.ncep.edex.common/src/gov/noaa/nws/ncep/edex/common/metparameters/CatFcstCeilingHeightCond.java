package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tech.units.indriya.AbstractUnit;

/**
 * Maps to the GEMPAK parameter FCIG
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class CatFcstCeilingHeightCond
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -1658711340119638316L;

    public CatFcstCeilingHeightCond() {
        super(AbstractUnit.ONE);
    }

}