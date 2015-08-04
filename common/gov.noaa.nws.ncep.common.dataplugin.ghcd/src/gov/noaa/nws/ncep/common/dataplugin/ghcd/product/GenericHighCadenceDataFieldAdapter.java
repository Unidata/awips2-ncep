/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.product;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 03/05/2014   1100        sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class GenericHighCadenceDataFieldAdapter extends
        XmlAdapter<Object, GenericHighCadenceDataField> {

    private DocumentBuilder documentBuilder;

    private JAXBContext jaxbContext;

    public GenericHighCadenceDataFieldAdapter() {
    }

    public GenericHighCadenceDataFieldAdapter(JAXBContext jaxbContext) {
        this();
        this.jaxbContext = jaxbContext;
    }

    private DocumentBuilder getDocumentBuilder() throws Exception {
        // Lazy load the DocumentBuilder as it is not used for unmarshalling.
        if (null == documentBuilder) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            documentBuilder = dbf.newDocumentBuilder();
        }
        return documentBuilder;
    }

    private JAXBContext getJAXBContext(Class<?> type) throws Exception {
        if (null == jaxbContext) {
            // A JAXBContext was not set, so create a new one based on the type.
            return JAXBContext.newInstance(type);
        }
        return jaxbContext;
    }

    @Override
    public GenericHighCadenceDataField unmarshal(Object e) throws Exception {

        Element el = (Element) e;
        String elValue = getTagValue(el.getTagName(), el);

        GenericHighCadenceDataField ghcdField = new GenericHighCadenceDataField(
                el.getTagName(), elValue);
        return ghcdField;
    }

    @Override
    public Object marshal(GenericHighCadenceDataField ghcdField)
            throws Exception {
        // 1. Build the JAXBElement to wrap the instance of Parameter.
        QName rootElement = new QName(ghcdField.getName());
        Object value = ghcdField.getValue();
        Class<?> type = value.getClass();
        JAXBElement jaxbElement = new JAXBElement(rootElement, type, value);

        // 2. Marshal the JAXBElement to a DOM element.
        Document document = getDocumentBuilder().newDocument();
        Marshaller marshaller = getJAXBContext(type).createMarshaller();
        marshaller.marshal(jaxbElement, document);
        Element element = document.getDocumentElement();
        element.setTextContent(value + "");
        return element;
    }

    private static String getTagValue(String sTag, Element eElement) {
        return eElement.getFirstChild().getNodeValue();
    }
}
