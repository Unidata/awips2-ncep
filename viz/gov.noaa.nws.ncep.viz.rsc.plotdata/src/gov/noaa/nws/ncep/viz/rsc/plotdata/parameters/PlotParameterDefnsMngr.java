package gov.noaa.nws.ncep.viz.rsc.plotdata.parameters;

import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NcUnits;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * This class reads parameter list from the xml file. And set the parameter
 * property if needed.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/09        172         M. Li       Initial Creation
 * 06/10        291         G. Zhang	Added support for synop/ship/ship6hr
 * 03/04/11     425         G. Hull     renamed from ParmList; create filename from plugin name,
 *                                      use SerializationUtil to un/marshal
 * 05/27/11     441         G. Hull     register NcUnits; Determine windBarb params from the plotMode 
 * 07/31/11     450         G. Hull     Make singleton. Use NcPathManager
 * 08/28/15   R7757         B. Hebbard  Cleanups; use IUFStatusHandler
 * 
 * </pre>
 * 
 * @author mli
 * @version 1
 */
public class PlotParameterDefnsMngr {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(PlotParameterDefnsMngr.class);

    private Map<String, LocalizationFile> locFilesMap = null;

    // map from the pluginName to the PlotParameterDefns
    private Map<String, PlotParameterDefns> plotPrmDefnsMap = null;

    private static PlotParameterDefnsMngr instance = null;

    public static PlotParameterDefnsMngr getInstance() {
        if (instance == null) {
            instance = new PlotParameterDefnsMngr();
        }

        return instance;
    }

    private PlotParameterDefnsMngr() {

        NcUnits.register(); // moved from PlotResource.initResource

        // Get all of the XML (PlotParameterDefns) files in the PlotParameters
        // directory.

        plotPrmDefnsMap = new HashMap<String, PlotParameterDefns>();

        locFilesMap = NcPathManager.getInstance().listFiles(
                NcPathConstants.PLOT_PARAMETERS_DIR, new String[] { ".xml" },
                false, true);

        for (LocalizationFile lFile : locFilesMap.values()) {

            File plotParamsFile = lFile.getFile();

            try {
                if (plotParamsFile != null && plotParamsFile.exists()) {

                    PlotParameterDefns paramDefnTable = readParameterFile(plotParamsFile);

                    if (paramDefnTable == null) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Failed to read plot parameter definitions file "
                                        + plotParamsFile);
                    }

                    if (plotPrmDefnsMap.containsKey(paramDefnTable.getPlugin())) {
                        // TODO: Investigate why this is happening for plugin
                        // airep (Warning condition)
                    }

                    plotPrmDefnsMap.put(paramDefnTable.getPlugin(),
                            paramDefnTable);

                }
            } catch (JAXBException exp) {
                statusHandler.handle(
                        Priority.PROBLEM,
                        "Error parsing PlotParameterDefns file: "
                                + lFile.getName() + " : " + exp.getMessage());
                continue;
            }
        }
    }

    private PlotParameterDefns readParameterFile(File xmlFile)
            throws JAXBException {

        PlotParameterDefns parmsDefnsList = null;

        try {
            FileReader fr = new FileReader(xmlFile);
            char[] b = new char[(int) xmlFile.length()];
            fr.read(b);
            fr.close();
            String str = new String(b);

            Object xmlObj = SerializationUtil.unmarshalFromXml(str.trim());

            parmsDefnsList = (PlotParameterDefns) xmlObj;

            return parmsDefnsList;

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "unmarshall error: " + e.getMessage());
        }

        return null;
    }

    public PlotParameterDefns getPlotParamDefns(String pluginName) {
        return plotPrmDefnsMap.get(pluginName);
    }

}