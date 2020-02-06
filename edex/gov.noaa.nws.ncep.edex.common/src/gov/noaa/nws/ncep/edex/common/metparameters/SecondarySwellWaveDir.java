package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Angle;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter DOS2
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class SecondarySwellWaveDir
        extends AbstractMetParameter<Angle>
        implements ISerializableObject {

    private static final long serialVersionUID = 828169481143715948L;

    public SecondarySwellWaveDir() {
        super(SI.RADIAN);
    }
}
