package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Speed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter VWND (m/sec) or VKNT (knots)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize


 public class WindDirectionVComp extends AbstractMetParameter<Speed> 
        implements ISerializableObject{

	/**
	 * 
	 */
	@DynamicSerializeElement
	private static final long serialVersionUID = -647302759085698122L;

	public WindDirectionVComp() throws Exception {
		 super(SI.METRE_PER_SECOND);
	}

	@DeriveMethod
	AbstractMetParameter<Speed> derive( WindSpeed w, WindDirection d ) throws InvalidValueException, NullPointerException{
		 if ( w.hasValidValue() && d.hasValidValue()){
		       Amount vWnd  = PRLibrary.prVwnd( w, d );
		       setValue( vWnd );
		 }else
			 setValueToMissing();
		return this;
	}
}
