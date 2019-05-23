package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.RateOfChangeInTemperatureWithHeight;
/**
 * Maps to the GEMPAK parameter LAPS
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
 
 public class TempLapseRate 
         extends AbstractMetParameter<RateOfChangeInTemperatureWithHeight>
        implements ISerializableObject{

     /**
	 * 
	 */
	private static final long serialVersionUID = 6960050211358125647L;

	public TempLapseRate(){
		 super( RateOfChangeInTemperatureWithHeight.UNIT );
     }
	
 }
