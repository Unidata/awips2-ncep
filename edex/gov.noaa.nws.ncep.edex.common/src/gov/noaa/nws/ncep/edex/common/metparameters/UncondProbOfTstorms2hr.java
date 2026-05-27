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
 * Maps to the parameter TS02
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class UncondProbOfTstorms2hr extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

	 /**
	 * 
	 */
	private static final long serialVersionUID = -7530180701363762459L;

	public UncondProbOfTstorms2hr() throws Exception {
		 super(AbstractUnit.ONE);
	}
	 
 }
