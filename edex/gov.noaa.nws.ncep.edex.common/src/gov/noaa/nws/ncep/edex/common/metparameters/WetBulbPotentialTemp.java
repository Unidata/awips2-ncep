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
 * Maps to any of  the GEMPAK parameters THWK/THWC or THWF
 * depending on the units in which the wet bulb potential temperature needs to be derived.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

 

public class WetBulbPotentialTemp
        extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 795571539832273959L;

	public WetBulbPotentialTemp() {
		 super( SI.KELVIN );
	}

	@DeriveMethod
    AbstractMetParameter<Temperature> derive(
            PressureLevel p, AirTemperature t, DewPointTemp d)
            throws InvalidValueException, NullPointerException {
	    if ( p.hasValidValue() && t.hasValidValue() && d.hasValidValue() ){
		     Amount val = PRLibrary.prThwc(p, t, d );                     	
		     setValue(val);
	     }
	    else
	       setValueToMissing();
	    
		return this;
	}
	
}











