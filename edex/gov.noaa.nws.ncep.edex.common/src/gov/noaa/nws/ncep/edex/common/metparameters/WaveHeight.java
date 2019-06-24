package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter WHGT/WHFT depending on the units used to measure the
 * wave height
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class WaveHeight extends AbstractMetParameter<Length> implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4484888842596046788L;

	public WaveHeight() {
		super( SI.METRE );
	}	
}

