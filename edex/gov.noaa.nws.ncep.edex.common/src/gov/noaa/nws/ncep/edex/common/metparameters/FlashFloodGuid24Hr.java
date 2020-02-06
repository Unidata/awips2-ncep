package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter FF24
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class FlashFloodGuid24Hr
        extends AbstractMetParameter<Length>
        implements ISerializableObject {

    private static final long serialVersionUID = -5911365117945390325L;

    public FlashFloodGuid24Hr() {
        super(SI.METRE);
    }

}
