package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter SEPA
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class IsentropesVerticalSeparation
        extends AbstractMetParameter<Pressure>
        implements ISerializableObject {

    private static final long serialVersionUID = -5980882473474381393L;

    public IsentropesVerticalSeparation() {
        super(SI.PASCAL);
    }

}
