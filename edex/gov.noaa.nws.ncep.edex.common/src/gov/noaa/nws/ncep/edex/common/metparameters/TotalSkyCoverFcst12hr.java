package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import tech.units.indriya.AbstractUnit;


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