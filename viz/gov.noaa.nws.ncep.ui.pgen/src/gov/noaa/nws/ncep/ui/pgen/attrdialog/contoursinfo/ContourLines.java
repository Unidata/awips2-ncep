package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourObject;

import java.util.List;

/**
 * Contour information dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 08/01/2015                P. Chowdhuri #R8213 CAVE>PGEN 
 *                                         - Refinements to contoursInfo.xml
 * </pre>
 * 
 * @author pchowdhuri
 * @version 1
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "lines")
public class ContourLines {

	@XmlElement(name = "object", required=true, nillable=false, type = ContourObject.class)
	private List<ContourObject> objects;

	public ContourLines() { }

	public List<ContourObject> getObjects() {
		return objects;
	}

	public void setObjects(List<ContourObject> objects) {
		this.objects = objects;
	}

}
