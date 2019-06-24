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
 * Maps to the GEMPAK parameter UWND (m/sec) or UKNT (knots)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize


public class WindDirectionUComp extends AbstractMetParameter<Speed> 
        implements ISerializableObject{

	/**
	 * 
	 */
	@DynamicSerializeElement
	private static final long serialVersionUID = -3330403426014946174L;

	public WindDirectionUComp() throws Exception {
		 super(SI.METRE_PER_SECOND);
	}
	
	@DeriveMethod
	AbstractMetParameter<Speed> derive( WindSpeed w, WindDirection d ) throws InvalidValueException, NullPointerException{
		 if ( w.hasValidValue() && d.hasValidValue()){
		       Amount uWnd  = PRLibrary.prUwnd( w, d );
		       setValue( uWnd );
		 }else
			 setValueToMissing();
		       return this;
	}
}
