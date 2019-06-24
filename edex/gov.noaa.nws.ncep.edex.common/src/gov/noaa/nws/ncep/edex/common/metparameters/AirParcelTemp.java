package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;
 
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AirParcelTemp extends AbstractMetParameter<Temperature> 
        implements ISerializableObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8870456977205187179L;

	public AirParcelTemp() {
		 super( SI.KELVIN );
	}

	@DeriveMethod		
	public AirParcelTemp derive(EquivPotentialTemp et, PressureLevel p, AirTemperature t ) throws InvalidValueException, NullPointerException  {
		if ( et.hasValidValue() && p.hasValidValue() && t.hasValidValue() ){
		    Amount val = PRLibrary.prTmst(et, p, t);
		    setValue(val);
		}
		else
			setValueToMissing();
		
		return this;
	}
}
