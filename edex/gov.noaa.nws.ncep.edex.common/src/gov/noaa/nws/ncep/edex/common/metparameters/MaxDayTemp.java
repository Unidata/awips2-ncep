package gov.noaa.nws.ncep.edex.common.metparameters;


import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import si.uom.SI;

/**
 * Maps to the GEMPAK parameter TMAX
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class MaxDayTemp extends AbstractMetParameter<Temperature> implements ISerializableObject {

	private static final long serialVersionUID = -2427044114816057551L;

	public MaxDayTemp() throws Exception{
		super(SI.KELVIN);
	}
}
