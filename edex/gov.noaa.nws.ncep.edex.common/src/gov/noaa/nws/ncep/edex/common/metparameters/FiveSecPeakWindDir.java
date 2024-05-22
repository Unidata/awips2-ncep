package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Angle;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter PWDR
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class FiveSecPeakWindDir
        extends AbstractMetParameter<Angle>
        implements ISerializableObject {

    private static final long serialVersionUID = -9021635625609421348L;

    public FiveSecPeakWindDir() {
        super(SI.RADIAN);
    }
}
