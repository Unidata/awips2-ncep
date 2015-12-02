package gov.noaa.nws.ncep.viz.resources.manager;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * This class creates the resource definitions for satellite nameId based on
 * sateliteNames.xml, bypassing any DB query as used for other resource
 * definitions. This class reads and parses the xml file; it also provides
 * methods for accessing data. If the xml file is not well-formed, AlertzViz
 * messages will be posted according to their reasons.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer       Description
 * ------------ ----------  -----------    --------------------------
 * 
 *  08/14/2015    R7656      R. Reynolds    Started
 * 
 * 
 * </pre>
 * 
 * @author rcr
 * @version 1
 */

public class SatelliteNameManager {

    private String SATELLITEID = "";

    private String st = null;

    private String rscImplementation = null;

    public String[] subParams = null;

    /**
     * The name of Resource Definition to be processed in this class.
     */
    public final static String ResourceDefnName = "SatelliteName";

    /**
     * The file name of local RD for satellitename.
     */
    public final static String XMLfilename = "satelliteNames.xml";

    /**
     * Internal use within a resource name.
     */
    public final static String delimiter = ":";

    /**
     * The singleton design pattern.
     */
    private static SatelliteNameManager instance = null;

    /**
     * The object that contain the information of satellite names.
     */
    private static SatelliteNameList list = null;

    /**
     * Used for displaying AlertViz messages.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SatelliteNameManager.class);

    /**
     * The Singleton design pattern.
     */
    private SatelliteNameManager() {
        list = getNameList();
    }

    public String[] getSubParams() {
        return subParams;
    }

    public void setSubParams(String[] st) {
        subParams = st;
    }

    public void setSatelliteId(String sid) {
        SATELLITEID = sid;
    }

    public String getSatelliteId() {
        return SATELLITEID;
    }

    /**
     * The singleton design pattern.
     * 
     * @return an instance of this class.
     */
    public static synchronized SatelliteNameManager getInstance() {
        if (instance == null) {
            instance = new SatelliteNameManager();
        }
        return instance;
    }

    public String getDisplayedNameByID(String satelliteId) {

        ArrayList<SatelliteName> nameList = list.getNameList();

        if (nameList == null) {
            list = null;
            return null;
        }

        for (SatelliteName name : nameList) {
            String alias = name.getAlias();

            if (name.getID().toString().equalsIgnoreCase(satelliteId))
                return name.getAlias();

        }

        return null;
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
     * Parse the contents of SatelliteNames.xml semantically.
     * 
     * @return an object of the class SatelliteNameList.
     */
    private SatelliteNameList getNameList() {

        SatelliteNameList list = retrieveFromFile();
        if (list == null)
            return null;

        ArrayList<SatelliteName> nameList = list.getNameList();

        if (nameList == null) {
            list = null;

            statusHandler.handle(UFStatus.Priority.ERROR,
                    "Zero entry of satelliteName in " + XMLfilename);

            return null;
        }

        return list;
    }

    /**
     * To read the file satelliteNames.xml and parse the file based on the
     * classes of SatelliteNameList and SatelliteName.
     * 
     * @return an object of the class SatelliteNameList.
     */
    private SatelliteNameList retrieveFromFile() {
        File xmlFile = null;

        NcPathManager pathMngr = NcPathManager.getInstance();

        try {
            Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                    NcPathConstants.SATELLITE_NAMES_DIR,
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
                    .newInstance(SatelliteNameList.class);
            Unmarshaller msh = context.createUnmarshaller();

            list = (SatelliteNameList) msh.unmarshal(xmlFile);

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
