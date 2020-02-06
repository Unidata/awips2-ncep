package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Time;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter POWW
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize



public class WindWavePeriod extends AbstractMetParameter<Time> implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9068132951552388458L;

	public WindWavePeriod() {
		 super( SI.SECOND );
	}
	
}
