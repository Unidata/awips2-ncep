package gov.noaa.nws.ncep.viz.rsc.asdi.rsc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

/**
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer       Description
 * ------------ ----------  -----------    --------------------------
 * 04/21/2017    R7656      R. Reynolds    Started
 * 
 * 
 * </pre>
 * 
 * @author rcr
 * @version 1
 */

public final class AirportNames {

    private static List<String> list = null;

    private static String AIRPORT_XML_FILE = "airports.xml";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AirportNames.class);

    /**
     * returns list of airports stored in XML
     * 
     * @return
     */
    public static List<String> getAirports() {

        File xmlFile = null;

        String filename = null;

        NcPathManager pathMngr = NcPathManager.getInstance();

        try {
            Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                    NcPathConstants.ASDI_AIRPORTS_DIR,
                    new String[] { AIRPORT_XML_FILE }, false, true);

            filename = lFiles.values().iterator().next().getFile()
                    .getAbsolutePath();

            xmlFile = new File(filename);

        } catch (Exception e) {
            statusHandler.handle(UFStatus.Priority.ERROR,
                    "Unable to locate " + AIRPORT_XML_FILE);

            list = null;
            return null;
        }

        try {

            InputStream inputStream = new FileInputStream(xmlFile);
            long inputFileCount = xmlFile.length();

            byte[] fileBytes = new byte[(int) inputFileCount];
            inputStream.read(fileBytes);

            String xmlData = new String(fileBytes);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(xmlData));
            Document doc = builder.parse(src);
            NodeList nodes = doc.getElementsByTagName("airport");

            list = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                NodeList name = element.getElementsByTagName("airportName");
                if (name != null) {
                    int elementCount = name.getLength();

                    if (elementCount > 0) {
                        String airP = "";
                        for (int j = 0; j < elementCount; j++) {

                            airP += name.item(j).getFirstChild()
                                    .getTextContent() + ",";

                        }
                        airP = airP.substring(0, airP.length() - 1);
                        list.add(airP);
                    }
                }
            }
            inputStream.close();
            return list;

        } catch (Exception e) {
            statusHandler.handle(UFStatus.Priority.ERROR,
                    "Unable to parse " + AIRPORT_XML_FILE);

            list = null;
        }

        return list;
    }
}