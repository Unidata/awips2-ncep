package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contour information dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 08/01/2015   8213        P.           CAVE>PGEN 
 *                          Chowdhuri     - Refinements to contoursInfo.xml
 * </pre>
 * 
 * @author pchowdhuri
 * @version 1
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "level")
public class ContourLevel {
	
	@XmlAttribute
	private String value;
	@XmlAttribute
	private String coord;
	@XmlAttribute
	private String cint;
	
	public ContourLevel() { }
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getCoord() {
		return coord;
	}

	public void setCoord(String coord) {
		this.coord = coord;
	}

	public String getCint() {
		return cint;
	}

	public void setCint(String cint) {
		this.cint = cint;
	}

}
