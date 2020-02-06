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
