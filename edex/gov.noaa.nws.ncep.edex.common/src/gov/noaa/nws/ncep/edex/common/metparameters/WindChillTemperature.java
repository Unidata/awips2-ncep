package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
 
/**
 * Maps to the GEMPAK parameter WCHT
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize



public class WindChillTemperature extends AbstractMetParameter<Temperature> 
        implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5387752025941448082L;

	public WindChillTemperature( ) {
		 super( SI.KELVIN );
	}

	@DeriveMethod
	public WindChillTemperature derive( AirTemperature t, WindSpeed ws) throws   InvalidValueException, NullPointerException {
        if ( t.hasValidValue() && ws.hasValidValue() ){      
		      Amount tempAmount =  PRLibrary.prWcht( t, ws);
              setValue(tempAmount);
        }else
            setValueToMissing();  
        	
         return this;
	}		

}
