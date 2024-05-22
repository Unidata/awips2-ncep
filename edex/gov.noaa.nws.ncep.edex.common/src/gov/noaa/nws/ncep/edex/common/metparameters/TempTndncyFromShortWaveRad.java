/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.edex.common.metparameters.quantity.TemperatureTendency;

/**
 * Maps to the GEMPAK parameter DTSW
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class TempTndncyFromShortWaveRad extends AbstractMetParameter<TemperatureTendency> 
        implements ISerializableObject{
	/**
	 * 
	 */
	@DynamicSerializeElement
	private static final long serialVersionUID = -7527533400470345687L;

	public TempTndncyFromShortWaveRad() throws Exception {
		 super(TemperatureTendency.UNIT);
	}
}
