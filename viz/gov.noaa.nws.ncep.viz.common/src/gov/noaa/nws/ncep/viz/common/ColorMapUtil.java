package gov.noaa.nws.ncep.viz.common;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * Facilitates loading of colormaps
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 11/17/2009   187          Q.Zhou      Initial created.
 * 12/17/2009                G. Hull     placeholder for category
 * 01/02/2010   204          M. Li       check for Radar or Sat resource
 * 03/14/2010                B. Hebbard  add path separator btw tblDir and rsc (2 places);
 *                                       fix circular build dependency on MosaicResource	
 * 03/21/2010   259          G. Hull     load by category        
 * 07/15/2011   450          G. Hull     use new NcPathManager 
 * 03/15/2012   621          S. Gurung   Added methods to read lockedColorMaps.tbl;
 *                                       load/check for locked colormaps.
 * 04/10/2013   #958         qzhou       Added SolarImage in getColorMapCategories.
 * 08/06/2013   2210         njensen     Moved colormaps to common_static
 * Nov 11, 2013 2361         njensen     Use ColorMap.JAXB for XML processing
 * 05/17/2016   R18398       S. Russell  Updated method loadLockedColorMap()
 *                                       and removed deprecated method calls to
 *                                       deprecated LocalizationFile.getFile(),
 *                                       and NcPathManager through the class.
 * </pre>
 * 
 * @author Q. Zhou
 * @version 1
 */

