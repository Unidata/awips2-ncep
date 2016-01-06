package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Simple JAXP Exception Handler.
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

public class SimpleHandler implements ErrorHandler {

    public void warning(SAXParseException spe) throws SAXException {
        spe.printStackTrace();
    }

    public void error(SAXParseException spe) throws SAXException {
    	spe.printStackTrace();
    }

    public void fatalError(SAXParseException spe) throws SAXException {
    	spe.printStackTrace();
    }
}
