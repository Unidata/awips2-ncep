package gov.noaa.nws.ncep.ui.nsharp.display;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.EditorInput;
import com.raytheon.viz.ui.input.InputManager;
import com.raytheon.viz.ui.panes.PaneManager;
import com.raytheon.viz.ui.panes.VizDisplayPane;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigStore;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpGraphProperty;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpAbstractPaneResource;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpPartListener;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpSkewTPaneResource;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpPaletteWindow;
import gov.noaa.nws.ncep.ui.pgen.tools.InputHandlerDefaultImpl;
import gov.noaa.nws.ncep.viz.ui.display.NCLoopProperties;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.skewt.NsharpEditor
 * 
 * This java class performs the NSHARP NsharpEditor functions. This code has
 * been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#     Engineer   Description
 * ------------- ----------- ---------- ----------------------------------------
 * Mar 23, 2010  229         Chin Chen  Initial coding Reused some software from
 *                                      com.raytheon.viz.skewt
 * Mar 24, 2011  R1G2-9      Chin Chen  migration
 * Jun 14, 2011  11-5        Chin Chen  migration
 * Mar 11, 2013  972         Greg Hull  rm paneNum and editorNum; rm
 *                                      AbstractNcEditor
 * Mar 25, 2013  972         Greg Hull  rm unused Add/RemoveListeners.
 * Jan 13, 2015  17008/5930  Chin Chen  NSHARP Hodograph Does Not Loop in D2D
 *                                      Lite Configuration
 * Aug 10, 2015  9396        Chin Chen  implement new OPC pane configuration
 * Jan 15, 2018  6746        bsteffen   Do not replace incoming display on swap.
 * Mar 21, 2018  6914        bsteffen   Reuse display panes when changing pane
 *                                      layout.
 * Aug 01, 2018  6858        bsteffen   Clone loop properties on constructor.
 * Sep 28, 2018  7479        bsteffen   Ensure reused panes have valid extents.
 * Nov 05, 2018  6800        bsteffen   Use a new handler when a procedure is loaded.
 * Apr 22, 2019  6660        bsteffen   Apply background color to all panes.
 * 
 * </pre>
 * 
 * @author Chin Chen
 */
