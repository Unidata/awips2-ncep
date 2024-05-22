package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter PRES This parameter is intended to be used as a
 * Vertical Coordinate.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class PressureLevel
        extends AbstractMetParameter<Pressure>
        implements ISerializableObject {

    private static final long serialVersionUID = 167491198657595212L;

    public PressureLevel() throws Exception {
        super(SI.PASCAL);
    }
}
