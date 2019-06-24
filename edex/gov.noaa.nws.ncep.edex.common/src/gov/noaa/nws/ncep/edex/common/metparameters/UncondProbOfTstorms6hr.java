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
 * Maps to the GEMPAK parameter TS06
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class UncondProbOfTstorms6hr extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

	 /**
	 * 
	 */
	private static final long serialVersionUID = -2848602546909481775L;

	public UncondProbOfTstorms6hr() throws Exception {
		 super(AbstractUnit.ONE);
	}
	 
 }