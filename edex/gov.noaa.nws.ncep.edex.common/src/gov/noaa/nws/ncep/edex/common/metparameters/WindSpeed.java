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
 * Maps to any of the GEMPAK parameters SPED (m/sec), SKNT (knots) 
 * or SMPH (miles/hour) based on the units in which the wind speed needs to be computed
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize



 public class WindSpeed extends AbstractMetParameter<Speed> 
        implements ISerializableObject {

	/**
	 * 
	 */
	@DynamicSerializeElement
	private static final long serialVersionUID = -4498547565649728275L;

	public WindSpeed() throws Exception {
		 super(SI.METRE_PER_SECOND);
	}
	
	@DeriveMethod
	public WindSpeed derive ( WindDirectionUComp u, WindDirectionVComp v ) throws InvalidValueException, NullPointerException{
		if ( u.hasValidValue() && v.hasValidValue() ){
		   Amount val = PRLibrary.prSped( u, v );
		   setValue ( val );
		}else
			setValueToMissing();
		
		return this;
	}
}