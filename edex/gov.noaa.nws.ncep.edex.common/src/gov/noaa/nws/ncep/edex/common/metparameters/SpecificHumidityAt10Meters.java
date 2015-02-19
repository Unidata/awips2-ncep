package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class SpecificHumidityAt10Meters extends AbstractMetParameter implements 
Dimensionless, ISerializableObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7004884934195917247L;

	public SpecificHumidityAt10Meters() {
		 super(UNIT );
	}

}
