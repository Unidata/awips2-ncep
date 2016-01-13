package gov.noaa.nws.ncep.viz.resources.manager;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * This class creates the resource definitions for satellite areaId based on
 * sateliteAreas.xml, bypassing any DB query as used for other resource
 * definitions. This class reads and parses the xml file; it also provides
 * methods for accessing data. If the xml file is not well-formed, AlertzViz
 * messages will be posted according to their reasons.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer       Description
 * ------------ ----------  -----------    --------------------------
 * 
 *  08/14/2015    R7656     R. Reynolds    Started
 *  12/01/2015    R12953     R Reynolds     Changes to better define name/alias in hashmap
 * 
 * 
 * </pre>
 * 
 * @author rcr
 * @version 1
 */

public class SatelliteImageTypeManager {

    private String st = null;

    private String rscImplementation = null;

    public static boolean isMcidas = false;

    public String[] subParams = null;

    public int satelliteIdStringPosition;

    public String satelliteName;

    private String attrName;

    protected HashMap<String, String> attributeNamesAndAliases = new HashMap<String, String>();

    /**
     * The name of Resource Definition to be processed in this class.
     */
    public final static String ResourceDefnName = "SatelliteImageType";

    /**
     * The file name of local RD for satellitearea.
     */
    public final static String XML_FILE_NAME = "satelliteImageTypes.xml";

    /**
     * Internal use within a resource name.
     */
    public final static String delimiter = ":";

    /**
     * The singleton design pattern.
     */
    private static SatelliteImageTypeManager instance = null;

    /**
     * The object that contain the information of satellite areas.
     * 
     */
    private static SatelliteImageTypeList list = null;

    /**
     * The map to save the mapping from station IDs in the DB to their aliases
     * to be displayed.
     */
    private static HashMap<String, String> SATandAS2ImageTypeIdMapping = new HashMap<String, String>();

    private static HashMap<String, String> SATandImage2AS2ImageTypeIdMapping = new HashMap<String, String>();

    /**
     * Used for displaying AlertViz messages.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SatelliteImageTypeManager.class);

    /**
     * The Singleton design pattern.
     */
    private SatelliteImageTypeManager() {
        list = getImageTypeList();
    }

    /**
     * The singleton design pattern.
     * 
     * @return an instance of this class.
     */
    public static synchronized SatelliteImageTypeManager getInstance() {
        if (instance == null) {
            instance = new SatelliteImageTypeManager();
        }
        return instance;
    }

    public void setSelectedAttrName(
            HashMap<String, String> attributeNamesAndAliases) {
        this.attributeNamesAndAliases = attributeNamesAndAliases;
    }

    public String getSelectedAttrName(String attributeName) {
        return attributeNamesAndAliases.get(attributeName);
    }

    public String getImageId_using_satId_and_ASname(String satId,
            String ASImageId) {

        String imageId = SATandAS2ImageTypeIdMapping.get(satId.trim() + ":"
                + ASImageId.trim());

        if (imageId == null || imageId.trim().isEmpty())
            imageId = SATandAS2ImageTypeIdMapping.get("default" + ":"
                    + ASImageId.trim());

        return imageId;

    }

    public String getASname_using_satId_and_imageId(String satId, String ImageId) {

        String asName = SATandImage2AS2ImageTypeIdMapping.get(satId.trim()
                + ":" + ImageId.trim());

        return asName;

    }

    public void setAppendedPart(String st) {
        this.st = st;
    }

    public String getAppendedPart() {
        return this.st;
    }

    public void setRscImplementation(String rscImplementation) {
        this.rscImplementation = rscImplementation;
    }

    public String getRscImplementation() {
        return this.rscImplementation;
    }

    /**
     * Parse the contents of SatelliteAreas.xml semantically.
     * 
     * @return an object of the class SatelliteAreaList.
     */
    public SatelliteImageTypeList getImageTypeList() {

        SatelliteImageTypeList list = retrieveFromFile();
        if (list == null)
            return null;

        List<SatelliteImageType> imageTypeList = list.getImageTypeList();

        if (imageTypeList == null) {
            list = null;

            statusHandler.handle(UFStatus.Priority.ERROR,
                    "Zero entry of satelliteImageType in " + XML_FILE_NAME);

            return null;
        }

        for (SatelliteImageType image : imageTypeList) {

            // If the areaID string is VALID and the alias string is VALID,
            // then accept this entry.
            SATandAS2ImageTypeIdMapping.put(image.getSatelliteId() + ":"
                    + image.getASImageTypeId(), image.getImageTypeId());

            SATandImage2AS2ImageTypeIdMapping.put(image.getSatelliteId() + ":"
                    + image.getImageTypeId(), image.getASImageTypeId());

        }

        return list;
    }

    /**
     * To read the file satelliteAreas.xml and parse the file based on the
     * classes of SatelliteAreaList and SatelliteArea.
     * 
     * @return an object of the class SatelliteAreaList.
     */
    private SatelliteImageTypeList retrieveFromFile() {
        File xmlFile = null;

        NcPathManager pathMngr = NcPathManager.getInstance();

        try {
            Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                    NcPathConstants.SATELLITE_IMAGETYPES_DIR,
                    new String[] { XML_FILE_NAME }, false, true);

            // Retrieve the first xml file only.
            String filename = lFiles.values().iterator().next().getFile()
                    .getAbsolutePath();
            xmlFile = new File(filename);
        } catch (Exception e) {
            statusHandler.handle(UFStatus.Priority.ERROR, "Unable to locate "
                    + XML_FILE_NAME);

            list = null;
            return null;
        }

        try {
            JAXBContext context = JAXBContext
                    .newInstance(SatelliteImageTypeList.class);
            Unmarshaller msh = context.createUnmarshaller();

            list = (SatelliteImageTypeList) msh.unmarshal(xmlFile);

        } catch (JAXBException e) {
            statusHandler.handle(UFStatus.Priority.ERROR, "Unable to parse "
                    + XML_FILE_NAME);

            list = null;

        } catch (Exception e) {
            statusHandler.handle(UFStatus.Priority.ERROR, "Unable to parse "
                    + XML_FILE_NAME);

            list = null;
        }

        return list;
    }
}
