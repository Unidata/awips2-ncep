package gov.noaa.nws.ncep.viz.resources.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.geotools.coverage.grid.GeneralGridGeometry;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.adapters.GridGeometrySerialized;
import com.raytheon.uf.common.serialization.jaxb.JAXBClassLocator;
import com.raytheon.uf.common.serialization.jaxb.JaxbDummyObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.VariableSubstitutionUtil;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.reflect.SubClassLocator;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.common.area.NcGridGeometryAdapter;
import gov.noaa.nws.ncep.viz.common.area.PredefinedAreaFactory;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsPaneManager;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsRenderableDisplay;
import gov.noaa.nws.ncep.viz.common.display.INcPaneID;
import gov.noaa.nws.ncep.viz.common.display.IPaneLayoutable;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayName;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;
import gov.noaa.nws.ncep.viz.ui.display.NcPaneID;
import gov.noaa.nws.ncep.viz.ui.display.NcPaneLayout;

/**
 * Bundle for Natl Cntrs Resources
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date         Ticket#     Engineer    Description
 *    ------------ ----------  ----------- --------------------------
 *    02/20/10       #226      ghull       added Pane layout info to Bundle class.
 *    09/02/10       #307      ghull       use one timeMatcher for all descriptors
 *    11/15/11                 ghull       add resolveLatestCycleTimes
 *    04/26/12       #585      sgurung     Added rbdSequence 
 *    06/13/12       #817      Greg Hull   add resolveDominantResource()  
 *    06/20/12       #647      Greg Hull   add clone()
 *    06/29/12       #568      Greg Hull   implement Comparable
 *    11/25/12       #630      Greg Hull   getDefaultRBD()
 *    02/22/10       #972      Greg Hull   created from old RbdBundle
 *    05/14/13       #862      Greg Hull   implement INatlCntrsPaneManager
 *    11/21/13       #1066     Greg Hull   save off Native gridGeometries during clone()
 *    10/29/13       #2491     bsteffen    Use custom JAXB context instead of SerializationUtil.
 *    05/15/2014     #1131     Quan Zhou   Added GRAPH_DISPLAY.
 *    05/24/14       R4078     S. Gurung   Added NMAP_RTKP_WORLD_DISPLAY in getDefaultRBD().
 *    11/12/2015     R8829     B. Yin      Sort resources in RBD by rendering order.
 *    03/10/2016     R16237    B. Yin      Get dominant resource within a group resource.
 *    09/23/2016     R21176    J.Huber     Resolve latest cycle time for grouped gridded resources in SPF.
 *    10/07/2016     R21481    Bugenhagen  Replaced use of temp file with ByteArrayOutputStream 
 *                                         in getRbd method.
 *    02/23/2017     R5940     B.Hebbard   In resolveLatestCycleTimes method,  
 *                                         replace call to getAvailableDataTimes 
 *                                         with call to resolveLatestCycleTime.
 * 
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractRBD<T extends AbstractRenderableDisplay>
        implements INatlCntrsPaneManager, Comparable<AbstractRBD<?>> {
    private static JAXBManager jaxb;

    @XmlElement
    protected NcDisplayType displayType = NcDisplayType.NMAP_DISPLAY;

    // Could be an INcPaneLayout but right now the NcPaneLayout is the only
    // one that exists/is supported and there is a problem marshalling this as
    // an interface.
    @XmlElement
    protected NcPaneLayout paneLayout = new NcPaneLayout(1, 1);

    // TODO : wanted this to be an INcPaneID but JAXB can't handle interfaces.
    // Since there is actually only the NcPaneID now, just make this an
    // NcPaneID.
    @XmlElement
    protected NcPaneID selectedPaneId = new NcPaneID();

    @XmlElement
    protected boolean geoSyncedPanes;

    @XmlElement
    protected boolean autoUpdate;

    @XmlElement
    protected int rbdSequence;

    @XmlElement
    private NCTimeMatcher timeMatcher;

    // only set if read from Localization
    private LocalizationFile lFile;

    // this is defined as AbstractRenderableDisplay for JaxB but all the
    // renderable displays in the RBD must implement INatlCntrsRenderableDisplay
    @XmlElement
    @XmlElementWrapper(name = "displayList")
    protected T[] displays;

    // if created from a loaded display, this it the display id.
    protected int displayId = -1;

    @XmlAttribute
    protected String rbdName;

    // true if edited from the LoadRbd dialog
    private Boolean isEdited = false;

    // true if this Rbd was created from the DefaultRbd
    @XmlElement
    protected Boolean isDefaultRbd = false;

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        for (T disp : getDisplays()) {
            if (disp != null) {
                ((INatlCntrsDescriptor) disp.getDescriptor())
                        .setAutoUpdate(autoUpdate);
            }
        }
    }

    public LocalizationFile getLocalizationFile() {
        return lFile;
    }

    public void setLocalizationFile(LocalizationFile lFile) {
        this.lFile = lFile;
    }

    @Override
    public NcDisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(NcDisplayType rbdType) {
        this.displayType = rbdType;
    }

    public boolean isGeoSyncedPanes() {
        return geoSyncedPanes;
    }

    public void setGeoSyncedPanes(boolean geoSyncedPanes) {
        this.geoSyncedPanes = geoSyncedPanes;
    }

    public NcPaneID getSelectedPaneId() {
        return selectedPaneId;
    }

    public void setSelectedPaneId(NcPaneID selectedPaneId) {
        this.selectedPaneId = selectedPaneId;
    }

    public void setPaneLayout(NcPaneLayout paneLayout) {
        this.paneLayout = paneLayout;
    }

    @Override
    public NcPaneLayout getPaneLayout() {
        return paneLayout;
    }

    // No id since it has not been assigned an id yet
    @Override
    public NcDisplayName getDisplayName() {
        return new NcDisplayName(displayId, getRbdName());
    }

    @Override
    public IPaneLayoutable getPane(INcPaneID pid) {
        if (paneLayout.containsPaneId(pid)
                && paneLayout.getPaneIndex(pid) < displays.length) {

            T pane = displays[paneLayout.getPaneIndex(pid)];

            if (pane != null && pane instanceof IPaneLayoutable) {
                return (IPaneLayoutable) pane;
            }
        }
        return null;
    }

    public Boolean isEdited() {
        return isEdited;
    }

    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
    }

    public Boolean getIsDefaultRbd() {
        return isDefaultRbd;
    }

    public void setIsDefaultRbd(Boolean isDefaultRbd) {
        this.isDefaultRbd = isDefaultRbd;
    }

    public void setRbdSequence(int seq) {
        this.rbdSequence = seq;
    }

    public int getRbdSequence() {
        return rbdSequence;
    }

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractRBD.class);

    /**
     * Default constructor
     */
    public AbstractRBD() {
        timeMatcher = null;
    }

    // used when creating an RBD to be written out.
    public AbstractRBD(NcPaneLayout paneLayout) {
        timeMatcher = null;
        setPaneLayout(paneLayout);
        try {
            displays = (T[]) NcDisplayMngr.createDisplaysForNcDisplayType(this,
                    paneLayout);
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, e.getMessage());
        }
    }

    public static AbstractRBD<?> clone(AbstractRBD<?> rbdBndl)
            throws VizException {
        try {
            NCTimeMatcher tm = new NCTimeMatcher(rbdBndl.getTimeMatcher());

            // HACK Alert ; for Mcidas GVAR projections, the NAV_BLOCK_BASE64
            // parameter is a String, but since the wkt format is assuming all
            // projection params are doubles, the CRS string will throw an error
            // on unmarshalling. This temporary hack will substitude a dummy
            // projection and save off the wkt for the GVARs and then call the
            // McidasSpatialFactory to handle the parsing.

            Map<String, GridGeometrySerialized> ggsMap = new HashMap<>();
            Map<String, AbstractRenderableDisplay> dispMap = new HashMap<>();

            NcGridGeometryAdapter geomAdapter = new NcGridGeometryAdapter();

            for (AbstractRenderableDisplay disp : rbdBndl.getDisplays()) {
                GeneralGridGeometry geom = disp.getDescriptor()
                        .getGridGeometry();

                if (geom.getEnvelope().getCoordinateReferenceSystem().getName()
                        .toString().startsWith("MCIDAS")) {

                    GridGeometrySerialized ggs = geomAdapter.marshal(geom);
                    ggsMap.put(((INatlCntrsRenderableDisplay) disp).getPaneId()
                            .toString(), ggs);
                    dispMap.put(((INatlCntrsRenderableDisplay) disp).getPaneId()
                            .toString(), disp);

                    // something valid as a placeholder....
                    disp.getDescriptor()
                            .setGridGeometry(PredefinedAreaFactory
                                    .getDefaultPredefinedAreaForDisplayType(
                                            rbdBndl.getDisplayType())
                                    .getGridGeometry());
                }
            }

            ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            getJaxbManager().marshalToStream(rbdBndl, outstream);
            AbstractRBD<?> clonedRbd = getRbd(outstream);

            for (AbstractRenderableDisplay disp : clonedRbd.getDisplays()) {
                String ggsKey = ((INatlCntrsRenderableDisplay) disp).getPaneId()
                        .toString();
                if (ggsMap.containsKey(ggsKey)) {
                    GeneralGridGeometry geom = geomAdapter
                            .unmarshal(ggsMap.get(ggsKey));
                    disp.getDescriptor().setGridGeometry(geom);
                    // another copy
                    geom = geomAdapter.unmarshal(ggsMap.get(ggsKey));
                    dispMap.get(ggsKey).getDescriptor().setGridGeometry(geom);
                }
            }
            // not serialized
            clonedRbd.displayId = rbdBndl.displayId;

            if (clonedRbd.getDisplayType() == null) {
                clonedRbd.setDisplayType(NcDisplayType.NMAP_DISPLAY);
            }

            clonedRbd.setTimeMatcher(tm);

            clonedRbd.setLocalizationFile(rbdBndl.getLocalizationFile());

            // set the RbdName inside the renderable display panes
            clonedRbd.setRbdName(clonedRbd.getRbdName());

            for (AbstractRenderableDisplay disp : clonedRbd.getDisplays()) {
                if (disp instanceof IPaneLayoutable) {
                    ((IPaneLayoutable) disp).setPaneManager(clonedRbd);
                }
            }

            return clonedRbd;

        } catch (SerializationException e) {
            throw new VizException(e);
        } catch (JAXBException e) {
            throw new VizException(e);
        } catch (VizException e) {
            throw new VizException("Error loading rbd " + rbdBndl.rbdName + " :"
                    + e.getMessage());
        } catch (IOException e) {
            throw new VizException(e);
        } catch (Exception e) {
            throw new VizException(e);
        }
    }

    public static AbstractRBD<?> createEmptyRbdForDisplayType(
            NcDisplayType dispType, NcPaneLayout pLayout) throws VizException {
        AbstractRBD<?> rbd = null;

        switch (dispType) {
        case NMAP_DISPLAY:
            rbd = new NcMapRBD(pLayout);
            break;
        case NTRANS_DISPLAY:
            rbd = new NTransRBD(pLayout);
            rbd.setRbdName("NTRANS");
            break;
        case SOLAR_DISPLAY:
            rbd = new SolarRBD(pLayout);
            rbd.setRbdName("Solar");
            break;
        case GRAPH_DISPLAY:
            rbd = new GraphRBD(pLayout);
            rbd.setRbdName("Graph");
            break;
        case NMAP_RTKP_WORLD_DISPLAY:
            break;
        case NSHARP_DISPLAY:
            break;
        default:
            break;
        }

        rbd.setIsDefaultRbd(true);
        rbd.setDisplayType(dispType);
        rbd.createDisplays();

        return rbd;
    }

    private void createDisplays() throws VizException {
        displays = (T[]) NcDisplayMngr.createDisplaysForNcDisplayType(this,
                getPaneLayout());
    }

    public static AbstractRBD<?> createRbdFromEditor(AbstractEditor ncEditor)
            throws VizException {
        if (ncEditor == null) {
            return null;
        }

        AbstractRBD<?> rbd = createEmptyRbdForDisplayType(
                NcEditorUtil.getNcDisplayType(ncEditor),
                (NcPaneLayout) NcEditorUtil.getPaneLayout(ncEditor));

        rbd.initRbdFromEditor(ncEditor);

        return rbd;
    }

    public void initRbdFromEditor(AbstractEditor ncEditor) throws VizException {

        selectedPaneId = new NcPaneID();

        NcDisplayName rbdDispName = NcEditorUtil.getDisplayName(ncEditor);
        displayId = rbdDispName.getId();
        rbdName = rbdDispName.getName();
        geoSyncedPanes = NcEditorUtil.arePanesGeoSynced(ncEditor);
        autoUpdate = NcEditorUtil.getAutoUpdate(ncEditor);

        displays = (T[]) NcDisplayMngr.createDisplaysForNcDisplayType(this,
                NcEditorUtil.getPaneLayout(ncEditor));

        for (int paneIndx = 0; paneIndx < paneLayout
                .getNumberOfPanes(); paneIndx++) {
            IDisplayPane pane = NcEditorUtil.getDisplayPane(ncEditor,
                    paneLayout.createPaneId(paneIndx));

            T rDispPane = (T) pane.getRenderableDisplay();

            if (rDispPane instanceof IPaneLayoutable) {
                ((IPaneLayoutable) rDispPane).setPaneManager(this);
            }
            displays[paneIndx] = rDispPane;
        }

        setTimeMatcher(new NCTimeMatcher(
                (NCTimeMatcher) displays[0].getDescriptor().getTimeMatcher()));
    }

    /**
     * @return the rbdName
     */
    public String getRbdName() {
        return rbdName;
    }

    /**
     * @param name
     *            the rbdName to set
     */
    public void setRbdName(String rbdName) {
        this.rbdName = rbdName;
    }

    public String toXML() throws VizException {
        try {
            return getJaxbManager().marshalToXml(this);
        } catch (JAXBException e) {
            throw new VizException(e);
        }
    }

    public INatlCntrsRenderableDisplay getDisplayPane(INcPaneID pid) {
        if (!paneLayout.containsPaneId(pid)) {
            statusHandler.handle(Priority.INFO,
                    "NcMapRBD.getDisplayPane: pane id " + pid.toString()
                            + " is out of range.");
            return null;
        }

        return (INatlCntrsRenderableDisplay) displays[paneLayout
                .getPaneIndex(pid)];
    }

    public abstract boolean addDisplayPane(T dispPane, NcPaneID pid);

    public T[] getDisplays() {
        return displays;
    }

    public void setDisplays(T[] displays) {
        this.displays = displays;
    }

    public static AbstractRBD<?> getDefaultRBD() throws VizException {
        NcDisplayType dfltDisplayType = NcDisplayType.NMAP_DISPLAY;
        return getDefaultRBD(dfltDisplayType);
    }

    public static AbstractRBD<?> getDefaultRBD(NcDisplayType displayType)
            throws VizException {

        String dfltRbdName = "";

        switch (displayType) {
        case NMAP_DISPLAY:
            dfltRbdName = NcPathConstants.DFLT_RBD;
            break;

        // If/when we need to we can save an NTRANS or SWPC default rbd to a
        // file and save in localization. Right now there is nothing in the RBD
        // so we just create an 'empty' one.
        case NTRANS_DISPLAY:
            dfltRbdName = null;
            return AbstractRBD.createEmptyRbdForDisplayType(displayType,
                    new NcPaneLayout(1, 1));
        case SOLAR_DISPLAY:
            dfltRbdName = null;
            return AbstractRBD.createEmptyRbdForDisplayType(displayType,
                    new NcPaneLayout(1, 1));

        case GRAPH_DISPLAY:
            dfltRbdName = null;
            return AbstractRBD.createEmptyRbdForDisplayType(displayType,
                    new NcPaneLayout(1, 1));

        case NMAP_RTKP_WORLD_DISPLAY:
            dfltRbdName = NcPathConstants.DFLT_RTKP_RBD;
            break;

        default:
            throw new VizException("Unable to find the default RBD name for "
                    + displayType.toString());
        }

        File rbdFile = NcPathManager.getInstance().getStaticFile(dfltRbdName);

        if (rbdFile == null) {
            throw new VizException("Unable to find the default RBD file for "
                    + displayType.toString());
        }

        try {
            AbstractRBD<?> dfltRbd = AbstractRBD.unmarshalRBDFromFile(rbdFile,
                    null);

            // shouldn't need this but just in case the user creates a default
            // with real resources in it
            dfltRbd.resolveLatestCycleTimes();
            dfltRbd.setIsDefaultRbd(true);

            return clone(dfltRbd);

        } catch (Exception ve) {
            throw new VizException(
                    "Error getting default RBD: " + ve.getMessage());
        }
    }

    public static AbstractRBD<?> getRbd(File rbdFile) throws VizException {

        AbstractRBD<?> rbd = unmarshalRBDFromFile(rbdFile, null);

        // check for any required data that may be null or not set.
        // This shouldn't happen except possibly from an out of date RBD. (ie
        // older version)
        //
        if (rbd.displays == null || rbd.displays.length == 0) {
            throw new VizException(
                    "Error unmarshalling RBD: the renderable display list is null");
        }

        return rbd;
    }

    /**
     * Get RBD from output stream
     * 
     * @param outstream
     * @return
     * @throws VizException
     */
    public static AbstractRBD<?> getRbd(ByteArrayOutputStream outstream)
            throws VizException {

        AbstractRBD<?> rbd = unmarshalRBD(outstream, null);

        // check for any required data that may be null or not set.
        // This shouldn't happen except possibly from an out of date RBD. (ie
        // older version)
        //
        if (rbd.displays == null || rbd.displays.length == 0) {
            throw new VizException(
                    "Error unmarshalling RBD: the renderable display list is null");
        }

        return rbd;
    }

    /**
     * Unmarshal a bundle from a file
     * 
     * @param fileName
     *            the bundle to load
     * 
     * @param variables
     *            Optional: A map containing key value pairs to be used to
     *            perform variable substitution.
     * 
     * @return bundle loaded
     * 
     * @throws VizException
     */
    private static AbstractRBD<?> unmarshalRBDFromFile(File fileName,
            Map<String, String> variables) throws VizException {
        String s = null;
        try {
            FileReader fr = new FileReader(fileName);
            char[] b = new char[(int) fileName.length()];
            fr.read(b);
            fr.close();
            s = new String(b);

        } catch (Exception e) {
            throw new VizException("Error opening RBD file " + fileName, e);
        }

        return unmarshalRBD(s, variables);

    }

    /**
     * Unmarshal a bundle from an output stream
     * 
     * @param outstream
     *            the output stream to load
     * 
     * @param variables
     *            Optional: A map containing key value pairs to be used to
     *            perform variable substitution.
     * 
     * @return bundle loaded
     * 
     * @throws VizException
     */
    private static AbstractRBD<?> unmarshalRBD(ByteArrayOutputStream outstream,
            Map<String, String> variables) throws VizException {

        byte[] bytes = outstream.toByteArray();
        ByteArrayInputStream instream = new ByteArrayInputStream(bytes);

        try {
            AbstractRBD<?> rbd = (AbstractRBD<?>) getJaxbManager()
                    .unmarshalFromInputStream(instream);
            if (rbd == null) {
                statusHandler.handle(Priority.INFO,
                        "Unmarshalled stream is not a valid RBD.");
                return null;
            }

            rbd.sortResourcesAndSetTimeMatcher();

            return rbd;

        } catch (SerializationException e) {
            throw new VizException("Error unmarshalling RBD", e);
        } catch (JAXBException e) {
            throw new VizException("JAXB error for RBD", e);
        }

    }

    /**
     * Unmarshal a bundle
     * 
     * @param bundle
     *            the bundle to load as a string
     * 
     * @param variables
     *            Optional: A map containing key value pairs to be used to
     *            perform variable substitution.
     * 
     * @return bundle loaded
     * 
     * @throws VizException
     */
    private static AbstractRBD<?> unmarshalRBD(String bundleStr,
            Map<String, String> variables) throws VizException {

        try {
            String substStr = VariableSubstitutionUtil
                    .processVariables(bundleStr, variables);

            AbstractRBD<?> b = (AbstractRBD<?>) getJaxbManager()
                    .unmarshalFromXml(substStr);

            if (b == null) {
                statusHandler.handle(Priority.INFO,
                        "Unmarshalled rbd file is not a valid RBD.");
                return null;
            }

            b.sortResourcesAndSetTimeMatcher();

            return b;

        } catch (Exception e) {
            throw new VizException("Error loading bundle", e);
        }
    }

    /**
     * Sort all resources for this RBD and set its time matcher.
     */
    private void sortResourcesAndSetTimeMatcher() {

        sortResourcesByRenderingOrder();

        if (getTimeMatcher() == null) {
            setTimeMatcher(new NCTimeMatcher());
        }

        // This will make sure that all descriptors use the same timeMatcher
        // instance. All the timeMatchers should be the same but we need to
        // share them.
        setTimeMatcher(getTimeMatcher());

    }

    /*
     * Sorts resources by rendering order.
     */
    private void sortResourcesByRenderingOrder() {
        for (T disp : getDisplays()) {
            for (ResourcePair rscPair : disp.getDescriptor()
                    .getResourceList()) {
                if (rscPair.getProperties().getRenderingOrderId() != null) {
                    rscPair.getProperties()
                            .setRenderingOrder(RenderingOrderFactory
                                    .getRenderingOrder(rscPair.getProperties()
                                            .getRenderingOrderId()));
                }
            }
            disp.getDescriptor().getResourceList().sort();
        }
    }

    /**
     * Builds and returns a JAXB manager which has the ability to
     * marshal/unmarshall all AbstractRBD types.
     * 
     * @return a JAXBManager to use for marshalling/unmarshalling RBDs.
     * @throws JAXBException
     *             if there are illegal JAXB annotations.
     */
    public static synchronized JAXBManager getJaxbManager()
            throws JAXBException {
        if (jaxb == null) {
            SubClassLocator locator = new SubClassLocator();
            Collection<Class<?>> classes = JAXBClassLocator
                    .getJAXBClasses(locator, AbstractRBD.class);
            locator.save();

            Class<?>[] jaxbClasses = new Class<?>[classes.size() + 1];
            classes.toArray(jaxbClasses);
            /*
             * Add JaxbDummyObject at the begining so properties are loaded
             * correctly
             */
            jaxbClasses[jaxbClasses.length - 1] = jaxbClasses[0];
            jaxbClasses[0] = JaxbDummyObject.class;

            jaxb = new JAXBManager(jaxbClasses);
        }
        return jaxb;
    }

    public NCTimeMatcher getTimeMatcher() {
        if (timeMatcher == null) {
            timeMatcher = (NCTimeMatcher) displays[0].getDescriptor()
                    .getTimeMatcher();
        }
        return timeMatcher;
    }

    public void setTimeMatcher(NCTimeMatcher timeMatcher) {

        this.timeMatcher = timeMatcher;

        for (T disp : getDisplays()) {
            if (disp != null) {
                disp.getDescriptor().setTimeMatcher(timeMatcher);

                timeMatcher.addDescriptor(
                        (INatlCntrsDescriptor) disp.getDescriptor());
            }
        }
    }

    // After an Rbd is unmarshalled it is possible for forecast resources
    // to have a cycle time of LATEST. We don't always want to resolve the
    // Rbd after unmarshalling it so we do this as a separate step here.
    //
    // We also need to unpack any grouped resources and make sure that any
    // non-dominant resources have the latest string replaced with the latest
    // cycle time.
    public boolean resolveLatestCycleTimes() {
        for (T disp : getDisplays()) {
            ResourceList displayResourceList = disp.getDescriptor()
                    .getResourceList();
            for (int r = 0; r < displayResourceList.size(); r++) {
                ResourcePair rp = displayResourceList.get(r);
                List<AbstractNatlCntrsRequestableResourceData> allResourcesList = new ArrayList<>();
                if (rp.getResourceData() instanceof GroupResourceData) {
                    GroupResourceData grpResourceData = (GroupResourceData) rp
                            .getResourceData();
                    for (ResourcePair singlePair : grpResourceData
                            .getResourceList()) {
                        AbstractResourceData singleAbstractData = singlePair
                                .getResourceData();
                        if (singleAbstractData instanceof AbstractNatlCntrsRequestableResourceData) {
                            allResourcesList.add(
                                    (AbstractNatlCntrsRequestableResourceData) singleAbstractData);
                        }
                    }
                } else if (rp
                        .getResourceData() instanceof AbstractNatlCntrsRequestableResourceData) {
                    allResourcesList
                            .add((AbstractNatlCntrsRequestableResourceData) rp
                                    .getResourceData());
                }
                for (AbstractNatlCntrsRequestableResourceData singleAbstractData : allResourcesList) {
                    AbstractNatlCntrsRequestableResourceData rscData = singleAbstractData;
                    ResourceName rscName = rscData.getResourceName();

                    if (rscName.isForecastResource()
                            && rscName.isLatestCycleTime()) {

                        // The rscData comes in with the ResourceName variable
                        // still including "LATEST". The resolveLatestCycleTime
                        // changes the ResourceName but does not reset the
                        // timeMatcher's ResourceName to match. So if the
                        // incoming ResourceName (which still has "LATEST") is
                        // the dominant resource we set a flag to reset the
                        // dominant resource's ResourceData to refresh it which
                        // in turn resets the ResourceName to have the proper
                        // latest cycle time instead of "LATEST".

                        boolean refreshResourceData = false;
                        if (timeMatcher.getDominantResourceName()
                                .equals(rscData.getResourceName())) {
                            refreshResourceData = true;
                        }
                        rscData.resolveLatestCycleTime();
                        if (refreshResourceData) {
                            timeMatcher.setDominantResourceData(rscData);
                        }
                        if (rscName.isLatestCycleTime()) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Unable to Resolve Latest cycle time for :"
                                            + rscName);
                        }
                    }
                }
            }
        }

        return true;
    }

    // if the dominantResourceName is set for the timeMatcher but the
    // dominantResourceData isn't then find the dominant resource in the list
    // and set it.
    public void resolveDominantResource() {

        ResourceName domRscName = timeMatcher.getDominantResourceName();

        if (domRscName != null && domRscName.isValid()
                && timeMatcher.getDominantResource() == null) {

            // loop thru the displays looking for the dominant resource
            for (T disp : getDisplays()) {
                setDominantResourceData(disp.getDescriptor().getResourceList(),
                        domRscName);
            }
        }

    }

    // if the timeline has not been created then
    // get the dominant resource and initialize the timeMatcher
    public boolean initTimeline() {

        resolveDominantResource();

        return timeMatcher.loadTimes(true);
    }

    @Override
    public int compareTo(AbstractRBD<?> rbd) {
        if (rbdSequence == rbd.rbdSequence) {
            return rbdName.compareTo(rbd.rbdName);
        } else {
            return rbdSequence - rbd.rbdSequence;
        }
    }

    /**
     * Sets the dominant resource data from a resource list.
     * 
     * @param rscList
     *            - a resource list.
     * @param domRscName
     *            - name of the dominant resource.
     */
    private void setDominantResourceData(ResourceList rscList,
            ResourceName domRscName) {
        for (ResourcePair rscPair : rscList) {
            AbstractResourceData rscPairResourceData = rscPair
                    .getResourceData();
            if (rscPairResourceData instanceof AbstractNatlCntrsRequestableResourceData) {
                AbstractNatlCntrsRequestableResourceData rdata = (AbstractNatlCntrsRequestableResourceData) rscPair
                        .getResourceData();
                if (domRscName.toString()
                        .equals(rdata.getResourceName().toString())) {
                    timeMatcher.setDominantResourceData(rdata);
                    return;
                }
            } else if (rscPairResourceData instanceof GroupResourceData) {
                setDominantResourceData(
                        ((GroupResourceData) rscPairResourceData)
                                .getResourceList(),
                        domRscName);
            }
        }

    }

}