public class ColorMapUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ColorMapUtil.class);

    ColorMapUtil() {
    }

    /**
     * Load a colormap by name
     * 
     * @param name
     *            name of the colormap
     * @return the colormap representation
     * @throws VizException
     */
    public static IColorMap loadColorMap(String cat, String name)
            throws VizException {

        String cmapCat = cat.substring(0, 1) + cat.substring(1).toLowerCase();
        String fname = null;
        ColorMap cm = null;

        // Read lockedColorMaps.tbl to get the list of locked color maps
        LockedColorMaps lockedCmaps = readLockedColorMapFile();

        if (lockedCmaps != null && lockedCmaps.isLocked(name)) {
            return loadLockedColorMap(cat, name);
        } else {

            fname = NcPathConstants.COLORMAPS_DIR + File.separator + cmapCat
                    + File.separator + name + ".cmap";

            try {

                File f = PathManagerFactory.getPathManager().getStaticFile(
                        fname);

                if (f != null) {

                    // JAXBManager.unmarshalFromInputStream(InputStream) does
                    // not work here. openInputStream() also depends on the
                    // deprecated getFile().
                    cm = ColorMap.JAXB
                            .unmarshalFromXmlFile(f.getAbsolutePath());

                    cm.setName(name);

                } else {
                    throw new VizException("Error finding colormap " + fname);
                }
            } catch (Exception e) {
                throw new VizException("Error umarshalling colormap " + fname,
                        e);
            }
        }

        return cm;
    }

    /**
     * Load a Color Map
     * 
     * @param cat
     * @param name
     * @param locked
     * @return
     * @throws VizException
     */
    public static IColorMap loadColorMap(String cat, String name, boolean locked)
            throws VizException {
        if (locked)
            return loadLockedColorMap(cat, name);
        else
            return loadColorMap(cat, name);
    }

    /**
     * Reports back about whether or not the color map exists
     * 
     * @param cat
     * @param name
     * @return
     */

    public static boolean colorMapExists(String cat, String name) {
        // TODO Add logic for if the colormap is in the base/site level and
        // provide appropriate confirmation msg

        String fname = name;

        if (!name.endsWith(".cmap")) {
            fname = fname + ".cmap";
        }
        File f = PathManagerFactory.getPathManager().getStaticFile(
                NcPathConstants.COLORMAPS_DIR + File.separator + cat
                        + File.separator + fname);

        return (f != null && f.exists());
    }

    /**
     * 
     * @return
     */
    public static String[] getColorMapCategories() {
        // TODO : read from Localization
        return new String[] { "Satellite", "Solarimage", "Radar", "Other" };
    }

    /**
     * Lists all the colormaps available in the colormaps dir
     * 
     * @param cat
     * @return
     */
    public static String[] listColorMaps(String cat) {

        NcPathManager pathMngr = NcPathManager.getInstance();

        String cmapCat = cat.substring(0, 1) + cat.substring(1).toLowerCase();

        Set<LocalizationContext> searchContexts = new HashSet<>();
        searchContexts.addAll(Arrays.asList(pathMngr
                .getLocalSearchHierarchy(LocalizationType.CAVE_STATIC)));

        Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                NcPathConstants.COLORMAPS_DIR + File.separator + cmapCat,
                new String[] { "cmap" }, false, true);

        ArrayList<String> cmaps = new ArrayList<>(lFiles.size());

        for (LocalizationFile lFile : lFiles.values()) {
            if (!lFile.exists()) {
                statusHandler.debug("cmap file " + lFile.getName()
                        + " doesn't exist.");
            }

            // lFile.getName() is /luts/Satellite/<fname>.cmap we want to
            // save <fname>
            String fname = lFile.getName();
            fname = fname
                    .substring(fname.lastIndexOf(System
                            .getProperty("file.separator")) + 1, fname
                            .lastIndexOf("."));
            cmaps.add(fname);
        }

        String[] cmapArr = cmaps.toArray(new String[0]);
        Arrays.sort(cmapArr);
        return cmapArr;
    }

    /**
     * Save the user altered color map. Assumes that a check/prompt for
     * overwriting has already been done.
     * 
     * @param colorMap
     * @param cmapCat
     * @param cmapName
     * @throws VizException
     */
    public static void saveColorMap(ColorMap colorMap, String cmapCat,
            String cmapName) throws VizException {

        String cmapFileName = NcPathConstants.COLORMAPS_DIR + File.separator
                + cmapCat + File.separator + cmapName;

        if (!cmapFileName.endsWith(".cmap")) {
            cmapFileName += ".cmap";
        }

        // Prompt to also save to DESK? or determine if we are logged in 'as' a
        // DESK and only
        // save to desk?
        LocalizationContext context = NcPathManager.getInstance().getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile lclFile = NcPathManager.getInstance()
                .getLocalizationFile(context, cmapFileName);

        try {
            SaveableOutputStream outstream = lclFile.openOutputStream();
            ColorMap.JAXB.marshalToStream(colorMap, outstream);
            outstream.save();
            outstream.close();

        } catch (SerializationException se) {
            throw new VizException("Unable to Marshal ColorMap "
                    + colorMap.getName());
        } catch (LocalizationException le) {
            throw new VizException("Unable to Localize ColorMap "
                    + colorMap.getName());
        } catch (Exception e) {
            throw new VizException("Unable to save ColorMap "
                    + colorMap.getName());
        }
    }

    /**
     * 
     * @param cmapCat
     * @param cmapName
     * @throws VizException
     */
    public static void deleteColorMap(String cmapCat, String cmapName)
            throws VizException {
        // TODO : add code to check for BASE/system context

        String cmapFilename = cmapName;

        if (!cmapFilename.endsWith(".cmap")) {
            cmapFilename += ".cmap";
        }

        LocalizationFile cmapFile = NcPathManager.getInstance()
                .getStaticLocalizationFile(
                        NcPathConstants.COLORMAPS_DIR + File.separator
                                + cmapCat + File.separator + cmapFilename);

        if (cmapFile.getContext().getLocalizationLevel() != LocalizationLevel.USER) {
            throw new VizException("Can't delete a Colormap localized at the "
                    + cmapFile.getContext().getLocalizationLevel().toString()
                    + " level");
        }

        // TODO : check if there is a BASE/SITE/DESK level colormap by the same
        // name and inform the user that they will be reverting to another
        // version of the file.

        try {
            cmapFile.delete();
        } catch (LocalizationException e) {
            throw new VizException(e);
        }
    }

    /**
     * Return an InputStream to the lockedColorMaps.tbl localization file
     * 
     * @return
     */
    public static InputStream getLockedColorMapInputStream() {

        LocalizationFile lf = null;
        InputStream instream = null;

        IPathManager pathManager = PathManagerFactory.getPathManager();

        Map<LocalizationLevel, LocalizationFile> files = pathManager
                .getTieredLocalizationFile(LocalizationType.CAVE_STATIC,
                        NcPathConstants.LOCKED_CMAP_TBL);

        if (files != null && !files.isEmpty()) {

            if (files.containsKey(LocalizationLevel.SITE)) {
                lf = files.get(LocalizationLevel.SITE);
            } else {
                lf = files.get(LocalizationLevel.BASE);
            }

            try {
                instream = lf.openInputStream();
            } catch (Exception e) {
                statusHandler.error("Error getting locked ColorMap file", e);
            }
        }

        return instream;
    }

    /**
     * Read lockedColorMaps.tbl localization file
     * 
     * @return
     */
    public static LockedColorMaps readLockedColorMapFile() {
        InputStream instream = ColorMapUtil.getLockedColorMapInputStream();
        LockedColorMaps lockedCmaps = null;

        try {
            lockedCmaps = new LockedColorMaps(instream);
            instream.close();

        } catch (IOException e) {
            statusHandler.error("Error using readLockedColorMapFile() ", e);
        }

        return lockedCmaps;
    }

    /**
     * Load a locked colormap by name
     * 
     * @param name
     *            name of the colormap
     * @return the colormap representation
     * @throws VizException
     */
    public static IColorMap loadLockedColorMap(String cat, String name)
            throws VizException {

        String path = null;
        InputStream instream = null;
        String cmapCat = cat.substring(0, 1) + cat.substring(1).toLowerCase();

        try {
            IPathManager pm = PathManagerFactory.getPathManager();

            path = NcPathConstants.COLORMAPS_DIR + File.separator + cmapCat
                    + File.separator + name + ".cmap";

            Map<LocalizationLevel, LocalizationFile> files = pm
                    .getTieredLocalizationFile(LocalizationType.CAVE_STATIC,
                            path);

            if (files != null && !files.isEmpty()) {

                if (files.containsKey(LocalizationLevel.SITE)) {
                    instream = files.get(LocalizationLevel.SITE)
                            .openInputStream();
                } else {
                    instream = files.get(LocalizationLevel.BASE)
                            .openInputStream();
                }

                ColorMap cm = (ColorMap) ColorMap.JAXB
                        .unmarshalFromInputStream(instream);

                cm.setName(name);
                instream.close();
                return cm;

            } else {
                throw new VizException("Can't find colormap: " + name
                        + " at LocalizationType.CAVE_STATIC at path: " + path);
            }
        } catch (Exception e) {
            throw new VizException("Unable to parse the colormap called "
                    + name + " at LocalizationLevel SITE or BASE at path "
                    + path, e);
        }
    }
}
