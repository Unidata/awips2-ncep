package gov.noaa.nws.ncep.viz.resources.manager;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.ArrayList;
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
 *  08/14/2015     R7656      R. Reynolds    Started
 *  10/15/2015     R7190      R. Reynolds  Added support for Mcidas
 * 
 * 
 * </pre>
 * 
 * @author rcr
 * @version 1
 */

public class SatelliteAreaManager {

    private String st = null;

    private String rscImplementation = null;

    public static boolean isMcidas = false;

    public String[] subParams = null;

    public int satelliteIdStringPosition;

    public String satelliteName;

    public String satelliteId = "";

    /**
     * The name of Resource Definition to be processed in this class.
     */
    public final static String ResourceDefnName = "SatelliteArea";

    /**
     * The file name of local RD for satellitearea.
     */
    public final static String XMLfilename = "satelliteAreas.xml";

    /**
     * Internal use within a resource name.
     */
    public final static String delimiter = ":";

    /**
     * The singleton design pattern.
     */
    private static SatelliteAreaManager instance = null;

    /**
     * The object that contain the information of satellite areas.
     * 
     */
    private static SatelliteAreaList list = null;

    /**
     * The map to save the mapping from station IDs in the DB to their aliases
     * to be displayed.
     */
    private static HashMap<String, String> areaID2AliasMapping = new HashMap<String, String>();;

    /**
     * Used for displaying AlertViz messages.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SatelliteAreaManager.class);

    /**
     * The Singleton design pattern.
     */
    private SatelliteAreaManager() {
        list = getAreaList();
    }

    /**
     * The singleton design pattern.
     * 
     * @return an instance of this class.
     */
    public static synchronized SatelliteAreaManager getInstance() {
        if (instance == null) {
            instance = new SatelliteAreaManager();
        }
        return instance;
    }

    /**
     * To get a list of aliases of local satellite area IDs.
     * 
     * @return a list of aliases to be displayed in the menu.
     */
    public List<String> getAreaAliases() {
        if (list == null)
            return null;

        return new ArrayList<String>(areaID2AliasMapping.values());
    }

    /**
     * To get a list of local area IDs.
     * 
     * @return a list of area IDs.
     */
    public List<String> getAreaIDs() {
        if (list == null)
            return null;

        return new ArrayList<String>(areaID2AliasMapping.keySet());
    }

    public String[] getSubParams() {
        return subParams;
    }

    public void setSubParams(String[] st) {
        subParams = st;
    }

    /**
     * To get the displayed alias for a specific satellite area ID.
     * 
     * @param dbName
     *            is a area ID.
     * @return the alias of the area ID.
     */
    public String getDisplayedName(String dbName) {
        if (list == null)
            return null;

        return areaID2AliasMapping.get(dbName);
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
    private SatelliteAreaList getAreaList() {

        SatelliteAreaList list = retrieveFromFile();
        if (list == null)
            return null;

        ArrayList<SatelliteArea> areaList = list.getAreaList();

        if (areaList == null) {
            list = null;

            statusHandler.handle(UFStatus.Priority.ERROR,
                    "Zero entry of satelliteArea in " + XMLfilename);

            return null;
        }

        for (SatelliteArea area : areaList) {
            String id = area.getAreaID();
            String alias = area.getAlias();

            // If the areaID string is EMPTY and the alias string is EMPTY,
            // then ignore this entry.
            if ((id == null || id.isEmpty())
                    && (alias == null || alias.isEmpty())) {
                statusHandler.handle(UFStatus.Priority.ERROR,
                        "An empty entry of satelliteArea in " + XMLfilename);
                continue;
            }

            // If the areaID string is EMPTY and the alias string is VALID,
            // then ignore this entry.
            if ((id == null || id.isEmpty()) && alias != null
                    && !(alias.isEmpty())) {
                statusHandler.handle(UFStatus.Priority.ERROR,
                        "Missing areaID for alias <" + alias + "> in "
                                + XMLfilename);
                continue;
            }

            // If the areaID string is VALID and the alias string is EMPTY,
            // then ignore this entry.
            if ((alias == null || alias.isEmpty()) && id != null
                    && !(id.isEmpty())) {
                statusHandler.handle(UFStatus.Priority.ERROR,
                        "Missing alias for areaID <" + id + "> in "
                                + XMLfilename);
                continue;
            }

            // If the areaID string is VALID and the alias string is VALID,
            // then accept this entry.
            areaID2AliasMapping.put(ResourceDefnName + delimiter
                    + area.getAreaID().toLowerCase(), area.getAlias()
                    .toUpperCase());
        }

        return list;
    }

    /**
     * To read the file satelliteAreas.xml and parse the file based on the
     * classes of SatelliteAreaList and SatelliteArea.
     * 
     * @return an object of the class SatelliteAreaList.
     */
    private SatelliteAreaList retrieveFromFile() {
        File xmlFile = null;

        NcPathManager pathMngr = NcPathManager.getInstance();

        try {
            Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                    NcPathConstants.SATELLITE_AREAS_DIR,
                    new String[] { XMLfilename }, false, true);

            // Retrieve the first xml file only.
            String filename = lFiles.values().iterator().next().getFile()
                    .getAbsolutePath();
            xmlFile = new File(filename);
        } catch (Exception e) {
            statusHandler.handle(UFStatus.Priority.ERROR, "Unable to locate "
                    + XMLfilename);

            list = null;
            return null;
        }

        try {
            JAXBContext context = JAXBContext
                    .newInstance(SatelliteAreaList.class);
            Unmarshaller msh = context.createUnmarshaller();

            list = (SatelliteAreaList) msh.unmarshal(xmlFile);

        } catch (JAXBException e) {
            statusHandler.handle(UFStatus.Priority.ERROR, "Unable to parse "
                    + XMLfilename);

            list = null;

        } catch (Exception e) {
            statusHandler.handle(UFStatus.Priority.ERROR, "Unable to parse "
                    + XMLfilename);

            list = null;
        }

        return list;
    }
}
