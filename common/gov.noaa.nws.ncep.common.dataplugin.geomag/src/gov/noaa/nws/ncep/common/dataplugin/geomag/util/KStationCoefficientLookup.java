package gov.noaa.nws.ncep.common.dataplugin.geomag.util;

import gov.noaa.nws.ncep.common.dataplugin.geomag.exception.GeoMagException;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.KStationCoeffTableReader;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.KStationCoefficient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.util.PropertiesUtil;

/*
 * The KStationCoefficient table Lookup.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * ate          Ticket#     Engineer          Description
 * -----------  ----------  ----------        --------------------------
 * 05/14/2013   #989        qzhou             Initial Creation
 * 07/06/2015   R8416       sgurung, jtravis  Read ks date ranges from properties file
 * </pre>
 * 
 * @author qzhou
 * @version 1
 */
//@formatter:off
public class KStationCoefficientLookup {

    /** The logger */
    protected transient Log logger = LogFactory.getLog(getClass());

    /** The singleton instance of GeoMagStationLookup **/
    private static KStationCoefficientLookup instance;

    /** A map of the stations. The key is the station code of the station */
    private Map<String, KStationCoefficient> coeff;

    private Properties ksDateRange = null;

    private static final String KSTANDARD_LOOKUP_FILE = "ncep" + File.separator
            + "geomag" + File.separator + "kStandardLookup.xml";

    private static final String KSDATERANGE_PROPERTIES_FILE = "ncep"
            + File.separator + "geomag" + File.separator
            + "ksDateRange.properties";

    // Non Leap Year Values
    public static final String NON_LEAP_YEAR_START_RANGE_1 = "NON_LEAP_YEAR_START_RANGE_1_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_2 = "NON_LEAP_YEAR_START_RANGE_2_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_3 = "NON_LEAP_YEAR_START_RANGE_3_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_4 = "NON_LEAP_YEAR_START_RANGE_4_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_5 = "NON_LEAP_YEAR_START_RANGE_5_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_6 = "NON_LEAP_YEAR_START_RANGE_6_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_7 = "NON_LEAP_YEAR_START_RANGE_7_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_8 = "NON_LEAP_YEAR_START_RANGE_8_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_9 = "NON_LEAP_YEAR_START_RANGE_9_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_10 = "NON_LEAP_YEAR_START_RANGE_10_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_11 = "NON_LEAP_YEAR_START_RANGE_11_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_12 = "NON_LEAP_YEAR_START_RANGE_12_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_13 = "NON_LEAP_YEAR_START_RANGE_13_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_14 = "NON_LEAP_YEAR_START_RANGE_14_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_15 = "NON_LEAP_YEAR_START_RANGE_15_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_16 = "NON_LEAP_YEAR_START_RANGE_16_MONTH_DAY";

    public static final String NON_LEAP_YEAR_START_RANGE_17 = "NON_LEAP_YEAR_START_RANGE_17_MONTH_DAY";

    public static final String NON_LEAP_YEAR_END_RANGE_17 = "NON_LEAP_YEAR_END_RANGE_17_MONTH_DAY";

    // Leap Year Values
    public static final String LEAP_YEAR_START_RANGE_2 = "LEAP_YEAR_START_RANGE_2_MONTH_DAY";

    public static final String LEAP_YEAR_START_RANGE_3 = "LEAP_YEAR_START_RANGE_3_MONTH_DAY";

    public boolean useDefaultKsDateRange = false;

    public static synchronized KStationCoefficientLookup getInstance() {
        if (instance == null) {

            try {
                instance = new KStationCoefficientLookup();
            } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

    private KStationCoefficientLookup() throws JAXBException {
        coeff = new HashMap<String, KStationCoefficient>();
        try {
            initStationList();
            initKsDateRange();
        } catch (GeoMagException e) {
            logger.error("Unable to initialize K stations list!", e);
        } catch (IOException e) { // failed to get KsDateRange property values
            useDefaultKsDateRange = true;
            logger.error("Unable to initialize Ks Date Ranges!", e);
        }
    }

    public KStationCoefficient getStationByCode(String stnCode) {
        return coeff.get(stnCode);
    }

    public Map<String, KStationCoefficient> getStationsByCodeMap() {
        return coeff;
    }

    private void initStationList() throws GeoMagException, JAXBException {
        IPathManager pathMgr = PathManagerFactory.getPathManager();

        LocalizationContext commonStaticBase = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.BASE);

        LocalizationContext commonStaticSite = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.SITE);

        String basePath = "";
        String sitePath = "";
        try {
            basePath = pathMgr.getFile(commonStaticBase, KSTANDARD_LOOKUP_FILE)
                    .getCanonicalPath();
            sitePath = pathMgr.getFile(commonStaticSite, KSTANDARD_LOOKUP_FILE)
                    .getCanonicalPath();
        } catch (IOException e) {
            logger.error("Error reading K stations coefficient table. ", e);
        }

        File baseStnsFile = new File(basePath);
        File siteStnsFile = new File(sitePath);
        KStationCoeffTableReader kStationCoeffTbl = null;

        // initialize using base version
        if (baseStnsFile.exists()) {
            kStationCoeffTbl = new KStationCoeffTableReader(
                    baseStnsFile.getPath());
        }

        // if site version exists, use the site version instead
        if (siteStnsFile.exists()) {
            kStationCoeffTbl = new KStationCoeffTableReader(
                    siteStnsFile.getPath());
        }

        List<KStationCoefficient> list = (kStationCoeffTbl != null) ? kStationCoeffTbl
                .getStationList() : new ArrayList<KStationCoefficient>();

        for (KStationCoefficient station : list) {
            coeff.put(station.getStationCode(), station);
        }
    }

    private void initKsDateRange() throws IOException {
        IPathManager pathMgr = PathManagerFactory.getPathManager();

        LocalizationContext commonStaticBase = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.BASE);

        LocalizationContext commonStaticSite = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.BASE);

        String basePath = pathMgr.getFile(commonStaticBase,
                KSDATERANGE_PROPERTIES_FILE).getCanonicalPath();

        String sitePath = pathMgr.getFile(commonStaticSite,
                KSDATERANGE_PROPERTIES_FILE).getCanonicalPath();

        File baseKsDateRangeFile = new File(basePath);
        File siteKsDateRangeFile = new File(sitePath);

        // initialize using base version
        if (baseKsDateRangeFile.exists()) {
            this.ksDateRange = PropertiesUtil.read(baseKsDateRangeFile);
        }

        // if site version exists, use the site version instead
        if (siteKsDateRangeFile.exists()) {
            this.ksDateRange = PropertiesUtil.read(siteKsDateRangeFile);
        }
    }

    public Properties getKsDateRange() {

        return this.ksDateRange;
    }
}
// @formatter:on