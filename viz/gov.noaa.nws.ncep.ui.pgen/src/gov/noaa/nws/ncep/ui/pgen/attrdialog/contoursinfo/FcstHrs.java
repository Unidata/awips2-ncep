package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourLabel;

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
@XmlRootElement(name = "fhrs")
public class FcstHrs {
	
	public FcstHrs() { }

	@XmlElement(name = "label", required=true, nillable=false, type = ContourLabel.class)
	private List<ContourLabel> clabels;

	public List<ContourLabel> getClabels() {
		return clabels;
	}

	public void setClabels(List<ContourLabel> clabels) {
		this.clabels = clabels;
	}
	
}
