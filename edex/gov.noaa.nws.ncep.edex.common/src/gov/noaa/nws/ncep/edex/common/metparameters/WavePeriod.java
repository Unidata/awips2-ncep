package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Time;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter WPER
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

 public class WavePeriod extends AbstractMetParameter<Time> implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7561463713517702035L;

	public WavePeriod() {
		 super( SI.SECOND );
	}
	
 }
