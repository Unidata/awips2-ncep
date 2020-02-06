 package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Energy;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter TKEL
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize


 
 public class TurbulentKineticEnergy extends AbstractMetParameter<Energy>
    implements ISerializableObject{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4336082923017129663L;

	public TurbulentKineticEnergy() {
		super( SI.JOULE );
	}
 }
