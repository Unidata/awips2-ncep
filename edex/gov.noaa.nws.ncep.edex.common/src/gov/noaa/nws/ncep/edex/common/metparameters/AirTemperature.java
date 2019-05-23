package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Temperature;
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
 * 
 * Maps to any of the GEMPAK parameters TMPC, TMPK and TMPF (depending on the unit used) 
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AirTemperature extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

	/**
	 * 
	 */
	@DynamicSerializeElement
	private static final long serialVersionUID = 6434922743958827566L;

	public AirTemperature( ) throws Exception {
		 super(SI.KELVIN);
	}

 	@DeriveMethod
	public AirTemperature derive( PressureLevel p, PotentialTemp pt) throws   InvalidValueException, NullPointerException {
         if ( p.hasValidValue() && pt.hasValidValue() ){       
 		        Amount tempAmount =  PRLibrary.prTmpk(p, pt);
                setValue(tempAmount);
         }
         else
        	 setValueToMissing();
         
                return this;
	}		
}
