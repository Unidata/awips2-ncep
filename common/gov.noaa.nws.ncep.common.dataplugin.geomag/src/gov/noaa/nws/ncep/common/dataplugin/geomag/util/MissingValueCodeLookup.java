package gov.noaa.nws.ncep.common.dataplugin.geomag.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.PropertiesUtil;

/**
 * MissingValueCodeLookup class reads the properties file and obtains the
 * missing value codes.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer   Description
 * -----------  ----------  ---------- --------------------------
 * 10/07/2015   R11429      sgurung,jtravis Initial Creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1
 */
public class MissingValueCodeLookup {

    /** The logger */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(MissingValueCodeLookup.class);

    /** The singleton instance of MissingValueCodeLookup **/
    private static MissingValueCodeLookup instance;

    private Properties stationMissingValues = null;

    private static final String STATIONS_MISSING_VALUES_PROPERTIES_FILE = "ncep"
            + File.separator
            + "geomag"
            + File.separator
            + "geoMagStations-MissingValues.properties";

    public static synchronized MissingValueCodeLookup getInstance() {
        if (instance == null) {
            try {
                instance = new MissingValueCodeLookup();
            } catch (IOException e) {
                logger.error(
                        "Unable to create an instace of MissingValueCodeLookup",
                        e);
            }
        }
        return instance;
    }

    public static synchronized MissingValueCodeLookup getInstance(File file) {
        if (instance == null) {
            try {
                instance = new MissingValueCodeLookup(file);
            } catch (IOException e) {
                logger.error(
                        "Unable to create an instace of MissingValueCodeLookup",
                        e);
            }
        }
        return instance;
    }

    /**
     * If file has been modified, then reload it again
     * 
     * @return
     * @return
     */
    public static void ReloadInstance() {
        instance = null;
    }

    private MissingValueCodeLookup() throws IOException {
        initStationsMissingValues();
    }

    private MissingValueCodeLookup(File file) throws IOException {

        // initialize
        if (file.exists()) {
            this.stationMissingValues = PropertiesUtil.read(file);
        }

    }

    private void initStationsMissingValues() throws IOException {

        IPathManager pathMgr = PathManagerFactory.getPathManager();

        LocalizationContext commonStaticBase = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.BASE);

        LocalizationContext commonStaticSite = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.SITE);

        String basePath = pathMgr.getFile(commonStaticBase,
                STATIONS_MISSING_VALUES_PROPERTIES_FILE).getCanonicalPath();

        String sitePath = pathMgr.getFile(commonStaticSite,
                STATIONS_MISSING_VALUES_PROPERTIES_FILE).getCanonicalPath();

        File basePropertiesFile = new File(basePath);
        File sitePropertiesFile = new File(sitePath);

        // initialize using base version
        if (basePropertiesFile.exists()) {
            this.stationMissingValues = PropertiesUtil.read(basePropertiesFile);
        }

        // if site version exists, use the site version instead
        if (sitePropertiesFile.exists()) {
            this.stationMissingValues = PropertiesUtil.read(sitePropertiesFile);
        }
    }

    public Properties getStationMissingValues() {

        return this.stationMissingValues;
    }

    public Vector<Double> getMissingValues(String id) {

        Vector<Double> mv = new Vector<Double>();

        String values = this.stationMissingValues.getProperty(id);

        if (values != null) {

            StringTokenizer st = new StringTokenizer(values, ",");

            while (st.hasMoreElements()) {
                String value = st.nextToken();

                try {

                    double d = Double.parseDouble(value);
                    mv.add(d);

                } catch (NumberFormatException e) {
                    logger.error("The value: <" + value
                            + "> from the properties file "
                            + "containing a list of the acceptable "
                            + "missing value codes by station is invalid", e);
                }

            }
        }

        return mv;

    }

    public Double getDefaultMissingValue() {

        String value = this.stationMissingValues
                .getProperty("DEFAULT_MISSING_VALUE");
        Double d = null;

        try {

            d = Double.parseDouble(value);

        } catch (NumberFormatException e) {
            logger.error("The value: <" + value + "> from the properties file "
                    + "containing a list of the acceptable "
                    + "missing value codes by station is invalid", e);
        }

        return d;

    }

}
