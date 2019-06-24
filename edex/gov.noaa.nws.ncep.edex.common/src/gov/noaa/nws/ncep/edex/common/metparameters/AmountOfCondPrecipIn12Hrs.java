package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.AmountOfPrecipitation;

/**
 * 
 * Maps to the bufrmos parameter condPrecipAmt_12hr
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AmountOfCondPrecipIn12Hrs
        extends AbstractMetParameter<AmountOfPrecipitation>
        implements ISerializableObject {

    private static final long serialVersionUID = -7386803479446558071L;

    public AmountOfCondPrecipIn12Hrs() throws Exception {
        super(AmountOfPrecipitation.UNIT);
    }
}
