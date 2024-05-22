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
 * Maps to the Bufrmos parameter severe24hr - new GEMPAK alias US24
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
 public class UncondProbOf24HrSevereWeather extends AbstractMetParameter<Dimensionless> 
        implements ISerializableObject {

	 /**
	 * 
	 */
	private static final long serialVersionUID = 7565653710046867602L;

	public UncondProbOf24HrSevereWeather()throws Exception {
		 super(AbstractUnit.ONE);
	}
 }