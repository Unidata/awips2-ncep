package gov.noaa.nws.ncep.edex.common.metparameters;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.PotentialForCyclonicUpdraftRotation;

/**
 * 
 * Maps to the modelsounding parameter srHel
 *
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class StormRelativeHelicity
        extends AbstractMetParameter<PotentialForCyclonicUpdraftRotation>
        implements ISerializableObject {

    private static final long serialVersionUID = 2352306262526992878L;

    public StormRelativeHelicity() {
        super(PotentialForCyclonicUpdraftRotation.UNIT);
    }
}
