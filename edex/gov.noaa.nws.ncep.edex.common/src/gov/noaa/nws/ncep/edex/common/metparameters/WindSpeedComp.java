package gov.noaa.nws.ncep.edex.common.metparameters;
import javax.measure.quantity.Speed;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter WCMP
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize



 public class WindSpeedComp extends AbstractMetParameter<Speed>
  implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7155251791270662526L;
	public WindSpeedComp() {
		 super( SI.METRE_PER_SECOND);
	} 
	@DeriveMethod
	AbstractMetParameter derive ( WindDirection wd, WindSpeed ws, WindCompDirection d) throws InvalidValueException, NullPointerException{
		if ( wd.hasValidValue() && ws.hasValidValue() && d.hasValidValue() ){     
		     Amount windSpeed = PRLibrary.prWcmp(wd , ws , d );
		     setValue(windSpeed);
		}else
			setValueToMissing();
		
		     return this;
	}
}
