/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tec.uom.se.AbstractUnit;

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
