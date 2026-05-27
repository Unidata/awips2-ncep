package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the modelsounding parameter snowFall
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class SnowFall
        extends AbstractMetParameter<Length>
        implements ISerializableObject {

    /**
     * 
     */
    private static final long serialVersionUID = 7759179226889594021L;

    public SnowFall() {
        super(SI.METRE);
    }

}
