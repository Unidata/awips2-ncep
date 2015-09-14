package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContoursInfo;

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
@XmlRootElement(name = "root")
public class ContourRoot {
	
	public ContourRoot() {}

	@XmlElement(name = "contoursInfo")
	private List<ContoursInfo> cntrList;

	public List<ContoursInfo> getCntrList() {
		return cntrList;
	}

	public void setCntrList(List<ContoursInfo> cntrList) {
		this.cntrList = cntrList;
	}

}
