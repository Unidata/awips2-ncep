package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourObject;

import java.util.List;

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
@XmlRootElement(name = "buttons")
public class ContourButtons {
	
	@XmlElement(name = "object", required=true, nillable=false, type = ContourObject.class)
	private List<ContourObject> objects;

	public ContourButtons() {};

	public List<ContourObject> getObjects() {
		return objects;
	}

	public void setObjects(List<ContourObject> objects) {
		this.objects = objects;
	}

}
