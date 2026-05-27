/*
 * com.raytheon.edex.plugin.grib.filenameprocessor.NcgribModelNameMap
 * 
 * 29 May 2013
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.edex.plugin.ncgrib;

import java.util.HashMap;
import java.util.regex.Pattern;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.JAXBManager;

/**
 * Class to hold the model name look up map for ncgrib data file
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 05/13        995         B. Yin      Initial Creation.
 * Oct 15, 2012 2473        bsteffen    Move to ncgrib plugin
 * 07/14/15     R8307       RCReynolds  Allow SITE localization file
 *                                      to override BASE
 * 04/18/2022   102937      smanoj      Fixed bug with SITE override.
 * 
 * </pre>
 * 
 * @author B. Yin
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcgribModelNameMap {

    /*
     * Mapping between file name templates and model names
     */
    private HashMap<String, String> mapping;

    private static final String NC_GRIB_MODEL_NAME_MAP_FILE = IPathManager.SEPARATOR
            + "grib" + IPathManager.SEPARATOR + "ncgrib"
            + IPathManager.SEPARATOR + "ncgribModelNameMap.xml";

    /**
     * Load the ncgrib model name mapping between file name templates and model
     * names.
     * 
     * @return NcgribModelNameMap
     * @throws Exception
     */
    public static NcgribModelNameMap load() throws Exception {

        IPathManager pathMgr = PathManagerFactory.getPathManager();

        LocalizationContext commonStaticBase = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.BASE);
        LocalizationContext commonStaticSite = pathMgr.getContext(
                LocalizationContext.LocalizationType.COMMON_STATIC,
                LocalizationContext.LocalizationLevel.SITE);

        LocalizationFile basePathFile = pathMgr.getLocalizationFile(
                commonStaticBase, NC_GRIB_MODEL_NAME_MAP_FILE);

        LocalizationFile sitePathFile = pathMgr.getLocalizationFile(
                commonStaticSite, NC_GRIB_MODEL_NAME_MAP_FILE);

        LocalizationFile pathFileToUse = null;
        if (sitePathFile.exists()) {
            pathFileToUse = sitePathFile;
        } else if (basePathFile.exists()) {
            pathFileToUse = basePathFile;
        }

        NcgribModelNameMap map = null;
        if (pathFileToUse != null) {
            map = pathFileToUse.jaxbUnmarshal(NcgribModelNameMap.class,
                    new JAXBManager(NcgribModelNameMap.class));
        }
        return map;

    }

    /**
     * Gets the model name from the mapping table for the specified file name.
     * 
     * @param fileName
     * @return model name
     */
    public String getModelName(String fileName) {
        String model = null;

        for (String namePattern : mapping.keySet()) {
            if (Pattern.matches(namePattern, fileName)) {
                model = mapping.get(namePattern);
                break;
            }
        }

        return model;
    }
}
