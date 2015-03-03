/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.common.tsScaleMngr;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * This class reads and writes xaxis scales. (It initially reads all the xml
 * files in the xAxisScales directory and unmarshals them as XAxisScales.)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 24, 2014   R4875     sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class XAxisScaleMngr {

    private static HashMap<String, XAxisScale> xAxisScales = null;

    private static XAxisScaleMngr instance = null;

    public static final String NullScaleName = "NoScale";

    private XAxisScaleMngr() {
    }

    public static synchronized XAxisScaleMngr getInstance() {
        if (instance == null)
            instance = new XAxisScaleMngr();
        return instance;
    }

    // read in all the xml files in the xAxisScales directory.
    synchronized private void readXAxisScales() {
        // xAxisScales = null;
        // if (xAxisScales == null) {

        xAxisScales = new HashMap<String, XAxisScale>();

        // get all of the xml (GraphAttributes) files in the
        // XAXIS_SCALE_DIR directory.
        // This will return files from all context levels.
        Map<String, LocalizationFile> xAxisScaleLclFiles = NcPathManager
                .getInstance().listFiles(NcPathConstants.XAXIS_SCALE_DIR,
                        new String[] { ".xml" }, true, true);

        for (LocalizationFile lFile : xAxisScaleLclFiles.values()) {
            try {
                XAxisScale xAxisScale = null;
                Object xmlObj = SerializationUtil
                        .jaxbUnmarshalFromXmlFile(lFile.getFile()
                                .getAbsolutePath());

                if (xmlObj instanceof XAxisScale) {
                    xAxisScale = (XAxisScale) xmlObj;

                    xAxisScale.setLocalizationFile(lFile);

                    if (!lFile.getName().equals(
                            xAxisScale.createLocalizationFilename())) {
                        // This will only cause a problem if the user
                        // creates a USER-level (uses naming convention) and
                        // then reverts back to the base version by deleting
                        // the user level file. The code will
                        // look for the base version using the naming
                        // convention and so won't find the file.
                        System.out
                                .println("Warning: GraphAttributes file doesn't follow the naming convention.\n");
                        System.out.println(lFile.getName() + " should be "
                                + xAxisScale.createLocalizationFilename());
                    }

                    xAxisScales.put(xAxisScale.getName(), xAxisScale);

                }
            } catch (SerializationException e) {
                System.out.println("Error unmarshalling file: "
                        + lFile.getFile().getAbsolutePath());
                System.out.println(e.getMessage());
            }
        }
        // if (xAxisScales.size() == 0)
        // xAxisScales = null;

        // }
    }

    public HashMap<String, XAxisScale> getXAxisScales() {

        readXAxisScales();

        return xAxisScales;
    }

    /**
     * Reads the contents of the table file
     * 
     * @param xmlFilename
     *            - full path of the xml table name
     * @return - a list of stations
     * @throws JAXBException
     */
    public XAxisScale getXAxisScale(String name) {
        readXAxisScales();

        XAxisScale xAxisScale = null;

        if (xAxisScales != null) {
            xAxisScale = xAxisScales.get(name);
        }

        return xAxisScale;
    }

    /*
     * Writes a JAXB-based object into the xml file and updates the map.
     */
    public void saveXAxisScale(XAxisScale scale) throws VizException {

        readXAxisScales();

        if (scale == null || scale.getSize() == 0 || scale.getName() == null
                || scale.getName().isEmpty()) {

            throw new VizException(
                    "saveXAxisScale: GraphAttributes is null or doesn't have a name?");
        }
        if (scale.getName().equals(NullScaleName)) {
            if (scale.getSize() != 0) {
                throw new VizException("Can't save a non-null GraphAttributes as "
                        + NullScaleName);
            }
        }

        // create a localization file for the GraphAttributes
        LocalizationContext userCntxt = NcPathManager.getInstance().getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile lFile = NcPathManager.getInstance()
                .getStaticLocalizationFile(scale.createLocalizationFilename());

        // if the file exists overwrite it.
        if (lFile == null
                || lFile.getContext().getLocalizationLevel() != LocalizationLevel.USER) {
            lFile = NcPathManager.getInstance().getLocalizationFile(userCntxt,
                    scale.createLocalizationFilename());
        }

        scale.setLocalizationFile(lFile);
        File xAxisScaleFile = lFile.getFile();

        try {
            SerializationUtil.jaxbMarshalToXmlFile(scale,
                    xAxisScaleFile.getAbsolutePath());

            lFile.save();

            // update this GraphAttributes in the map
            xAxisScales.put(scale.getName(), scale);

        } catch (LocalizationOpFailedException e) {
            throw new VizException(e);
        } catch (SerializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteXAxisScale(String scaleName) throws VizException {

        XAxisScale delScale = getXAxisScale(scaleName);

        if (delScale == null) {
            throw new VizException("Could not find GraphAttributes, " + scaleName);
        }

        LocalizationFile lFile = delScale.getLocalizationFile();

        if (lFile == null
                || !lFile.getFile().exists()
                || lFile.getContext().getLocalizationLevel() != LocalizationLevel.USER) {
            throw new VizException("File "
                    + delScale.createLocalizationFilename()
                    + " doesn't exist or is not a User Level GraphAttributes.");
        }

        try {
            String lFileName = lFile.getName();

            lFile.delete();
            xAxisScales.remove(scaleName);

            lFile = NcPathManager.getInstance().getStaticLocalizationFile(
                    lFileName);

            // If there is another file of the same name in the BASE/SITE/DESK
            // then
            // update the xAxisScales with this version.
            if (lFile != null) {
                if (lFile.getContext().getLocalizationLevel() == LocalizationLevel.USER) {
                    System.out
                            .println("Delete GraphAttributes successful but unexplained error occurred.");
                    return;
                    // throw new VizException(
                    // "Unexplained error deleting GraphAttributes.");
                }
                try {
                    XAxisScale xAxisScale = null;
                    Object xmlObj = SerializationUtil
                            .jaxbUnmarshalFromXmlFile(lFile.getFile()
                                    .getAbsolutePath());

                    if (xmlObj instanceof XAxisScale) {
                        xAxisScale = (XAxisScale) xmlObj;

                        xAxisScale.setLocalizationFile(lFile);

                        if (xAxisScale.getName() != null) {
                            xAxisScales.put(xAxisScale.getName(), xAxisScale);
                        }
                    }
                } catch (SerializationException e) {
                    System.out.println("Error unmarshalling file: "
                            + lFile.getFile().getAbsolutePath());
                    System.out.println(e.getMessage());
                }

            }

            // TODO : check if there is a base or site level file of the same
            // name and
            // update with it....
        } catch (LocalizationOpFailedException e) {
            throw new VizException("Error Deleting GraphAttributes, " + scaleName
                    + "\n" + e.getMessage());
        }
    }

    public XAxisScale getDefaultxAxisScale() {
        XAxisScale dfltPM = new XAxisScale();
        dfltPM.setName(NullScaleName);
        dfltPM.getXAxisScaleElements();
        return dfltPM;
    }

    // TODO Add logic for if the GraphAttributes is in the base/site level and
    // provide appropriate confirmation message
    public static boolean xAxisScaleFileExists(String name) {
        String fname = name;
        if (!name.endsWith(".xml")) {
            fname = fname + ".xml";
        }
        File f = NcPathManager.getInstance().getStaticFile(
                NcPathConstants.XAXIS_SCALE_DIR + File.separator + fname);

        return (f != null && f.exists());
    }

}