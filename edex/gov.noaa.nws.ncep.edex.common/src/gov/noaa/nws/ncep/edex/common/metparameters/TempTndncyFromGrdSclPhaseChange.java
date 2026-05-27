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
 * Maps to the GEMPAK parameter DTGP
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class TempTndncyFromGrdSclPhaseChange extends AbstractMetParameter<TemperatureTendency> 
        implements ISerializableObject{
	/**
	 * 
	 */
	@DynamicSerializeElement
	private static final long serialVersionUID = 8176965639807536650L;

	public TempTndncyFromGrdSclPhaseChange() throws Exception {
		 super(TemperatureTendency.UNIT);
	}
}