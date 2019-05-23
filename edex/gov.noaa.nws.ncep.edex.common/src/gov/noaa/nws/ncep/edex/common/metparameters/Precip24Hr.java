package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter P24I or P24M - based on whether the
 * precipitation is measured in inches or mm
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class Precip24Hr
        extends AbstractMetParameter<Length>
        implements ISerializableObject {

    private static final long serialVersionUID = 8411794382636632766L;

    public Precip24Hr() {
        super(SI.METRE);
    }

}
