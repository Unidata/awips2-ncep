package gov.noaa.nws.ncep.edex.common.metparameters;
import javax.measure.quantity.Temperature;
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
 * Maps to the GEMPAK parameter WCEQ
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize



public class WindChillEquivalentTemp extends AbstractMetParameter<Temperature>
    implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7672651387146926009L;

	public WindChillEquivalentTemp( ) {
		 super( SI.KELVIN );
	}
	
	@DeriveMethod
	public WindChillEquivalentTemp derive( AirTemperature t, WindSpeed ws) throws   InvalidValueException, NullPointerException {
        if ( t.hasValidValue() && ws.hasValidValue() ){
		     Amount tempAmount =  PRLibrary.prWceq(t, ws);
             setValue(tempAmount);
        }
        else
           setValueToMissing();
         
        return this;
	}		

}
