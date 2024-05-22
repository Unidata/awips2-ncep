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
 * Maps to the parameter TS12
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

 public class UncondProbOfTstorms12hr extends AbstractMetParameter<Dimensionless> 
        implements ISerializableObject {

	 /**
	 * 
	 */
	private static final long serialVersionUID = 3873261645845080924L;

	public UncondProbOfTstorms12hr()throws Exception {
		 super(AbstractUnit.ONE);
	}
	 
  }