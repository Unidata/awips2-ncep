package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;
/**
 * Maps to the GEMPAK parameter ITOP or HTOI depending on whether the 
 * top of icing was measured in feet or meters
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

  public class TopOfIcing extends AbstractMetParameter<Length>
 implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 368270798514444926L;

	public TopOfIcing() {
		 super( SI.METRE );
	}
	
   }