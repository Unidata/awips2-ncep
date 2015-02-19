package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.units.UnitAdapter;

/**
 * Maps to the GEMPAK parameter TMIN
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize


public class MinNightTemp extends AbstractMetParameter implements
		Temperature,ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -669548000408286037L;

	public MinNightTemp() throws Exception{
		super( new UnitAdapter().marshal(UNIT) );
	}
 }

