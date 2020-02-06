package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Time;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;
/**
 * Maps to the GEMPAK parameter PWMN
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class TimeOf5SecPeakWindInMins extends AbstractMetParameter<Time>
implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -989016891839815811L;

	public TimeOf5SecPeakWindInMins(){
		 super( SI.SECOND );
	}
	
}
