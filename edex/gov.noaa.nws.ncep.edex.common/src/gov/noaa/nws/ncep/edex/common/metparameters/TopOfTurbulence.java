package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;
/**
 * Maps to the GEMPAK parameter TTOT or HTOT depending on whether the 
 * top of turbulence was measured in feet or meters
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

  public class TopOfTurbulence extends AbstractMetParameter<Length>
		implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3105613869840547034L;

	public TopOfTurbulence() {
		 super( SI.METRE );
	}
	
   }