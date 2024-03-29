package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Frequency;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to GEMPAK parameter BVFQ
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class BruntVaisalaFreq
        extends AbstractMetParameter<Frequency>
        implements ISerializableObject {

    private static final long serialVersionUID = 6363221742540720813L;

    public BruntVaisalaFreq() {
        super(SI.HERTZ);
    }
}
