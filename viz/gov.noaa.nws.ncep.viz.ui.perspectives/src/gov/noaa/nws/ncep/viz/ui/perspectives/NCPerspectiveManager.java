package gov.noaa.nws.ncep.viz.ui.perspectives;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.raytheon.uf.common.dataplugin.satellite.units.SatelliteUnits;
import com.raytheon.uf.common.derivparam.library.DerivedParameterGenerator;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IVizEditorChangedListener;
import com.raytheon.uf.viz.core.ProgramArguments;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.alerts.observers.ProductAlertObserver;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.input.InputAdapter;
import com.raytheon.viz.ui.perspectives.AbstractCAVEPerspectiveManager;

import gov.noaa.nws.ncep.staticdataprovider.StaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenFileNameDisplay;
import gov.noaa.nws.ncep.viz.common.area.AreaMenusMngr;
import gov.noaa.nws.ncep.viz.common.area.NcAreaProviderMngr;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsRenderableDisplay;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.gempak.grid.mapper.GridMapper;
import gov.noaa.nws.ncep.viz.gempak.grid.units.GempakGridParmInfoLookup;
import gov.noaa.nws.ncep.viz.gempak.grid.units.GempakGridVcrdInfoLookup;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.resourceManager.ui.ResourceManagerDialog;
import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.NcMapRBD;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceBndlLoader;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.SpfsManager;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcSatelliteUnits;
import gov.noaa.nws.ncep.viz.tools.frame.FrameDataDisplay;
import gov.noaa.nws.ncep.viz.tools.imageProperties.FadeDisplay;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcEditor;
import gov.noaa.nws.ncep.viz.ui.display.NCLegendResource;
import gov.noaa.nws.ncep.viz.ui.display.NCLegendResource.LegendMode;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * Manages the life cycle of the National Centers Perspectives
 * 
 * Installs a perspective watcher that handles the transitions in and out of the
 * National Centers perspectives.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#     Engineer     Description
 * ------------	----------	-----------	--------------------------
 * 12/2008      22          M. Li       Created
 * 03/2009      75          B. Hebbard  Rename class and all references NMAP->NC
 * 08/05/09                 G. Hull     Load a default RBD
 * 09/27/09     #169        G. Hull     create an NCMapEditor and remove non NC editors
 * 11/05/09     183         Q. Zhou     Added Fading scale
 * 11/13/09     180         G. Hull     NmapCommon.NatlCntrsPerspectiveID
 * 02/20/10     226         G. Hull     Use RbdBundle
 * 03/16/10   238, 239      Archana     Added FrameDataDisplay to the status bar.
 * 05/23/10   dr11 migration G. Hull    manage Cave's TimeDisplay 
 * 05/26/10                 G. Hull     Call NcSatelliteUnits
 * 08/27/10     #303        G. Hull     Set the editor name based on the default RBD name 
 * 09/23/10     #307        G. Hull     Load spf from the command line.
 * 10/20/10     #307        G. Hull     NcAutoUpdater
 * 03/22/11   r1g2-9        G. Hull     extend AbstractCAVEPerspectiveManager
 * 06/07/11     #445        X. Guo      Data Manager Performance Improvements
 *                                      Initialize Data resources
 * 07/28/2011    450        G. Hull     NcPathManager
 * 10/25/2011   #467        G. Hull     close the ResourceManager on deactivate/close
 * 10/26/2011               X. Guo      Init ncgrib inventory
 * 11/22/2011   #514        G. Hull     add an IVizEditorChangedListener to update the GUI when the editor changes
 * 12/13/2011               J. Wu       Added PGEN file name display
 * 02/06/2011               S. Gurung   Commented out code in handleKeyUp and handleKeyDown methods (See NCLegendHandler) 
 * 02/15/2012   627        Archana      Updated the call to addRbd() to accept 
 *                                      a NCMapEditor object as one of the arguments
 *                                      Removed the call to setNcEditor() and updated initFromEditor()
 *                                      to take an editor as one of the arguments    
 * 04/16/2012   740         B. Yin      Start the static data service before opening map editor.  
 * 03/01/2012   #606        G. Hull     initialize NcInventory
 * 05/15/2012               X. Guo      initialize NcGridInventory
 * 05/17/2012               X. Guo      Changed "true" to "false" to initialize NcGridInventory
 * 06/01/2012   #815        G. Hull     Create DESK Level for Localization.
 * 06/13/2012   #817        G. Hull     for -spf arg, create one rbdLoader and call initTimeline on the rbds.
 * 12/12/2012   #630        G. Hull     rm check for suspendZoom in displayChangeLister. code moved to refreshGUIElements 
 * 01/20/2013   #972        G. Hull     set new NcContextActivator   
 * 03/15/2013   #972/#875   G. Hull     override openNewEditor() to be called after raytheon code change to NewAbstractEditor
 * 04/15/2013   #864        G. Hull     display warnings from RD loading.
 * 04/17/2013   #863        G. Hull     Initialize Predefined Areas
 * 11/14/2013   #2361       N. Jensen   Initialize SerializationUtil when activated
 * 11/13/2013   #1051       G. Hull     override getTitle() to include the desk.
 * 07/28/2015   R7785       A. Su       Added the runAsyncTasks() method to run asynchronous tasks,
 *                                      such as getting an instance of DerivedParameterGenerator.
 * 09/25/2015   R8833       N. Jensen   Added right click option to reverse legend mode
 * 09/25/2015   5645        bsteffen    Eclipse 4: Use HandlerService for command execution.
 * 02/04/2016   R13171      E. Brown    Added: short right click launches resource manager, added: item to launch resource
 *                                      manager in context menu that opens on long right mouse button
 * 03/15/2016   R16112      E. Brown    Opening resource manager by right clicking goes to correct "Create RBD" tab
 * 04/04/2016   R17317      A. Su       Added to close resource manager by right mouse click if dialog is open.
 * 04/05/2016   RM#10435    rjpeter     Removed Inventory usage.
 * 01/17/2017   RM#15784    Chin Chen   Fixed bug that cause error when attempting to open editor from NSHARP editor
 * </pre>
 * 
 * @author
 * @version 1.0
 */

