package gov.noaa.nws.ncep.edex.common.sounding;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines
 * 
 * This java class provides available sounding time lines for used with NC sounding query.
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 11/30/2010	TBD			Chin Chen	Initial coding
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class NcSoundingTimeLines implements  ISerializableObject{
	@DynamicSerializeElement
    private static final long serialVersionUID = 1324632468L;
	@DynamicSerializeElement
    private Object[] timeLines;
	public Object[] getTimeLines() {
		return timeLines;
	}
	public void setTimeLines(Object[] timeLines) {
		this.timeLines = timeLines;
	}
}
