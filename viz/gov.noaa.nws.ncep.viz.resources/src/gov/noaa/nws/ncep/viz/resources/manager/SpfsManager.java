package gov.noaa.nws.ncep.viz.resources.manager;

import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;

/**
 * Common class for constants, utility methods ... *
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 02/24/10       #226      Greg Hull    Break out from NmapCommon
 * 03/04/10       #226      Greg Hull    special case for PGEN
 * 06/22/10       #273      Greg Hull    move some methods to ResourceDefnsMngr
 * 07/28/11       #450      Greg Hull    split out Predefined Areas methods and
 *                                       renamed to SpfsManager. Refactored to 
 *                                       read from NcPathManager
 * 11/14/11       #???      B. Hebbard   Optionally have saveRbdToSpf(...) switch
 *                                       cycle times to LATEST before marshaling,
 *                                       and restore actual times afterwards.
 * 11/15/11                 ghull        add resolveLatestCycleTimes
 * 01/01/12                 J. Zeng      Add listener for multiple CAVEs to get SPFs info from each other
 * 04/29/12       #606      Greg Hull    now called from the PerspectiveManager. Better timing/count output. 
 * 06/13/12       #817      Greg Hull    add resolveDominantResource() 
 * 06/18/12       #713      Greg Hull    in addRbd() overwrite existing rbds if they exist.
 * 06/29/12       #568      Greg Hull    add deleteRbd()
 * 07/22/12       #568      Greg Hull    return Rbds and rbdNames sorted by seq num.
 * 02/10/13       #972      Greg Hull    changed to work with AbstractRbds
 * 05/19/13       #1001     Greg Hull    getRbdsFromSpf(), trap RBD errors
 * 03/06/14       ?         B. Yin       Replaced SerializationUtil with JAXBManager.
 * 03/25/15                 B. Hebbard   Reformat only, to code standards
 * 03/25/15       R4983     B. Hebbard   In saveRbdToSpf(..), if saving cycle times as
 *                                       LATEST, apply to dominant resource name too;
 *                                       also fix save to resourceNameToCycleTimeMap to use
 *                                       as key resourceName after modification.
 * 12/09/15        4834     njensen      Updated for LocalizationFile.delete() signature
 * 
 * 06/23/15       R6821     J. Bernier   Added overloaded getRbdsFromSpf method
 *                                       to allow passing rbd name for blender.
 * 03/01/16       R6821     K.Bugenhagen Cleanup: mostly changed sysout statements to IUFStatusHandler
 * 06/28/2016     R17025    S.Russell    Removed else clause for updates in 
 *                                       fileUpdated() method.
 * 08/30/2016     R17027    mkean        Added a conditional to detect repeated calls to removeEntryByFile(). 
 *                                       If so then return instead of throwing AlertViz (current behavior).
 * 09/23/2016     R21176    J.Huber      Modify cycle time in ResourceName of grouped gridded resources
 *                                       to LATEST if latest is selected on save.
 * </pre>
 * 
 * @author
 * @version 1
 */
