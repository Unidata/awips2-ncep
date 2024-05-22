package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

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
@XmlRootElement(name = "cfiles")
public class ContourFiles {
	
	@XmlElement(name = "path", required=true, nillable=false, type = String.class)
	ArrayList<String> paths;

	public List<String> getPaths() {
		return paths;
	}

	public void setPaths(List<String> paths) {
		this.paths = (ArrayList<String>)paths;
	}

}
