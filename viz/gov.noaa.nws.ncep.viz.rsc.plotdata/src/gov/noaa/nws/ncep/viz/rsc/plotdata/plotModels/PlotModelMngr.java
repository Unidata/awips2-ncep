package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter;
import gov.noaa.nws.ncep.edex.common.metparameters.PrecipitableWaterForEntireSounding;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.ObsSndType;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile.PfcSndType;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.soundingrequest.NcSoundingQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * This class reads and writes plotModels. It initially reads all the xml files
 * in the plotModels directory and unmarshals them as plotModels.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/15        #217        Greg Hull   Initial Creation
 * 03/21        R1G2-9      Greg Hull   synchronized readPlotModels()     
 * 03/04/11      425        Greg Hull   change category to plugin
 * 03/08/11      425        Greg Hull   add deletePlotModel
 * 08/15/11      450        Greg Hull   NcPathManager and save LocalizationFiles;
 *                                      use SerializationUtil instead of JaxBContext
 * 07142015     RM#9173     Chin Chen   use NcSoundingQuery.genericSoundingDataQuery() to query uair and modelsounding data
 *                                      add a static query function querySoundingData() for all plot model methods to use for ncuair and modelsounding
 *                                      sounding data use SerializationUtil instead of JaxBContext 
 * 07/20/15	    #8051   Jonas Okwara    Modified readPlotModel method to read PLOT_MODEL directory
 * 07/24/15     #8051   Jonas Okwara    Switched from SerializationUtil methods to jaxbMarshal and jaxbUnmarshal methods
 * 10/01/2015   R8051   Edwin Brown     Clean up work
 * 09/22/2016   RM15953 R.Reynolds      Added capability for wind interpolation
 * 09/26/2016   R20482  Bugenhagen      Handle saving localization file without 
 *                                      throwing exception due to updated 
 *                                      checksum.  Fixed issue with deletion.
 *                                      Replaced deprecated calls
 *                                      to LocalizationFile.save method.
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class PlotModelMngr {

    private static HashMap<String, PlotModel> plotModels = null;

    private static PlotModelMngr instance = null;

    protected String XML = ".xml";

    protected String DEFAULT = "default";

    protected String NONE = "none";

    final String DFLT_SVG_TEMPLATE_FILE = "standardPlotModelTemplate.svg";

    private static JAXBManager Jaxb;

    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(PlotModelMngr.class);

    private boolean verbose = false;

    private PlotModelMngr() {

        try {
            Jaxb = new JAXBManager(PlotModel.class);
        } catch (JAXBException e) {
            String statusString = "Couldn't create JAXBMAnager.";
            statusHandler.handle(Priority.INFO, statusString, e);
        }
    }

    public static synchronized PlotModelMngr getInstance() {
        if (instance == null)
            instance = new PlotModelMngr();
        return instance;
    }

    private boolean isNonPlotModelDirectory(LocalizationFile lFile) {

        if ((lFile.getFile().getPath().contains("Parameters"))
                || (lFile.getFile().getPath().contains("Filters"))) {
            // TO DO: Replace this with standard debugging approach
            if (verbose) {
                System.out
                        .printf("%s is in a non plot model directory so probably not a plot model, don't add. %n",
                                lFile.getFile().getPath());
            }

            return true;
        }

        return false;
    }

    // Read in all of the xml files in the PlotModels directory and add them to
    // plotModels hash map. Note: the Plot Model manager dialog builds it's list
    // of plot models using a different mechanism, maybe resource definitions
    synchronized private void readPlotModels() {

        if (plotModels == null) {
            // This runs when the plot model manager dialog is brought up the
            // first time
            plotModels = new HashMap<>();

            // Get all of the xml (plotModel) files in the PLOT_MODELS
            // directory. This will return files from all context levels.
            // This is recursive to pick up files in the 'plugin' subdirectories
            // but as a result the PlotParameterDefns in PlotModels /
            // PlotParameters will also be picked up so we have to ignore them.
            // Get a listing of all of the XML files in the PLOT_MODEL_DIR using
            // the NCPathManager
            Map<String, LocalizationFile> plotModelLocalizationFiles = NcPathManager
                    .getInstance().listFiles(NcPathConstants.PLOT_MODELS_DIR,
                            new String[] { XML }, true, true);

            // Loop through all of the xml files in the plot model directory
            for (LocalizationFile lFile : plotModelLocalizationFiles.values()) {

                try {

                    // If "Parameters" or "Filters" is in the file path continue
                    // to next file. (Don't load the non plot model files in
                    // PlotParameters/ and ConditionalFilters/)
                    if (isNonPlotModelDirectory(lFile)) {
                        continue;
                    }

                    // Try to unmarshal the plot model.
                    PlotModel plotModel = lFile.jaxbUnmarshal(PlotModel.class,
                            Jaxb);

                    plotModel.setLocalizationFile(lFile);

                    LocalizationLevel lLvl = lFile.getContext()
                            .getLocalizationLevel();

                    // Build localization label string
                    // TO DO: This smashes the plugin and the plotModel name
                    // together with no
                    // space. If anything in the code parses this it would be
                    // unable to separate
                    String plotModelLocalizationLabel = plotModel.getPlugin()
                            + plotModel.getName() + " (" + lLvl.name() + ")";

                    // Store plotModel and it's localization string into
                    // plotModels hash map
                    plotModels.put(plotModelLocalizationLabel, plotModel);

                    // Print info about the plot model that was added
                    if (verbose) {
                        System.out
                                .printf("Read plot model: label:%s lFile.getName:%s getPlugin:%s  plotModel.getName:%s  lLvel:%s %n",
                                        plotModelLocalizationLabel,
                                        lFile.getName(), plotModel.getPlugin(),
                                        plotModel.getName(), lLvl.name());
                    }

                } catch (LocalizationException e) {
                    String statusString = "Tried to load " + lFile.getName()
                            + " and it's not a plot model";
                    statusHandler.handle(Priority.INFO, statusString, e);
                }

            }

            if (plotModels.size() == 0)
                plotModels = null;
        }
    }

    /**
     * 
     * @return ArrayList
     */
    public ArrayList<String> getPlugins() {
        readPlotModels();

        ArrayList<String> pluginList = new ArrayList<>();
        for (PlotModel pm : plotModels.values()) {
            if (!pluginList.contains(pm.getPlugin())) {
                pluginList.add(pm.getPlugin());
            }
        }
        return pluginList;
    }

    // if null cat then get all the plotModels.
    // Note: programmer used "cat" for category, seems to be used
    // interchangeably with "plugin"/plgn
    public HashMap<String, PlotModel> getPlotModelsByPlugin(String plgn) {

        readPlotModels();

        HashMap<String, PlotModel> plotModelsByPlugin = new HashMap<>();

        for (PlotModel pm : plotModels.values()) {
            if (plgn == null || plgn.equalsIgnoreCase(pm.getPlugin())) {
                LocalizationLevel lLvl = pm.getLocalizationFile().getContext()
                        .getLocalizationLevel();
                plotModelsByPlugin.put(pm.getName() + " (" + lLvl.name() + ")",
                        pm);
                if (verbose) {
                    String tempString = pm.getPlugin() + " " + pm.getName()
                            + " (" + lLvl.name() + ")";
                    System.out.printf(tempString);
                }
            }
        }

        return plotModelsByPlugin;
    }

    /**
     * Reads the contents of the table file
     * 
     * @param xmlFilename
     *            full path of the xml table name
     * @return - a list of stations
     * @throws JAXBException
     */
    public PlotModel getPlotModel(String plgn, String plotModelName) {
        readPlotModels();

        PlotModel plotModel = getPlotModelsByPlugin(plgn).get(plotModelName);
        return plotModel;
    }

    // Writes a JAXB-based object into the xml file and updates the map.
    public void savePlotModel(PlotModel plotModel) throws VizException,
            LocalizationException {

        readPlotModels();

        if (plotModel == null || plotModel.getName() == null) {
            throw new VizException(
                    "savePlotModel: PlotModel is null or doesn't have a name?");
        }

        // Get userContxt for the source plotModel
        LocalizationContext userCntxt = NcPathManager.getInstance().getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);
        // This creates the destination lFile (and new file name path)
        LocalizationFile lFile = NcPathManager.getInstance()
                .getLocalizationFile(userCntxt,
                        plotModel.createLocalizationFilename());

        plotModel.setLocalizationFile(lFile);

        try {
            SaveableOutputStream outstream = lFile.openOutputStream();
            Jaxb.marshalToStream(plotModel, outstream);
            outstream.save();
            outstream.close();

            LocalizationLevel lLvl = lFile.getContext().getLocalizationLevel();
            // update this PlotModel in the map
            plotModels.put(plotModel.getPlugin() + plotModel.getName() + " ("
                    + lLvl.name() + ")", plotModel);

        } catch (LocalizationException | IOException e) {
            statusHandler.error(
                    "Error saving plot model: " + plotModel.getName(), e);
        } catch (SerializationException e) {
            statusHandler.error(
                    "Error marshalling plot model " + plotModel.getName(), e);
        }

    }

    public void deletePlotModel(String pluginName, String pltMdlName)
            throws VizException {

        PlotModel delPltMdl = getPlotModel(pluginName, pltMdlName);

        if (verbose)
            System.out.printf("Delete %s-%s\n", pluginName, pltMdlName);

        if (delPltMdl == null) {
            throw new VizException(
                    "Trying to delete, but could not find plot model, "
                            + pltMdlName + ", for plugin, " + pluginName);
        }

        LocalizationFile lFile = delPltMdl.getLocalizationFile();
        if (lFile == null
                || !lFile.getFile().exists()
                || lFile.getContext().getLocalizationLevel() != LocalizationLevel.USER) { // sanity
                                                                                          // check
            throw new VizException("File "
                    + delPltMdl.createLocalizationFilename()
                    + " doesn't exist or is not a User Level Plot Model.");
        }

        try {
            String lFileName = lFile.getName();
            lFile.delete();

            // Give the localization server time to delete the file before
            // getting the BASE, SITE or DESK level file.
            Thread.sleep(500);

            plotModels.remove(pluginName + pltMdlName);
            lFile = NcPathManager.getInstance().getStaticLocalizationFile(
                    lFileName);

            // If there is another file of the same name in the BASE/SITE/DESK
            // then update the plotmodels with this version.

            if (lFile != null) {
                if (lFile.getContext().getLocalizationLevel() == LocalizationLevel.USER) {
                    throw new VizException(
                            "Unexplained error deleting PlotModel.");
                }
                try {
                    PlotModel plotModel = lFile.jaxbUnmarshal(PlotModel.class,
                            Jaxb);
                    plotModel.setLocalizationFile(lFile);
                    if (plotModel.getPlugin() != null) {
                        plotModels.put(
                                plotModel.getPlugin() + plotModel.getName(),
                                plotModel);
                    }
                } catch (LocalizationException e) {
                    throw new VizException("Error unmarshalling file: "
                            + lFile.getFile().getAbsolutePath()
                            + e.getMessage());

                }
            }

        } catch (LocalizationException e) {
            throw new VizException("Error Deleting PlotModel, " + pltMdlName
                    + ", for plugin, " + pluginName + "\n" + e.getMessage());
        } catch (InterruptedException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

    }

    public PlotModel getDefaultPlotModel() {
        PlotModel dfltPM = new PlotModel();
        dfltPM.setName(DEFAULT);
        dfltPM.setPlugin(NONE);
        dfltPM.setSvgTemplate(getDefaultSvgTemplate());
        dfltPM.getAllPlotModelElements();
        return dfltPM;
    }

    public String getDefaultSvgTemplate() {
        return DFLT_SVG_TEMPLATE_FILE;
    }

    // querySoundingData used to query ncuair and modelsounding data.
    public static NcSoundingCube querySoundingData(long[] refTime,
            long[] rangeTime, String[] stnIdAry, String plugin, String level,
            Map<String, RequestConstraint> constraintMap,
            HashMap<String, AbstractMetParameter> paramsToPlot) {
        String sndType = "";
        if (plugin.equals("modelsounding")) {
            if (!constraintMap.containsKey("reportType")) {
                System.out
                        .println("requestUpperAirData: missing modelName (reportType) for modelsounding plugin");
                return null;
            }
            String modelName = constraintMap.get("reportType")
                    .getConstraintValue();
            if (modelName.startsWith("NAM") || modelName.startsWith("ETA")) {
                sndType = PfcSndType.NAMSND.toString();
            } else if (modelName.startsWith("GFS")) {
                sndType = PfcSndType.GFSSND.toString();
            } else {
                System.out
                        .println("requestUpperAirData: unreconized modelsounding model name "
                                + modelName);
                return null;
            }
        } else if (plugin.equals("ncuair")) {
            sndType = ObsSndType.NCUAIR.toString();
        } else {
            System.out.println("requestUpperAirData: unreconized plugin name "
                    + plugin);
            return null;
        }

        boolean pwRequired = paramsToPlot
                .containsKey(PrecipitableWaterForEntireSounding.class
                        .getSimpleName());

        return NcSoundingQuery.genericSoundingDataQuery(refTime, rangeTime,
                null, null, null, stnIdAry, sndType,
                NcSoundingLayer.DataType.ALLDATA, true, level, null, true,
                true, pwRequired, true);
    }
}