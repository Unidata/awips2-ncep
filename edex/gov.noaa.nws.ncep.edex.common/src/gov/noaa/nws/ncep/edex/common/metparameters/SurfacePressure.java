package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Pressure;
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
 * Maps to the GEMPAK parameter PALT
 * This met parameter also be used to represent a StationPressure.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class SurfacePressure extends AbstractMetParameter<Pressure>
        implements ISerializableObject {
     /**
	 * 
	 */
	private static final long serialVersionUID = 2544704276202774995L;

	public SurfacePressure() throws Exception {
		 super(SI.PASCAL);
     }

 	@DeriveMethod
    AbstractMetParameter<Pressure> derive(
            SeaLevelPressure altm, StationElevation selv)
            throws InvalidValueException, NullPointerException {
 		
 		if( altm.hasValidValue() && selv.hasValidValue() ) {
 			
 			Amount val = PRLibrary.prPalt ( altm, selv );
 			this.setValue( val );
 		}
 		else {
 			this.setValueToMissing();
 		}
 		return this;
 	}
}