public class NCPerspectiveManager extends AbstractCAVEPerspectiveManager {
    /** The National Centers Perspective Class */
    // put this in common to avoid dependencies on this project
    public static final String NC_PERSPECTIVE = NmapCommon.NatlCntrsPerspectiveID;

    public static final String newDisplayCmd = "gov.noaa.nws.ncep.viz.ui.newMapEditor";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NCPerspectiveManager.class);

    private IVizEditorChangedListener displayChangeListener = null;

    @Override
    protected String getTitle(String title) {

        String desk = LocalizationManager
                .getContextName(LocalizationLevel.valueOf("DESK"));

        if ((desk == null) || desk.isEmpty() || desk.equalsIgnoreCase("none")) {
            desk = "NONE";
        }
        return title + ": "
                + LocalizationManager.getContextName(LocalizationLevel.SITE)
                + "/" + desk + " - " + getLabel();
    }

    // Issue the newDisplay command the same as if called from the main menu
    // new Display
    @Override
    public AbstractEditor openNewEditor() {

        AbstractEditor curEd = (AbstractEditor) EditorUtil.getActiveEditor();

        if (curEd == null) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error getting current editor ");
            return null;
        }
        ICommandService service = (ICommandService) curEd.getSite()
                .getService(ICommandService.class);
        IHandlerService handlerService = curEd.getSite()
                .getService(IHandlerService.class);

        Command cmd = service.getCommand(newDisplayCmd);
        if (cmd == null) {
            statusHandler.handle(Priority.PROBLEM, "Error getting cmd: ",
                    newDisplayCmd);
            return null;
        }
        try {
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("promptForName", "false");

            Object obj = handlerService.executeCommand(
                    ParameterizedCommand.generateCommand(cmd, params), null);

            if ((obj != null) && (obj instanceof AbstractEditor)) {

                return (AbstractEditor) obj;
            }

            statusHandler.handle(Priority.PROBLEM, "sanity check: cmd, "
                    + newDisplayCmd + ", not returning an editor object");

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error executing cmd: " + newDisplayCmd);
        }

        return null;
    }

    /**
     * Run any tasks here that are desired to be started as early as possible.
     * The tasks should be put into different threads to take advantage of
     * multicore processor.
     */
    protected void runAsyncTasks() {

        Thread newThread = new Thread() {
            @Override
            public void run() {
                // Initialize derived parameters in order to speed up
                // the first-time data selection of SURFACE or UPPER_AIR.
                DerivedParameterGenerator.getInstance();
            }
        };
        newThread.start();
    }

    @Override
    protected void open() {
        runAsyncTasks();

        contextActivator = new NcContextActivator(page);

        // force DESK level to be created.
        NcPathManager.getInstance();

        GridMapper.GridMapperInit();
        GempakGridParmInfoLookup.getInstance();
        GempakGridVcrdInfoLookup.getInstance();

        displayChangeListener = new IVizEditorChangedListener() {
            @Override
            public void editorChanged(IDisplayPaneContainer container) {
                if (container == null) {
                    return;
                } else if (container instanceof AbstractNcEditor) {

                    NcEditorUtil
                            .refreshGUIElements((AbstractNcEditor) container);
                } else {
                    statusHandler.handle(Priority.PROBLEM,
                            "Container is not instance of AbstractNcEditor");
                }
            }
        };

        // Add an observer to process the dataURI Notification msgs from edex.
        ProductAlertObserver.addObserver(null, new NcAutoUpdater());

        /*
         * NatlCntrs uses a different equation to compute the Temperature values
         * from a Satellite IR image so this will override the 'IRPixel' label
         * used by satellite images and will create our Units and UnitConverter
         * class to do the conversion.
         */
        NcSatelliteUnits.register();

        // read in and validate all of the Predefined Area files.
        try {
            List<VizException> warnings = NcAreaProviderMngr.initialize();

            if ((warnings != null) && !warnings.isEmpty()) {
                final StringBuffer msgBuf = new StringBuffer(
                        "The following Warnings occurs while loading the Predefined Areas::\n\n");
                int numWarns = 0;
                for (VizException vizex : warnings) {
                    msgBuf.append(" -- " + vizex.getMessage() + "\n");

                    if (++numWarns > 20) {
                        msgBuf.append(" .....and more....");
                        break;
                    }
                }

                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        MessageDialog warnDlg = new MessageDialog(
                                perspectiveWindow.getShell(), "Warning", null,
                                msgBuf.toString(), MessageDialog.WARNING,
                                new String[] { "OK" }, 0);
                        warnDlg.open();
                    }
                });

            }
        } catch (VizException el) {
            MessageDialog errDlg = new MessageDialog(
                    perspectiveWindow.getShell(), "Error", null,
                    "Error Reading in Predefined Areas:\n\n" + el.getMessage(),
                    MessageDialog.ERROR, new String[] { "OK" }, 0);
            errDlg.open();
        }

        AreaMenusMngr.getInstance();

        // Force the RBDs to read from localization to save time
        // bringing up the RBD manager
        SpfsManager.getInstance();

        // Initialize the NcInventory. This cache is stored on the server side
        // and will only need initialization for the first instance of cave.
        try {
            // force reading in of the resource definitions
            ResourceDefnsMngr.getInstance();

            if (!ResourceDefnsMngr.getInstance().getBadResourceDefnsErrors()
                    .isEmpty()) {

                final StringBuffer errBuf = new StringBuffer(
                        "There were errors creating the following Resource Defintions:\n\n");
                int numErrs = 0;
                for (VizException vizex : ResourceDefnsMngr.getInstance()
                        .getBadResourceDefnsErrors()) {
                    errBuf.append(" -- " + vizex.getMessage() + "\n");

                    if (++numErrs > 20) {
                        errBuf.append(" .....and more....");
                        break;
                    }
                }

                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        MessageDialog errDlg = new MessageDialog(
                                perspectiveWindow.getShell(), "Error", null,
                                errBuf.toString(), MessageDialog.ERROR,
                                new String[] { "OK" }, 0);
                        errDlg.open();
                    }
                });
            }

            if (!ResourceDefnsMngr.getInstance().getResourceDefnWarnings()
                    .isEmpty()) {

                final StringBuffer msgBuf = new StringBuffer(
                        "The following Warnings occurs while loading the Resource Definitions::\n\n");
                int numWarns = 0;
                for (VizException vizex : ResourceDefnsMngr.getInstance()
                        .getResourceDefnWarnings()) {
                    msgBuf.append(" -- " + vizex.getMessage() + "\n");

                    if (++numWarns > 20) {
                        msgBuf.append(" .....and more....");
                        break;
                    }
                }

                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        MessageDialog warnDlg = new MessageDialog(
                                perspectiveWindow.getShell(), "Warning", null,
                                msgBuf.toString(), MessageDialog.WARNING,
                                new String[] { "OK" }, 0);
                        warnDlg.open();
                    }
                });
            }

        } catch (VizException el) {
            MessageDialog errDlg = new MessageDialog(
                    perspectiveWindow.getShell(), "Error", null,
                    "Error Initializing NcInventory:\n\n" + el.getMessage(),
                    MessageDialog.ERROR, new String[] { "OK" }, 0);
            errDlg.open();
        }

        // Load either the default RBD or RBDs in the command line spf
        List<AbstractRBD<?>> rbdsToLoad = new ArrayList<AbstractRBD<?>>();

        String spfName = ProgramArguments.getInstance().getString("-spf");

        if ((spfName != null) && !spfName.isEmpty()) {
            String[] grpAndSpf = spfName.split(File.separator);

            // the name of the spf should include a group name
            // TODO : check that there is a group and if not use a default.
            if (grpAndSpf.length != 2) {
                System.out.println(
                        "The -spf argument is specified without an spf group (ex spfGroupName/spfName.");
                // load the default rbd...
                MessageDialog errDlg = new MessageDialog(
                        perspectiveWindow.getShell(), "Error", null,
                        "The -spf arguement is missing an SPF group name.\nEx. \"SpfGroupName/SpfName\"",
                        MessageDialog.WARNING, new String[] { "OK" }, 0);
                errDlg.open();
            } else {

                try {
                    // resolve Latest Cycle times
                    rbdsToLoad = SpfsManager.getInstance()
                            .getRbdsFromSpf(grpAndSpf[0], grpAndSpf[1], true);
                } catch (VizException e) {
                    MessageDialog errDlg = new MessageDialog(
                            perspectiveWindow.getShell(), "Error", null,
                            "The -spf arguement, " + spfName
                                    + " doen't exist\n",
                            MessageDialog.WARNING, new String[] { "OK" }, 0);
                    errDlg.open();
                }
            }
        }

        if (rbdsToLoad.isEmpty()) {
            try {
                AbstractRBD<?> dfltRbd = NcMapRBD.getDefaultRBD();
                rbdsToLoad.add(dfltRbd);

            } catch (Exception ve) {
                statusHandler.handle(Priority.PROBLEM,
                        "Could not load rbd: " + ve.getMessage(), ve);

            }
        }

        // start data provider before creating ncmapeditor
        StaticDataProvider.start();

        ResourceBndlLoader rbdLoader = new ResourceBndlLoader("Loading SPF: ");

        // loop thru the rbds and load them into a new editor.
        for (final AbstractRBD<?> rbd : rbdsToLoad) {

            rbd.initTimeline();

            AbstractEditor editor;

            try {
                editor = NcDisplayMngr.createNatlCntrsEditor(
                        rbd.getDisplayType(), rbd.getRbdName(),
                        rbd.getPaneLayout());
                rbdLoader.addRBD(rbd, editor);

            } catch (final VizException e) {
                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        MessageDialog errDlg = new MessageDialog(
                                perspectiveWindow.getShell(), "Error", null,
                                "Error Creating Eclipse Editor for RBD "
                                        + rbd.getRbdName() + "\n"
                                        + e.getMessage(),
                                MessageDialog.ERROR, new String[] { "OK" }, 0);
                        errDlg.open();
                    }
                });
            }
        }

        VizApp.runAsync(rbdLoader);
    }

    @Override
    public void activate() {
        super.activate();

        // initialize SerializationUtil's JAXBContext until deprecated
        // ISerializableObject usage is replaced
        try {
            SerializationUtil.getJaxbContext();
        } catch (JAXBException e) {
            statusHandler.handle(Priority.CRITICAL,
                    "An error occured initializing Serialization", e);
        }

        // add an EditorChangedListener
        VizWorkbenchManager.getInstance().addListener(displayChangeListener);

        // read in and validate all of the Predefined Area files.

        List<VizException> warnings = NcAreaProviderMngr.reinitialize();

        if ((warnings != null) && !warnings.isEmpty()) {
            final StringBuffer msgBuf = new StringBuffer(
                    "The following Warnings occurs while re-initializing the Predefined Areas::\n\n");
            int numWarns = 0;
            for (VizException vizex : warnings) {
                msgBuf.append(" -- " + vizex.getMessage() + "\n");

                if (++numWarns > 20) {
                    msgBuf.append(" .....and more....");
                    break;
                }
            }

            VizApp.runAsync(new Runnable() {
                @Override
                public void run() {
                    MessageDialog warnDlg = new MessageDialog(
                            perspectiveWindow.getShell(), "Warning", null,
                            msgBuf.toString(), MessageDialog.WARNING,
                            new String[] { "OK" }, 0);
                    warnDlg.open();
                }
            });
        }

        // re-layout the shell since we added widgets
        perspectiveWindow.getShell().layout(true, true);

        NcSatelliteUnits.register();
    }

    @Override
    public void deactivate() {
        super.deactivate();

        VizWorkbenchManager.getInstance().removeListener(displayChangeListener);

        SatelliteUnits.register();

        // would rather do this another way, preferably by having
        // ResourceManagerDialog extend CaveSWTDialog (do this later) or
        // by implementing a perspective closed listener (cyclical dependency
        // problem)
        ResourceManagerDialog.close();
    }

    @Override
    protected List<ContributionItem> getStatusLineItems() {
        List<ContributionItem> stsLineDisplays = new ArrayList<ContributionItem>();
        // in reverse order
        stsLineDisplays.add(new FadeDisplay());
        stsLineDisplays.add(PgenFileNameDisplay.getInstance());
        stsLineDisplays.add(FrameDataDisplay.createInstance());

        return stsLineDisplays;
    }

    @Override
    public void close() {
        super.close();

        VizWorkbenchManager.getInstance().removeListener(displayChangeListener);
        displayChangeListener = null;

        ResourceManagerDialog.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.ui.AbstractVizPerspective#getPerspectiveInputHandlers
     * (com.raytheon.viz.ui.editor.AbstractEditor)
     */
    @Override
    public IInputHandler[] getPerspectiveInputHandlers(
            final AbstractEditor editor) {
        // currently only implementing handleMouseWheel which is now done below.
        IInputHandler[] superHandlers = super.getPerspectiveInputHandlers(
                editor);

        /*
         * If this is a GLMapEditor from D2D then just return the
         * abstractEditors handlers (this won't last long since the perspective
         * will remove/save off the editors.
         */

        if (!NcDisplayMngr.isNatlCntrsEditor(editor)) {
            return superHandlers;
        }

        IInputHandler handler = new InputAdapter() {

            @Override
            public boolean handleMouseUp(int x, int y, int mouseButton) {

                // Launch Resource Manager dialog on a right mouseUp
                // and close it on a right mouseUp if the dialog is open.
                if (mouseButton == 3) {
                    if (ResourceManagerDialog.isOpen()) {

                        ResourceManagerDialog.close();
                    } else {
                        launchResourceManager();
                    }
                }
                return false;
            }

        };

        ArrayList<IInputHandler> handlers = new ArrayList<>();
        handlers.add(handler);
        return handlers.toArray(new IInputHandler[handlers.size()]);
    }

    /**
     * Opens the resource manager dialog window
     */
    private void launchResourceManager() {
        // Set the commandId for this item
        String commandId = "gov.noaa.nws.ncep.viz.actions.resourceManager";
        /*
         * This code taken directly from
         * com.raytheon.viz.ui.glmap.actions.ClearAction
         * 
         * Finds the AbstractHandler currently registered with this commandId
         */
        IEditorPart part = VizWorkbenchManager.getInstance().getActiveEditor();
        ICommandService service = (ICommandService) part.getSite()
                .getService(ICommandService.class);
        Command cmd = service.getCommand(commandId);

        if (cmd == null) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error getting command \"cmd\": ", commandId);
            return;
        }
        try {

            /*
             * Set up information to pass to the AbstractHandler
             */
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("editor", part);
            // Open resource manager in with "Create RBD" tab open
            params.put("mode", "CREATE_RBD");
            ExecutionEvent exec = new ExecutionEvent(cmd, params, null, "mode");

            // Execute the handler
            cmd.executeWithChecks(exec);

            // Update the GUI elements on the menus and toolbars
            for (String toolbarID : NmapCommon.getGUIUpdateElementCommands()) {
                service.refreshElements(toolbarID, null);
            }
        } catch (Exception e) {
            // Error executing Handler

            statusHandler.handle(Priority.PROBLEM,
                    "Error executing command \"cmd\": " + commandId, e);
        }

    }

    @Override
    public void addContextMenuItems(IMenuManager menuManager,
            IDisplayPaneContainer container, IDisplayPane pane) {
        if (!(container instanceof AbstractEditor)) {
            return;
        }

        // TODO : add menu actions to minimize/maximize the selected pane.

        if ((container instanceof AbstractNcEditor) && (pane
                .getRenderableDisplay() instanceof INatlCntrsRenderableDisplay)) {

            // Put "Launch Resource Manager" item above hide/show legends
            menuManager.add(new Action("Resource Manager") {
                @Override
                public void run() {
                    launchResourceManager();
                }
            });

            // Right click option to reverse legend mode
            final NCLegendResource lg;
            List<NCLegendResource> legends = pane.getDescriptor()
                    .getResourceList()
                    .getResourcesByTypeAsType(NCLegendResource.class);
            if (!legends.isEmpty()) {
                lg = legends.get(0);
                LegendMode mode = lg.getLegendMode();
                switch (mode) {
                case HIDE:
                    menuManager.add(new Action(LegendMode.SHOW.toString()) {
                        @Override
                        public void run() {
                            lg.setLegendMode(LegendMode.SHOW);
                        }
                    });
                    break;
                case SHOW:
                    menuManager.add(new Action(LegendMode.HIDE.toString()) {
                        @Override
                        public void run() {
                            lg.setLegendMode(LegendMode.HIDE);
                        }
                    });
                    break;
                }
            }
        }
    }
}
