package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tec.uom.se.AbstractUnit;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
/**
 * Maps to the GEMPAK parameter CL12
 */
 public class TotalSkyCoverFcst12hr extends AbstractMetParameter<Dimensionless> 
        implements ISerializableObject {

	 /**
	 * 
	 */
	private static final long serialVersionUID = 4375299581116360694L;

	public TotalSkyCoverFcst12hr() {
		 super( AbstractUnit.ONE );
	}
	 
  
 }