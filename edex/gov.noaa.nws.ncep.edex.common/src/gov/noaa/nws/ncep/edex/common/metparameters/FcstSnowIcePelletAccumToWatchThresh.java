package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import tec.uom.se.AbstractUnit;

/**
 * Maps to the GEMPAK parameter SNRT
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class FcstSnowIcePelletAccumToWatchThresh extends
        AbstractMetParameter<Dimensionless> implements ISerializableObject
{

    private static final long serialVersionUID = 4266261492936535275L;

    public FcstSnowIcePelletAccumToWatchThresh() {
        super(AbstractUnit.ONE);
    }

    @DeriveMethod
       AbstractMetParameter<Dimensionless> derive ( FcstSnowIcePelletAccumulation12Hrs si12, SnowIcePelletWatchThresh snip){
		if ( si12.hasValidValue() && snip.hasValidValue() ){
		     Amount val = new Amount ( si12.doubleValue() / snip.doubleValue() , AbstractUnit.ONE );
		     setValue( val );
		}else
			setValueToMissing();
		return this;
	  }
}