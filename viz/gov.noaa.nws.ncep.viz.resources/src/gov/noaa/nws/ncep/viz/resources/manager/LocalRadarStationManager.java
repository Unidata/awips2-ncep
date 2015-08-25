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
 * This class creates the resource definitions for LocalRadar based on
 * localRadarStations.xml, bypassing any DB query as used for other resource
 * definitions. This class reads and parses the xml file; it also provides
 * methods for accessing data. If the xml file is not well-formed, AlertzViz
 * messages will be posted according to their reasons.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 *  05/14/15      R7656      A. Su      Initial creation.
 *  06/04/15      R7656      A. Su      Added comments to clarify how to handle empty strings in the xml file.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 */

public class LocalRadarStationManager {

    /**
     * The name of Resource Definition to be processed in this class.
     */
    public final static String ResourceDefnName = "LocalRadar";

    /**
     * The file name of local RD for LocalRadar.
     */
    public final static String XMLfilename = "localRadarStations.xml";

    /**
     * Internal use within a resource name.
     */
    private final static String delimiter = ":";

    /**
     * The singleton design pattern.
     */
    private static LocalRadarStationManager instance = null;;

    /**
     * The object that contain the information of local radar stations.
     */
    private static LocalRadarStationList list = null;

    /**
     * The map to save the mapping from station IDs in the DB to their aliases
     * to be displayed.
     */
    private static HashMap<String, String> stationID2AliasMapping = new HashMap<String, String>();;

    /**
     * Used for displaying AlertViz messages.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalRadarStationManager.class);

    /**
     * The Singleton design pattern.
     */
    private LocalRadarStationManager() {
        list = getStationList();
    }

    /**
     * The singleton design pattern.
     * 
     * @return an instance of this class.
     */
    public static synchronized LocalRadarStationManager getInstance() {
        if (instance == null) {
            instance = new LocalRadarStationManager();
        }
        return instance;
    }

    /**
     * To get a list of aliases of local radar station IDs.
     * 
     * @return a list of aliases to be displayed in the menu.
     */
    public List<String> getStationAliases() {
        if (list == null)
            return null;

        return new ArrayList<String>(stationID2AliasMapping.values());
    }

    /**
     * To get a list of local station IDs.
     * 
     * @return a list of station IDs.
     */
    public List<String> getStationIDs() {
        if (list == null)
            return null;

        return new ArrayList<String>(stationID2AliasMapping.keySet());
    }

    /**
     * To get the displayed alias for a specific radar station ID.
     * 
     * @param dbName
     *            is a station ID.
     * @return the alias of the station ID.
     */
    public String getDisplayedName(String dbName) {
        if (list == null)
            return null;

        return stationID2AliasMapping.get(dbName);
    }

    /**
     * Parse the contents of localRadarStations.xml semantically.
     * 
     * @return an object of the class LocalRadarStationList.
     */
    private LocalRadarStationList getStationList() {

        LocalRadarStationList list = retrieveFromFile();
        if (list == null)
            return null;

        ArrayList<LocalRadarStation> stationList = list.getStationList();

        if (stationList == null) {
            list = null;

            statusHandler.handle(UFStatus.Priority.ERROR,
                    "Zero entry of radarStation in " + XMLfilename);

            return null;
        }

        for (LocalRadarStation station : stationList) {
            String id = station.getStationID();
            String alias = station.getAlias();

            // If the stationID string is EMPTY and the alias string is EMPTY,
            // then ignore this entry.
            if ((id == null || id.isEmpty())
                    && (alias == null || alias.isEmpty())) {
                statusHandler.handle(UFStatus.Priority.ERROR,
                        "An empty entry of radarStation in " + XMLfilename);
                continue;
            }

            // If the stationID string is EMPTY and the alias string is VALID,
            // then ignore this entry.
            if ((id == null || id.isEmpty()) && alias != null
                    && !(alias.isEmpty())) {
                statusHandler.handle(UFStatus.Priority.ERROR,
                        "Missing stationID for alias <" + alias + "> in "
                                + XMLfilename);
                continue;
            }

            // If the stationID string is VALID and the alias string is EMPTY,
            // then ignore this entry.
            if ((alias == null || alias.isEmpty()) && id != null
                    && !(id.isEmpty())) {
                statusHandler.handle(UFStatus.Priority.ERROR,
                        "Missing alias for stationID <" + id + "> in "
                                + XMLfilename);
                continue;
            }

            // If the stationID string is VALID and the alias string is VALID,
            // then accept this entry.
            stationID2AliasMapping.put(ResourceDefnName + delimiter
                    + station.getStationID().toLowerCase(), station.getAlias()
                    .toUpperCase());
        }

        return list;
    }

    /**
     * To read the file localRadarStations.xml and parse the file based on the
     * classes of LocalRadarStationList and LocalRadarStation.
     * 
     * @return an object of the class LocalRadarStationList.
     */
    private LocalRadarStationList retrieveFromFile() {
        File xmlFile = null;

        NcPathManager pathMngr = NcPathManager.getInstance();

        try {
            Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                    NcPathConstants.LCL_RDA_DIR, new String[] { XMLfilename },
                    false, true);

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
                    .newInstance(LocalRadarStationList.class);
            Unmarshaller msh = context.createUnmarshaller();

            list = (LocalRadarStationList) msh.unmarshal(xmlFile);

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
