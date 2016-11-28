package gov.noaa.nws.ncep.viz.resources.manager;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
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
import com.raytheon.uf.viz.core.RecordFactory;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.common.SelectableFrameTimeMatcher;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceExtPointMngr;
import gov.noaa.nws.ncep.viz.resources.manager.AttrSetGroup.RscAndGroupName;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinitionFilters.ResourceDefinitionFilter;

/**
 * `
 * 
 * <pre>
 *  SOFTWARE HISTORY
 *  Date         Ticket#     Engineer    Description
 *  ------------ ----------  ----------- --------------------------
 *  06/09/10       #273      Greg Hull    Created
 *  08/31/10       #303      Greg Hull    Update PGEN AttrSetGroups
 *  09/07/10       #307      Greg Hull    add dfltTimeRange, timelineGenMethod
 *  10/14/10       #227      M. Li        add EnsembleRscCategory
 *  11/27/10       #365      Greg Hull    dynamically generated resource types and sub-types
 *  02/08/11       #365      Greg Hull    dynamically generated local radars.
 *  02/28/11       #408      Greg Hull    Replace Forecast/Observed with a filter
 *  06/07/11       #445      Xilin Guo    Data Manager Performance Improvements
 *  07/15/11       #450      Greg Hull    refactor; support Localization using NcPathManager
 *  07/15/11       #450      Greg Hull    Break resourceDefns.xml into multiple files,
 *                                        Break AttrSetGroup files into multiple files
 *  07/15/11       #450      Greg Hull    rm .prm files and put into ResourceDefn
 *  10/25/11       #467      Greg Hull    recreate AttrSet on edit.
 *  10/26/11                 Xilin Guo    Added dynamicUpdateNcGribInventoryDB to update
 *                                        Ncgrid inventory
 *  11/14/11                 Xilin Guo    Fixed stringIndexOutOfBoundsExpection problem in 
 *                                        NcGrib inventory
 *  11/11/11                 Greg Hull    don't store AttrSetGroupNames in the ResourceDefns
 *  12/01/11      #518       Greg Hull    add getDefaultFrameTimesSelections()
 *  12/08/11                 Shova Gurung Modified dynamicUpdateRadarDataResource() to fix a bug (Local Radar
 *                                        data resource names not showing up correctly after being populated, if database was empty at CAVE startup)
 *  01/09/11      #561       Greg Hull    save the Locator Resource
 *  01/20/12      #606       Greg Hull    use '@' to reference plotModel files.
 *  04/23/12      #606       Greg Hull    use '@' to reference conditional filters for plot data
 *  05/27/12      #606       Greg Hull    get a list of inventoryDefinitions from Edex and save the alias in the ResourceDefn.
 *  06/05/12      #816       Greg Hull    return RD comparator. Change method to get RDs by
 *                                        category to return RD instead of type name.
 *  11/2012       #885       T. Lee       processed unmapped satellite projection                                
 *  11/15/12      #950       Greg Hull    don't fail if an empty list of inventorys is returned
 *  12/16/12      #957       Greg Hull    change getAttrSetsForResource to return AttributeSets list
 *  12/18/12      #957       Greg Hull    patch the bug when deleting a localization file
 *  02/13/13      #972       Greg Hull    ResourceCategory class and NcDisplayType
 *  04/10/13      #864       Greg Hull    read/save new ResourceFilters file
 *  04/24/13      #838       B. Hebbard   Allow getAllResourceParameters to handle NTRANS (paramVal2 no longer assumed numeric/km)
 *  06/05/13      #998       Greg Hull    init subTypesList when creating new RD.
 *  08/2013       #1031      Greg Hull    retry on inventory directory request
 *  12/4/13       #1074      Greg Hull    another hack for the rscName<->parameterValues mapping; check for
 *                                        'native' in satellite subType and set to 0 resolution.
 *  05/15/2014    #1131      Quan Zhou    added rparameters dfltGraphRange, dfltHourSnap
 *  
 *  
 *  09/18/2014    R4508      S. Gurung    Added "TIME_SERIES_DIR" to refdParamDirectories. Modified getAllResourceParameters()
 *                                        to get params from sub-types for high-cadence data.
 *  08/17/2015    R7755      J. Lopez     Only the lowest level localization file is read, moved isEnabled" flag  is moved to Resource Definitions and 
 *                                        removed "Filterable Labels" section from Resource Definition editor. Replaced serialization with JaxB
 *  09/25/2015    R12042     J. Lopez     Fix the bug where Attribute Set Groups and the Attributes are not loaded
 *  10/15/2015    R7190      R. Reynolds  Added support for Mcidas
 *  12/08/2015    #12868     P. Moyer     Added alphabetical sort for Resource Defintion filter combo box
 *  04/05/2016    RM10435    rjpeter      Removed Inventory usage.
 *  04/22/2016    R15718     J. Beck      Made empty attribute sets trigger a Priority.DEBUG message, and changed the descriptive text.
 *                                        On startup empty attribute set messages are no longer sent to the AlertViz pop-up: sent to System Log instead.
 *                                        Buoy error messages have been fixed: they were due to incorrect spelling.
 *  09/26/2016   R20482      K.Bugenhagen Handle saving localization files without 
 *                                        throwing exception due to updated 
 *                                        checksum.  Fixed issue with removal of
 *                                        resource definitions, attribute sets,
 *                                        and attribute set groups. Replaced 
 *                                        deprecated calls to LocalizationFile.save 
 *                                        method.  Cleanup.
 *  11/25/2016    R17956     E. Brown     Modifications to method getAllResourceParameters(). If the rscImplementation is PGEN then get the string subType
 *                                        (e.g. "14-CCFP.21112016.08.xml ebrown OAX” and parse into subTypeParamsArr[]. Use that and subTypeGenParams 
 *                                        (e.g. "activityLabel”, “forecaster”, and “site”) to build paramsMap and return it
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class ResourceDefnsMngr {

    // one instance per user. (Currently only the 'base' used)
    //
    private static Map<String, ResourceDefnsMngr> instanceMap = new HashMap<>();

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ResourceDefnsMngr.class);

    // TODO : fold this into NcPathMngr
    private final NcPathManager pathMngr;

    private HashMap<String, ResourceDefinition> resourceDefnsMap = null;

    // a map from either the rscType or the rscImpl (depending on if
    // AttrSetGroups apply)
    // to a list of available Attribute Set Files returned from Localization.
    private Map<String, Map<String, AttributeSet>> attrSetMap;

    // map from the rscType+groupName to the AttrSetGroup that
    // holds the list of attrSets
    private Map<String, AttrSetGroup> attrSetGroupsMap;

    // for parameters which reference a file under localization.
    // (ie. ColorBars and PlotModels and the plot model conditional filters)
    // NOTE that Conditional Filters dir is under the Plot Models so
    // this is not strictly necessary.
    private static final String[] refdParamDirectories = {
            NcPathConstants.PLOT_MODELS_DIR,
            NcPathConstants.CONDITIONAL_FILTERS_DIR,
            NcPathConstants.COLORBARS_DIR, NcPathConstants.TIME_SERIES_DIR };

    private static Map<String, LocalizationFile> refdParamFilesMap;

    public static final HashMap<String, String> paramInfoForRscDefnParamsMap = new HashMap<>();

    {
        paramInfoForRscDefnParamsMap.put("frameSpan", "Integer");
        paramInfoForRscDefnParamsMap.put("timeMatchMethod", "TimeMatchMethod");
        paramInfoForRscDefnParamsMap.put("dfltNumFrames", "Integer");
        paramInfoForRscDefnParamsMap.put("dfltGraphRange", "Integer");
        paramInfoForRscDefnParamsMap.put("dfltHourSnap", "Integer");
        paramInfoForRscDefnParamsMap.put("dfltTimeRange", "Integer");
        paramInfoForRscDefnParamsMap.put("dfltFrameTimes", "String");
        paramInfoForRscDefnParamsMap.put("timelineGenMethod",
                "TimelineGenMethod");
        paramInfoForRscDefnParamsMap.put("isForecast", "Boolean");
    }

    private static String ATTR_SET_FILE_EXT = ".attr";

    private static ResourceDefinition locatorRscDefn = null;

    private static List<VizException> badRscDefnsList = new ArrayList<>();

    private static List<VizException> rscDefnsWarningsList = new ArrayList<>();

    private static Map<String, TreeMap<LocalizationLevel, ResourceDefinitionFilter>> rscFiltersMap = null;

    private static JAXBManager jaxBResourceDefinition = null;

    private static JAXBManager jaxBResourceFilter = null;

    private static JAXBManager jaxBAttrSetGroup = null;

    public static synchronized ResourceDefnsMngr getInstance()
            throws VizException {
        return getInstance("base");
    }

    public static synchronized ResourceDefnsMngr getInstance(String user)
            throws VizException {
        ResourceDefnsMngr instance = instanceMap.get(user);

        if (instance == null) {
            try {
                instance = new ResourceDefnsMngr(user);
                instance.readResourceDefns();
            } catch (VizException ve) {
                throw ve;
            }
            instanceMap.put(user, instance);
        }

        return instance;
    }

    private ResourceDefnsMngr(String user) {

        // check for an environment variable to override the inventory
        // strategies.

        pathMngr = NcPathManager.getInstance();
        try {
            jaxBResourceDefinition = new JAXBManager(ResourceDefinition.class);
            jaxBResourceFilter = new JAXBManager(
                    ResourceDefinitionFilters.class);
            jaxBAttrSetGroup = new JAXBManager(AttrSetGroup.class);
        } catch (JAXBException e) {

            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

    }

    public List<VizException> getBadResourceDefnsErrors() {
        return badRscDefnsList;
    }

    public List<VizException> getResourceDefnWarnings() {
        return rscDefnsWarningsList;
    }

    public List<ResourceDefinition> getAllResourceDefinitions() {
        return new ArrayList<>(resourceDefnsMap.values());
    }

    // The RDs should already have been read in.
    // fill in the rscFiltersmap from all localization files (all levels that
    // is.)
    // if no file is present, enable everything.
    public void readResourceFilters() throws VizException {
        if (rscFiltersMap == null) {
            rscFiltersMap = new HashMap<>();

            // If a RD is not found in a Filters file then it will be enabled by
            // default.
            Boolean dfltEnableState = true;

            // gets the lowest level Resource filter
            LocalizationFile filtFile = pathMngr.getStaticLocalizationFile(
                    NcPathConstants.RESOURCE_FILTERS);

            // if there is no filter files then display a warning and continue
            if (filtFile == null) {
                dfltEnableState = true;
                rscDefnsWarningsList.add(new VizException("Could not find any "
                        + NcPathConstants.RESOURCE_FILTERS + " files. \n"
                        + "All Rsc Defns will be enabled w/o any filters"));
            }

            LocalizationFile rscFiltersLFile = filtFile;

            rscFiltersLFile
                    .addFileUpdatedObserver(new ILocalizationFileObserver() {
                        @Override
                        public void fileUpdated(FileUpdatedMessage msg) {

                            // if deleting, adding or updating then reread the
                            // file.
                            rscFiltersMap = new HashMap<>();

                            LocalizationFile filtFile = pathMngr
                                    .getStaticLocalizationFile(
                                            NcPathConstants.RESOURCE_FILTERS);

                            try {
                                readRscFilter(filtFile);
                            } catch (LocalizationException e) {
                                rscDefnsWarningsList.add(new VizException(e));
                                statusHandler.handle(Priority.PROBLEM,
                                        ("error serializing"
                                                + filtFile.getFile()
                                                        .getAbsolutePath()
                                                + " : " + e.getMessage()));
                            }

                        }
                    });

            try {

                readRscFilter(rscFiltersLFile);
            } catch (LocalizationException e) {
                rscDefnsWarningsList.add(new VizException(e));

                statusHandler
                        .handle(Priority.PROBLEM,
                                ("error serializing"
                                        + rscFiltersLFile.getFile()
                                                .getAbsolutePath()
                                        + " : " + e.getMessage()));

            }

            // loop thru all the RDs and if there is no entry in the filters
            // map, add one
            // at the same level as the RD.

            for (ResourceDefinition rd : resourceDefnsMap.values()) {
                String rdName = rd.getResourceDefnName();

                TreeMap<LocalizationLevel, ResourceDefinitionFilter> filterTreeMap = rscFiltersMap
                        .get(rdName);

                if (filterTreeMap == null) {
                    filterTreeMap = new TreeMap<>(
                            LocalizationLevel.REVERSE_COMPARATOR);

                    filterTreeMap.put(
                            rd.getLocalizationFile().getContext()
                                    .getLocalizationLevel(),
                            new ResourceDefinitionFilter(rdName,
                                    dfltEnableState, null,
                                    rd.getLocalizationFile().getContext()
                                            .getLocalizationLevel()));
                    rscFiltersMap.put(rdName, filterTreeMap);

                }
            }
        }

        return;
    }

    private void readRscFilter(LocalizationFile locFile)
            throws LocalizationException {

        LocalizationLevel lLvl = locFile.getContext().getLocalizationLevel();
        TreeMap<LocalizationLevel, ResourceDefinitionFilter> filterTreeMap = null;
        synchronized (rscFiltersMap) {

            ResourceDefinitionFilters rscDfnFilters = null;

            rscDfnFilters = locFile.jaxbUnmarshal(
                    ResourceDefinitionFilters.class, jaxBResourceFilter);

            for (ResourceDefinitionFilter rFilt : rscDfnFilters
                    .getResourceDefinitionFiltersList()) {
                String rdName = rFilt.getRscDefnName();
                rFilt.setLocLevel(lLvl);

                filterTreeMap = rscFiltersMap.get(rdName);

                // if there is already an entry in the map, add this one to th
                if (filterTreeMap == null) {
                    // store entries in reverse order since we only want to
                    // access the highest level. Others are there in case we
                    // need to back out.
                    filterTreeMap = new TreeMap<>(
                            LocalizationLevel.REVERSE_COMPARATOR);
                    rscFiltersMap.put(rdName, filterTreeMap);
                }

                filterTreeMap.put(lLvl, rFilt);
            }
        }
    }

    //
    private void readResourceDefns() throws VizException {
        if (resourceDefnsMap != null) {
            return;
        }

        // this was used to maintain the order in the resourceDefnsTable but now
        // that these are separate files, I don't know that this will work. Need
        // to find another way to get these in the right order for the GUI.
        long t0 = System.currentTimeMillis();

        Map<String, LocalizationFile> lFiles = pathMngr.listFiles(
                NcPathConstants.RSC_DEFNS_DIR, new String[] { "xml" }, true,
                true);

        if (lFiles.containsKey(NcPathConstants.RESOURCE_FILTERS)) {
            lFiles.remove(NcPathConstants.RESOURCE_FILTERS);
        }

        // Create the map resource definitions

        resourceDefnsMap = new HashMap<>();

        for (LocalizationFile lFile : lFiles.values()) {
            try {
                readResourceDefn(lFile);

                // TODO : add localization observer to update the Map when a
                // localization file has
                // changed on another cave.
                lFile.addFileUpdatedObserver(new ILocalizationFileObserver() {
                    @Override
                    public void fileUpdated(FileUpdatedMessage message) {

                        statusHandler.handle(Priority.INFO,
                                ("Localization File for RD, "
                                        + message.getFileName()
                                        + " has been updated.\n"
                                        + "To get these changes you will need to restart cave."));
                    }
                });
            } catch (VizException e) {
                out.println("Error creating ResourceDefn from file: "
                        + lFile.getName());
                out.println(" --->" + e.getMessage());
                badRscDefnsList.add(e);
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
        long t1 = System.currentTimeMillis();
        out.println("Time to read " + lFiles.values().size()
                + " Resource Definitions: " + (t1 - t0) + " ms");

        // read in the rscFiltersMap,
        try {
            readResourceFilters();
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        // Note: Temporary solution. Would prefer to fold this into the
        // referencing
        // attr set file if/when changed to xml format.

        t0 = System.currentTimeMillis();

        readRefParamFiles();

        t1 = System.currentTimeMillis();
        out.println("Time to read colorbars: " + (t1 - t0) + " ms");

        // read in the attrSetGroupsMap (this needs the resourceDefnsMap
        // to be set.)

        readAttrSets();
        long t2 = System.currentTimeMillis();

        out.println("Time to read Attr Sets: " + (t2 - t1) + " ms");
    }

    private void readResourceDefn(LocalizationFile lFile) throws VizException {

        File rscDefnFile = lFile.getFile();

        try {
            ResourceDefinition rscDefn = lFile.jaxbUnmarshal(
                    ResourceDefinition.class, jaxBResourceDefinition);

            // TODO : If the definitions are modified and written out, this will
            // drop any invalid resourceDefns.
            // Should we save these write them out anyway? Make them disabled?

            // Validate that the resource implementation is present and that the
            // parameters are defined

            String rscImpl = rscDefn.getRscImplementation();

            if (!ResourceExtPointMngr.getInstance().getAvailResources()
                    .contains(rscImpl)) {

                throw new VizException("The Resource implementation: " + rscImpl
                        + " for " + rscDefn.getResourceDefnName() + " is not "
                        + "specified in a NC-Resource extention point");
            } else {
                rscDefn.validateResourceParameters();

                if (resourceDefnsMap
                        .containsKey(rscDefn.getResourceDefnName())) {
                    throw new VizException("Failed to create Rsc Defn '"
                            + rscDefn.getResourceDefnName() + "' from file: "
                            + rscDefnFile.getAbsolutePath()
                            + " because there is another Rsc Defn with this name.");
                }

                if (rscDefn.isRequestable()) {
                    if (rscDefn.getPluginName() == null) {
                        throw new VizException("Failed to create Rsc Defn "
                                + rscDefn.getResourceDefnName()
                                + ": Requestable Resource is missing required pluginName parameter");
                    }

                    if (!RecordFactory.getInstance().getSupportedPlugins()
                            .contains(rscDefn.getPluginName())) {
                        rscDefnsWarningsList.add(new VizException("Disabling "
                                + rscDefn.getResourceDefnName()
                                + " because plugin, " + rscDefn.getPluginName()
                                + " is not activated."));
                    }
                }

                resourceDefnsMap.put(rscDefn.getResourceDefnName(), rscDefn);

                if (rscImpl.equals("Locator")) {
                    locatorRscDefn = rscDefn;
                }

                // TODO : Change this to set the LocalizationFile or the context
                rscDefn.setLocalizationFile(lFile);

            }
        } catch (Exception e) {
            throw new VizException("Error parsing "
                    + rscDefnFile.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private void readRefParamFiles() {
        refdParamFilesMap = new HashMap<>();

        // This will find all .xml (must be AttrSetGroup xml files) for all
        // of the Resource implementations.

        for (String refDir : refdParamDirectories) {

            Map<String, LocalizationFile> lclFiles = pathMngr.listFiles(refDir,
                    new String[] { ".xml" }, true, false);

            if (lclFiles.isEmpty()) {
                out.println("Error finding Files in " + refDir
                        + " for parameter references?");
                continue;
            }

            for (LocalizationFile lclFile : lclFiles.values()) {

                lclFile.addFileUpdatedObserver(new ILocalizationFileObserver() {
                    @Override
                    public void fileUpdated(FileUpdatedMessage fumsg) {
                        String fName = fumsg.getFileName();
                        LocalizationFile lFile;

                        // if the file had been deleted
                        if (fumsg.getChangeType() == FileChangeType.DELETED) {
                            refdParamFilesMap.remove(fName);
                            // if reverted. (ie DELETED and there is a lower
                            // level file available)

                            lFile = pathMngr.getStaticLocalizationFile(
                                    fumsg.getFileName());
                        } else {
                            // get the ADDED, UPDATED file
                            lFile = pathMngr.getLocalizationFile(
                                    fumsg.getContext(), fumsg.getFileName());
                        }

                        // update the map with the new file
                        if (lFile != null) {
                            refdParamFilesMap.put(fName, lFile);
                        }
                    }
                });

                if (!lclFile.isDirectory()) {
                    // TODO : should we unmarsh here to validate?

                    refdParamFilesMap.put(lclFile.getName(), lclFile);
                }

            }
        }
    }

    // initialize the attrSetMap and the attrSetGroupsMap
    private void readAttrSets() throws VizException {

        attrSetGroupsMap = new HashMap<>();

        // This will find all .xml (must be AttrSetGroup xml files) for all
        // of the Resource implementations.
        Map<String, LocalizationFile> attrSetGrpLclFiles = pathMngr.listFiles(
                NcPathConstants.ATTR_SET_GROUPS_DIR, new String[] { ".xml" },
                true, true);

        if (attrSetGrpLclFiles.isEmpty()) {
            out.println("Error finding AttrSetGroup Files?");
            return;
        }

        // the sub-dirs under the attrSetGroups dir must match a
        // resourceImplClass

        // check that the naming convention is used. If not then there can be a
        // potential problem if the
        // group is edited since it will be given a different localization Name.

        for (LocalizationFile lclFile : attrSetGrpLclFiles.values()) {

            // TODO : add localization observer to update the Map when a
            // localization file has
            // changed on another cave.

            File asgFile = lclFile.getFile();

            if (!asgFile.exists()) {
                out.println("Can't open AttrSetGroup file: "
                        + asgFile.getAbsolutePath());
                continue;
            }
            AttrSetGroup asg = null;
            try {
                asg = lclFile.jaxbUnmarshal(AttrSetGroup.class,
                        jaxBAttrSetGroup);

            } catch (LocalizationException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }

            // add the ASG's in the list to the map. (PGEN is a special case
            // since
            // 1 'default' ASG applies to all PGEN resources.)
            asg.setLocalizationFile(lclFile);

            // if not PGEN then
            // validate that there is a resourceImpl for this attrSetGroup
            if (!asg.getRscAndGroupName().isPGEN()) {

                ResourceDefinition asgRscDefn = getResourceDefinition(
                        asg.getResource());

                if (asgRscDefn == null) {
                    out.println("AttrSetGroup file " + asgFile.getName()
                            + " has a unknown resource:" + asg.getResource());
                    continue;
                }

                String rscImpl = asgRscDefn.getRscImplementation();

                if (!ResourceExtPointMngr.getInstance().getAvailResources()
                        .contains(rscImpl)) {

                    out.println(
                            "Can't  find Resource Implementation (class) for "
                                    + "attrSetGroup : " + rscImpl);

                    out.println(
                            "The Resource implementation should be specified in "
                                    + "a NC-Resource extention point");
                    // don't fail; go ahead and put the attrset in the map
                }

                // check that the file name follows the convention otherwise
                // there could be a
                // problem if the user edits it since the name for the
                // USER-level file will be
                // different and not recognized as the same group.
                String lFileName = NcPathConstants.ATTR_SET_GROUPS_DIR
                        + File.separator + rscImpl + File.separator
                        + asg.getResource() + "-" + asg.getAttrSetGroupName()
                        + ".xml";

                if (!lFileName.equals(lclFile.getName())) {
                    out.println("Warning: Localization file for AttrSetGroup, "
                            + lclFile.getName()
                            + " doesn't follow the naming convention.("
                            + lFileName + ")");
                }
            }

            if (attrSetGroupsMap
                    .containsKey(asg.getRscAndGroupName().toString())) {
                statusHandler.handle(Priority.INFO,
                        (asg.getRscAndGroupName().toString()
                                + " already in the map"));
            }

            attrSetGroupsMap.put(asg.getRscAndGroupName().toString(), asg);

        }

        // Next set the attrSetMap.

        // This is a map from the resource type or rsc impl to a map of the
        // available attribute sets
        attrSetMap = new HashMap<>();

        // first get the attrSets for the AttrSetGroups.
        // In this case the key is the resource Implementation instead of the
        // resource type
        Map<String, LocalizationFile> attrSetLclFiles = pathMngr.listFiles(
                NcPathConstants.ATTR_SET_GROUPS_DIR,
                new String[] { ATTR_SET_FILE_EXT }, true, true);

        if (attrSetLclFiles.isEmpty()) {
            out.println("Error finding AttrSets (for AttrSetGroups) Files?");
            return;
        }

        // the sub-dirs under attrSetGroups must match a resourceImplClass
        for (LocalizationFile asLclFile : attrSetLclFiles.values()) {

            String rscImpl = asLclFile.getFile().getParentFile().getName();

            if (!attrSetMap.containsKey(rscImpl)) {
                attrSetMap.put(rscImpl, new HashMap<String, AttributeSet>());
            }
            try {

                AttributeSet aSet = AttributeSet.createAttributeSet(rscImpl,
                        asLclFile);

                attrSetMap.get(rscImpl).put(aSet.getName(), aSet);
            } catch (VizException e) {
                out.println("Error Creating AttributeSet " + asLclFile.getName()
                        + ": " + e.getMessage());
            }
        }

        // Next get the attrSets for other resources which have attribute sets
        // of their own.
        // In this case the key is the resource type name.
        attrSetLclFiles = pathMngr.listFiles(NcPathConstants.RSC_DEFNS_DIR,
                new String[] { ATTR_SET_FILE_EXT }, true, true);

        if (attrSetLclFiles.isEmpty()) {
            out.println("Error finding AttrSet Files?");
            return;
        }

        for (LocalizationFile asLclFile : attrSetLclFiles.values()) {

            // Some resources may have more organizational directories than
            // others. The resource
            // type is the lowest directory.
            String dirs[] = asLclFile.getName().split(File.separator);
            if ((dirs == null) || (dirs.length < 3)) {
                continue;
            }

            String rscType = dirs[dirs.length - 2];

            if (!attrSetMap.containsKey(rscType)) {
                attrSetMap.put(rscType, new HashMap<String, AttributeSet>());
            }

            try {
                AttributeSet aSet = AttributeSet.createAttributeSet(rscType,
                        asLclFile);

                attrSetMap.get(rscType).put(aSet.getName(), aSet);
            } catch (VizException e) {

                statusHandler
                        .handle(Priority.PROBLEM,
                                ("Error Creating AttributeSet "
                                        + asLclFile.getName() + ": "
                                        + e.getMessage()));
            }
        }

        // validate that the attrSets referenced from the attrSetGroups actually
        // exist (PGEN is a special case)
        for (AttrSetGroup asg : attrSetGroupsMap.values()) {

            String rscType = asg.getResource();
            String attrSetMapKey = "";

            if (asg.getRscAndGroupName().isPGEN()) {
                attrSetMapKey = asg.getResource();
            } else {
                ResourceDefinition rscDefn = getResourceDefinition(rscType);
                if (rscDefn != null) {
                    attrSetMapKey = rscDefn.getRscImplementation();
                }
            }

            for (String asName : new ArrayList<>(asg.getAttrSetNames())) {

                if (!attrSetMap.containsKey(attrSetMapKey)
                        || !attrSetMap.get(attrSetMapKey).containsKey(asName)) {
                    asg.removeAttrSet(asName);

                    /*
                     * If an XML file has an empty <name> element, then we log a
                     * DEBUG message to say so. We will use
                     * "XML <name> element is empty" rather than "doesn't exist"
                     * . Then, we send the message to the System Log, not the
                     * AlertViz Popup. We do this by using Priority.DEBUG
                     * instead of Priority.PROBLEM in statusHandler.
                     */

                    statusHandler.handle(Priority.DEBUG,
                            ("attrSet " + asName + " in attrSetGroup "
                                    + asg.getResource() + File.separator
                                    + asg.getAttrSetGroupName()
                                    + " XML <name> element is empty."));
                }
            }
        }
    }

    public boolean isResourceNameValid(ResourceName rscName) {
        if ((rscName == null) || (rscName.getRscCategory() == null)
                || (rscName.getRscCategory() == ResourceCategory.NullCategory)
                || (rscName.getRscType() == null)
                || rscName.getRscType().isEmpty()
                || (rscName.getRscAttrSetName() == null)
                || rscName.getRscAttrSetName().isEmpty()) {

            return false;
        }

        ResourceDefinition rd = getResourceDefinition(rscName);

        if (rd == null) {
            return false;
        }

        // if there is a generating type then check for a ':'
        if (!rd.getRscTypeGenerator().isEmpty()) {
            if (rscName.getRscType()
                    .indexOf(ResourceName.generatedTypeDelimiter) == -1) {
                return false;
            }
        }
        // if there is no group/subType, make sure there isn't supposed to be
        // one.
        if ((rscName.getRscGroup() == null)
                || rscName.getRscGroup().isEmpty()) {

            if (rd.applyAttrSetGroups()
                    || !rd.getSubTypeGenerator().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public ResourceDefinition getResourceDefinition(ResourceName rscName) {
        return (rscName != null ? getResourceDefinition(rscName.getRscType())
                : null);
    }

    public boolean findResourceDefinition(String rscType) {
        if (resourceDefnsMap.containsKey(rscType)) {
            return true;
        }

        // allow for generated types which will have a ':'
        int indx = rscType.indexOf(":");

        if (indx == -1) {
            return false;
        }

        return resourceDefnsMap.containsKey(rscType.substring(0, indx));
    }

    public ResourceDefinition getResourceDefinition(String rscType) {
        if (resourceDefnsMap.containsKey(rscType)) {
            return resourceDefnsMap.get(rscType);
        }

        // allow for generated types which will have a ':'
        int indx = rscType.indexOf(":");

        if (indx != -1) {
            if (resourceDefnsMap.containsKey(rscType.substring(0, indx))) {
                return resourceDefnsMap.get(rscType.substring(0, indx));
            }
        }

        return null;
    }

    // defined as an array but for now only assume 1 disp type in the list.
    public ResourceCategory[] getResourceCategories(boolean includeDisabled,
            NcDisplayType[] matchingDispTypes) {

        if (matchingDispTypes.length != 1) {
            statusHandler.handle(Priority.PROBLEM,
                    ("getResourceCategories called with more than one display type. Only matching the first one"));
        }

        ArrayList<ResourceCategory> catsList = new ArrayList<>();

        // loop thru all the available categories in order and if a resource
        // defn exists for it then
        for (ResourceCategory rc : ResourceCategory.values()) {

            for (ResourceDefinition rscDefn : resourceDefnsMap.values()) {

                if (rc == rscDefn.getResourceCategory()) {

                    if (includeDisabled || rscDefn.isEnabled()) {

                        if (rscDefn
                                .isDisplayTypeSupported(matchingDispTypes[0])) {

                            if (!catsList.contains(rc)) {
                                catsList.add(rc);
                                break;
                            }
                        }
                    }
                }
            }
        }

        ResourceCategory[] catsArray = catsList
                .toArray(new ResourceCategory[0]);

        return catsArray;

    }

    // map the Full Resource Name to the location of the resource bundle
    // template file
    // for the resource.
    // The rsc name is the RBD Category/Type/Group/AttributeSet. The fcst/obs is
    // not saved so we try both to find a match.
    public File getRscBundleTemplateFile(String rscType) {
        ResourceDefinition rscDefn = getResourceDefinition(rscType);

        if (rscDefn == null) {
            return null;
        }

        // get the name of the NC resource which will implement the resource
        String rscImplName = rscDefn.getRscImplementation();

        if (!rscImplName.endsWith(".xml")) {
            rscImplName = rscImplName + ".xml";
        }

        File rscTemplateFile = pathMngr.getStaticFile(
                NcPathConstants.RSC_TMPLTS_DIR + File.separator + rscImplName);

        return (rscTemplateFile.exists() ? rscTemplateFile : null);
    }

    public boolean doesResourceUseAttrSetGroups(String rscType) {
        ResourceDefinition rscDefn = getResourceDefinition(rscType);

        return (rscDefn == null ? false : rscDefn.applyAttrSetGroups());
    }

    public String getDefaultFrameTimesSelections(ResourceName rscName)
            throws VizException {

        ResourceDefinition rscDefn = getResourceDefinition(
                rscName.getRscType());

        if (rscDefn == null) {
            return null;
        }

        HashMap<String, String> paramsMap = new HashMap<>(
                rscDefn.getResourceParameters(false));

        AttributeSet attrSet = getAttrSet(rscName);

        if (attrSet != null) {
            paramsMap.putAll(attrSet.getAttributes());
        }

        if (paramsMap.containsKey("GDATTIM")) {

            // check syntax
            new SelectableFrameTimeMatcher(paramsMap.get("GDATTIM"));

            return paramsMap.get("GDATTIM");
        } else if ((rscDefn.getDfltFrameTimes() != null)
                && !rscDefn.getDfltFrameTimes().isEmpty()) {

            // check syntax
            new SelectableFrameTimeMatcher(rscDefn.getDfltFrameTimes());

            return rscDefn.getDfltFrameTimes();
        } else {
            return null;
        }
    }

    // get the Attribute Set File for the given resource name. This may either
    // be in the AttrSetGroup directory or directly under the resources config
    // dir.

    // Get all parameters needed to instantiate the bundle template
    // This includes parameters from the ResourceDefinition, attributes and
    // timeMatching/frameCount...

    public HashMap<String, String> getAllResourceParameters(
            ResourceName rscName) throws VizException {

        ResourceDefinition rscDefn = getResourceDefinition(
                rscName.getRscType());

        if (rscDefn == null) {
            return null;
        }

        // first get the parameters defined by the RscDefn and use the default
        // values for any that are not present.
        HashMap<String, String> paramsMap = new HashMap<>(
                rscDefn.getResourceParameters(true));

        // next get the attributes
        AttributeSet attrSet = getAttrSet(rscName);

        if (attrSet != null) {
            paramsMap.putAll(attrSet.getAttributes());
        }

        if (paramsMap.containsKey("GDATTIM")) {
            paramsMap.remove("GDATTIM");
        }

        // and now create the parameters from the rscDefinitions file.
        // (frameInterval, timeMatchMethod)
        if (rscDefn
                .getResourceCategory() != ResourceCategory.OverlayRscCategory) {

            paramsMap.put("frameSpan",
                    Integer.toString(rscDefn.getFrameSpan()));
            paramsMap.put("timeMatchMethod",
                    rscDefn.getTimeMatchMethod().toString());
            paramsMap.put("dfltNumFrames",
                    Integer.toString(rscDefn.getDfltFrameCount()));
            paramsMap.put("dfltTimeRange",
                    Integer.toString(rscDefn.getDfltTimeRange()));
            paramsMap.put("timelineGenMethod",
                    rscDefn.getTimelineGenMethod().toString());

            paramsMap.put("isForecast",
                    (rscDefn.isForecast() ? "true" : "false"));
        }

        // if this is a generated type get the parameter value from the type in
        // the ResourceName

        String typeGenParam = rscDefn.getRscTypeGenerator();

        if (!typeGenParam.isEmpty()) {

            String rscType = rscName.getRscType();
            int indx = rscType.indexOf(":");
            if (indx == -1) {
                throw new VizException(
                        "sanity check: Can't parse generated typ from "
                                + "Resource name :" + rscName.toString());
            }

            String typeName = rscType.substring(indx + 1);

            paramsMap.put(typeGenParam, typeName);
        }

        // If there is a generated sub-type then we will need to set a parameter
        // for this
        // (In this case the name of the parameter in the paramsMap must be the
        // same as the
        // name of the variable in the BundleTemplate.)

        String[] subTypeGenParams = rscDefn.getSubTypeGenParamsList();

        if (rscDefn.getRscImplementation().equals("McidasSatellite")) {

            String subType = rscName.getRscGroup();

            String subTypeParamsArr[] = subType.split("_");

            if (subTypeParamsArr != null) {
                for (int i = 0; i < subTypeParamsArr.length; i++) {

                    if (subTypeGenParams[i].toString()
                            .equalsIgnoreCase("resolution")) {
                        if (subTypeParamsArr[i].toLowerCase()
                                .endsWith(("km"))) {
                            subTypeParamsArr[i] = subTypeParamsArr[i].substring(
                                    0, subTypeParamsArr[i].length() - 2);
                        }
                        if (subTypeParamsArr[i].equals("native")) {
                            subTypeParamsArr[i] = "0";
                        }

                    }

                    paramsMap.put(subTypeGenParams[i], subTypeParamsArr[i]);

                }
            }

        } else if (rscDefn.getRscImplementation().equals("PGEN")) {
            // Build paramsMap based on the implementation being PGEN

            String subType = rscName.getRscGroup();

            String subTypeParamsArr[] = subType.split(" ");

            // If there's a space in one of the subTypes, there's going to be a
            // parsing problem. If the 2 arrays are different lengths it results
            // in an array index out of bounds when it tries to create
            // paramsMap. For example, it happened when
            // forecaster "John Doe" was split into 2
            // different params so one array was size 4 and the other size 5. It
            // assigned "John" as forecaster and Doe as site.
            if (subTypeGenParams.length < subTypeParamsArr.length) {
                String errorMessage = "Looks like there was a space in one of the subType parameters, so one subType got parsed into two. subTypeGenParams array: "
                        + Arrays.toString(subTypeGenParams)
                        + " subTypeParamsArr array: "
                        + Arrays.toString(subTypeParamsArr);
                statusHandler.handle(Priority.PROBLEM, (errorMessage));

                // Fail gracefully and just return a blank paramsMap
                return paramsMap;
            }

            // If there's a blank subType (e.g. site), there might be a problem
            // so give a
            // warning
            if (subTypeParamsArr.length > 1) { // There's always going to be an
                                               // activityLabel, but if there's
                                               // no data it will be blank
                for (int i = 0; i < subTypeParamsArr.length; i++) {
                    if (subTypeParamsArr[i].isEmpty()) {
                        statusHandler.handle(Priority.PROBLEM,
                                ("The subType parameter '" + subTypeGenParams[i]
                                        + "' is blank, so the name is not parsing correctly."));
                    }
                }
            }

            if (subTypeParamsArr != null) {
                for (int i = 0; i < subTypeParamsArr.length; i++) {
                    // Build the return value paramsMap using the two arrays
                    paramsMap.put(subTypeGenParams[i], subTypeParamsArr[i]);
                }
            }
        }

        else if (subTypeGenParams.length == 1)

        {
            paramsMap.put(subTypeGenParams[0], rscName.getRscGroup());
        }

        // TODO : Note this currently only works for Satellite because its
        // the only resource that uses 2 generating params, (the area and
        // resolution)
        // A trailing 'km' which means this parsing code is not generic

        else if (subTypeGenParams.length == 2)

        {

            String subType = rscName.getRscGroup();
            int indx = subType.lastIndexOf('_');
            if (indx != -1) {
                String paramVal1 = subType.substring(0, indx);

                String paramVal2 = subType.endsWith("km")
                        ? subType.substring(indx + 1, subType.length() - 2)
                        : subType.substring(indx + 1, subType.length());
                // TODO : get rid of these hacks and redesign the resoureName
                // <-> parameter value mapping
                if (paramVal2.equals("native")) {
                    paramVal2 = "0";
                }
                // TODO -- Can't make following sanity-check / cleanup anymore
                // because paramVal2
                // for NTRANS is productName, which isn't all numeric.
                // Trouble...?

                paramsMap.put(subTypeGenParams[0], paramVal1);
                paramsMap.put(subTypeGenParams[1], paramVal2);
            }
        }

        // Note this currently only works for High cadence (ghcd) because its
        // the only resource that uses 3 generating params,
        // (the instrument, dataResolUnits and
        // dataResolVal)
        else if (subTypeGenParams.length == 3) {

            String subType = rscName.getRscGroup();

            String subTypeParamsArr[] = subType.split("_");

            if (subTypeParamsArr != null) {
                for (int i = 0; i < subTypeParamsArr.length; i++) {
                    paramsMap.put(subTypeGenParams[i], subTypeParamsArr[i]);
                }
            }
        }

        return paramsMap;
    }

    //
    public static HashMap<String, String> readAttrSetFile(File asFile)
            throws VizException {

        // parse the attrset file to get the attrs to substitude into the
        // Bundle Template file.
        HashMap<String, String> rscAttrMap = new HashMap<>();

        if (asFile.length() == 0) {
            return rscAttrMap;
        }

        try {
            FileReader freader = new FileReader(asFile);

            BufferedReader breader = new BufferedReader(freader);
            String prmStr = breader.readLine().trim();

            while (prmStr != null) {

                if (prmStr.isEmpty() || (prmStr.charAt(0) == '!')) {

                    prmStr = breader.readLine();
                    continue;
                }

                int eq_indx = prmStr.indexOf('=');
                if (eq_indx == -1) {
                    throw new VizException(
                            "The resource prm file, " + asFile.getName()
                                    + ", has a non-comment line with no '='");

                } else {
                    String prmKey = prmStr.substring(0, eq_indx).trim();
                    String prmVal = prmStr.substring(eq_indx + 1).trim();

                    // '@' used to be a reference to a file in the same
                    // directory but with
                    // the localization, and since this is only used for
                    // colorbars,
                    if (!prmVal.isEmpty() && (prmVal.charAt(0) == '@')) {
                        try {
                            String refdLclName = NcPathConstants.NCEP_ROOT
                                    + prmVal.substring(1);

                            if (!refdParamFilesMap.containsKey(refdLclName)) {
                                throw new VizException("Error reading file: "
                                        + asFile.getAbsolutePath()
                                        + " : Unable to find file for parameter reference "
                                        + prmVal + "'.");

                            }
                            File lFile = refdParamFilesMap.get(refdLclName)
                                    .getFile(true);

                            if (!lFile.exists()) {
                                throw new VizException("Error reading file: "
                                        + asFile.getAbsolutePath()
                                        + " : File for parameter reference "
                                        + prmVal + "' doesn't exist.");
                            }

                            FileReader fr = new FileReader(lFile);
                            char[] b = new char[(int) lFile.length()];
                            fr.read(b);
                            fr.close();

                            prmVal = new String(b).trim();
                            // remove the xml header
                            if (prmVal.startsWith("<?xml")) {
                                if (prmVal.indexOf("?>") != -1) {
                                    prmVal = prmVal
                                            .substring(prmVal.indexOf("?>") + 2)
                                            .trim();
                                }
                            }
                        } catch (FileNotFoundException fnf) {
                            throw new VizException(fnf);
                        } catch (IOException ioe) {
                            throw new VizException(ioe);
                        } catch (LocalizationException lex) {
                            throw new VizException(lex);
                        }
                    }

                    rscAttrMap.put(prmKey.trim(), prmVal.trim());

                }
                prmStr = breader.readLine();

            }

            breader.close();

        } catch (FileNotFoundException fnf) {
            throw new VizException(
                    "Can't find referenced file: " + asFile.getAbsolutePath());

        } catch (IOException fnf) {
            throw new VizException(
                    "Can't open referenced file: " + asFile.getAbsolutePath());

        }

        return rscAttrMap;
    }

    // sort with the Obs types first and then the Fcst, and then alphabetically
    public Comparator<ResourceDefinition> getDefaultRscDefnComparator() {

        return new Comparator<ResourceDefinition>() {
            @Override
            public int compare(ResourceDefinition rscDefn1,
                    ResourceDefinition rscDefn2) {

                if (rscDefn1 == null) {
                    return 1;
                }
                if (rscDefn2 == null) {
                    return -1;
                }

                // categories will be the same for the types but we may want to
                // order them differently
                // based on the category
                // for Surf or UAIR, Obs before Fcst
                if ((rscDefn1
                        .getResourceCategory() == ResourceCategory.SurfaceRscCategory)
                        || (rscDefn1
                                .getResourceCategory() == ResourceCategory.UpperAirRscCategory)) {

                    if ((!rscDefn1.isForecast() && rscDefn2.isForecast())
                            || (rscDefn1.isForecast()
                                    && !rscDefn2.isForecast())) {

                        return (rscDefn1.isForecast() ? 1 : -1);
                    }
                }
                // for Radar, Mosaics before Local Radar
                else if (rscDefn1
                        .getResourceCategory() == ResourceCategory.RadarRscCategory) {
                    if (rscDefn1.getRscImplementation().equals("RadarMosaic")) {
                        return -1;
                    } else if (rscDefn2.getRscImplementation()
                            .equals("RadarMosaic")) {
                        return 1;
                    }
                }

                return rscDefn1.getResourceDefnName()
                        .compareToIgnoreCase(rscDefn2.getResourceDefnName());
            }
        };
    }

    public void setResourceEnable(String rscType, Boolean enabled) {
        ResourceDefinitionFilter rdFilt = getResourceDefnFilter(rscType);

        ResourceDefinition rd = getResourceDefinition(rscType);
        if (rd.isEnabled() != enabled) {
            rd.setIsEnabled(enabled);
            setResourceDefnFilters(rdFilt);
        }
    }

    public void setResourceDefnFilters(ResourceDefinitionFilter rdFilt) {
        String rscType = rdFilt.getRscDefnName();

        synchronized (rscFiltersMap) {
            if (!rscFiltersMap.containsKey(rscType)) {
                rscFiltersMap.put(rscType,
                        new TreeMap<LocalizationLevel, ResourceDefinitionFilter>(
                                LocalizationLevel.REVERSE_COMPARATOR));
            }

            // get the highest priority filters. (stored in reverse order)
            TreeMap<LocalizationLevel, ResourceDefinitionFilter> filtMap = rscFiltersMap
                    .get(rscType);
            filtMap.put(LocalizationLevel.USER, rdFilt);
        }
    }

    // Don't return null. If there is no entry then create one
    public ResourceDefinitionFilter getResourceDefnFilter(String rscType) {

        if (!rscFiltersMap.containsKey(rscType)
                || rscFiltersMap.get(rscType).keySet().isEmpty()) {
            // should we add and entry in the map here?
            return new ResourceDefinitionFilter(rscType, false, null,
                    LocalizationLevel.USER);
        }

        // get the highest priority filters. (stored in reverse order)
        TreeMap<LocalizationLevel, ResourceDefinitionFilter> filtMap = rscFiltersMap
                .get(rscType);
        Iterator<LocalizationLevel> iter = filtMap.keySet().iterator();
        LocalizationLevel llvl = iter.next();

        ResourceDefinitionFilter rdFilters = filtMap.get(llvl);
        return rdFilters;
    }

    // loop thru all the rsc defns for this cat and return a list of all
    // filter labels.
    public List<String> getAllFilterLabelsForCategory(ResourceCategory rscCat,
            NcDisplayType dispType) {
        // getResourceDefnsForCategory

        ArrayList<String> filterLabelsList = new ArrayList<>();

        List<ResourceDefinition> rdList;
        try {
            rdList = getResourceDefnsForCategory(rscCat, "", dispType, false,
                    false);

            for (ResourceDefinition rd : rdList) {
                ResourceDefinitionFilter rdFilt = getResourceDefnFilter(
                        rd.getResourceDefnName());

                for (String filtStr : rdFilt.getFilters()) {
                    if (!filterLabelsList.contains(filtStr)) {
                        filterLabelsList.add(filtStr);
                    }
                }
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        // sorts list items into ascending alphabetical order
        Collections.sort(filterLabelsList);

        return filterLabelsList;
    }

    public List<ResourceDefinition> getResourceDefnsForCategory(
            ResourceCategory rscCat) throws VizException {
        return getResourceDefnsForCategory(rscCat, "", null, false, true);
    }

    public List<ResourceDefinition> getResourceDefnsForCategory(
            ResourceCategory rscCat, String filterStr, NcDisplayType dispType,
            Boolean includeGeneratedTypes, Boolean includeDisabledRscDefns)
                    throws VizException {
        List<ResourceDefinition> resourceDefnsList = new ArrayList<>();

        // for other resources, get all of the resources in the given category.
        for (ResourceDefinition rscDefn : resourceDefnsMap.values()) {

            if (!includeDisabledRscDefns && !rscDefn.isEnabled()) {
                continue;
            }

            if ((filterStr == null) || filterStr.isEmpty()
                    || getResourceDefnFilter(rscDefn.getResourceDefnName())
                            .testFilter(filterStr)) {

                if (rscDefn.getResourceCategory().equals(rscCat)) {
                    if (rscDefn.getRscTypeGenerator().isEmpty()) {
                        resourceDefnsList.add(rscDefn);
                    }

                    if (includeGeneratedTypes) {
                        // TODO : could(should?) generate these from
                        // ResourceDefinition
                        for (String genType : rscDefn.getGeneratedTypesList()) {
                            ResourceDefinition genRscDefn = new ResourceDefinition(
                                    rscDefn);
                            genRscDefn.setResourceDefnName(genType);
                            resourceDefnsList.add(genRscDefn);
                        }
                    } else if (!rscDefn.getRscTypeGenerator().isEmpty()) {
                        resourceDefnsList.add(rscDefn);
                    }
                }
            }
        }
        return resourceDefnsList;

    }

    // if these resource type uses attributeSetGroups then return the specified
    // attrSetGroups, if not then look for groups which are 'sub-types' which
    // are
    // given as
    public String[] getResourceSubTypes(String rscType) throws VizException {

        ResourceDefinition rscDefn = getResourceDefinition(rscType);

        if (rscDefn == null) {
            return new String[0];
        }

        // generate the subTypes
        return rscDefn.generatedSubTypesList().toArray(new String[0]);
    }

    // look up using a resource name
    public AttrSetGroup getAttrSetGroupForResource(ResourceName rscName) {
        ResourceDefinition rscDefn = getResourceDefinition(rscName);
        if (rscDefn == null) {
            return null;
        }
        return getAttrSetGroupForResource(new RscAndGroupName(
                rscDefn.getResourceDefnName(), rscName.getRscGroup()));
    }

    // lookup using the rscType and the asg name
    public AttrSetGroup getAttrSetGroupForResource(
            RscAndGroupName attrSetGroupName) {
        return attrSetGroupsMap.get(attrSetGroupName.toString());
    }

    // this is all of the attribute set groups available for a resource
    // (the returned list references the actual AttrSetGroup objects)
    public ArrayList<AttrSetGroup> getAttrSetGroupsForResource(String rscType) {
        // loop thru all the entries in the attrSetGroupsMap and return those
        // that match the rscImpl
        ArrayList<AttrSetGroup> attrSetGroupsList = new ArrayList<>();

        ResourceDefinition rscDefn = getResourceDefinition(rscType);

        if (rscDefn == null) {
            return attrSetGroupsList;
        } else {
            for (AttrSetGroup asg : attrSetGroupsMap.values()) {
                if (asg.getResource().equals(rscDefn.getResourceDefnName())) {
                    attrSetGroupsList.add(asg);
                }
            }
        }

        return attrSetGroupsList;
    }

    public List<String> getAttrSetGroupNamesForResource(String rscType) {

        List<String> asgNameList = new ArrayList<>();

        ArrayList<AttrSetGroup> asgList = getAttrSetGroupsForResource(rscType);

        for (AttrSetGroup asg : asgList) {
            asgNameList.add(asg.getAttrSetGroupName());
        }

        return asgNameList;

    }

    public AttributeSet getAttrSet(ResourceName rscName) { // , String asName )
                                                           // {
        String asName = rscName.getRscAttrSetName();
        ResourceDefinition rscDefn = getResourceDefinition(rscName);
        String asgName = rscName.getRscGroup();

        return getAttrSet(rscDefn, asgName, asName);
    }

    // the asgName is not required but if given a sanity check will be done
    // to ensure that the attrSet is actually in the given attrSetGroup.
    public AttributeSet getAttrSet(ResourceDefinition rscDefn, String asName) {
        return getAttrSet(rscDefn, null, asName);
    }

    public AttributeSet getAttrSet(ResourceDefinition rscDefn, String asgName,
            String asName) {

        String asMapKey = (rscDefn.applyAttrSetGroups()
                ? rscDefn.getRscImplementation()
                : rscDefn.getResourceDefnName());

        Map<String, AttributeSet> attrSetFiles = attrSetMap.get(asMapKey);

        if ((attrSetFiles == null) || !attrSetFiles.containsKey(asName)) {
            return null;
        }

        if (rscDefn.applyAttrSetGroups() && (asgName != null)
                && !asgName.isEmpty()) {

            RscAndGroupName rscGrpName = new RscAndGroupName(
                    rscDefn.getResourceDefnName(), asgName);

            // Should we check that the asName is actually in the asGroup?
            AttrSetGroup asg = getAttrSetGroupForResource(rscGrpName);

            if ((asg == null) || !asg.getAttrSetNames().contains(asName)) {
                out.println("Warning: AttrSet, " + asName + ", is not in group "
                        + asgName);
                return null;
            }
        }

        return attrSetFiles.get(asName);

    }

    // get a list of all the available attribute sets for this resource defn.
    public ArrayList<String> getAvailAttrSets(ResourceDefinition rscDefn) {

        String asMapKey = (rscDefn.applyAttrSetGroups()
                ? rscDefn.getRscImplementation()
                : rscDefn.getResourceDefnName());
        if (!attrSetMap.containsKey(asMapKey)) {
            return new ArrayList<>();
        }

        return new ArrayList<>(attrSetMap.get(asMapKey).keySet());
    }

    public ArrayList<String> getAvailAttrSetsForRscImpl(String rscImpl) {
        ArrayList<String> attrSetList = new ArrayList<>();
        if (attrSetMap.containsKey(rscImpl)) {

            attrSetList.addAll(attrSetMap.get(rscImpl).keySet());
        } else {
            out.println("No available attribute sets for " + rscImpl);
        }
        return attrSetList;
    }

    public List<AttributeSet> getAttrSetsForResource(ResourceName rscName,
            boolean matchGroup) {
        ResourceDefinition rscDefn = getResourceDefinition(
                rscName.getRscType());
        List<AttributeSet> asList = new ArrayList<>();

        if (rscDefn == null) {
            return null;
        }

        if (rscDefn.applyAttrSetGroups()) {
            AttrSetGroup asg = getAttrSetGroupForResource(rscName);
            if (asg != null) {
                for (String asName : asg.getAttrSetNames()) {
                    AttributeSet as = getAttrSet(rscDefn,
                            asg.getAttrSetGroupName(), asName);
                    if (as != null) {
                        asList.add(as);
                    }
                }
            }
        } else {
            // if there is supposed to be a generated group but there is none
            // then
            if (matchGroup && rscName.getRscGroup().isEmpty()
                    && !rscDefn.getSubTypeGenerator().isEmpty()) {
            } else {
                for (String asName : getAvailAttrSets(rscDefn)) {
                    AttributeSet as = getAttrSet(rscDefn, asName);
                    if (as != null) {
                        asList.add(as);
                    }
                }
            }
        }

        return asList;
    }

    public List<ResourceName> getAllSelectableResourceNamesForResourcDefn(
            ResourceDefinition rscDefn) throws VizException {
        List<ResourceName> rscNamesList = new ArrayList<>();

        // build a list of all the possible requestable resourceNames

        ResourceName rscName = new ResourceName();
        ResourceCategory rscCat = rscDefn.getResourceCategory();
        rscName.setRscCategory(rscCat);

        // TODO : need to improve the way this works. Never liked it.
        List<String> rscTypes = new ArrayList<>();

        rscTypes.add(rscName.getRscType());

        for (String rscType : rscTypes) {
            ResourceDefinition rd = getResourceDefinition(rscType);

            rscName.setRscType(rscType);

            List<String> asgList;

            if (rd.applyAttrSetGroups()) {
                asgList = getAttrSetGroupNamesForResource(rscType);
            } else {
                asgList = rd.generatedSubTypesList();
            }

            if (asgList.isEmpty()) {
                rscName.setRscGroup("");

                for (AttributeSet attrSet : getAttrSetsForResource(rscName,
                        false)) {
                    rscName.setRscAttrSetName(attrSet.getName());
                    rscNamesList.add(new ResourceName(rscName));
                }
            } else {
                for (String rscGroup : asgList) {

                    rscName.setRscGroup(rscGroup);

                    for (AttributeSet attrSet : getAttrSetsForResource(rscName,
                            false)) {
                        rscName.setRscAttrSetName(attrSet.getName());
                        rscNamesList.add(new ResourceName(rscName));
                    }
                }
            }
        }

        return rscNamesList;
    }

    public String getResourceImplementation(String rscType) {
        ResourceDefinition rscDefn = getResourceDefinition(rscType);

        return (rscDefn == null ? null : rscDefn.getRscImplementation());
    }

    // return true if the resource was replaced with another from a higher
    // context level.
    public Boolean removeResourceDefn(ResourceDefinition rscDefn)
            throws VizException {
        if (rscDefn == null) {
            throw new VizException("Resource Defn is null?");
        }
        LocalizationFile lFile = rscDefn.getLocalizationFile();

        if (lFile == null) {
            throw new VizException("Resource Defn File is null?");
        }

        // sanity check (button should be disabled if BASE/SITE context)
        if (lFile.getContext()
                .getLocalizationLevel() != LocalizationLevel.USER) {
            throw new VizException(
                    "Can't Remove Base or Site Level Resource Types.");
        }

        try {
            String lFileName = lFile.getName();

            lFile.delete();

            rscDefn.dispose();

            resourceDefnsMap.remove(rscDefn.getResourceDefnName());

            // Give the localization server time to delete the file before
            // getting the BASE, SITE or DESK level file.
            Thread.sleep(500);

            // get the BASE, SITE or DESK level file to replace the deleted one.
            lFile = NcPathManager.getInstance()
                    .getStaticLocalizationFile(lFileName);

            if (lFile != null) {
                readResourceDefn(lFile);
            }

            // remove the entry in the Filters file
            if (rscFiltersMap.containsKey(rscDefn.getResourceDefnName())) {
                // sanity check. Should be there.
                TreeMap<LocalizationLevel, ResourceDefinitionFilter> filtMap = rscFiltersMap
                        .get(rscDefn.getResourceDefnName());
                if (filtMap.containsKey(LocalizationLevel.USER)) {
                    filtMap.remove(LocalizationLevel.USER);
                    saveResourceDefnFiltersFile();
                } else {
                    statusHandler.handle(Priority.INFO,
                            ("sanity check: removing RD "
                                    + rscDefn.getResourceDefnName()
                                    + ". Missing USER level in the filtersTreeMap."));
                }
            } else {
                statusHandler.handle(Priority.INFO,
                        ("sanity check: removing RD "
                                + rscDefn.getResourceDefnName()
                                + ". Missing entry in the filtersMap."));

            }

        } catch (LocalizationException e) {
            throw new VizException(e);
        } catch (InterruptedException e) {
            throw new VizException(e);
        } catch (VizException e) {
            throw e;
        }

        return false;
    }

    // add/replace the given attrSetGroup in the map.
    public void saveAttrSetGroup(AttrSetGroup attrSetGroup)
            throws VizException {
        if (attrSetGroup == null) {
            throw new VizException("null attr set group");
        }

        LocalizationFile asgLclFile = attrSetGroup.getLocalizationFile();
        LocalizationContext asgLclContext = null;
        String lFileName = null;

        // other wise create a new name
        ResourceDefinition rscDefn = getResourceDefinition(
                attrSetGroup.getResource());
        if (rscDefn == null) {
            throw new VizException(
                    "Unknown resource " + attrSetGroup.getResource());
        }

        lFileName = NcPathConstants.ATTR_SET_GROUPS_DIR + File.separator
                + rscDefn.getRscImplementation() + File.separator
                + attrSetGroup.getResource() + "-"
                + attrSetGroup.getAttrSetGroupName() + ".xml";

        // if this is not a USER level file, we need to create another
        // Localization File at the USER Level
        if ((asgLclContext == null) || (asgLclContext
                .getLocalizationLevel() != LocalizationLevel.USER)) {

            asgLclContext = pathMngr.getContext(LocalizationType.CAVE_STATIC,
                    LocalizationLevel.USER);

            asgLclFile = pathMngr.getLocalizationFile(asgLclContext, lFileName);
        }

        try {
            SaveableOutputStream outstream = asgLclFile.openOutputStream();
            jaxBAttrSetGroup.marshalToStream(attrSetGroup, outstream);
            outstream.save();
            outstream.close();
        } catch (LocalizationException | IOException e) {
            statusHandler.error("Error saving attribute set group "
                    + attrSetGroup.getAttrSetGroupName(), e);
        } catch (SerializationException e) {
            statusHandler.error("Error marshalling attribute set group "
                    + attrSetGroup.getAttrSetGroupName(), e);
        }

        attrSetGroup.setLocalizationFile(asgLclFile);

        if (attrSetGroupsMap
                .containsKey(attrSetGroup.getRscAndGroupName().toString())) {

        }

        attrSetGroupsMap.put(attrSetGroup.getRscAndGroupName().toString(),
                attrSetGroup);

    }

    // remove this from the attrSetGroupsMap and remove the name from
    // the resource definition.
    public void removeAttrSetGroup(String asgName, String rscType)
            throws VizException {
        //
        ResourceDefinition rscDefn = getResourceDefinition(rscType);

        if (rscDefn == null) {
            throw new VizException("Unable to find rscDefn " + rscType);
        }

        AttrSetGroup asg = getAttrSetGroupForResource(
                new RscAndGroupName(rscType, asgName));

        if (asg == null) {
            throw new VizException(
                    "Unable to find attrSetGroup " + rscType + "/" + asgName);
        }

        LocalizationFile lFile = asg.getLocalizationFile();

        if (lFile == null) {
            throw new VizException(
                    "Error Removing AttrSetGroup: LFile is null");
        } else if (lFile.getContext()
                .getLocalizationLevel() != LocalizationLevel.USER) {
            throw new VizException(
                    "Can't Remove Base or Site Level Attribute Set Groups.");
        }

        String lFileName = lFile.getName();

        try {
            lFile.delete();
            attrSetGroupsMap.remove(asg.getRscAndGroupName().toString());

            // Give the localization server time to delete the file before
            // getting the BASE, SITE or DESK level file.
            Thread.sleep(500);

            // get the BASE, SITE or DESK level file to replace the deleted one.
            NcPathManager z = NcPathManager.getInstance();
            LocalizationFile x = z.getStaticLocalizationFile(lFileName);
            lFile = x;

            if (lFile != null) {

                File asgFile = lFile.getFile();
                if (!asgFile.exists()) {
                    throw new VizException("Can't open AttrSetGroup file: "
                            + asgFile.getAbsolutePath());
                }
                try {
                    asg = lFile.jaxbUnmarshal(AttrSetGroup.class,
                            jaxBAttrSetGroup);

                } catch (LocalizationException e) {

                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                }

                // add the ASG's in the list to the map. (PGEN is a special case
                // since
                // 1 'default' ASG applies to all PGEN resources.)
                asg.setLocalizationFile(lFile);

                String rscImpl = "";
                ResourceDefinition asgRscDefn = getResourceDefinition(
                        asg.getResource());

                if (asgRscDefn == null) {
                    throw new VizException("AttrSetGroup file "
                            + asgFile.getName() + " has a unknown resource:"
                            + asg.getResource());
                }

                if (!asgRscDefn.isPgenResource()) {
                    rscImpl = asgRscDefn.getRscImplementation();

                    // validate that there is a resourceImpl for this
                    // attrSetGroup
                    //
                    if (!ResourceExtPointMngr.getInstance().getAvailResources()
                            .contains(rscImpl)) {
                        throw new VizException(
                                ("Can't  find Resource Implementation (class) for "
                                        + "attrSetGroup : " + rscImpl
                                        + "\nThe Resource implementation should be specified in "
                                        + "a NC-Resource extention point"));
                        // don't fail; go ahead and put the attrset in the map
                    }
                }

                attrSetGroupsMap.put(asg.getRscAndGroupName().toString(), asg);
            }

        } catch (LocalizationException e) {
            throw new VizException(e.getMessage());
        } catch (InterruptedException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

    }

    public void saveAttrSet(ResourceDefinition rscDefn, String asName,
            String attrsStr) throws VizException {

        String applicableRsc = (rscDefn.applyAttrSetGroups()
                ? rscDefn.getRscImplementation()
                : rscDefn.getResourceDefnName());

        AttributeSet aSet = getAttrSet(rscDefn, asName);
        LocalizationFile lFile = null;
        LocalizationContext userCntxt = NcPathManager.getInstance().getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        boolean newAttrSet = (aSet == null);

        // create a new LocalizationFile. The path of the file is based on
        // the resouce it applies to.
        if (newAttrSet) {
            String attrSetLclName;

            if (rscDefn.applyAttrSetGroups()) {
                attrSetLclName = NcPathConstants.ATTR_SET_GROUPS_DIR
                        + File.separator + rscDefn.getRscImplementation()
                        + File.separator + asName + ".attr";
            } else {
                attrSetLclName = rscDefn.getLocalizationFile().getName();
                attrSetLclName = attrSetLclName.substring(0,
                        attrSetLclName.lastIndexOf(File.separator))
                        + File.separator + asName + ".attr";
            }

            // create the path for localization to the config dir for the
            // resource
            lFile = NcPathManager.getInstance().getLocalizationFile(userCntxt,
                    attrSetLclName);

        } else { // if the aSet exists check that the context level is USER and
                 // change if needed
            lFile = aSet.getFile();
            if (lFile.getContext()
                    .getLocalizationLevel() != LocalizationLevel.USER) {

                lFile = NcPathManager.getInstance()
                        .getLocalizationFile(userCntxt, lFile.getName());

                aSet.setFile(lFile);
            }
        }

        try {
            // Read the localization file again to get the latest checksum
            // from the EDEX server and attempt to save again. Otherwise, save
            // will fail due to checksums not matching.
            LocalizationContext context = pathMngr.getContext(
                    LocalizationType.CAVE_STATIC, LocalizationLevel.USER);
            lFile = pathMngr.getLocalizationFile(context, lFile.getName());
            SaveableOutputStream outstream = lFile.openOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outstream);
            writer.write(attrsStr);
            writer.close();
            outstream.save();
            outstream.close();
        } catch (LocalizationException e) {
            MessageDialog errDlg = new MessageDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                    "Error", null,
                    "Error saving attribute set localization file: "
                            + lFile.getName() + e.getMessage(),
                    MessageDialog.ERROR, new String[] { "OK" }, 0);
            errDlg.open();

        } catch (IOException e1) {
            statusHandler.error("Error saving attribute set " + aSet.getName(),
                    e1);
        }

        // update the attrSetMap and attrSetGroupsMap with a new/renamed
        // attrSet.
        aSet = AttributeSet.createAttributeSet(applicableRsc, lFile);

        if (!attrSetMap.containsKey(applicableRsc)) {
            // sanity check since this shouldn't happen.
            attrSetMap.put(applicableRsc, new HashMap<String, AttributeSet>());
        }

        Map<String, AttributeSet> asFileMap = attrSetMap.get(applicableRsc);

        // add or overwrite the localizationFile for this attrSet

        asFileMap.put(asName, aSet);
    }

    // remove the attr set file and remove the attr set name from the attr set
    // groups that reference it

    public boolean removeAttrSet(ResourceName rscName) throws VizException {

        ResourceDefinition rscDefn = getResourceDefinition(rscName);
        if (rscDefn == null) {
            throw new VizException(
                    "can't find rscDefn for " + rscName.toString());
        }

        String attrSetName = rscName.getRscAttrSetName();

        // delete the file and take it out of the rsc dfn list
        LocalizationFile asLclFile = getAttrSet(rscName).getFile();

        if (asLclFile == null) {
            throw new VizException(
                    "Attr Set File: " + rscName.toString() + " not found");
        }

        // sanity check (button should be disabled if BASE/SITE context)
        if (asLclFile.getContext()
                .getLocalizationLevel() != LocalizationLevel.USER) {
            throw new VizException(
                    "Can't Remove Base or Site Level Attribute Sets.");
        }

        try {
            String lFileName = asLclFile.getName();

            if (attrSetMap.containsKey(rscDefn.getResourceDefnName())) {
                attrSetMap.get(rscDefn.getResourceDefnName())
                        .remove(attrSetName);
            }

            asLclFile.delete();

            // Give the localization server time to delete the file before
            // getting the BASE, SITE or DESK level file.
            Thread.sleep(500);

            // get the BASE, SITE or DESK level file to replace the deleted one.
            asLclFile = NcPathManager.getInstance()
                    .getStaticLocalizationFile(lFileName);

            if (asLclFile != null) {

                String rscImpl = asLclFile.getFile().getParentFile().getName();

                if (!attrSetMap.containsKey(rscImpl)) {
                    attrSetMap.put(rscImpl,
                            new HashMap<String, AttributeSet>());
                }
                try {
                    AttributeSet aSet = AttributeSet.createAttributeSet(rscImpl,
                            asLclFile);

                    attrSetMap.get(rscImpl).put(aSet.getName(), aSet);
                } catch (VizException e) {
                    out.println("Error Creating AttributeSet "
                            + asLclFile.getName() + ": " + e.getMessage());
                }
            }

        } catch (LocalizationException lopex) {
            throw new VizException(lopex.getMessage());
        } catch (InterruptedException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        // if removing an attr set that is part of a group then check for
        // references
        // to it in other groups and edit and save the groups

        if (rscDefn.applyAttrSetGroups()) {
            String rscImpl = rscDefn.getRscImplementation();
            for (AttrSetGroup asg : attrSetGroupsMap.values()) {
                // if this is a BASE or SITE level group then it can't reference
                // a user-defined attrSet.
                if ((asg.getLocalizationFile().getContext()
                        .getLocalizationLevel() == LocalizationLevel.USER)
                        && asg.getResource()
                                .equals(rscDefn.getResourceDefnName())) {

                    if (asg.getAttrSetNames().contains(attrSetName)) {
                        asg.removeAttrSet(attrSetName);

                        saveAttrSetGroup(asg);
                    }
                }
            }

            // loop thru all the resources for this implementation and
            // check if there is a reference to this attrSet.
            for (String rscType : getRscTypesForRscImplementation(rscImpl)) {

                ResourceDefinition rd = getResourceDefinition(rscType);

                if (rd == null) { // sanity check
                    continue;
                }

                // loop thru all of the attrSetGroups for this resource

                for (AttrSetGroup asg : getAttrSetGroupsForResource(rscType)) {

                    if ((asg != null)
                            && asg.getAttrSetNames().contains(attrSetName)) {

                        asg.removeAttrSet(attrSetName);
                        saveAttrSetGroup(asg);
                    }
                }
            }
        }

        return true;
    }

    public ArrayList<String> getRscTypesForRscImplementation(String rscImpl) {
        ArrayList<String> rscTypes = new ArrayList<>();

        for (ResourceDefinition rscDefn : resourceDefnsMap.values()) {
            if (rscDefn.getRscImplementation().equals(rscImpl)) {
                rscTypes.add(rscDefn.getResourceDefnName());
            }
        }
        return rscTypes;
    }

    // get a list of all the USER-level RD filters and add them to the Filters
    // file.
    public void saveResourceDefnFiltersFile() throws VizException {
        ResourceDefinitionFilters rscDfnFilters = new ResourceDefinitionFilters();

        // loop thru the user-level filters
        for (String rdName : rscFiltersMap.keySet()) {

            // get the highest priority filters. (stored in reverse order)
            TreeMap<LocalizationLevel, ResourceDefinitionFilter> filtMap = rscFiltersMap
                    .get(rdName);

            ResourceDefinitionFilter existingRDFilt = null;
            for (LocalizationLevel level : LocalizationLevel.values()) {
                existingRDFilt = filtMap.get(level);
                if (existingRDFilt != null) {
                    break;
                }

            }
            // add existing filter
            rscDfnFilters.getResourceDefinitionFiltersList()
                    .add(existingRDFilt);
            // add user filter
            ResourceDefinitionFilter rdFilt = filtMap
                    .get(LocalizationLevel.USER);
            if (rdFilt != null) {
                rscDfnFilters.getResourceDefinitionFiltersList().add(rdFilt);
            }
        }

        // NOTE : should we 'patch' anything that may be out of order/missing.

        LocalizationContext context = pathMngr.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile rscFiltersLFile = pathMngr.getLocalizationFile(context,
                NcPathConstants.RESOURCE_FILTERS);

        try {
            SaveableOutputStream outstream = rscFiltersLFile.openOutputStream();
            jaxBResourceFilter.marshalToStream(rscDfnFilters, outstream);
            outstream.save();
            outstream.close();
        } catch (LocalizationException | IOException e) {
            statusHandler
                    .error("Error saving resource definitions filters file  "
                            + rscFiltersLFile.getPath(), e);
        } catch (SerializationException e) {
            statusHandler.error("Error marshalling esource definitions filters",
                    e);
        }

    }

    // put the new/edited rscDefn in the map, write it out and initialize the
    // inventory

    public boolean saveResourceDefn(ResourceDefinition rscDefn)
            throws VizException {

        rscDefn.validateResourceParameters();

        boolean createRscDefn = (getResourceDefinition(
                rscDefn.getResourceDefnName()) == null);

        LocalizationFile lFile;
        LocalizationContext userContext = NcPathManager.getInstance()
                .getContext(LocalizationType.CAVE_STATIC,
                        LocalizationLevel.USER);

        // if this is a new rsc the LocalizationFile should not be set but the
        // name should be.
        if (createRscDefn) {
            lFile = NcPathManager.getInstance().getLocalizationFile(userContext,
                    rscDefn.getLocalizationName());
        } else {
            lFile = rscDefn.getLocalizationFile();

            if (lFile.getContext()
                    .getLocalizationLevel() != LocalizationLevel.USER) {
                lFile = NcPathManager.getInstance()
                        .getLocalizationFile(userContext, lFile.getName());
            }
        }

        rscDefn.setLocalizationFile(lFile);

        try {
            // Read the localization file again to get the latest checksum
            // from the EDEX server and attempt to save again. Otherwise, save
            // will fail due to checksums not matching.
            // LocalizationContext context = pathMngr.getContext(
            // LocalizationType.CAVE_STATIC, LocalizationLevel.USER);
            // lFile = pathMngr.getLocalizationFile(context, lFile.getName());
            lFile = pathMngr.getLocalizationFile(userContext, lFile.getName());
            SaveableOutputStream outstream = lFile.openOutputStream();
            jaxBResourceDefinition.marshalToStream(rscDefn, outstream);
            outstream.save();
            outstream.close();

            List<ResourceDefinition> rdList = new ArrayList<>();
            rdList.add(rscDefn);
            resourceDefnsMap.put(rscDefn.getResourceDefnName(), rscDefn);

        } catch (LocalizationException | IOException e) {
            statusHandler.error("Error saving file " + lFile.getPath()
                    + " for resource definition "
                    + rscDefn.getResourceDefnName(), e);
        } catch (SerializationException e) {
            statusHandler.error("Error marshalling resource definition "
                    + rscDefn.getResourceDefnName(), e);
        }

        return true;
    }

    public ResourceDefinition getLocatorResourceDefinition() {
        return locatorRscDefn;
    }
}
