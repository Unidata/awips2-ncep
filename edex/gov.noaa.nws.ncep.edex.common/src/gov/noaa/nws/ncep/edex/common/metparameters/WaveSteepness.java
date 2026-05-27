/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tech.units.indriya.AbstractUnit;

/**
 * Maps to the parameter WSTP
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class WaveSteepness extends AbstractMetParameter<Dimensionless> 
        implements ISerializableObject {
        /**
	 * 
	 */
	private static final long serialVersionUID = 2152727813308552985L;

		public WaveSteepness( ){
        	super ( AbstractUnit.ONE );
        }
}