public class SpfsManager implements ILocalizationFileObserver {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SpfsManager.class);

    private static SpfsManager instance = null;

    // save LocalizationFile for comparison on subsequent removals
    private static LocalizationFile lastLocalizationFileRemoved = null;

    // Might not want to rely on these counts for anything critical.
    private static long rbdCount = 0;

    // TODO : do we want to store the NcMapRBD or just the LocalizationFile
    // (Store the NcMapRBD but don't give it out unless we are making a copy
    // of it (by unmarshalling the file in LFile.)
    private Map<String, Map<String, Map<String, AbstractRBD<?>>>> spfsMap = null;

    public static SpfsManager getInstance() {
        if (instance == null) {
            instance = new SpfsManager();
        }

        return instance;
    }

    private SpfsManager() {
        findAvailSpfs();
    }

    private void findAvailSpfs() {

        spfsMap = new TreeMap<>();

        // This will find all the directories for SPFs and SPF groups as well
        // as the RBDs
        Map<String, LocalizationFile> spfsLFileMap = NcPathManager
                .getInstance().listFiles(NcPathConstants.SPFS_DIR, null, true,
                        false);

        // Loop thru the LocalizationFiles and save to
        for (LocalizationFile lFile : spfsLFileMap.values()) {

            lFile.addFileUpdatedObserver(this);

            String[] dirs = lFile.getPath().split(File.separator);

            // If this is a SPF Group or SPF directory
            // NOTE: Wait to add the Group/SPF so that only directories
            // that have RBDs in them get added to the map.
            if (lFile.getFile().isDirectory()) {
                // if an SPF Group
                if (dirs.length > 4) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Found dir under SPFs with more than 3 paths:"
                                    + lFile.getPath());
                }
                // if this is an RBD, check for .xml and in an SPF directory
            } else {
                if (!lFile.getPath().endsWith(".xml")) {
                    statusHandler.handle(
                            Priority.PROBLEM,
                            "Non-xmlfile found under SPFs dir:"
                                    + lFile.getPath());

                } else if (dirs.length != 5) {
                    statusHandler.handle(
                            Priority.PROBLEM,
                            "xml file found in non-SPF directory? "
                                    + lFile.getPath());
                } else {
                    try {
                        AbstractRBD<?> rbd = NcMapRBD.getRbd(lFile.getFile());

                        rbd.setLocalizationFile(lFile);
                        addRbd(dirs[2], dirs[3], rbd);
                    } catch (VizException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Error unmarshalling rbd: " + lFile.getPath()
                                        + "\n" + e.getMessage());
                    }
                }
            }
        }
    }

    // It may be possible to have groups in 2 different contexts. I don't
    // think we will need to store the context for each group though.
    public void addSpfGroup(String grpName) {
        synchronized (spfsMap) {
            if (!spfsMap.containsKey(grpName)) {
                spfsMap.put(grpName,
                        new TreeMap<String, Map<String, AbstractRBD<?>>>());
            }
        }
    }

    public void addSpf(String grpName, String spfName) {

        synchronized (spfsMap) {
            addSpfGroup(grpName);

            Map<String, Map<String, AbstractRBD<?>>> grpMap = spfsMap
                    .get(grpName);

            if (!grpMap.containsKey(spfName)) {
                grpMap.put(spfName, new TreeMap<String, AbstractRBD<?>>());
            }
        }
    }

    public void addRbd(String grpName, String spfName, AbstractRBD<?> rbd) {
        synchronized (spfsMap) {
            addSpfGroup(grpName);
            addSpf(grpName, spfName);

            Map<String, Map<String, AbstractRBD<?>>> gMap = spfsMap
                    .get(grpName);
            Map<String, AbstractRBD<?>> sMap = gMap.get(spfName);

            if (!sMap.containsKey(rbd.getRbdName())) {
                rbdCount++;
            }
            sMap.put(rbd.getRbdName(), rbd);
        }
    }

    // Return an array of all the sub directories in the spf groups dir.
    public String[] getAvailSPFGroups() {
        String[] avail_groups = spfsMap.keySet().toArray(new String[0]);
        Arrays.sort(avail_groups);
        return avail_groups;
    }

    // Return an array of all the spf (.xml) files in the given spf group dir.
    public String[] getSpfNamesForGroup(String grpName) {

        Map<String, Map<String, AbstractRBD<?>>> grpMap = spfsMap.get(grpName);

        if (grpMap == null) {
            return new String[] {};
        }

        String[] spfNames = grpMap.keySet().toArray(new String[0]);

        Arrays.sort(spfNames);

        return spfNames;
    }

    public String[] getRbdNamesForSPF(String grpName, String spfName) {
        Map<String, Map<String, AbstractRBD<?>>> grpMap = spfsMap.get(grpName);

        if (grpMap == null) {
            return new String[] {};
        }
        Map<String, AbstractRBD<?>> sMap = grpMap.get(spfName);

        if (sMap == null) {
            return new String[] {};
        }

        // Sort according to the sequence number in the RBD.
        AbstractRBD<?>[] rbds = sMap.values().toArray(new AbstractRBD<?>[0]);
        Arrays.sort(rbds);

        String rbdNames[] = new String[rbds.length];
        for (int i = 0; i < rbds.length; i++) {
            rbdNames[i] = rbds[i].getRbdName();
        }
        return rbdNames;
    }

    // Return a copy of the Rbds in the given SPF.
    public List<AbstractRBD<?>> getRbdsFromSpf(String grpName, String spfName,
            boolean resolveLatestCycleTimes) throws VizException {
        return getRbdsFromSpf(grpName, spfName, null, resolveLatestCycleTimes);
    }

    // Return a copy of the Rbds in the given SPF.
    public List<AbstractRBD<?>> getRbdsFromSpf(String grpName, String spfName,
            String rbdName, boolean resolveLatestCycleTimes)
            throws VizException {

        if (grpName == null || spfName == null) {
            throw new VizException("spf group or spf name is null");
        }

        Map<String, Map<String, AbstractRBD<?>>> grpMap = spfsMap.get(grpName);
        if (grpMap == null) {
            throw new VizException("SPF Group " + grpName + " doesn't exist.");
        }
        Map<String, AbstractRBD<?>> sMap = grpMap.get(spfName);

        if (sMap == null) {
            throw new VizException("SPF " + spfName + " doesn't exist.");
        }

        List<AbstractRBD<?>> clonedRbsList = new ArrayList<>();

        for (AbstractRBD<?> rbd : sMap.values()) {

            try {
                AbstractRBD<?> clonedRBD = AbstractRBD.clone(rbd);
                clonedRBD.resolveDominantResource();

                if (resolveLatestCycleTimes) {
                    clonedRBD.resolveLatestCycleTimes();

                    // If unable to resolve the cycle time then leave as
                    // latest and resources will have to gracefully handle
                    // NoData.
                }
                clonedRbsList.add(clonedRBD);
            } catch (VizException ve) {
                // Print a msg but still return other good rbds in the spf
                statusHandler.handle(Priority.PROBLEM, "Error cloning RBD: "
                        + rbd.rbdName + ".\n" + ve.getMessage());
            }
        }

        AbstractRBD<?> rbdsList[] = new AbstractRBD<?>[clonedRbsList.size()];
        rbdsList = clonedRbsList.toArray(new AbstractRBD<?>[0]);
        Arrays.sort(rbdsList);

        // Make a copy to allow the user to modify the list.
        return new ArrayList<>(Arrays.asList(rbdsList));
    }

    // TODO : decide what is/isn't a valid rbd name ...
    public boolean isValidRbdName(String rbdName) {
        if (rbdName != null && !rbdName.isEmpty()) {
            if (!rbdName.contains(File.separator)) {
                // more invalid checks....

                return true;
            }
        }
        return false;
    }

    // Create a new SPF with the given rbds. The rbdsList should be in order and
    // the SPF should not exist yet.
    public void createSpf(String grpName, String spfName,
            List<AbstractRBD<?>> rbdsList, Boolean saveRefTime,
            Boolean saveCycleTime) throws VizException {

        // make sure the spf doesn't exist.
        if (rbdsList.isEmpty() || grpName == null || grpName.isEmpty()
                || spfName == null || spfName.isEmpty()) {
            throw new VizException(
                    "Error creating SPF. Null spf name or no rbds are selected.");
        }

        Map<String, Map<String, AbstractRBD<?>>> grpMap = spfsMap.get(grpName);

        if (grpMap != null) {
            Map<String, AbstractRBD<?>> sMap = grpMap.get(spfName);

            if (sMap != null) {
                throw new VizException("The SPF " + grpName + File.separator
                        + spfName + " already exists.");
            }
        }

        for (AbstractRBD<?> rbd : rbdsList) {
            saveRbdToSpf(grpName, spfName, rbd, saveRefTime, saveCycleTime);
        }

        // The updating of the SpfManager is done in the Notification thread to
        // give it time to update before refetching the rbds again.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
        }
    }

    // The SPF should already exist. This will delete any existing Rbds that
    // aren't in the given list.
    public void saveSpf(String grpName, String spfName,
            List<AbstractRBD<?>> rbdsList, Boolean saveRefTime,
            Boolean saveCycleTime) throws VizException {

        if (rbdsList.isEmpty() || grpName == null || grpName.isEmpty()
                || spfName == null || spfName.isEmpty()) {
            throw new VizException(
                    "Error saving SPF. Null spf name or no rbds are selected.");
        }

        // Get the current Rbds so we can delete those that have been removed.
        List<AbstractRBD<?>> existingRbds = getRbdsFromSpf(grpName, spfName,
                false);

        boolean deleteRbdFlag = true;

        for (AbstractRBD<?> existingRbd : existingRbds) {
            deleteRbdFlag = true;

            for (AbstractRBD<?> rbd : rbdsList) {
                if (existingRbd.getRbdName().equals(rbd.getRbdName())) {
                    deleteRbdFlag = false;
                    break;
                }
            }
            if (deleteRbdFlag) {
                deleteRbd(existingRbd);
            }
        }

        // TODO : it would be nice if we could determine if the spf has
        // changed so that we don't have to override BASE/SITE level rbds that
        // haven't changed.
        for (AbstractRBD<?> rbd : rbdsList) {
            saveRbdToSpf(grpName, spfName, rbd, saveRefTime, saveCycleTime);
        }

        // The updating of the SpfManager is done in the Notification thread to
        // give it time to update before refetching the rbds again.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
        }
    }

    public void saveRbdToSpf(String grpName, String spfName,
            AbstractRBD<?> rbd, boolean saveRefTime, boolean saveCycleTime)
            throws VizException {

        // The localization code will handle creating the group and spf
        // directories if needed
        String rbdLclName = NcPathConstants.SPFS_DIR + File.separator + grpName
                + File.separator + spfName + File.separator + rbd.getRbdName()
                + ".xml";
        LocalizationContext usrCntxt = NcPathManager.getInstance().getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile lFile = NcPathManager.getInstance()
                .getLocalizationFile(usrCntxt, rbdLclName);

        if (lFile == null || lFile.getPath() == null) {
            throw new VizException("Error creating localization file for rbd: "
                    + rbdLclName);
        }
        File rbdFile = lFile.getFile();

        // If the user elects not to save out the refTime then don't marshal it
        // out.
        DataTime savedRefTime = rbd.getTimeMatcher().getRefTime();

        if (!saveRefTime) {
            rbd.getTimeMatcher().setCurrentRefTime();
        }

        // If user requested saving cycle times as LATEST (as opposed to
        // Constant), then for each requestable forecast resource in the RBD,
        // make it so -- before marshaling out to XML. But first, save the
        // 'real' cycle time in a map, for restoration later (below).
        //
        // TODO : do we still have to do this now that we can clone the RBDs?
        //
        Map<String, DataTime> resourceNameToCycleTimeMap = new HashMap<>();

        if (!saveCycleTime) {

            // For each display pane
            for (AbstractRenderableDisplay display : rbd.getDisplays()) {

                // For each resource in the display
                for (ResourcePair rp : display.getDescriptor()
                        .getResourceList()) {

                    AbstractResourceData ard = rp.getResourceData();
                    List<AbstractNatlCntrsRequestableResourceData> allResourcesList = new ArrayList<>();
                    // Add to list if it is ungrouped. If it comes across a
                    // group the group needs to be pulled apart and processed.
                    if (ard instanceof AbstractNatlCntrsRequestableResourceData) {
                        allResourcesList
                                .add((AbstractNatlCntrsRequestableResourceData) ard);
                    } else if (ard instanceof GroupResourceData) {
                        GroupResourceData grpResourceData = (GroupResourceData) ard;
                        for (ResourcePair singlePair : grpResourceData
                                .getResourceList()) {
                            AbstractResourceData singleAbstractData = singlePair
                                    .getResourceData();
                            if (singleAbstractData instanceof AbstractNatlCntrsRequestableResourceData) {
                                allResourcesList
                                        .add((AbstractNatlCntrsRequestableResourceData) singleAbstractData);
                            }
                        }
                    }
                    // For each resource in the list, check to see if it is a
                    // forecast resource set the cycle time to "LATEST" and
                    // store current cycle time.
                    for (AbstractNatlCntrsRequestableResourceData singleAbstractData : allResourcesList) {
                        AbstractNatlCntrsRequestableResourceData singleRequestableData = singleAbstractData;
                        ResourceName singleResourceName = singleRequestableData
                                .getResourceName();
                        if (singleResourceName.isForecastResource()) {
                            DataTime savedCycleTime = singleResourceName
                                    .getCycleTime();
                            singleResourceName.setCycleTimeLatest();
                            resourceNameToCycleTimeMap.put(
                                    singleResourceName.toString(),
                                    savedCycleTime);
                        }
                    }
                }
                // Modify dominant resource name in time matcher too
                ResourceName dominantResourceName = rbd.getTimeMatcher()
                        .getDominantResourceName();
                if (dominantResourceName.isForecastResource()) {
                    DataTime savedCycleTime = dominantResourceName
                            .getCycleTime();
                    dominantResourceName.setCycleTimeLatest();
                    resourceNameToCycleTimeMap.put(
                            dominantResourceName.toString(), savedCycleTime);
                }
            }
        }
        // Marshal out the rbd to the file on disk, set the localizationFile
        // for the rbd, save the localization file and update the spfsMap
        // with the rbd and possible new group and spf.

        try {
            AbstractRBD.getJaxbManager().marshalToXmlFile(rbd,
                    rbdFile.getAbsolutePath());

            rbd.setLocalizationFile(lFile);

            lFile.save();

            addRbd(grpName, spfName, rbd);

            lFile.addFileUpdatedObserver(this);

        } catch (LocalizationException e) {
            throw new VizException(e);
        } catch (JAXBException e) {
            throw new VizException(e);
        } catch (SerializationException e) {
            throw new VizException(e);
        } finally {
            if (!saveRefTime) {
                rbd.getTimeMatcher().setRefTime(savedRefTime);
            }

            // If we saved cycle times as LATEST (as opposed to Constant),
            // then restore the 'real' cycle times in each requestable
            // forecast resource in the RBD. (See above.)
            if (!saveCycleTime) {

                // For each display pane.
                for (AbstractRenderableDisplay display : rbd.getDisplays()) {

                    // For each resource.
                    for (ResourcePair rp : display.getDescriptor()
                            .getResourceList()) {

                        AbstractResourceData ard = rp.getResourceData();
                        List<AbstractNatlCntrsRequestableResourceData> allResourcesRestoreList = new ArrayList<>();

                        // If the resource is ungrouped add it to list. If
                        // it comes across a group it needs to re-process
                        // the group to get each resource and then add it to
                        // the list.
                        if (ard instanceof AbstractNatlCntrsRequestableResourceData) {
                            allResourcesRestoreList
                                    .add((AbstractNatlCntrsRequestableResourceData) ard);
                        } else if (ard instanceof GroupResourceData) {
                            GroupResourceData grpResourceData = (GroupResourceData) ard;
                            for (ResourcePair singlePair : grpResourceData
                                    .getResourceList()) {
                                AbstractResourceData singleAbstractData = singlePair
                                        .getResourceData();
                                if (singleAbstractData instanceof AbstractNatlCntrsRequestableResourceData) {
                                    allResourcesRestoreList
                                            .add((AbstractNatlCntrsRequestableResourceData) singleAbstractData);
                                }
                            }
                        }

                        // For each resource in the list restore the
                        // current latest cycle time.
                        for (AbstractNatlCntrsRequestableResourceData singleAbstractData : allResourcesRestoreList) {
                            AbstractNatlCntrsRequestableResourceData singleRequestableData = singleAbstractData;
                            ResourceName singleResourceName = singleRequestableData
                                    .getResourceName();
                            if (singleResourceName.isForecastResource()
                                    && singleResourceName.isLatestCycleTime()
                                    && resourceNameToCycleTimeMap
                                            .containsKey(singleResourceName
                                                    .toString())) {
                                singleResourceName
                                        .setCycleTime(resourceNameToCycleTimeMap
                                                .get(singleResourceName
                                                        .toString()));
                            }
                        }
                    }
                }
            }
            // Restore dominant resource name cycle time in time matcher too
            ResourceName dominantResourceName = rbd.getTimeMatcher()
                    .getDominantResourceName();
            if (dominantResourceName.isForecastResource()
                    && dominantResourceName.isLatestCycleTime() // better be
                    && resourceNameToCycleTimeMap
                            .containsKey(dominantResourceName.toString())) {
                dominantResourceName.setCycleTime(resourceNameToCycleTimeMap
                        .get(dominantResourceName.toString()));
            }
        }
    }

    public void deleteSpfGroup(String delGroup) throws VizException {
        LocalizationFile groupLocDir = NcPathManager.getInstance()
                .getStaticLocalizationFile(
                        NcPathConstants.SPFS_DIR + File.separator + delGroup);

        if (groupLocDir == null) {
            throw new VizException("Could not find Localization File for:\n"
                    + NcPathConstants.SPFS_DIR + File.separator + delGroup);
        } else if (groupLocDir.getContext().getLocalizationLevel() != LocalizationContext.LocalizationLevel.USER) {
            throw new VizException("Can not delete a non-user defined SPF.");
        } else if (getSpfNamesForGroup(delGroup).length > 0) {
            throw new VizException("Can't delete non-empty SPF:\n" + delGroup);
        } else if (!groupLocDir.isDirectory()) {
            throw new VizException(
                    "Localization File for SPF is not a directory:\n"
                            + delGroup);
        }

        // Note that this will trigger the fileUpdated which will remove the
        // group from the map.
        try {
            groupLocDir.delete();
        } catch (LocalizationException e) {
            throw new VizException(e);
        }
    }

    // Use this to check to see if the given User-level SPF has a superceding
    // SPF.
    public LocalizationContext getSpfContext(String spfGroup, String spfName)
            throws VizException {
        LocalizationFile spfLocDir = NcPathManager.getInstance()
                .getStaticLocalizationFile(
                        NcPathConstants.SPFS_DIR + File.separator + spfGroup
                                + File.separator + spfName);
        return (spfLocDir == null ? null : spfLocDir.getContext());
    }

    // Ideally we could just check that the SPF dir is in User-Level Context.
    // The SPF Manager GUI should enforce this but it may be possible for the
    // Localization perspective to be used to create USER level SPFs with SITE
    // or DESK level RBDs. This method will return false if the SPF dir or any
    // of its RBDs have a non USER level file.

    public Boolean isUserLevelSpf(String spfGroup, String spfName) {
        try {

            for (AbstractRBD<?> rbd : getRbdsFromSpf(spfGroup, spfName, false)) {

                // TODO : should we look for the File if the LocalizationFile is
                // not set? Just assume that the RBD hasn't been created yet....
                if (rbd.getLocalizationFile() != null) {

                    if (rbd.getLocalizationFile().getContext()
                            .getLocalizationLevel() != LocalizationLevel.USER) {
                        return false;
                    }
                }
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error getting Spf Localization Dir");
            // Assume it hasn't been created yet.
        }

        return true;
    }

    // This assumes that all the RBDs and the SPF all are in the USER's
    // Localization. This will delete all the user-level RBDs in the SPF as well
    // as the SPF. If there are SITE, or Base level files in the SPF then we
    // will 'revert' back to.

    public void deleteSpf(String spfGroup, String delSpfName)
            throws VizException {

        LocalizationFile spfLocDir = NcPathManager.getInstance()
                .getStaticLocalizationFile(
                        NcPathConstants.SPFS_DIR + File.separator + spfGroup
                                + File.separator + delSpfName);

        if (spfLocDir == null) {
            throw new VizException("Could not find Localization File for:\n"
                    + NcPathConstants.SPFS_DIR + File.separator + spfGroup
                    + File.separator + delSpfName);
        } else if (!isUserLevelSpf(spfGroup, delSpfName)) {
            throw new VizException(
                    "Either the SPF Localization Dir or one of the RBD "
                            + "Localization Files is not in the User-Level Context.");
        } else if (!spfLocDir.isDirectory()) { // sanity check
            throw new VizException(
                    "Localization File for SPF is not a directory:\n"
                            + spfGroup + File.separator + delSpfName);
        }

        // Get a list of the RBDs in this spf and delete them.
        List<AbstractRBD<?>> existingRbds = getRbdsFromSpf(spfGroup,
                delSpfName, false);

        for (AbstractRBD<?> delRbd : existingRbds) {
            deleteRbd(delRbd);
        }

        // Note that this will trigger the fileUpdated which will remove the spf
        // from the map
        try {
            spfLocDir.delete();

            // The updating of the SpfManager is done in the Notification thread
            // to give it time to update before refetching the rbds again.
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
            }

        } catch (LocalizationException e) {
            throw new VizException(e);
        }
    }

    public void deleteRbd(AbstractRBD<?> rbd) throws VizException {
        LocalizationFile lFile = rbd.getLocalizationFile();

        if (lFile == null) {
            throw new VizException("Rbd, " + rbd.getRbdName()
                    + " has no Localization File to delete.");
        } else if (lFile.getContext().getLocalizationLevel() != LocalizationLevel.USER) {
            throw new VizException("Can not delete a non-USER level RBD: "
                    + rbd.getRbdName());
        }

        // This will trigger the fileUpdated method which will
        // remove the Rbd from the map.
        try {
            lFile.delete();
        } catch (LocalizationException e) {
            throw new VizException(e);
        }

        rbd.setLocalizationFile(null);
    }

    /**
     * Removes a given RBD/SPF from localization (static Maps)
     * 
     * @param localizationFile
     */
    public void removeEntryByFile(LocalizationFile localizationFile) {

        // was the given localizationFile removed in previous call?
        if (localizationFile != null
                && localizationFile.equals(lastLocalizationFileRemoved)) {

            statusHandler.handle(
                    Priority.INFO,
                    "LocalizationFile was already removed: "
                            + localizationFile.getPath());
            return;
        }
        lastLocalizationFileRemoved = localizationFile;

        Map<LocalizationLevel, LocalizationFile> superFiles = NcPathManager
                .getInstance().getTieredLocalizationFile(
                        localizationFile.getPath());
        superFiles.remove(LocalizationLevel.USER);

        if (!superFiles.isEmpty()) {
            statusHandler.handle(Priority.PROBLEM, "Removing File "
                    + localizationFile.getPath()
                    + " that has a lower level File. Need to Revert.");
        }

        String spfPaths[] = localizationFile.getPath().split(File.separator);
        int i = NcPathConstants.SPFS_DIR.split(File.pathSeparator).length;
        int pathCount = spfPaths.length - i - 1;

        String spfGroup = spfPaths[i + 1];

        Map<String, Map<String, AbstractRBD<?>>> grpMap = spfsMap.get(spfGroup);

        if (grpMap == null) {
            statusHandler.handle(Priority.PROBLEM, "Could not find Group "
                    + spfGroup + " for RBD " + localizationFile.getPath());
            return;
        }

        // If this is an Spf Group then remove it and return
        if (pathCount == 1) {

            // sanity check that the group is empty
            if (spfsMap.containsKey(spfGroup)) {
                if (!spfsMap.get(spfGroup).isEmpty()) {
                    statusHandler.handle(Priority.PROBLEM,
                            "deleting non-empty SPF Group: " + spfGroup);
                }
            }

            spfsMap.remove(spfGroup);

            return;
        }

        String spfName = spfPaths[i + 2];

        Map<String, AbstractRBD<?>> sMap = grpMap.get(spfName);

        if (sMap == null) {
            statusHandler.handle(Priority.PROBLEM, "Could not find SPF "
                    + spfName + " for RBD " + localizationFile.getPath());
            return;
        }

        // If this is an Spf then remove it and return
        if (pathCount == 2) {
            if (grpMap.containsKey(spfName)) {
                if (!grpMap.get(spfName).isEmpty()) {
                    statusHandler.handle(Priority.PROBLEM,
                            "deleting non-empty SPF : " + spfName);
                }
            }

            grpMap.remove(spfName);

            if (grpMap.isEmpty()) {
                spfsMap.remove(spfGroup);
            }
            return;
        }

        long saveRbdCount = rbdCount;

        for (String rbdName : sMap.keySet()) {
            LocalizationFile lf = sMap.get(rbdName).getLocalizationFile();
            if (lf != null && lf.getPath().equals(localizationFile.getPath())) {

                sMap.remove(rbdName);

                rbdCount--;
                break;
            }
        }

        if (saveRbdCount == rbdCount) {
            statusHandler.handle(
                    Priority.PROBLEM,
                    "Could not find rbd to remove for File:"
                            + localizationFile.getPath());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationFileObserver#fileUpdated
     * (com.raytheon.uf.common.localization.FileUpdatedMessage)
     */
    @Override
    public void fileUpdated(FileUpdatedMessage message) {
        String chgFile = message.getFileName();
        FileChangeType chgType = message.getChangeType();
        LocalizationContext chgContext = message.getContext();

        LocalizationFile lFile = NcPathManager.getInstance()
                .getLocalizationFile(chgContext, chgFile);
        String[] dirsf = chgFile.split(File.separator);

        // The actual adding and updating of data to the files takes place
        // in raytheon...localization.LocalizationManager.upload()
        // there is no need here to process FileUpdatedMessage for updates.
        // That processing/this method also belongs to a deprecated interface.

        try {
            // File Added
            if (chgType == FileChangeType.ADDED) {

                if (!chgFile.endsWith(".xml")) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Non-xmlfile found under SPFs dir:" + chgFile);
                } else if (dirsf.length != 5) {
                    statusHandler.handle(Priority.PROBLEM,
                            "xml file found in non-SPF directory? " + chgFile);
                } else {
                    AbstractRBD<?> rbd = NcMapRBD.getRbd(lFile.getFile());
                    rbd.setLocalizationFile(lFile);
                    addRbd(dirsf[2], dirsf[3], rbd);
                }
            } // File Deleted
            else if (chgType == FileChangeType.DELETED) {
                removeEntryByFile(lFile);
            }

        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, "Error unmarshalling rbd: "
                    + chgFile + "\n" + e.getMessage());
        }
    }
}
