package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to either the GEMPAK parameter T6XC or T6XF depending on the unit used
 * to measure the temperature
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class Max6HrTemp
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    private static final long serialVersionUID = -6320369876092404681L;

    public Max6HrTemp() throws Exception {
        super(SI.KELVIN);
    }

}
