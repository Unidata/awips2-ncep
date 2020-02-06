package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter WCMS
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize 
public class CeilingFromSeaLevelWorstCase extends AbstractMetParameter<Length> implements ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5379275105368701383L;

	public CeilingFromSeaLevelWorstCase(){
		 super(SI.METRE );
	}

	@DeriveMethod
	 AbstractMetParameter derive  ( CeilingFromSeaLevel  cmsl, ProbableCeilingAsMeanSeaLevel tcms ){
		if ( cmsl.hasValidValue() ){
		   Amount val = ( !tcms.hasValidValue()   ? cmsl : tcms  ); //prWcms
		   setValue( val );
		}
		else
			setValueToMissing();
		return this;
	}
}
