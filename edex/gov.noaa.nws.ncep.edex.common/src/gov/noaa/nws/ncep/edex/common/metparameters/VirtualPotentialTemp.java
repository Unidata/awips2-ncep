/**
 * 
 */
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
 * Maps to the GEMPAK parameter THTV
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class VirtualPotentialTemp extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7559368640781326821L;


	public VirtualPotentialTemp() {
		 super( SI.KELVIN );
	}

	@DeriveMethod
    AbstractMetParameter<Temperature> derive(VirtualTemp v, PressureLevel p)
            throws InvalidValueException, NullPointerException {
  	        Amount val = PRLibrary.prThta( v , p ) ;  
  	        setValue ( val );
		        return this;
    }
	
}




