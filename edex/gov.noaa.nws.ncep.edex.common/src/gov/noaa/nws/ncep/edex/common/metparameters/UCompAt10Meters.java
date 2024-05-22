package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Speed;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize


public class UCompAt10Meters extends AbstractMetParameter<Speed> 
        implements ISerializableObject{


	/**
	 * 
	 */
	private static final long serialVersionUID = 6465946066649208891L;

	public UCompAt10Meters() {
		 super(  SI.METRE_PER_SECOND );
	}
	
}