public class NsharpEditor extends AbstractEditor
        implements IRenderableDisplayChangedListener {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpEditor.class);

    private boolean restarting = false;

    private static int DISPLAY_Head = 0;

    private int DISPLAY_SKEWT;

    private int DISPLAY_WITO;

    private int DISPLAY_INSET;

    private int DISPLAY_HODO;

    private int DISPLAY_TIMESTN;

    private int DISPLAY_DATA;

    private int DISPLAY_SPC_GRAPHS;

    private int DISPLAY_FUTURE;

    private int DISPLAY_TOTAL;

    public static final String EDITOR_ID = "gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor";

    private NsharpResourceHandler rscHandler;

    private double skewTHeightHintRatio;

    private double skewTWidthHintRatio;

    private double witoHeightHintRatio;

    private double witoWidthHintRatio;

    private double hodoHeightHintRatio;

    private double hodoWidthHintRatio;

    private double insetHeightHintRatio;

    private double insetWidthHintRatio;

    private double timeStnHeightHintRatio;

    private double timeStnWidthHintRatio;

    private double dataHeightHintRatio;

    private double dataWidthHintRatio;

    private double leftGroupWidthRatio;

    private double leftTopGroupHeightRatio;

    private double topGroupHeightRatio, botGroupHeightRatio;

    private String paneConfigurationName;

    private IRenderableDisplay[] displayArray;

    private ResizeListener resizeLsner;

    public NsharpResourceHandler getRscHandler() {
        return rscHandler;
    }

    // Note: nsharpComp used to store composite for each pane.
    private Composite[] nsharpComp;

    private Composite parentComp, baseComposite;

    private Group rightTopGp = null, leftTopGp = null, leftBotGp = null, leftGp,
            rightGp, topGp, botGp;

    /** input managers */
    protected InputManager skewtInputManager;

    protected InputManager hodoInputManager;

    protected InputManager timeStnInputManager;

    protected InputManager dataInputManager;

    protected InputManager insetInputManager;

    protected InputManager spcGraphsInputManager;

    private NsharpSkewTPaneMouseHandler skewtPaneMouseHandler = null;

    private NsharpHodoPaneMouseHandler hodoPaneMouseHandler = null;

    private NsharpTimeStnPaneMouseHandler timeStnPaneMouseHandler = null;

    private NsharpDataPaneMouseHandler dataPaneMouseHandler = null;

    private NsharpAbstractMouseHandler insetPaneMouseHandler = null;

    private NsharpAbstractMouseHandler spcGraphsPaneMouseHandler = null;

    protected VizDisplayPane displayPanes[];

    protected VizDisplayPane selectedPane;

    public static NsharpEditor getActiveNsharpEditor() {
        IEditorPart ep = EditorUtil.getActiveEditor();
        if (ep instanceof NsharpEditor) {
            return (NsharpEditor) ep;
        }

        // It might be desirable to stop here so that we only have an "active"
        // editor if it really is active.
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
            return null;
        }
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }
        IWorkbenchPage activePage = window.getActivePage();
        if (activePage == null) {
            return null;
        }
        IEditorReference[] references = activePage.getEditorReferences();

        for (IEditorReference ref : references) {
            ep = ref.getEditor(false);
            if (ep instanceof NsharpEditor) {
                return (NsharpEditor) ep;
            }
        }
        return null;
    }

    @Override
    public void createPartControl(Composite comp) {
        parentComp = comp;
        if (baseComposite != null) {
            baseComposite.dispose();
        }
        baseComposite = new Composite(comp, SWT.NONE);
        final GridLayout mainGL;
        resizeLsner = new ResizeListener(paneConfigurationName);
        baseComposite.addListener(SWT.Resize, resizeLsner);
        if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_2_STR)) {
            mainGL = new GridLayout(2, false);
            baseComposite.setLayout(mainGL);
            mainGL.horizontalSpacing = 0;
            mainGL.marginHeight = 0;
            leftGroupWidthRatio = NsharpConstants.PANE_DEF_CFG_2_LEFT_GP_WIDTH_RATIO;
            leftTopGroupHeightRatio = NsharpConstants.PANE_DEF_CFG_2_LEFT_TOP_GP_HEIGHT_RATIO;
            skewTHeightHintRatio = NsharpConstants.PANE_DEF_CFG_2_SKEWT_HEIGHT_RATIO;
            skewTWidthHintRatio = NsharpConstants.PANE_DEF_CFG_2_SKEWT_WIDTH_RATIO;
            witoHeightHintRatio = NsharpConstants.PANE_DEF_CFG_2_WITO_HEIGHT_RATIO;
            witoWidthHintRatio = NsharpConstants.PANE_DEF_CFG_2_WITO_WIDTH_RATIO;
            hodoHeightHintRatio = NsharpConstants.PANE_DEF_CFG_2_HODO_HEIGHT_RATIO;
            hodoWidthHintRatio = NsharpConstants.PANE_DEF_CFG_2_HODO_WIDTH_RATIO;
            insetHeightHintRatio = NsharpConstants.PANE_DEF_CFG_2_INSET_HEIGHT_RATIO;
            insetWidthHintRatio = NsharpConstants.PANE_DEF_CFG_2_INSET_WIDTH_RATIO;
            timeStnHeightHintRatio = NsharpConstants.PANE_DEF_CFG_2_TIMESTN_HEIGHT_RATIO;
            timeStnWidthHintRatio = NsharpConstants.PANE_DEF_CFG_2_TIMESTN_WIDTH_RATIO;
            dataHeightHintRatio = NsharpConstants.PANE_DEF_CFG_2_DATA_HEIGHT_RATIO;
            dataWidthHintRatio = NsharpConstants.PANE_DEF_CFG_2_DATA_WIDTH_RATIO;
            createDefConfig2();
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
            mainGL = new GridLayout(2, false);
            mainGL.horizontalSpacing = 0;
            mainGL.marginHeight = 0;
            baseComposite.setLayout(mainGL);
            leftGroupWidthRatio = NsharpConstants.PANE_DEF_CFG_1_LEFT_GP_WIDTH_RATIO;
            leftTopGroupHeightRatio = NsharpConstants.PANE_DEF_CFG_1_LEFT_TOP_GP_HEIGHT_RATIO;
            skewTHeightHintRatio = NsharpConstants.PANE_DEF_CFG_1_SKEWT_HEIGHT_RATIO;
            skewTWidthHintRatio = NsharpConstants.PANE_DEF_CFG_1_SKEWT_WIDTH_RATIO;
            witoHeightHintRatio = NsharpConstants.PANE_DEF_CFG_1_WITO_HEIGHT_RATIO;
            witoWidthHintRatio = NsharpConstants.PANE_DEF_CFG_1_WITO_WIDTH_RATIO;
            hodoHeightHintRatio = NsharpConstants.PANE_DEF_CFG_1_HODO_HEIGHT_RATIO;
            hodoWidthHintRatio = NsharpConstants.PANE_DEF_CFG_1_HODO_WIDTH_RATIO;
            insetHeightHintRatio = NsharpConstants.PANE_DEF_CFG_1_INSET_HEIGHT_RATIO;
            insetWidthHintRatio = NsharpConstants.PANE_DEF_CFG_1_INSET_WIDTH_RATIO;
            timeStnHeightHintRatio = NsharpConstants.PANE_DEF_CFG_1_TIMESTN_HEIGHT_RATIO;
            timeStnWidthHintRatio = NsharpConstants.PANE_DEF_CFG_1_TIMESTN_WIDTH_RATIO;
            dataHeightHintRatio = NsharpConstants.PANE_DEF_CFG_1_DATA_HEIGHT_RATIO;
            dataWidthHintRatio = NsharpConstants.PANE_DEF_CFG_1_DATA_WIDTH_RATIO;
            createDefConfig1();
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
            mainGL = new GridLayout(1, true);
            mainGL.horizontalSpacing = 0;
            mainGL.marginHeight = 0;
            baseComposite.setLayout(mainGL);
            topGroupHeightRatio = NsharpConstants.PANE_SPCWS_CFG_TOP_GP_HEIGHT_RATIO;
            botGroupHeightRatio = NsharpConstants.PANE_SPCWS_CFG_BOT_GP_HEIGHT_RATIO;
            skewTHeightHintRatio = NsharpConstants.PANE_SPCWS_CFG_SKEWT_HEIGHT_RATIO;
            skewTWidthHintRatio = NsharpConstants.PANE_SPCWS_CFG_SKEWT_WIDTH_RATIO;
            witoHeightHintRatio = NsharpConstants.PANE_SPCWS_CFG_WITO_HEIGHT_RATIO;
            witoWidthHintRatio = NsharpConstants.PANE_SPCWS_CFG_WITO_WIDTH_RATIO;
            hodoHeightHintRatio = NsharpConstants.PANE_SPCWS_CFG_HODO_HEIGHT_RATIO;
            hodoWidthHintRatio = NsharpConstants.PANE_SPCWS_CFG_HODO_WIDTH_RATIO;
            insetHeightHintRatio = NsharpConstants.PANE_SPCWS_CFG_INSET_HEIGHT_RATIO;
            insetWidthHintRatio = NsharpConstants.PANE_SPCWS_CFG_INSET_WIDTH_RATIO;
            dataHeightHintRatio = NsharpConstants.PANE_SPCWS_CFG_DATA_HEIGHT_RATIO;
            dataWidthHintRatio = NsharpConstants.PANE_SPCWS_CFG_DATA_WIDTH_RATIO;
            createSPCWsConfig();
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
            mainGL = new GridLayout(1, true);
            mainGL.horizontalSpacing = 0;
            mainGL.marginHeight = 0;
            baseComposite.setLayout(mainGL);
            topGroupHeightRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_TOP_GP_HEIGHT_RATIO;
            botGroupHeightRatio = 1 - topGroupHeightRatio;
            skewTHeightHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_SKEWT_HEIGHT_RATIO;
            skewTWidthHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_SKEWT_WIDTH_RATIO;
            timeStnHeightHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_TIMESTN_HEIGHT_RATIO;
            timeStnWidthHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_TIMESTN_WIDTH_RATIO;
            hodoHeightHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_HODO_HEIGHT_RATIO;
            hodoWidthHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_HODO_WIDTH_RATIO;
            dataHeightHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_DATA_HEIGHT_RATIO;
            dataWidthHintRatio = NsharpConstants.PANE_SIMPLE_D2D_CFG_DATA_WIDTH_RATIO;
            createSimpleD2DConfig();
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            mainGL = new GridLayout(1, true);
            mainGL.horizontalSpacing = 0;
            mainGL.marginHeight = 0;
            baseComposite.setLayout(mainGL);
            topGroupHeightRatio = 1;
            botGroupHeightRatio = 0;
            skewTHeightHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_SKEWT_HEIGHT_RATIO;
            skewTWidthHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_SKEWT_WIDTH_RATIO;
            hodoHeightHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_HODO_HEIGHT_RATIO;
            hodoWidthHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_HODO_WIDTH_RATIO;
            timeStnHeightHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_TIMESTN_HEIGHT_RATIO;
            timeStnWidthHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_TIMESTN_WIDTH_RATIO;
            dataWidthHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_DATA_WIDTH_RATIO;
            dataHeightHintRatio = NsharpConstants.PANE_LITE_D2D_CFG_DATA_HEIGHT_RATIO;
            createLiteD2DConfig();
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            mainGL = new GridLayout(1, true);
            mainGL.horizontalSpacing = 0;
            mainGL.marginHeight = 0;
            baseComposite.setLayout(mainGL);
            topGroupHeightRatio = 1;
            botGroupHeightRatio = 0;
            skewTHeightHintRatio = NsharpConstants.PANE_OPC_CFG_SKEWT_HEIGHT_RATIO;
            skewTWidthHintRatio = NsharpConstants.PANE_OPC_CFG_SKEWT_WIDTH_RATIO;
            hodoHeightHintRatio = NsharpConstants.PANE_OPC_CFG_HODO_HEIGHT_RATIO;
            hodoWidthHintRatio = NsharpConstants.PANE_OPC_CFG_HODO_WIDTH_RATIO;
            timeStnHeightHintRatio = NsharpConstants.PANE_OPC_CFG_TIMESTN_HEIGHT_RATIO;
            timeStnWidthHintRatio = NsharpConstants.PANE_OPC_CFG_TIMESTN_WIDTH_RATIO;
            dataWidthHintRatio = NsharpConstants.PANE_OPC_CFG_DATA_WIDTH_RATIO;
            dataHeightHintRatio = NsharpConstants.PANE_OPC_CFG_DATA_HEIGHT_RATIO;
            /* share d2d lite code */
            createLiteD2DConfig();
        }
        skewtInputManager = new InputManager(this);
        timeStnInputManager = new InputManager(this);
        dataInputManager = new InputManager(this);
        insetInputManager = new InputManager(this);
        spcGraphsInputManager = new InputManager(this);
        try {
            for (int i = 0; i < displayPanes.length; i++) {
                if (displayPanes[i] == null && nsharpComp[i] != null) {
                    displayPanes[i] = new VizDisplayPane(this, nsharpComp[i],
                            displaysToLoad[i]);

                    displayPanes[i].setRenderableDisplay(displaysToLoad[i]);
                }
            }
            registerHandlers();
            // set default selected pane to skewT pane
            selectedPane = displayPanes[0];

            for (int i = 0; i < displayPanes.length; i++) {
                if (displayPanes[i] != null) {
                    displayPanes[i].addListener(SWT.MouseEnter,
                            new PaneMouseListener(i));
                }
            }
        } catch (Exception e) {
            final String errMsg = "Error setting up NsharpEditor";
            UFStatus.getHandler().handle(Priority.SIGNIFICANT, errMsg, e);
        }
        if (!restarting) {
            contributePerspectiveActions();
        }

    }

    private void createSPCWsConfig() {
        topGp = new Group(baseComposite, SWT.NONE);
        GridData topGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        topGp.setLayoutData(topGpGd);
        GridLayout topGpLayout = new GridLayout(3, false);
        topGpLayout.marginWidth = 0;
        topGpLayout.marginHeight = 0;
        topGpLayout.verticalSpacing = 0;
        topGp.setLayout(topGpLayout);

        // skewt composite
        Composite skewtComp = new Composite(topGp, SWT.NONE);
        GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        skewtComp.setLayoutData(skewtGd);
        GridLayout skewtLayout = new GridLayout(1, true);
        skewtLayout.marginWidth = 0;
        skewtLayout.marginHeight = 0;
        skewtLayout.verticalSpacing = 0;
        skewtComp.setLayout(skewtLayout);
        nsharpComp[DISPLAY_SKEWT] = skewtComp;
        // wito composite
        Composite witoComp = new Composite(topGp, SWT.NONE);
        GridData witoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        witoComp.setLayoutData(witoGd);
        GridLayout witoLayout = new GridLayout(1, true);
        witoLayout.marginWidth = 0;
        witoLayout.marginHeight = 0;
        witoLayout.verticalSpacing = 0;
        witoComp.setLayout(witoLayout);
        nsharpComp[DISPLAY_WITO] = witoComp;

        // right-top group : right part of top group
        rightTopGp = new Group(topGp, SWT.NONE);
        GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightTopGp.setLayoutData(rightTopGpGd);
        GridLayout rightTopGpLayout = new GridLayout(1, true);
        rightTopGpLayout.marginWidth = 0;
        rightTopGpLayout.marginHeight = 0;
        rightTopGpLayout.verticalSpacing = 0;
        rightTopGp.setLayout(rightTopGpLayout);

        // hodo composite
        Composite hodoComp = new Composite(rightTopGp, SWT.NONE);
        GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        hodoComp.setLayoutData(hodoGd);
        GridLayout hodoLayout = new GridLayout(1, true);
        hodoLayout.marginHeight = 0;
        hodoLayout.marginWidth = 0;
        hodoLayout.verticalSpacing = 0;
        hodoComp.setLayout(hodoLayout);
        nsharpComp[DISPLAY_HODO] = hodoComp;

        // inset composite
        Composite insetComp = new Composite(rightTopGp, SWT.NONE);
        GridData insetGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        insetComp.setLayoutData(insetGd);
        GridLayout insetLayout = new GridLayout(1, true);
        insetLayout.marginHeight = 0;
        insetLayout.marginWidth = 0;
        insetComp.setLayout(insetLayout);
        nsharpComp[DISPLAY_INSET] = insetComp;

        botGp = new Group(baseComposite, SWT.NONE);
        GridData botGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        botGp.setLayoutData(botGpGd);
        GridLayout botGpLayout = new GridLayout(2, true);
        botGpLayout.marginWidth = 0;
        botGpLayout.marginHeight = 0;
        botGpLayout.verticalSpacing = 0;
        botGp.setLayout(botGpLayout);
        // data composite
        Composite dataComp = new Composite(botGp, SWT.NONE);
        GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        dataComp.setLayoutData(dataGd);
        GridLayout dataLayout = new GridLayout(1, true);
        dataLayout.marginHeight = 0;
        dataLayout.marginWidth = 0;
        dataComp.setLayout(dataLayout);
        nsharpComp[DISPLAY_DATA] = dataComp;
        // spc composite
        Composite spcComp = new Composite(botGp, SWT.NONE);
        GridData spcGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        spcComp.setLayoutData(spcGd);
        GridLayout spcLayout = new GridLayout(1, true);
        spcLayout.marginHeight = 0;
        spcLayout.marginWidth = 0;
        spcComp.setLayout(spcLayout);
        nsharpComp[DISPLAY_SPC_GRAPHS] = spcComp;
    }

    private void createSimpleD2DConfig() {
        topGp = new Group(baseComposite, SWT.NONE);
        GridData topGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        topGp.setLayoutData(topGpGd);
        GridLayout topGpLayout = new GridLayout(2, false);
        topGpLayout.marginWidth = 0;
        topGpLayout.marginHeight = 0;
        topGpLayout.verticalSpacing = 0;
        topGp.setLayout(topGpLayout);

        // skewt composite
        Composite skewtComp = new Composite(topGp, SWT.NONE);
        GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        skewtComp.setLayoutData(skewtGd);
        GridLayout skewtLayout = new GridLayout(1, true);
        skewtLayout.marginWidth = 0;
        skewtLayout.marginHeight = 0;
        skewtLayout.verticalSpacing = 0;
        skewtComp.setLayout(skewtLayout);
        nsharpComp[DISPLAY_SKEWT] = skewtComp;

        // right-top group : right part of top group
        rightTopGp = new Group(topGp, SWT.NONE);
        GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightTopGp.setLayoutData(rightTopGpGd);
        GridLayout rightTopGpLayout = new GridLayout(1, true);
        rightTopGpLayout.marginWidth = 0;
        rightTopGpLayout.marginHeight = 0;
        rightTopGpLayout.verticalSpacing = 0;
        rightTopGp.setLayout(rightTopGpLayout);

        // time-stn composite
        Composite timeStnComp = new Composite(rightTopGp, SWT.NONE);
        GridData timeStnGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        timeStnComp.setLayoutData(timeStnGd);
        GridLayout timeStnLayout = new GridLayout(1, true);
        timeStnLayout.marginHeight = 0;
        timeStnLayout.marginWidth = 0;
        timeStnLayout.verticalSpacing = 0;
        timeStnComp.setLayout(timeStnLayout);
        nsharpComp[DISPLAY_TIMESTN] = timeStnComp;

        // future composite
        Composite futureComp = new Composite(rightTopGp, SWT.NONE);
        GridData fuGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        futureComp.setLayoutData(fuGd);
        GridLayout futureLayout = new GridLayout(1, true);
        futureLayout.marginHeight = 0;
        futureLayout.marginWidth = 0;
        futureComp.setLayout(futureLayout);
        nsharpComp[DISPLAY_FUTURE] = futureComp;

        botGp = new Group(baseComposite, SWT.NONE);
        GridData botGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        botGp.setLayoutData(botGpGd);
        GridLayout botGpLayout = new GridLayout(2, false);
        botGpLayout.marginWidth = 0;
        botGpLayout.marginHeight = 0;
        botGpLayout.verticalSpacing = 0;
        botGp.setLayout(botGpLayout);

        // hodo composite
        Composite hodoComp = new Composite(botGp, SWT.NONE);
        GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        hodoComp.setLayoutData(hodoGd);
        GridLayout hodoLayout = new GridLayout(1, true);
        hodoLayout.marginHeight = 0;
        hodoLayout.marginWidth = 0;
        hodoLayout.verticalSpacing = 0;
        hodoComp.setLayout(hodoLayout);
        nsharpComp[DISPLAY_HODO] = hodoComp;

        // data composite
        Composite dataComp = new Composite(botGp, SWT.NONE);
        GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        dataComp.setLayoutData(dataGd);
        GridLayout dataLayout = new GridLayout(1, true);
        dataLayout.marginHeight = 0;
        dataLayout.marginWidth = 0;
        dataComp.setLayout(dataLayout);
        nsharpComp[DISPLAY_DATA] = dataComp;
    }

    private void createLiteD2DConfig() {
        topGp = new Group(baseComposite, SWT.NONE);
        GridData topGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        topGp.setLayoutData(topGpGd);
        GridLayout topGpLayout = new GridLayout(2, false);
        topGpLayout.marginWidth = 0;
        topGpLayout.marginHeight = 0;
        topGpLayout.verticalSpacing = 0;
        topGp.setLayout(topGpLayout);
        if (rscHandler.getCurrentGraphMode() != NsharpConstants.GRAPH_HODO) {
            // skewt composite
            Composite skewtComp = new Composite(topGp, SWT.NONE);
            GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
            skewtComp.setLayoutData(skewtGd);
            GridLayout skewtLayout = new GridLayout(1, true);
            skewtLayout.marginWidth = 0;
            skewtLayout.marginHeight = 0;
            skewtLayout.verticalSpacing = 0;
            skewtComp.setLayout(skewtLayout);
            nsharpComp[DISPLAY_SKEWT] = skewtComp;
        } else {
            // hodo composite
            Composite hodoComp = new Composite(topGp, SWT.NONE);
            GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, false);
            hodoComp.setLayoutData(hodoGd);
            GridLayout hodoLayout = new GridLayout(1, true);
            hodoLayout.marginHeight = 0;
            hodoLayout.marginWidth = 0;
            hodoLayout.verticalSpacing = 0;
            hodoComp.setLayout(hodoLayout);
            nsharpComp[DISPLAY_HODO] = hodoComp;
        }

        // only has right group
        rightTopGp = new Group(topGp, SWT.NONE);
        GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightTopGp.setLayoutData(rightTopGpGd);
        GridLayout rightTopGpLayout = new GridLayout(1, true);
        rightTopGpLayout.marginWidth = 0;
        rightTopGpLayout.marginHeight = 0;
        rightTopGpLayout.verticalSpacing = 0;
        rightTopGp.setLayout(rightTopGpLayout);

        // time-stn composite
        Composite timeStnComp = new Composite(rightTopGp, SWT.NONE);
        GridData timeStnGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        timeStnComp.setLayoutData(timeStnGd);
        GridLayout timeStnLayout = new GridLayout(1, true);
        timeStnLayout.marginHeight = 0;
        timeStnLayout.marginWidth = 0;
        timeStnLayout.verticalSpacing = 0;
        timeStnComp.setLayout(timeStnLayout);
        nsharpComp[DISPLAY_TIMESTN] = timeStnComp;

        // data composite
        Composite dataComp = new Composite(rightTopGp, SWT.NONE);
        GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        dataComp.setLayoutData(dataGd);
        GridLayout dataLayout = new GridLayout(1, true);
        dataLayout.marginHeight = 0;
        dataLayout.marginWidth = 0;
        dataComp.setLayout(dataLayout);
        nsharpComp[DISPLAY_DATA] = dataComp;

    }

    private void createDefConfig2() {
        leftGp = new Group(baseComposite, SWT.NONE);
        GridData leftGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftGp.setLayoutData(leftGpGd);
        GridLayout leftGpLayout = new GridLayout(1, true);
        leftGpLayout.marginWidth = 0;
        leftGpLayout.marginHeight = 0;
        leftGpLayout.verticalSpacing = 0;
        leftGp.setLayout(leftGpLayout);

        // left-top group : upper part of left group
        leftTopGp = new Group(leftGp, SWT.NONE);
        GridData leftTopGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftTopGp.setLayoutData(leftTopGpGd);
        GridLayout leftTopGpLayout = new GridLayout(2, false);
        leftTopGpLayout.marginWidth = 0;
        leftTopGpLayout.marginHeight = 0;
        leftTopGpLayout.verticalSpacing = 0;
        leftTopGp.setLayout(leftTopGpLayout);

        // skewt composite
        Composite skewtComp = new Composite(leftTopGp, SWT.NONE);
        GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        skewtComp.setLayoutData(skewtGd);
        GridLayout skewtLayout = new GridLayout(1, true);
        skewtLayout.marginWidth = 0;
        skewtLayout.marginHeight = 0;
        skewtLayout.verticalSpacing = 0;
        skewtComp.setLayout(skewtLayout);
        nsharpComp[DISPLAY_SKEWT] = skewtComp;
        // wito composite
        Composite witoComp = new Composite(leftTopGp, SWT.NONE);
        GridData witoGd = new GridData(SWT.END, SWT.FILL, false, true);
        witoComp.setLayoutData(witoGd);
        GridLayout witoLayout = new GridLayout(1, true);
        witoLayout.marginWidth = 0;
        witoLayout.marginHeight = 0;
        witoLayout.verticalSpacing = 0;
        witoComp.setLayout(witoLayout);
        nsharpComp[DISPLAY_WITO] = witoComp;
        // inset composite
        Composite insetComp = new Composite(leftGp, SWT.NONE);
        GridData insetGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        insetComp.setLayoutData(insetGd);
        GridLayout insetLayout = new GridLayout(1, true);
        insetLayout.marginHeight = 0;
        insetLayout.marginWidth = 0;
        insetComp.setLayout(insetLayout);
        nsharpComp[DISPLAY_INSET] = insetComp;
        // right group
        rightGp = new Group(baseComposite, SWT.NONE);
        GridData rightGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightGp.setLayoutData(rightGpGd);
        GridLayout rightGpLayout = new GridLayout(1, true);
        rightGpLayout.marginWidth = 0;
        rightGpLayout.marginHeight = 0;
        rightGpLayout.verticalSpacing = 0;
        rightGp.setLayout(rightGpLayout);

        // right-top group : upper part of right group
        rightTopGp = new Group(rightGp, SWT.NONE);
        GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightTopGp.setLayoutData(rightTopGpGd);
        GridLayout rightTopGpLayout = new GridLayout(2, false);
        rightTopGpLayout.marginWidth = 0;
        rightTopGpLayout.marginHeight = 0;
        rightTopGpLayout.verticalSpacing = 0;
        rightTopGp.setLayout(rightTopGpLayout);
        // hodo composite
        Composite hodoComp = new Composite(rightTopGp, SWT.NONE);
        GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        hodoComp.setLayoutData(hodoGd);
        GridLayout hodoLayout = new GridLayout(1, true);
        hodoLayout.marginHeight = 0;
        hodoLayout.marginWidth = 0;
        hodoLayout.verticalSpacing = 0;
        hodoComp.setLayout(hodoLayout);
        nsharpComp[DISPLAY_HODO] = hodoComp;
        // time-stn composite
        Composite timeStnComp = new Composite(rightTopGp, SWT.NONE);
        GridData timeStnGd = new GridData(SWT.END, SWT.FILL, false, true);
        timeStnComp.setLayoutData(timeStnGd);
        GridLayout timeStnLayout = new GridLayout(1, true);
        timeStnLayout.marginHeight = 0;
        timeStnLayout.marginWidth = 0;
        timeStnLayout.verticalSpacing = 0;
        timeStnComp.setLayout(timeStnLayout);
        nsharpComp[DISPLAY_TIMESTN] = timeStnComp;
        // data composite
        Composite dataComp = new Composite(rightGp, SWT.NONE);
        GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        dataComp.setLayoutData(dataGd);
        GridLayout dataLayout = new GridLayout(1, true);
        dataLayout.marginHeight = 0;
        dataLayout.marginWidth = 0;
        dataComp.setLayout(dataLayout);
        nsharpComp[DISPLAY_DATA] = dataComp;
    }

    private void createDefConfig1() {
        leftGp = new Group(baseComposite, SWT.NONE);
        GridData leftGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);

        leftGp.setLayoutData(leftGpGd);
        GridLayout leftGpLayout = new GridLayout(1, true);
        leftGpLayout.marginWidth = 0;
        leftGpLayout.marginHeight = 0;
        leftGpLayout.verticalSpacing = 0;
        leftGp.setLayout(leftGpLayout);

        // left-top group : upper part of left group
        leftTopGp = new Group(leftGp, SWT.NONE);
        GridData leftTopGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftTopGp.setLayoutData(leftTopGpGd);
        GridLayout leftTopGpLayout = new GridLayout(2, false);
        leftTopGpLayout.marginWidth = 0;
        leftTopGpLayout.marginHeight = 0;
        leftTopGpLayout.verticalSpacing = 0;
        leftTopGp.setLayout(leftTopGpLayout);

        // skewt composite
        Composite skewtComp = new Composite(leftTopGp, SWT.NONE);
        GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        skewtComp.setLayoutData(skewtGd);
        GridLayout skewtLayout = new GridLayout(1, true);
        skewtLayout.marginWidth = 0;
        skewtLayout.marginHeight = 0;
        skewtLayout.verticalSpacing = 0;
        skewtComp.setLayout(skewtLayout);
        nsharpComp[DISPLAY_SKEWT] = skewtComp;
        // wito composite
        Composite witoComp = new Composite(leftTopGp, SWT.NONE);
        GridData witoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        witoComp.setLayoutData(witoGd);
        GridLayout witoLayout = new GridLayout(1, true);
        witoLayout.marginWidth = 0;
        witoLayout.marginHeight = 0;
        witoLayout.verticalSpacing = 0;
        witoComp.setLayout(witoLayout);
        nsharpComp[DISPLAY_WITO] = witoComp;
        // left-bottom group
        leftBotGp = new Group(leftGp, SWT.NONE);
        GridData leftBotGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftBotGp.setLayoutData(leftBotGpGd);
        GridLayout leftBotGpLayout = new GridLayout(2, true);
        leftBotGpLayout.marginWidth = 0;
        leftBotGpLayout.marginHeight = 0;
        leftBotGpLayout.verticalSpacing = 0;
        leftBotGp.setLayout(leftBotGpLayout);
        // time-stn composite
        Composite timeStnComp = new Composite(leftBotGp, SWT.NONE);
        GridData timeStnGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        timeStnComp.setLayoutData(timeStnGd);
        GridLayout timeStnLayout = new GridLayout(1, true);
        timeStnLayout.marginHeight = 0;
        timeStnLayout.marginWidth = 0;
        timeStnLayout.verticalSpacing = 0;
        timeStnComp.setLayout(timeStnLayout);
        nsharpComp[DISPLAY_TIMESTN] = timeStnComp;
        // inset composite
        Composite insetComp = new Composite(leftBotGp, SWT.NONE);
        GridData insetGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        insetComp.setLayoutData(insetGd);
        GridLayout insetLayout = new GridLayout(1, true);
        insetLayout.marginHeight = 0;
        insetLayout.marginWidth = 0;
        insetComp.setLayout(insetLayout);
        nsharpComp[DISPLAY_INSET] = insetComp;
        // right group
        rightGp = new Group(baseComposite, SWT.NONE);
        GridData rightGpGd = new GridData(SWT.END, SWT.FILL, true, true);

        rightGp.setLayoutData(rightGpGd);
        GridLayout rightGpLayout = new GridLayout(1, true);
        rightGpLayout.marginWidth = 0;
        rightGpLayout.marginHeight = 0;
        rightGpLayout.verticalSpacing = 0;
        rightGp.setLayout(rightGpLayout);

        // hodo composite
        Composite hodoComp = new Composite(rightGp, SWT.NONE);
        GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        hodoComp.setLayoutData(hodoGd);
        GridLayout hodoLayout = new GridLayout(1, true);
        hodoLayout.marginHeight = 0;
        hodoLayout.marginWidth = 0;
        hodoLayout.verticalSpacing = 0;
        hodoComp.setLayout(hodoLayout);
        nsharpComp[DISPLAY_HODO] = hodoComp;
        // data composite
        Composite dataComp = new Composite(rightGp, SWT.NONE);
        GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        dataComp.setLayoutData(dataGd);
        GridLayout dataLayout = new GridLayout(1, true);
        dataLayout.marginHeight = 1;
        dataLayout.marginWidth = 0;
        dataComp.setLayout(dataLayout);
        nsharpComp[DISPLAY_DATA] = dataComp;
    }

    protected void registerHandlers() {
        for (int i = 0; i < displayPanes.length; i++) {
            if (displayPanes[i] != null) {
                IDisplayPane pane = displayPanes[i];
                InputHandlerDefaultImpl mouseHandler = null;
                InputManager inputMgr;
                // Enable the mouse inspect adapter
                if (i == DISPLAY_SKEWT) {
                    skewtPaneMouseHandler = new NsharpSkewTPaneMouseHandler(
                            this, pane);
                    mouseHandler = skewtPaneMouseHandler;
                    inputMgr = skewtInputManager;
                } else if (i == DISPLAY_HODO) {
                    hodoInputManager = new InputManager(this);
                    hodoPaneMouseHandler = new NsharpHodoPaneMouseHandler(this,
                            pane);
                    mouseHandler = hodoPaneMouseHandler;
                    inputMgr = hodoInputManager;
                } else if (i == DISPLAY_TIMESTN) {
                    timeStnPaneMouseHandler = new NsharpTimeStnPaneMouseHandler(
                            this, pane);
                    mouseHandler = timeStnPaneMouseHandler;
                    inputMgr = timeStnInputManager;
                } else if (i == DISPLAY_INSET) {
                    insetPaneMouseHandler = new NsharpAbstractMouseHandler(this,
                            pane);
                    mouseHandler = insetPaneMouseHandler;
                    inputMgr = insetInputManager;
                } else if (i == DISPLAY_SPC_GRAPHS) {
                    spcGraphsPaneMouseHandler = new NsharpAbstractMouseHandler(
                            this, pane);
                    mouseHandler = spcGraphsPaneMouseHandler;
                    inputMgr = spcGraphsInputManager;
                } else if (i == DISPLAY_DATA) {
                    dataPaneMouseHandler = new NsharpDataPaneMouseHandler(this,
                            pane);
                    mouseHandler = dataPaneMouseHandler;
                    inputMgr = dataInputManager;
                } else {
                    continue;
                }

                inputMgr.registerMouseHandler(mouseHandler);

                pane.addListener(SWT.MouseUp, inputMgr);
                pane.addListener(SWT.MouseDown, inputMgr);
                pane.addListener(SWT.MouseMove, inputMgr);
                pane.addListener(SWT.MouseWheel, inputMgr);
                pane.addListener(SWT.MouseHover, inputMgr);
                pane.addListener(SWT.MouseEnter, inputMgr);
                pane.addListener(SWT.MouseExit, inputMgr);
                pane.addListener(SWT.MenuDetect, inputMgr);
                pane.addListener(SWT.KeyUp, inputMgr);
                pane.addListener(SWT.KeyDown, inputMgr);
            }
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput editorInput)
            throws PartInitException {
        NsharpConfigManager configMgr = NsharpConfigManager.getInstance();
        NsharpConfigStore configStore = configMgr
                .retrieveNsharpConfigStoreFromFs();
        NsharpGraphProperty graphConfigProperty = configStore
                .getGraphProperty();
        paneConfigurationName = graphConfigProperty.getPaneConfigurationName();
        if (!paneConfigurationName.equals(NsharpConstants.PANE_SPCWS_CFG_STR)
                && !paneConfigurationName
                        .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)
                && !paneConfigurationName
                        .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                && !paneConfigurationName
                        .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            paneConfigurationName = NsharpConstants.PANE_SIMPLE_D2D_CFG_STR;
        }
        initPaneIndices();
        EditorInput edInput = (EditorInput) editorInput;

        displayArray = createRenderableDisplayArray(
                edInput.getRenderableDisplays());
        nsharpComp = new Composite[DISPLAY_TOTAL];
        displayPanes = new VizDisplayPane[DISPLAY_TOTAL];

        for (IRenderableDisplay display : displayArray) {
            // attempt to find and reuse rscHandler if possible
            List<NsharpAbstractPaneResource> paneRscs = display.getDescriptor()
                    .getResourceList()
                    .getResourcesByTypeAsType(NsharpAbstractPaneResource.class);
            for (NsharpAbstractPaneResource paneRsc : paneRscs) {
                NsharpResourceHandler handler = paneRsc.getRscHandler();
                if (handler != null) {
                    rscHandler = handler;
                    break;
                }
            }
            if (rscHandler != null) {
                break;
            }
        }

        edInput.setRenderableDisplays(displayArray);

        super.init(site, edInput);

        this.setTabTitle("NsharpEditor");

        // Note: NsharpResourceHandler should be created after editor is
        // created, so all display pane properties and
        // pane resource are also constructed
        // bsteffen: only create rscHandler if it doesn't exist already
        if (rscHandler == null) {
            rscHandler = new NsharpResourceHandler(displayArray, this);
        } else {
            rscHandler.updateDisplay(displayArray, paneConfigurationName);

        }
        NsharpPaletteWindow paletteWin = NsharpPaletteWindow.getInstance();
        if (paletteWin != null) {
            paletteWin.restorePaletteWindow(paneConfigurationName,
                    rscHandler.getCurrentGraphMode(),
                    rscHandler.isInterpolateIsOn(), rscHandler.isOverlayIsOn(),
                    rscHandler.isCompareStnIsOn(), rscHandler.isCompareTmIsOn(),
                    rscHandler.isEditGraphOn(), rscHandler.isCompareSndIsOn());
        }
        createPaneResource();

        rscHandler.resetData();
        // listen for changes to renderable displays
        addRenderableDisplayChangedListener(this);
        // add a new part listener if not added yet
        NsharpPartListener.addPartListener();

    }

    @Override
    public void setFocus() {
        if (selectedPane != null) {
            selectedPane.setFocus();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (this) {
            if (skewtPaneMouseHandler != null && skewtInputManager != null) {
                skewtPaneMouseHandler.setEditor(null);
                skewtPaneMouseHandler.disposeCursor();
                skewtInputManager.unregisterMouseHandler(skewtPaneMouseHandler);
                skewtPaneMouseHandler = null;
                skewtInputManager = null;
            }
            if (hodoPaneMouseHandler != null && hodoInputManager != null) {
                hodoPaneMouseHandler.setEditor(null);
                hodoInputManager.unregisterMouseHandler(hodoPaneMouseHandler);
                hodoPaneMouseHandler = null;
                hodoInputManager = null;
            }
            if (dataPaneMouseHandler != null && dataInputManager != null) {
                dataPaneMouseHandler.setEditor(null);
                dataInputManager.unregisterMouseHandler(dataPaneMouseHandler);
                dataPaneMouseHandler = null;
                dataInputManager = null;
            }
            if (insetPaneMouseHandler != null && insetInputManager != null) {
                insetPaneMouseHandler.setEditor(null);
                insetInputManager.unregisterMouseHandler(insetPaneMouseHandler);
                insetPaneMouseHandler = null;
                insetInputManager = null;
            }
            if (spcGraphsPaneMouseHandler != null
                    && spcGraphsInputManager != null) {
                spcGraphsPaneMouseHandler.setEditor(null);
                spcGraphsInputManager
                        .unregisterMouseHandler(spcGraphsPaneMouseHandler);
                spcGraphsPaneMouseHandler = null;
                spcGraphsInputManager = null;
            }
            if (timeStnPaneMouseHandler != null
                    && timeStnInputManager != null) {
                timeStnPaneMouseHandler.setEditor(null);
                timeStnInputManager
                        .unregisterMouseHandler(timeStnPaneMouseHandler);
                timeStnPaneMouseHandler = null;
                timeStnInputManager = null;
            }
            // bsteffen only dispose the rscHandler if not swapping.
            if (!displayArray[DISPLAY_Head].isSwapping()) {
                if (rscHandler != null) {
                    rscHandler.disposeInternal();
                }
            }
            rscHandler = null;
        }
        // }
        IWorkbenchPage wpage = getSite().getPage();
        IViewPart vpart = wpage
                .findView("gov.noaa.nws.ncep.ui.nsharp.defaultview1");
        if (vpart != null) {
            wpage.hideView(vpart);
        }

    }

    public void restartEditor(String paneConfigurationName) {
        if (rscHandler != null
                && !this.paneConfigurationName.equals(paneConfigurationName)) {
            rscHandler.setPaneConfigurationName(paneConfigurationName);
        }
        this.paneConfigurationName = paneConfigurationName;

        if (skewtPaneMouseHandler != null && skewtInputManager != null) {
            skewtPaneMouseHandler.setEditor(null);
            skewtInputManager.unregisterMouseHandler(skewtPaneMouseHandler);
            skewtPaneMouseHandler = null;
            skewtInputManager = null;
        }
        if (hodoPaneMouseHandler != null && hodoInputManager != null) {
            hodoPaneMouseHandler.setEditor(null);
            hodoInputManager.unregisterMouseHandler(hodoPaneMouseHandler);
            hodoPaneMouseHandler = null;
            hodoInputManager = null;
        }
        if (dataPaneMouseHandler != null && dataInputManager != null) {
            dataPaneMouseHandler.setEditor(null);
            dataInputManager.unregisterMouseHandler(dataPaneMouseHandler);
            dataPaneMouseHandler = null;
            dataInputManager = null;
        }
        if (insetPaneMouseHandler != null && insetInputManager != null) {
            insetPaneMouseHandler.setEditor(null);
            insetInputManager.unregisterMouseHandler(insetPaneMouseHandler);
            insetPaneMouseHandler = null;
            insetInputManager = null;
        }
        if (spcGraphsPaneMouseHandler != null
                && spcGraphsInputManager != null) {
            spcGraphsPaneMouseHandler.setEditor(null);
            spcGraphsInputManager
                    .unregisterMouseHandler(spcGraphsPaneMouseHandler);
            spcGraphsPaneMouseHandler = null;
            spcGraphsInputManager = null;
        }
        if (timeStnPaneMouseHandler != null && timeStnInputManager != null) {
            timeStnPaneMouseHandler.setEditor(null);
            timeStnInputManager.unregisterMouseHandler(timeStnPaneMouseHandler);
            timeStnPaneMouseHandler = null;
            timeStnInputManager = null;
        }
        baseComposite.removeListener(SWT.Resize, resizeLsner);
        rightTopGp = leftTopGp = leftBotGp = leftGp = rightGp = topGp = botGp = null;
        updateEditor();
        restarting = true;
        createPartControl(parentComp);
        editorInput.setRenderableDisplays(getRenderableDisplays());
        // Chin: note: after reset all resource in editor, editor displays an
        // empty screen. Refresh() does not work.
        // Therefore, play the following trick. I.e. bring map editor to top and
        // then this editor to top. After this trick,
        // editor displays normally.
        parentComp.layout();
        NsharpPaletteWindow paletteWin = NsharpPaletteWindow.getInstance();
        if (paletteWin != null) {
            paletteWin.updateSpecialGraphBtn(paneConfigurationName);
        }
    }

    /**
     * Perform a refresh asynchronously
     * 
     */
    @Override
    public void refresh() {
        if (displayPanes != null) {
            for (IDisplayPane pane : displayPanes) {
                if (pane != null) {
                    pane.refresh();
                }
            }
        }
    }

    @Override
    public Coordinate translateClick(double x, double y) {
        double[] grid = getActiveDisplayPane().screenToGrid(x, y, 0);

        return new Coordinate(grid[0], grid[1], grid[2]);
    }

    @Override
    public double[] translateInverseClick(Coordinate c) {

        if (Double.isNaN(c.z)) {
            c.z = 0.0;
        }
        return getActiveDisplayPane()
                .gridToScreen(new double[] { c.x, c.y, c.z });
    }

    public void resetGraph() {
        for (int i = 0; i < DISPLAY_TOTAL; i++) {
            if (displayPanes[i] != null) {
                displayPanes[i].getRenderableDisplay().getExtent().reset();
                displayPanes[i].getRenderableDisplay().zoom(1.0);
                displayPanes[i].getRenderableDisplay().refresh();
            }
        }
        if (rscHandler != null) {
            if (rscHandler.getWitoPaneRsc() != null) {
                rscHandler.getWitoPaneRsc().createAllWireFrameShapes();
            }
        }
    }

    @Override
    public void registerMouseHandler(IInputHandler handler,
            IInputHandler.InputPriority priority) {
        if (skewtInputManager != null) {
            if (handler instanceof NsharpSkewTPaneMouseHandler) {
                if (skewtPaneMouseHandler != null) {
                    skewtInputManager
                            .unregisterMouseHandler(skewtPaneMouseHandler);
                    skewtPaneMouseHandler.disposeCursor();
                }
                skewtPaneMouseHandler = (NsharpSkewTPaneMouseHandler) handler;
                skewtPaneMouseHandler.setEditor(this);
                skewtPaneMouseHandler
                        .setCurrentPane(displayPanes[DISPLAY_SKEWT]);

            }
            skewtInputManager.registerMouseHandler(handler, priority);
        }
    }

    /**
     * Register a mouse handler to a map
     * 
     * @param handler
     *            the handler to register
     */
    @Override
    public void registerMouseHandler(IInputHandler handler) {
        if (skewtInputManager != null) {
            if (handler instanceof NsharpSkewTPaneMouseHandler) {
                if (skewtPaneMouseHandler != null) {
                    skewtInputManager
                            .unregisterMouseHandler(skewtPaneMouseHandler);
                    skewtPaneMouseHandler.disposeCursor();
                }
                skewtPaneMouseHandler = (NsharpSkewTPaneMouseHandler) handler;
                skewtPaneMouseHandler.setEditor(this);
                skewtPaneMouseHandler
                        .setCurrentPane(displayPanes[DISPLAY_SKEWT]);
            }
            skewtInputManager.registerMouseHandler(handler);
        }
    }

    /**
     * Unregister a mouse handler to a map
     * 
     * @param handler
     *            the handler to unregister
     */
    @Override
    public void unregisterMouseHandler(IInputHandler handler) {
        // already unregistered
        if (skewtInputManager != null) {
            skewtInputManager.unregisterMouseHandler(handler);
        }
    }

    @Override
    public IDisplayPane[] getDisplayPanes() {
        // changed for D2D
        if (displayPanes.length <= 0) {
            return displayPanes;
        }
        return new IDisplayPane[] { displayPanes[DISPLAY_Head] };
    }

    public IRenderableDisplay[] getRenderableDisplays() {
        IRenderableDisplay[] displays = new IRenderableDisplay[displayPanes.length];
        int i = 0;
        for (IDisplayPane pane : displayPanes) {
            if (pane != null) {
                displays[i] = pane.getRenderableDisplay();
            }
            i += 1;
        }
        return displays;
    }

    @Override
    public IEditorInput getEditorInput() {
        return editorInput;
    }

    @Override
    public InputManager getMouseManager() {
        return skewtInputManager;
    }

    @Override
    public IDisplayPane getActiveDisplayPane() {
        return selectedPane;
    }

    @Override
    public NCLoopProperties getLoopProperties() {
        LoopProperties loopProperties = this.editorInput.getLoopProperties();
        if (!(loopProperties instanceof NCLoopProperties)) {
            this.editorInput
                    .setLoopProperties(new NCLoopProperties(loopProperties));
            loopProperties = this.editorInput.getLoopProperties();
        }
        return (NCLoopProperties) loopProperties;
    }

    @Override
    protected PaneManager getNewPaneManager() {
        return null;
    }

    private void initPaneIndices() {
        if (paneConfigurationName.equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
            DISPLAY_SKEWT = 0;
            DISPLAY_WITO = DISPLAY_SKEWT + 1;
            DISPLAY_INSET = DISPLAY_WITO + 1;
            DISPLAY_HODO = DISPLAY_INSET + 1;
            DISPLAY_DATA = DISPLAY_HODO + 1;
            DISPLAY_SPC_GRAPHS = DISPLAY_DATA + 1;
            DISPLAY_TOTAL = DISPLAY_SPC_GRAPHS + 1;
            DISPLAY_FUTURE = -1;
            DISPLAY_TIMESTN = -1;
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
            DISPLAY_SKEWT = 0;
            DISPLAY_TIMESTN = DISPLAY_SKEWT + 1;
            DISPLAY_HODO = DISPLAY_TIMESTN + 1;
            DISPLAY_DATA = DISPLAY_HODO + 1;
            DISPLAY_FUTURE = DISPLAY_DATA + 1;
            DISPLAY_TOTAL = DISPLAY_FUTURE + 1;
            DISPLAY_WITO = -1;
            DISPLAY_INSET = -1;
            DISPLAY_SPC_GRAPHS = -1;
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)) {
            NsharpPaletteWindow win = NsharpPaletteWindow.getInstance();
            if (win != null && win
                    .getCurrentGraphMode() == NsharpConstants.GRAPH_HODO) {
                DISPLAY_HODO = 0;
                DISPLAY_TIMESTN = DISPLAY_HODO + 1;
                DISPLAY_SKEWT = -1;
            } else {
                DISPLAY_SKEWT = 0;
                DISPLAY_TIMESTN = DISPLAY_SKEWT + 1;
                DISPLAY_HODO = -1;
            }
            DISPLAY_DATA = DISPLAY_TIMESTN + 1;
            DISPLAY_TOTAL = DISPLAY_DATA + 1;
            DISPLAY_FUTURE = -1;
            DISPLAY_WITO = -1;
            DISPLAY_INSET = -1;
            DISPLAY_SPC_GRAPHS = -1;
        } else if (paneConfigurationName
                .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
            NsharpPaletteWindow win = NsharpPaletteWindow.getInstance();
            if (win != null && win
                    .getCurrentGraphMode() == NsharpConstants.GRAPH_HODO) {
                DISPLAY_HODO = 0;
                DISPLAY_TIMESTN = DISPLAY_HODO + 1;
                DISPLAY_SKEWT = -1;
            } else {
                DISPLAY_SKEWT = 0;
                DISPLAY_TIMESTN = DISPLAY_SKEWT + 1;
                DISPLAY_HODO = -1;
            }
            DISPLAY_DATA = DISPLAY_TIMESTN + 1;
            DISPLAY_TOTAL = DISPLAY_DATA + 1;
            DISPLAY_FUTURE = -1;
            DISPLAY_WITO = -1;
            DISPLAY_INSET = -1;
            DISPLAY_SPC_GRAPHS = -1;
        } else {
            /* case of default1 and default 2 pane configurations */
            DISPLAY_SKEWT = 0;
            DISPLAY_WITO = DISPLAY_SKEWT + 1;
            DISPLAY_INSET = DISPLAY_WITO + 1;
            DISPLAY_HODO = DISPLAY_INSET + 1;
            DISPLAY_TIMESTN = DISPLAY_HODO + 1;
            DISPLAY_DATA = DISPLAY_TIMESTN + 1;
            DISPLAY_TOTAL = DISPLAY_DATA + 1;
            DISPLAY_FUTURE = -1;
            DISPLAY_SPC_GRAPHS = -1;
        }
    }

    /*
     * Note: initDisplayPublicParms() should be called before calling this
     * function
     */
    private IRenderableDisplay[] createRenderableDisplayArray(
            IRenderableDisplay[] existing) {
        IRenderableDisplay[] displayArray = new IRenderableDisplay[DISPLAY_TOTAL];

        for (IRenderableDisplay display : existing) {
            int index = -1;
            /*
             * Existing extents from bundles can have empty areas that cause
             * problems so set a default area, actual extents will be filled in
             * later.
             */
            Rectangle extentArea = null;
            if (display instanceof NsharpSkewTPaneDisplay) {
                index = DISPLAY_SKEWT;
                extentArea = NsharpConstants.SKEWT_DISPLAY_REC;
            } else if (display instanceof NsharpWitoPaneDisplay) {
                index = DISPLAY_WITO;
                extentArea = NsharpConstants.WITO_DISPLAY_REC;
            } else if (display instanceof NsharpInsetPaneDisplay) {
                index = DISPLAY_INSET;
                extentArea = NsharpConstants.INSET_DISPLAY_REC;
            } else if (display instanceof NsharpHodoPaneDisplay) {
                index = DISPLAY_HODO;
                extentArea = NsharpConstants.HODO_DISPLAY_REC;
            } else if (display instanceof NsharpTimeStnPaneDisplay) {
                index = DISPLAY_TIMESTN;
                extentArea = NsharpConstants.TIMESTN_DISPLAY_REC;
            } else if (display instanceof NsharpDataPaneDisplay) {
                index = DISPLAY_DATA;
                extentArea = NsharpConstants.DATA_DISPLAY_REC;
            } else if (display instanceof NsharpSpcGraphsPaneDisplay) {
                index = DISPLAY_SPC_GRAPHS;
                extentArea = NsharpConstants.SPC_GRAPH_DISPLAY_REC;
            } else if (display instanceof NsharpAbstractPaneDisplay) {
                index = DISPLAY_FUTURE;
                extentArea = NsharpConstants.FUTURE_DISPLAY_REC;
            }
            if (index >= 0) {
                displayArray[index] = display;
                display.setExtent(new PixelExtent(extentArea));
                display.setContainer(null);
            }
        }
        if (DISPLAY_SKEWT >= 0 && displayArray[DISPLAY_SKEWT] == null) {
            displayArray[DISPLAY_SKEWT] = new NsharpSkewTPaneDisplay(
                    new PixelExtent(NsharpConstants.SKEWT_DISPLAY_REC),
                    DISPLAY_SKEWT);
        }
        if (DISPLAY_WITO >= 0 && displayArray[DISPLAY_WITO] == null) {
            displayArray[DISPLAY_WITO] = new NsharpWitoPaneDisplay(
                    new PixelExtent(NsharpConstants.WITO_DISPLAY_REC),
                    DISPLAY_WITO);
        }
        if (DISPLAY_INSET >= 0 && displayArray[DISPLAY_INSET] == null) {
            displayArray[DISPLAY_INSET] = new NsharpInsetPaneDisplay(
                    new PixelExtent(NsharpConstants.INSET_DISPLAY_REC),
                    DISPLAY_INSET);
        }
        if (DISPLAY_HODO >= 0 && displayArray[DISPLAY_HODO] == null) {
            displayArray[DISPLAY_HODO] = new NsharpHodoPaneDisplay(
                    new PixelExtent(NsharpConstants.HODO_DISPLAY_REC),
                    DISPLAY_HODO);
        }
        if (DISPLAY_TIMESTN >= 0 && displayArray[DISPLAY_TIMESTN] == null) {
            displayArray[DISPLAY_TIMESTN] = new NsharpTimeStnPaneDisplay(
                    new PixelExtent(NsharpConstants.TIMESTN_DISPLAY_REC),
                    DISPLAY_TIMESTN);
        }
        if (DISPLAY_DATA >= 0 && displayArray[DISPLAY_DATA] == null) {
            displayArray[DISPLAY_DATA] = new NsharpDataPaneDisplay(
                    new PixelExtent(NsharpConstants.DATA_DISPLAY_REC),
                    DISPLAY_DATA);
        }
        if (DISPLAY_SPC_GRAPHS >= 0
                && displayArray[DISPLAY_SPC_GRAPHS] == null) {
            displayArray[DISPLAY_SPC_GRAPHS] = new NsharpSpcGraphsPaneDisplay(
                    new PixelExtent(NsharpConstants.SPC_GRAPH_DISPLAY_REC),
                    DISPLAY_SPC_GRAPHS);
        }
        if (DISPLAY_FUTURE >= 0 && displayArray[DISPLAY_FUTURE] == null) {
            displayArray[DISPLAY_FUTURE] = new NsharpAbstractPaneDisplay(
                    new PixelExtent(NsharpConstants.FUTURE_DISPLAY_REC),
                    DISPLAY_FUTURE);
        }
        return displayArray;
    }

    /*
     * Note: initDisplayPublicParms() and createRenderableDisplayArray() should
     * be called before calling this function
     */
    private void createPaneResource() {
        for (int i = 0; i < displayArray.length; i += 1) {
            ResourceList resources = displayArray[i].getDescriptor()
                    .getResourceList();
            List<NsharpAbstractPaneResource> nsharpResources = resources
                    .getResourcesByTypeAsType(NsharpAbstractPaneResource.class);
            for (NsharpAbstractPaneResource nsharpResource : nsharpResources) {
                nsharpResource.setRscHandler(rscHandler);
                if (nsharpResource instanceof NsharpSkewTPaneResource) {
                    ((NsharpSkewTPaneResource) nsharpResource)
                            .setCurrentGraphMode(
                                    rscHandler.getCurrentGraphMode());
                }
            }
        }
    }

    private void updateEditor() {
        IDisplayPane[] oldPanes = displayPanes;
        IRenderableDisplay[] oldDisplayArray = displayArray;
        initPaneIndices();
        IRenderableDisplay[] newDisplayArray = createRenderableDisplayArray(
                oldDisplayArray);
        for (IDisplayPane pane : oldPanes) {
            if (pane.getRenderableDisplay().getContainer() == null) {
                /*
                 * If the display was reused, remove it so it doesn't dispose.
                 */
                pane.setRenderableDisplay(null);
            }
            pane.dispose();
        }
        displayArray = newDisplayArray;
        nsharpComp = new Composite[DISPLAY_TOTAL];
        displayPanes = new VizDisplayPane[DISPLAY_TOTAL];
        // CHIN task#5930 use same loop properties
        EditorInput edInput = new EditorInput(
                this.editorInput.getLoopProperties(), displayArray);
        this.setInput(edInput);
        this.displaysToLoad = displayArray;
        for (IRenderableDisplay display : displayArray) {
            if (display != null) {
                this.initDisplay(display);
            }
        }
        rscHandler.updateDisplay(displayArray, paneConfigurationName);
        rscHandler.resetRscSoundingData();

        createPaneResource();

    }

    public static NsharpEditor createOrOpenEditor() {
        NsharpEditor editor = getActiveNsharpEditor();
        if (editor != null) {
            return editor;
        } else {
            try {
                /*
                 * Chin note: all initialization logics now are done in init()
                 * method to support standard eclipse Editor open procedure. At
                 * init(), display array and EditorInput will be created based
                 * on user configured pane configuration. Therefore, we just
                 * create a dummy EditorInput here. The following
                 * IWorkbenchPage.openEditor() will eventually invoke init()
                 * method.
                 */
                IRenderableDisplay[] tempDisp = new IRenderableDisplay[1];
                tempDisp[0] = new NsharpSkewTPaneDisplay(
                        new PixelExtent(NsharpConstants.SKEWT_DISPLAY_REC), 0);
                EditorInput edInput = new EditorInput(new NCLoopProperties(),
                        tempDisp);
                editor = (NsharpEditor) PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage()
                        .openEditor(edInput, EDITOR_ID);

            } catch (PartInitException e) {
                statusHandler.error("Cannot open NSharp Editor.", e);
            }
            return editor;
        }
    }

    public static void bringEditorToTop() {
        NsharpEditor editor = getActiveNsharpEditor();
        if (editor != null) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .bringToTop(editor);

        }
    }

    public void refreshGUIElements() {
        ICommandService service = getSite().getService(ICommandService.class);
        String[] guiUpdateElementCommands = {
                "gov.noaa.nws.ncep.viz.ui.options.SyncPanes",
                "gov.noaa.nws.ncep.viz.ui.actions.loopBackward",
                "gov.noaa.nws.ncep.viz.ui.actions.loopForward",
                "gov.noaa.nws.ncep.viz.ui.actions.rock",
                "gov.noaa.nws.ncep.viz.ui.actions.frameTool",
                "gov.noaa.nws.ncep.viz.ui.autoUpdate",
                "gov.noaa.nws.ncep.viz.ui.actions.hideFrames" };

        // Update the GUI elements on the menus and toolbars
        for (String toolbarID : guiUpdateElementCommands) {
            service.refreshElements(toolbarID, null);
        }

    }

    public VizDisplayPane getSelectedPane() {
        return selectedPane;
    }

    public void setSelectedPane(VizDisplayPane selectedPane) {
        this.selectedPane = selectedPane;
    }

    class PaneMouseListener implements Listener {
        private int paneIndex;

        public PaneMouseListener(int index) {
            super();
            this.paneIndex = index;
        }

        @Override
        public void handleEvent(Event e) {
            if (e.button == 0) {
                selectedPane = displayPanes[paneIndex];
            }
        }
    }

    class ResizeListener implements Listener {
        private String paneConfigurationName = "";

        public ResizeListener(String name) {
            super();
            this.paneConfigurationName = name;
        }

        @Override
        public void handleEvent(Event event) {
            int skewTHeightHint = 0;
            int skewTWidthHint = 0;
            int witoHeightHint = 0;
            int witoWidthHint = 0;
            int hodoHeightHint = 0;
            int hodoWidthHint = 0;
            int insetHeightHint = 0;
            int insetWidthHint = 0;
            int timeStnHeightHint = 0;
            int timeStnWidthHint = 0;
            int dataHeightHint = 0;
            int dataWidthHint = 0;
            int spcHeightHint = 0;
            int spcWidthHint = 0;
            int futureHeightHint = 0;
            int futureWidthHint = 0;
            int baseHeight = baseComposite.getSize().y;
            int baseWidth = baseComposite.getSize().x;
            if (paneConfigurationName
                    .equals(NsharpConstants.PANE_DEF_CFG_2_STR)) {
                skewTHeightHint = (int) (baseHeight * skewTHeightHintRatio);
                skewTWidthHint = (int) (baseWidth * leftGroupWidthRatio
                        * skewTWidthHintRatio);
                witoHeightHint = (int) (baseHeight * witoHeightHintRatio);
                witoWidthHint = (int) (baseWidth * leftGroupWidthRatio
                        * witoWidthHintRatio);
                hodoHeightHint = (int) (baseHeight * hodoHeightHintRatio);
                hodoWidthHint = (int) (baseWidth * (1 - leftGroupWidthRatio)
                        * hodoWidthHintRatio);
                insetHeightHint = (int) (baseHeight * insetHeightHintRatio);
                insetWidthHint = (int) (baseWidth * (leftGroupWidthRatio)
                        * insetWidthHintRatio);
                timeStnHeightHint = (int) (baseHeight * timeStnHeightHintRatio);
                timeStnWidthHint = (int) (baseWidth * (1 - leftGroupWidthRatio)
                        * timeStnWidthHintRatio);
                dataHeightHint = (int) (baseHeight * dataHeightHintRatio);
                dataWidthHint = (int) (baseWidth * (1 - leftGroupWidthRatio)
                        * dataWidthHintRatio);
            } else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
                skewTHeightHint = (int) (baseHeight * skewTHeightHintRatio);
                skewTWidthHint = (int) (baseWidth * leftGroupWidthRatio
                        * skewTWidthHintRatio);
                witoHeightHint = (int) (baseHeight * witoHeightHintRatio);
                witoWidthHint = (int) (baseWidth * leftGroupWidthRatio
                        * witoWidthHintRatio);
                hodoHeightHint = (int) (baseHeight * hodoHeightHintRatio);
                hodoWidthHint = (int) (baseWidth * (1 - leftGroupWidthRatio)
                        * hodoWidthHintRatio);
                insetHeightHint = (int) (baseHeight * insetHeightHintRatio);
                insetWidthHint = (int) (baseWidth * (leftGroupWidthRatio)
                        * insetWidthHintRatio);
                timeStnHeightHint = (int) (baseHeight * timeStnHeightHintRatio);
                timeStnWidthHint = (int) (baseWidth * (leftGroupWidthRatio)
                        * timeStnWidthHintRatio);
                dataHeightHint = (int) (baseHeight * dataHeightHintRatio);
                dataWidthHint = (int) (baseWidth * (1 - leftGroupWidthRatio)
                        * dataWidthHintRatio);
            } else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
                skewTHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * skewTHeightHintRatio);
                skewTWidthHint = (int) (baseWidth * skewTWidthHintRatio);
                witoHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * witoHeightHintRatio);
                witoWidthHint = (int) (baseWidth * witoWidthHintRatio);
                hodoHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * hodoHeightHintRatio);
                hodoWidthHint = (int) (baseWidth * hodoWidthHintRatio);
                insetHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * insetHeightHintRatio);
                insetWidthHint = (int) (baseWidth * insetWidthHintRatio);
                dataHeightHint = (int) (baseHeight * botGroupHeightRatio
                        * dataHeightHintRatio);
                dataWidthHint = (int) (baseWidth * dataWidthHintRatio);
                spcHeightHint = (int) (baseHeight * botGroupHeightRatio
                        * dataHeightHintRatio);
                spcWidthHint = (int) (baseWidth * dataWidthHintRatio);
            } else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
                skewTHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * skewTHeightHintRatio);
                skewTWidthHint = (int) (baseWidth * skewTWidthHintRatio);
                timeStnHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * timeStnHeightHintRatio);
                timeStnWidthHint = (int) (baseWidth * timeStnWidthHintRatio);
                futureHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * (1 - timeStnHeightHintRatio));
                futureWidthHint = timeStnWidthHint;
                dataHeightHint = (int) (baseHeight * botGroupHeightRatio
                        * dataHeightHintRatio);
                dataWidthHint = (int) (baseWidth * dataWidthHintRatio);
                hodoHeightHint = (int) (baseHeight * botGroupHeightRatio
                        * hodoHeightHintRatio);
                hodoWidthHint = (int) (baseWidth * hodoWidthHintRatio);
            }
            // RM#9396 OPC share code with d2d lite
            else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
                skewTHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * skewTHeightHintRatio);
                skewTWidthHint = (int) (baseWidth * skewTWidthHintRatio);
                // skewt and hodo share same pane real estate
                hodoHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * skewTHeightHintRatio);
                hodoWidthHint = (int) (baseWidth * skewTWidthHintRatio);
                timeStnHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * timeStnHeightHintRatio);
                timeStnWidthHint = (int) (baseWidth * timeStnWidthHintRatio);
                dataHeightHint = (int) (baseHeight * topGroupHeightRatio
                        * (1 - timeStnHeightHintRatio));
                dataWidthHint = timeStnWidthHint;
            }
            if (paneConfigurationName.equals(NsharpConstants.PANE_DEF_CFG_2_STR)
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_DEF_CFG_1_STR)) {
                GridData leftGpGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                leftGpGd.widthHint = (int) (baseWidth * leftGroupWidthRatio);
                leftGp.setLayoutData(leftGpGd);

                GridData rightGpGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                rightGpGd.widthHint = (int) (baseWidth
                        * (1 - leftGroupWidthRatio));
                rightGp.setLayoutData(rightGpGd);

                GridData leftTopGpGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                leftTopGpGd.heightHint = (int) (baseHeight
                        * leftTopGroupHeightRatio);
                leftTopGp.setLayoutData(leftTopGpGd);
                if (leftBotGp != null) {
                    GridData leftBotGpGd = new GridData(SWT.FILL, SWT.FILL,
                            true, true);
                    leftBotGpGd.heightHint = (int) (baseHeight
                            * (1 - leftTopGroupHeightRatio));
                    leftBotGp.setLayoutData(leftBotGpGd);
                }
                GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                skewtGd.heightHint = skewTHeightHint;
                skewtGd.widthHint = skewTWidthHint;
                nsharpComp[DISPLAY_SKEWT].setLayoutData(skewtGd);
                GridData witoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                witoGd.heightHint = witoHeightHint;
                witoGd.widthHint = witoWidthHint;
                nsharpComp[DISPLAY_WITO].setLayoutData(witoGd);

                GridData timeStnGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                timeStnGd.heightHint = timeStnHeightHint;
                timeStnGd.widthHint = timeStnWidthHint;
                nsharpComp[DISPLAY_TIMESTN].setLayoutData(timeStnGd);
                GridData insetGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                insetGd.heightHint = insetHeightHint;
                insetGd.widthHint = insetWidthHint;
                nsharpComp[DISPLAY_INSET].setLayoutData(insetGd);

                GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                hodoGd.heightHint = hodoHeightHint;
                hodoGd.widthHint = hodoWidthHint;
                nsharpComp[DISPLAY_HODO].setLayoutData(hodoGd);

                GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                dataGd.heightHint = dataHeightHint;
                dataGd.widthHint = dataWidthHint;
                nsharpComp[DISPLAY_DATA].setLayoutData(dataGd);

            } else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_SPCWS_CFG_STR)) {
                GridData topGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                topGpGd.heightHint = (int) (baseHeight * topGroupHeightRatio);
                topGp.setLayoutData(topGpGd);

                // skewt composite
                GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                skewtGd.heightHint = skewTHeightHint;
                skewtGd.widthHint = skewTWidthHint;
                nsharpComp[DISPLAY_SKEWT].setLayoutData(skewtGd);

                // wito composite
                GridData witoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                witoGd.heightHint = witoHeightHint;
                witoGd.widthHint = witoWidthHint;
                nsharpComp[DISPLAY_WITO].setLayoutData(witoGd);

                // right-top group : right part of top group
                GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                rightTopGpGd.widthHint = (int) (baseWidth * hodoWidthHintRatio);
                rightTopGp.setLayoutData(rightTopGpGd);

                // hodo composite
                GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                hodoGd.heightHint = hodoHeightHint;
                hodoGd.widthHint = hodoWidthHint;
                nsharpComp[DISPLAY_HODO].setLayoutData(hodoGd);

                // inset composite
                GridData insetGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                insetGd.heightHint = insetHeightHint;
                insetGd.widthHint = insetWidthHint;
                nsharpComp[DISPLAY_INSET].setLayoutData(insetGd);

                GridData botGpGd = new GridData(SWT.FILL, SWT.END, true, true);
                botGpGd.heightHint = (int) (baseHeight * botGroupHeightRatio);
                botGp.setLayoutData(botGpGd);
                // data composite
                GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
                dataGd.heightHint = dataHeightHint;
                dataGd.widthHint = dataWidthHint;
                nsharpComp[DISPLAY_DATA].setLayoutData(dataGd);
                // spc composite
                GridData spcGd = new GridData(SWT.FILL, SWT.FILL, true, false);
                spcGd.heightHint = spcHeightHint;
                spcGd.widthHint = spcWidthHint;
                nsharpComp[DISPLAY_SPC_GRAPHS].setLayoutData(spcGd);

            } else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_SIMPLE_D2D_CFG_STR)) {
                GridData topGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                topGpGd.heightHint = skewTHeightHint;
                topGp.setLayoutData(topGpGd);

                // skewt composite
                GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                skewtGd.heightHint = skewTHeightHint;
                skewtGd.widthHint = skewTWidthHint;
                nsharpComp[DISPLAY_SKEWT].setLayoutData(skewtGd);

                // right-top group : right part of top group
                GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                rightTopGpGd.widthHint = timeStnWidthHint;
                rightTopGp.setLayoutData(rightTopGpGd);

                GridData timeStnGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                timeStnGd.heightHint = timeStnHeightHint;
                timeStnGd.widthHint = timeStnWidthHint;
                nsharpComp[DISPLAY_TIMESTN].setLayoutData(timeStnGd);

                // future composite
                GridData futureGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                futureGd.heightHint = futureHeightHint;
                futureGd.widthHint = futureWidthHint;
                nsharpComp[DISPLAY_FUTURE].setLayoutData(futureGd);

                GridData botGpGd = new GridData(SWT.FILL, SWT.END, true, true);
                botGpGd.heightHint = (int) (baseHeight * botGroupHeightRatio);
                botGp.setLayoutData(botGpGd);
                // hodo composite
                GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true, false);
                hodoGd.heightHint = hodoHeightHint;
                hodoGd.widthHint = hodoWidthHint;
                nsharpComp[DISPLAY_HODO].setLayoutData(hodoGd);
                GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
                dataGd.heightHint = dataHeightHint;
                dataGd.widthHint = dataWidthHint;
                nsharpComp[DISPLAY_DATA].setLayoutData(dataGd);
            }
            // RM#9396 OPC share code with d2dlite start
            else if (paneConfigurationName
                    .equals(NsharpConstants.PANE_LITE_D2D_CFG_STR)
                    || paneConfigurationName
                            .equals(NsharpConstants.PANE_OPC_CFG_STR)) {
                GridData topGpGd = new GridData(SWT.FILL, SWT.FILL, true, true);
                topGpGd.heightHint = skewTHeightHint;
                topGp.setLayoutData(topGpGd);

                if (rscHandler
                        .getCurrentGraphMode() != NsharpConstants.GRAPH_HODO) {
                    // skewt composite
                    GridData skewtGd = new GridData(SWT.FILL, SWT.FILL, true,
                            true);
                    skewtGd.heightHint = skewTHeightHint;
                    skewtGd.widthHint = skewTWidthHint;
                    nsharpComp[DISPLAY_SKEWT].setLayoutData(skewtGd);
                } else {
                    // hodo composite
                    GridData hodoGd = new GridData(SWT.FILL, SWT.FILL, true,
                            false);
                    hodoGd.heightHint = hodoHeightHint;
                    hodoGd.widthHint = hodoWidthHint;
                    nsharpComp[DISPLAY_HODO].setLayoutData(hodoGd);
                }

                // right-top group : right part of top group
                GridData rightTopGpGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                rightTopGpGd.widthHint = timeStnWidthHint;
                rightTopGp.setLayoutData(rightTopGpGd);

                GridData timeStnGd = new GridData(SWT.FILL, SWT.FILL, true,
                        true);
                timeStnGd.heightHint = timeStnHeightHint;
                timeStnGd.widthHint = timeStnWidthHint;
                nsharpComp[DISPLAY_TIMESTN].setLayoutData(timeStnGd);

                GridData dataGd = new GridData(SWT.FILL, SWT.FILL, true, false);
                dataGd.heightHint = dataHeightHint;
                dataGd.widthHint = dataWidthHint;
                nsharpComp[DISPLAY_DATA].setLayoutData(dataGd);
            }
            for (int i = 0; i < DISPLAY_TOTAL; i++) {
                if (displayArray[i] != null && !displayArray[i].getDescriptor()
                        .getResourceList().isEmpty()) {
                    ResourcePair rscPair = displayArray[i].getDescriptor()
                            .getResourceList().get(0);
                    if (rscPair
                            .getResource() instanceof NsharpAbstractPaneResource) {
                        NsharpAbstractPaneResource paneRsc = (NsharpAbstractPaneResource) rscPair
                                .getResource();
                        paneRsc.setResize(true);
                    }
                }
            }

        }

    }

    @Override
    public boolean isDirty() {
        if (!isCloseable()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        this.editorInput = (EditorInput) input;
    }

    @Override
    public void renderableDisplayChanged(IDisplayPane pane,
            IRenderableDisplay newRenderableDisplay, DisplayChangeType type) {

        if (type == DisplayChangeType.ADD
                && newRenderableDisplay instanceof NsharpAbstractPaneDisplay) {
            for (int i = 0; i < displayArray.length; i += 1) {
                if (displayPanes[i] == pane) {
                    displayArray[i] = newRenderableDisplay;
                }
            }
            NsharpAbstractPaneDisplay newNsharpDisplay = (NsharpAbstractPaneDisplay) newRenderableDisplay;
            NsharpResourceHandler newHandler = newNsharpDisplay.getDescriptor()
                    .getRscHandler();
            if (newHandler != null && newHandler != rscHandler) {
                rscHandler = newHandler;
                createPaneResource();
            } else if (newHandler == null) {
                ResourceList resources = newRenderableDisplay.getDescriptor()
                        .getResourceList();
                /*
                 * Ensure resources are instantiated so it is possible to set
                 * the resource handler.
                 */
                resources.instantiateResources(
                        newRenderableDisplay.getDescriptor(), true);
                if (newRenderableDisplay instanceof NsharpSkewTPaneDisplay) {
                    /*
                     * This is a new display loaded into an existing editor,
                     * probably by the bundle loader, so reset all the data.
                     */
                    rscHandler = new NsharpResourceHandler(displayArray, this);
                }
                createPaneResource();
            }
            rscHandler.updateDisplay(displayArray, paneConfigurationName);
            rscHandler.resetData();
        }
    }

    public String getPaneConfigurationName() {
        return paneConfigurationName;
    }

    @Override
    public void setColor(BGColorMode mode, RGB newColor) {
        for (IDisplayPane pane : displayPanes) {
            if (pane != null) {
                IRenderableDisplay disp = pane.getRenderableDisplay();
                if (disp != null) {
                    disp.setBackgroundColor(newColor);
                }
            }
        }
        this.refresh();
    }

}