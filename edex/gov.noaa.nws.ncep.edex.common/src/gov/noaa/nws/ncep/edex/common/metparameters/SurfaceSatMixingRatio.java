/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;
import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import 
gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tech.units.indriya.AbstractUnit;
 
/**
 * Maps to the GEMPAK parameter SMXS
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

 public final class SurfaceSatMixingRatio extends AbstractMetParameter<Dimensionless>
 implements ISerializableObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8450901636891230762L;

	public SurfaceSatMixingRatio(){
		 super( AbstractUnit.ONE );
	}
	
 	@DeriveMethod
	public SurfaceSatMixingRatio derive ( AirTemperature t , SurfacePressure p ) throws InvalidValueException, NullPointerException {
       if  ( t.hasValidValue() && p.hasValidValue() ){
 		     Amount mixingRatio = PRLibrary.prMixr( t, p );
		     setValue(mixingRatio);
       }
       else
    	   setValueToMissing();
       
		return this;
	}
}
