/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.ContoursAttrDlg
 *
 * October 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourButtons;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourDefault;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourLines;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourObject;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContoursInfo;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourCircle;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourLine;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.contours.IContours;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.display.ILine;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Arc;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.SinglePointElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.ui.pgen.graphtogrid.G2GCommon;
import gov.noaa.nws.ncep.ui.pgen.graphtogrid.GraphToGridParamDialog;
import gov.noaa.nws.ncep.ui.pgen.palette.PgenPaletteWindow;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductConfigureDialog;
import gov.noaa.nws.ncep.ui.pgen.producttypes.PgenLayer;
import gov.noaa.nws.ncep.ui.pgen.producttypes.ProductType;
import gov.noaa.nws.ncep.ui.pgen.tools.AbstractPgenTool;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenContoursTool;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenContoursTool.PgenContoursHandler;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenSelectHandler;

/**
 * Singleton attribute dialog for Contours.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * --------------------------------------------------------------------------------------
 * 10/09        #167        J. Wu       Initial Creation.
 * 12/09        #167        J. Wu       Allow editing line and label attributes.
 * 03/10        #215        J. Wu       Make grid from contours.
 * 06/10        #215        J. Wu       Added capability to draw mix/max symbols.
 * 11/10        #345        J. Wu       Added capability to draw circle.
 * 12/10        #321        J. Wu       Added capability to move labels.
 * 12/10        #167        J. Wu       Added a second symbol.
 * 12/10        #312        J. Wu       track the last-status of "Closed" button.
 * 01/11        #203        J. Wu       Allow user to define more quick access symbols.
 * 02/11        #?          J. Wu       Implemented default "settings".
 * 04/11        #?          B. Yin      Re-factor IAttribute
 * 07/11        #?          J. Wu       Updated symbol selection panel and allowed closed
 * 08/11        #?          J. Wu       TTR78: keep line/symbol/label attr window open.
 * 01/12        #?          J. Wu       Fixed exceptions when closing the dialog.
 * 05/12        756         B. Yin      Fixed the place symbol exception
 * 12/13        1084        J. Wu       Add table-control for Cint in contoursInfo.xml
 * 04/14        #1117       J. Wu       Add quick-access line types, set label focus,
 *                                      up/down arrow hotkeys to change label.
 * 04/21/2014   TTR992      D. Sushon   Contour tool's Label >> Edit option should not close
 *                                      main Contour tool window on Cancel, changing a
 *                                      symbol's label should not change the symbol;
 * 04/21/2014   TTR992      D. Sushon   Contour tool's Label >> Edit option should not close
 *                                      main Contour tool window on Cancel, changing a
 *                                      symbol's label should not change the symbol;
 * 04/25/2014   TTR868      D. Sushon   Editing/moving contour text attribute causes contour
 *                                      to disappear.
 * 04/29/2014   trac 1132   D. Sushon   In testing solution for TTR 868, it was found that
 *                                      similar changes to labels for other contour objects
 *                                      caused a similar error (label or object or both
 *                                      would disappear), this should now be fixed.
 * 05/02/2014   ?           D. Sushon   In testing solution for trac 1132, it was found that
 *                                      multiple instances of the contour dialog's Edit
 *                                      windows could be created with no way to remove,
 *                                      should now be fixed.
 * 05/14        TTR1008     J. Wu       Set default contour parameters through settings_tbl.xml.
 * 05/14        TTR990      J. Wu       Set default attributes for different contour labels.
 * 08/14        ?           J. Wu       "Edit" line color should go to contoursAttrSettings to take effect..
 * 01/15        R5199/T1058 J. Wu       Load/Save settings for different settings tables.
 * 01/15        R5200/T1059 J. Wu       Add setSettings(de) to remember last-used attributes.
 * 01/15        R5201/T1060 J. Wu       Add getLabelTempKey(adc).
 * 01/15        R5413       B. Yin      Added open methods for circle and line dialogs.
 * 07/15        R8352       J. Wu       Make label list collapsible and remove select/delete buttons.
 * 08/01/2015   R8213       P.          CAVE>PGEN
 *                          Chowdhuri    - Refinements to contoursInfo.xml
 * 08/15        R8552       J. Wu       Limit contours' hotkeys within its own context.
 * 09/29/2015   R8163       J. Wu       Prevent exception when contour type changes.
 * 11/18/2015   R12829      J. Wu       Link "Contours Parameter" to the one defined on layer.
 * 01/14/2016   R13168      J. Wu       Add "One Contours per Layer" rule.
 * 01/27/2016   R13166      J. Wu       Add symbol only & label only capability.
 * 03/30/2016   R16622      J. Wu       Use current date/time as default.
 * 03/23/2016   R16613      J. Huber    Change "Hide Labels" to "Collapse Levels".
 * 04/11/2016   R17056      J. Wu       Match contour line/symbol color with settings.
 * 05/07/2016   R17379      J. Wu       Overwrite contour level when user types in new value.
 * 05/16/2016   R18388      J. Wu       Use some contants in PgenConstant.
 * 05/17/2016   5641        njensen     Don't activate context outside of NCP
 * 06/01/2016   R18387      B. Yin      Removed "Edit" and "All" buttons.
 * 07/01/2016   R17377      J. Wu       Add "shiftDownInContourDialog" flag.
 * 07/21/2016   R16077      J. Wu       Allow number of labels to be 0 for contour lines.
 * 07/26/2019   66393       mapeters    Handle parm-specific contours settings
 * 08/07/2019   66393       mapeters    Only get available symbol/line types for the current parm
 * 09/06/2019   64150       ksunil      Change private class visibility to protected
 * 09/06/2019   64970       ksunil      Call changeMinmaxType when label/symbol only checkbox is pressed.
 * 02/10/2020   74136       smanoj      Fixed NullPointerException when drawingLayer is null
 * 10/30/2020   84101       smanoj      Add "Snap Labels to ContourLine" option on the
 *                                      Contours Attributes dialog.
 * </pre>
 *
 * @author J. Wu
 */

public class ContoursAttrDlg extends AttrDlg
        implements IContours, SelectionListener, ILine {

    private static final IUFStatusHandler handler = UFStatus
            .getHandler(ContoursAttrDlg.class);

    private static ContoursAttrDlg INSTANCE = null;

    private static ContoursInfoDlg contoursInfoDlg = null;

    private static GraphToGridParamDialog g2gDlg = null;

    /**
     * Defines which type of DE to be drawn.
     */
    public static enum ContourDrawingStatus {
        DRAW_LINE, DRAW_SYMBOL, DRAW_CIRCLE, SELECT
    }

    private static String labelSuffix = "|label";

    private ContourDrawingStatus drawingStatus = ContourDrawingStatus.DRAW_LINE;

    private String contourParm = "HGMT";

    private String contourLevel = "1000";

    private String contourFcstHr = "f000";

    private Calendar contourTime1 = Calendar.getInstance();

    private Calendar contourTime2 = Calendar.getInstance();

    private String defCint = ContoursInfoDlg.getCints()
            .get(contourParm + "-" + contourLevel);

    private String contourCint = (defCint != null
            && defCint.trim().length() > 0) ? defCint : "10/0/100";

    private final String SELECT_CONTOURLINE = "Select";

    private final String ADD_CONTOURLINE = "Add";

    private final String DELETE_CONTOURLINE = "Delete";

    private final int MAX_QUICK_LINES = 8;

    private final int MAX_QUICK_SYMBOLS = 15;

    private final int ROUND_UP_ONE_HOUR = 15; // Threshold to advance 1 hour

    private int numOfContrSymbols = 2;

    private int numOfButtons = 0;

    private Composite top;

    private Group attrComp;

    private Group textGrp;

    private Composite infoComp = null;

    private Button infoBtn = null;

    private Text labelTxt = null;

    private Button lineClosedBtn = null;

    private Composite labelGrp = null;

    private Button labelColorChkBox = null;

    private Button symbolOnlyBtn = null;

    private Button labelOnlyBtn = null;

    private Button hideSymbolLabelBtn = null;

    private ArrayList<Button> labelBtns = null;

    private Spinner labelNumSpinner = null;

    private List<Button> contourLineBtns = null;

    private Button activeContourLineBtn = null;

    private List<Button> contourSymbolBtns = null;

    private Button activeContourSymbolBtn = null;

    private Button circleTypeBtn = null;

    private Button hideCircleLabelBtn = null;

    private Button hideLabelListBtn = null;

    private final String EXPAND_LEVELS = "Expand Levels";

    private final String COLLAPSE_LEVELS = "Collapse Levels";

    private final String HIDE_BUTTON = "hideLabelListBtn";

    private Contours currentContours = null;

    private LinkedHashMap<String, String> lineIconType = null;

    private LinkedHashMap<String, IConfigurationElement> lineItemMap = null;

    private LinkedHashMap<String, IConfigurationElement> symbolItemMap = null;

    private LineTypeSelectionDlg lineTypePanel = null;

    private SymbolTypeSelectionDlg symbolTypePanel = null;

    private final int SYMBOLCOL = 16;

    /**
     * Default colors for the default and active product of layer name button.
     */
    private final Color defaultButtonColor = Color.lightGray;

    private final Color activeButtonColor = Color.green;

    /**
     * Line attribute dialog
     */
    private ContourLineAttrDlg lineAttrDlg;

    /**
     * stored line attribute
     */
    private Line lineTemplate = null;

    /**
     * Label text attribute dialog
     */
    private LabelAttrDlg labelAttrDlg;

    /**
     * stored label text attributeSlider
     */
    private gov.noaa.nws.ncep.ui.pgen.elements.Text labelTemplate = null;

    /**
     * Contours symbol attribute dialog
     */
    private ContourMinmaxAttrDlg minmaxAttrDlg;

    /**
     * stored symbol attribute
     */
    private Symbol minmaxTemplate = null;

    /**
     * Line attribute dialog
     */
    private ContourCircleAttrDlg circleAttrDlg;

    /**
     * stored line attribute
     */
    private Arc circleTemplate = null;

    /**
     * stored default and last-used attributes
     */
    private HashMap<String, HashMap<String, AbstractDrawableComponent>> contoursAttrSettingsMap = null;

    private HashMap<String, AbstractDrawableComponent> contoursAttrSettings = null;

    private PgenContoursTool tool;

    /*
     * stored last-used attributes for check buttons
     */
    private Map<String, Boolean> btnLastStatus = null;

    /*
     * A context activation for contours-specific hotkeys.
     */
    private IContextActivation pgenContoursContextActivation;

    /*
     * A flag to check if "shift" key is pressed down.
     *
     * When this dialog is up, its "labelTxt" always has keyboard focus.
     * However, other tools like panning tool needs the status of SHIFT key to
     * be activated. So we need to trace this "SHIFT" key and allow the contour
     * drawing tool to retrieve its status to decide if the panning tool should
     * be activated.
     */
    private boolean shiftDownInContourDialog;

    private Button toggleSnapLblChkBox = null;

    private boolean toggleSnapLblChecked = true;

    /**
     * Private constructor
     *
     * @param parShell
     * @throws VizException
     */
    private ContoursAttrDlg(Shell parShell) throws VizException {

        super(parShell);

        retrieveIconType();

        if (contourLineBtns == null) {
            contourLineBtns = new ArrayList<>();
        }

        if (contourSymbolBtns == null) {
            contourSymbolBtns = new ArrayList<>();
        }

        // Create a map to store the last status of check buttons.
        if (btnLastStatus == null) {
            btnLastStatus = new HashMap<>();
        }

    }

    /**
     * Creates a Contours attribute dialog if the dialog does not exist and
     * returns the instance. If the dialog exists, return the instance.
     *
     * @param parShell
     * @return
     */
    public static ContoursAttrDlg getInstance(Shell parShell) {

        if (INSTANCE == null) {

            try {

                INSTANCE = new ContoursAttrDlg(parShell);

            } catch (VizException e) {

                handler.error("Error creating Contours Attributes Dialog", e);

            }
        }

        return INSTANCE;

    }

    /**
     * Creates the dialog area
     */
    @Override
    public Control createDialogArea(Composite parent) {

        top = (Composite) super.createDialogArea(parent);
        this.getShell().setText("Contours Attributes");

        // Create the main layout for the dialog.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        mainLayout.verticalSpacing = 0;
        top.setLayout(mainLayout);

        // Button to pop up the dialog to editing the contour's info.
        infoComp = new Composite(top, SWT.NONE);
        infoComp.setLayoutData(
                new GridData(SWT.CENTER, SWT.DEFAULT, true, false));

        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 15;
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        layout.horizontalSpacing = 1;
        infoComp.setLayout(layout);

        infoBtn = new Button(infoComp, SWT.PUSH);
        infoBtn.setToolTipText("Bring up the contours info attribute dialog");
        setInfoBtnText();

        infoBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                openContourInfoDlg();
            }
        });

        // Button to activate the graph-to-grid processing.
        Button makeGridBtn = new Button(infoComp, SWT.PUSH);
        makeGridBtn.setText("Make Grid");
        makeGridBtn.setToolTipText("Generate grid from this Contours");
        makeGridBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                openG2GDlg();
            }
        });

        addSeparator(top);

        /*
         * Create a composite to editing other attributes.
         */
        attrComp = new Group(top, SWT.SHADOW_ETCHED_OUT);
        GridLayout layout1 = new GridLayout(1, false);
        layout1.marginHeight = 1;
        layout1.marginWidth = 1;
        layout1.verticalSpacing = 0;
        attrComp.setLayout(layout1);

        /*
         * Create a composite to editing the line attributes.
         */
        Composite lineComp = new Composite(attrComp, SWT.NONE);
        GridLayout layout2 = new GridLayout(2, false);
        layout2.horizontalSpacing = 1;
        layout2.marginWidth = 1;
        layout2.marginHeight = 1;
        layout2.verticalSpacing = 0;
        lineComp.setLayout(layout2);

        Composite closelineComp = new Composite(lineComp, SWT.NONE);
        GridLayout closelineCompGl = new GridLayout(2, false);
        closelineCompGl.verticalSpacing = 0;
        closelineCompGl.marginHeight = 0;
        closelineCompGl.marginWidth = 1;
        closelineComp.setLayout(closelineCompGl);

        /*
         * Create contour line type buttons.
         */
        Composite contourLineComp = new Composite(closelineComp, SWT.NONE);
        GridLayout contourLineCompGl = new GridLayout(2, false);
        contourLineCompGl.marginHeight = 1;
        contourLineCompGl.marginWidth = 1;
        contourLineCompGl.verticalSpacing = 1;
        contourLineCompGl.horizontalSpacing = 0;
        contourLineComp.setLayout(contourLineCompGl);

        int ii = 0;
        for (String str : getContourLineSymbols()) {

            if (ii < MAX_QUICK_LINES) {

                ii++;

                Button btn = new Button(contourLineComp, SWT.PUSH);
                btn.setToolTipText(lineItemMap.get(str).getAttribute("label"));
                btn.setImage(getIcon(str));
                btn.setData(str);

                contourLineBtns.add(btn);

                btn.addListener(SWT.MouseDoubleClick, new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        openLineTypePanel();
                    }
                });

                btn.addListener(SWT.MouseDown, new Listener() {
                    @Override
                    public void handleEvent(Event event) {

                        Button btnClicked = (Button) event.widget;
                        if (ContoursAttrDlg.this.drawContourLine()
                                && btnClicked == activeContourLineBtn) {
                            openLineTypePanel();
                        } else {
                            activeContourLineBtn = btnClicked;

                            for (Button b : contourLineBtns) {
                                if (b == activeContourLineBtn) {
                                    DrawableElement de = (Line) contoursAttrSettings
                                            .get(b.getData().toString());
                                    setButtonColor(b, defaultButtonColor,
                                            de.getColors()[0]);
                                    lineTemplate = (Line) de;
                                    closeAttrEditDialogs();
                                    openLineAttrDlg();
                                } else {
                                    setButtonColor(b, activeButtonColor,
                                            defaultButtonColor);
                                }
                            }

                            boolean typeChanged = changeLineType();

                            if (!typeChanged && !ContoursAttrDlg.this
                                    .drawContourLine()) {
                                setDrawingStatus(
                                        ContourDrawingStatus.DRAW_LINE);
                            }
                        }
                    }
                });
            }
        }

        activeContourLineBtn = contourLineBtns.get(0);

        if (lineTemplate == null) {
            lineTemplate = (Line) contoursAttrSettings
                    .get(activeContourLineBtn.getData().toString());
        }

        DrawableElement des = (Line) contoursAttrSettings
                .get(activeContourLineBtn.getData().toString());
        setButtonColor(activeContourLineBtn, defaultButtonColor,
                des.getColors()[0]);

        lineClosedBtn = new Button(closelineComp, SWT.CHECK);
        lineClosedBtn.setText("Closed");
        lineClosedBtn.setToolTipText("Click to draw a closed line");
        lineClosedBtn.setData("lineClosedBtn");

        setBtnStatus(lineClosedBtn, false);
        lineClosedBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                saveBtnLastStatus(lineClosedBtn);
            }
        });

        Composite editlineComp = new Composite(lineComp, SWT.NONE);
        GridLayout editLineGl = new GridLayout(2, false);
        editLineGl.horizontalSpacing = 1;
        editLineGl.marginWidth = 1;
        editLineGl.verticalSpacing = 0;
        editLineGl.marginHeight = 0;
        editlineComp.setLayout(editLineGl);

        /*
         * Create buttons for adding Min/Max
         */
        Composite contourSymbolComp = new Composite(attrComp, SWT.NONE);
        GridLayout layoutm = new GridLayout(2, false);
        layoutm.marginWidth = 1;
        layoutm.marginHeight = 1;
        layoutm.verticalSpacing = 1;

        if (numOfContrSymbols == 1) {
            layoutm.horizontalSpacing = 72;
        } else if (numOfContrSymbols == 2) {
            layoutm.horizontalSpacing = 38;
        } else {
            layoutm.horizontalSpacing = 0;
        }

        contourSymbolComp.setLayout(layoutm);

        Composite contourSymbolTypeComp = new Composite(contourSymbolComp,
                SWT.NONE);
        GridLayout contourSymbolTypeCompGl = new GridLayout(2, false);
        contourSymbolTypeCompGl.marginHeight = 1;
        contourSymbolTypeCompGl.marginWidth = 0;
        contourSymbolTypeCompGl.verticalSpacing = 0;
        contourSymbolTypeComp.setLayout(contourSymbolTypeCompGl);

        Composite contoursComp = new Composite(contourSymbolTypeComp, SWT.NONE);
        GridLayout contoursCompGl = new GridLayout(2, false);
        contoursCompGl.marginHeight = 0;
        contoursCompGl.marginWidth = 0;
        contoursCompGl.verticalSpacing = 0;
        contoursCompGl.horizontalSpacing = 0;
        contoursComp.setLayout(contoursCompGl);

        ii = 0;
        for (String str : getContourSymbols()) {

            if (ii < MAX_QUICK_SYMBOLS) {

                ii++;

                Button btn = new Button(contoursComp, SWT.PUSH);
                btn.setToolTipText(
                        symbolItemMap.get(str).getAttribute("label"));
                btn.setImage(getIcon(str));
                btn.setData(str);

                contourSymbolBtns.add(btn);

                btn.addListener(SWT.MouseDoubleClick, new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        openSymbolPanel();
                    }
                });

                btn.addListener(SWT.MouseDown, new Listener() {
                    @Override
                    public void handleEvent(Event event) {

                        Button btnClicked = (Button) event.widget;
                        if (ContoursAttrDlg.this.drawSymbol()
                                && btnClicked == activeContourSymbolBtn) {
                            openSymbolPanel();
                        } else {
                            activeContourSymbolBtn = btnClicked;

                            for (Button b : contourSymbolBtns) {
                                if (b == activeContourSymbolBtn) {
                                    DrawableElement de = (SinglePointElement) contoursAttrSettings
                                            .get(b.getData().toString());
                                    setButtonColor(b, defaultButtonColor,
                                            de.getColors()[0]);
                                    minmaxTemplate = (Symbol) de;
                                    closeAttrEditDialogs();
                                    openSymbolAttrDlg();
                                } else {
                                    setButtonColor(b, activeButtonColor,
                                            defaultButtonColor);
                                }
                            }

                            /*
                             * Change min/max types if one is selected.
                             * Otherwise, set to draw a new one.
                             */
                            boolean typeChanged = changeMinmaxType();
                            if (!typeChanged
                                    && !ContoursAttrDlg.this.drawSymbol()) {
                                setDrawingStatus(
                                        ContourDrawingStatus.DRAW_SYMBOL);
                            }
                        }
                    }
                });
            }
        }

        activeContourSymbolBtn = contourSymbolBtns.get(0);
        if (minmaxTemplate == null) {
            try {
                minmaxTemplate = (Symbol) contoursAttrSettings
                        .get(activeContourSymbolBtn.getData().toString());
            } catch (NullPointerException npe) {
                handler.error("Error getting min-max template", npe);
            }
        }

        DrawableElement de = (SinglePointElement) contoursAttrSettings
                .get(activeContourSymbolBtn.getData().toString());
        setButtonColor(activeContourSymbolBtn, defaultButtonColor,
                de.getColors()[0]);

        Composite editSymbolAttrComp = new Composite(contourSymbolComp,
                SWT.NONE);
        GridLayout editSymbolAttrCompGl = new GridLayout(3, false);
        editSymbolAttrCompGl.marginWidth = 0;
        editSymbolAttrCompGl.horizontalSpacing = 0;
        editSymbolAttrComp.setLayout(editSymbolAttrCompGl);

        Composite editSubtypeComp = new Composite(editSymbolAttrComp, SWT.NONE);
        GridLayout editSubtypeCompGl = new GridLayout(1, false);
        editSubtypeCompGl.marginWidth = 0;
        editSubtypeCompGl.horizontalSpacing = 0;
        editSubtypeCompGl.verticalSpacing = 0;
        editSubtypeCompGl.marginHeight = 0;
        editSubtypeComp.setLayout(editSubtypeCompGl);

        symbolOnlyBtn = new Button(editSubtypeComp, SWT.CHECK);
        symbolOnlyBtn.setText("Symb Only");
        symbolOnlyBtn.setToolTipText("Check to add only a symbol");
        symbolOnlyBtn.setAlignment(SWT.LEFT);
        symbolOnlyBtn.setSize(5, 5);
        symbolOnlyBtn.setData("symbolOnlyBtn");

        setBtnStatus(symbolOnlyBtn, false);
        symbolOnlyBtn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {

                if (((Button) e.widget).getSelection()) {
                    labelOnlyBtn.setSelection(false);
                    hideSymbolLabelBtn.setSelection(false);
                    hideSymbolLabelBtn.setEnabled(false);
                } else {
                    if (!labelOnlyBtn.getSelection()) {
                        hideSymbolLabelBtn.setEnabled(true);
                    }
                }
                changeMinmaxType();
                saveBtnLastStatus(symbolOnlyBtn);
                saveBtnLastStatus(labelOnlyBtn);
                saveBtnLastStatus(hideSymbolLabelBtn);
            }
        });

        labelOnlyBtn = new Button(editSubtypeComp, SWT.CHECK);
        labelOnlyBtn.setText("Label Only");
        labelOnlyBtn.setToolTipText("Check to add only a label");
        labelOnlyBtn.setAlignment(SWT.LEFT);
        labelOnlyBtn.setSize(5, 5);
        labelOnlyBtn.setData("labelOnlyBtn");

        setBtnStatus(labelOnlyBtn, false);
        labelOnlyBtn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {

                if (((Button) e.widget).getSelection()) {

                    symbolOnlyBtn.setSelection(false);
                    hideSymbolLabelBtn.setSelection(false);
                    hideSymbolLabelBtn.setEnabled(false);
                } else {
                    if (!symbolOnlyBtn.getSelection()) {
                        hideSymbolLabelBtn.setEnabled(true);
                    }
                }
                changeMinmaxType();
                saveBtnLastStatus(symbolOnlyBtn);
                saveBtnLastStatus(labelOnlyBtn);
                saveBtnLastStatus(hideSymbolLabelBtn);
            }
        });

        hideSymbolLabelBtn = new Button(editSubtypeComp, SWT.CHECK);
        hideSymbolLabelBtn.setText("Hide Label");
        hideSymbolLabelBtn
                .setToolTipText("Check to hide the label for this symbol");
        hideSymbolLabelBtn.setAlignment(SWT.LEFT);
        hideSymbolLabelBtn.setSize(5, 5);
        hideSymbolLabelBtn.setData("hideSymbolLabelBtn");
        setBtnStatus(hideSymbolLabelBtn, false);

        if (symbolOnlyBtn.getSelection() || labelOnlyBtn.getSelection()) {
            hideSymbolLabelBtn.setEnabled(false);
        }

        hideSymbolLabelBtn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (symbolOnlyBtn.getSelection()
                        || labelOnlyBtn.getSelection()) {
                    hideSymbolLabelBtn.setSelection(false);
                }

                saveBtnLastStatus(hideSymbolLabelBtn);
            }
        });

        /*
         * Create buttons for adding circle
         */
        Composite circleComp = new Composite(attrComp, SWT.NONE);
        GridLayout layoutc = new GridLayout(2, false);
        layoutc.horizontalSpacing = 12;
        layoutc.marginHeight = 1;
        layoutc.marginWidth = 1;
        layoutc.verticalSpacing = 1;
        circleComp.setLayout(layoutc);

        Composite circleTypeComp = new Composite(circleComp, SWT.NONE);
        GridLayout circleTypeCompGl = new GridLayout(3, false);
        circleTypeCompGl.marginHeight = 1;
        circleTypeCompGl.marginWidth = 1;
        circleTypeCompGl.verticalSpacing = 0;
        circleTypeComp.setLayout(circleTypeCompGl);

        circleTypeBtn = new Button(circleTypeComp, SWT.PUSH);
        circleTypeBtn.setToolTipText("Click to select a line type for circle");
        circleTypeBtn.setImage(getIcon(PgenConstant.CIRCLE));
        circleTypeBtn.setData(PgenConstant.CIRCLE);
        if (circleTemplate == null) {
            circleTemplate = (Arc) contoursAttrSettings
                    .get(PgenConstant.CIRCLE);
            circleTemplate.setPgenType(PgenConstant.CIRCLE);
        }
        setButtonColor(circleTypeBtn, circleTemplate.getColors()[0]);

        circleTypeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (!ContoursAttrDlg.this.drawCircle()) {
                    if (circleTemplate == null) {
                        circleTemplate = (Arc) contoursAttrSettings
                                .get(PgenConstant.CIRCLE);
                        circleTemplate.setPgenType(PgenConstant.CIRCLE);
                    }
                    setButtonColor(circleTypeBtn,
                            circleTemplate.getColors()[0]);
                    setDrawingStatus(ContourDrawingStatus.DRAW_CIRCLE);
                    closeAttrEditDialogs();
                    openCircleAttrDlg();
                }
            }
        });

        hideCircleLabelBtn = new Button(circleTypeComp, SWT.CHECK);
        hideCircleLabelBtn.setText("Hide\nLabel");
        hideCircleLabelBtn
                .setToolTipText("Check to hide the label for this circle");
        hideCircleLabelBtn.setData("hideCircleLabelBtn");

        setBtnStatus(hideCircleLabelBtn, false);
        hideCircleLabelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                saveBtnLastStatus(hideCircleLabelBtn);
            }
        });

        Composite editCircleComp = new Composite(circleComp, SWT.NONE);
        GridLayout editCircleCompGl = new GridLayout(2, false);
        editCircleCompGl.marginHeight = 0;
        editCircleCompGl.verticalSpacing = 0;
        editCircleCompGl.marginWidth = 1;
        editCircleCompGl.horizontalSpacing = 1;
        editCircleComp.setLayout(editCircleCompGl);

        // Create a composite to editing the label attributes.
        textGrp = new Group(attrComp, SWT.SHADOW_ETCHED_IN);
        textGrp.setText("Label");
        GridLayout labelGl = new GridLayout(1, false);
        labelGl.marginHeight = 1;
        labelGl.marginWidth = 1;
        labelGl.verticalSpacing = 1;
        labelGl.horizontalSpacing = 1;

        textGrp.setLayout(labelGl);

        Composite textComp = new Composite(textGrp, SWT.NONE);
        GridLayout layout3 = new GridLayout(2, false);
        layout3.horizontalSpacing = 1;
        layout3.marginWidth = 1;
        layout3.verticalSpacing = 0;
        layout3.marginHeight = 0;

        textComp.setLayout(layout3);

        Composite textValueComp = new Composite(textComp, SWT.NONE);
        GridLayout layout4 = new GridLayout(4, false);
        layout4.horizontalSpacing = 1;
        layout4.marginWidth = 1;

        textValueComp.setLayout(layout4);

        labelTxt = new Text(textValueComp, SWT.SINGLE);
        labelTxt.setLayoutData(new GridData(45, 15));
        labelTxt.setEditable(true);
        labelTxt.setText("0");
        labelTxt.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                float value = 0;
                try {
                    value = Float.parseFloat(getLabel());
                    if (value == G2GCommon.RMISSD
                            || value == -G2GCommon.RMISSD) {
                        lineClosedBtn.setSelection(true);
                        saveBtnLastStatus(lineClosedBtn);
                    }
                } catch (NumberFormatException el) {
                    //
                }

            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        });

        // Key listener to check the status of "shift" key.
        labelTxt.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.SHIFT) {
                    shiftDownInContourDialog = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.keyCode == SWT.SHIFT) {
                    shiftDownInContourDialog = false;
                }
            }
        });

        Button valueUpArrow = new Button(textValueComp, SWT.ARROW | SWT.UP);
        valueUpArrow.setLayoutData(new GridData(20, 22));
        valueUpArrow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                changeLabel(true);
            }

        });

        Button valueDownArrow = new Button(textValueComp, SWT.ARROW | SWT.DOWN);
        valueDownArrow.setLayoutData(new GridData(20, 22));
        valueDownArrow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                changeLabel(false);
            }

        });

        labelNumSpinner = new Spinner(textValueComp, SWT.BORDER);
        labelNumSpinner.setMinimum(0);
        labelNumSpinner.setMaximum(10);
        labelNumSpinner.setSelection(1);
        labelNumSpinner.setIncrement(1);
        labelNumSpinner.setPageIncrement(1);

        Composite editTextComp = new Composite(textComp, SWT.NONE);
        GridLayout editTextCompGl = new GridLayout(2, false);
        editTextCompGl.verticalSpacing = 0;
        editTextCompGl.marginHeight = 1;
        editTextCompGl.marginWidth = 1;
        editTextCompGl.horizontalSpacing = 1;
        editTextComp.setLayout(editTextCompGl);

        Composite toggleLabelSnapComp = new Composite(textGrp, SWT.NONE);
        GridLayout layoutToggleSnap = new GridLayout(4, false);
        layoutToggleSnap.horizontalSpacing = 1;
        layoutToggleSnap.marginWidth = 1;
        layoutToggleSnap.verticalSpacing = 0;
        layoutToggleSnap.marginHeight = 0;
        toggleLabelSnapComp.setLayout(layoutToggleSnap);
        toggleSnapLblChkBox = new Button(toggleLabelSnapComp, SWT.CHECK);
        toggleSnapLblChkBox.setText("Snap Labels to ContourLine");
        toggleSnapLblChkBox.setToolTipText(
                "Check to snap Labels onto the Contour while editing the line");
        toggleSnapLblChkBox.setData("toggleSnapLblChkBox");
        toggleSnapLblChkBox.setSelection(true);
        toggleSnapLblChecked = true;
        toggleSnapLblChkBox.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                toggleSnapLblChecked = toggleSnapLblChkBox.getSelection();
            }
        });

        Composite applyLineColorComp = new Composite(textGrp, SWT.NONE);
        GridLayout layout6 = new GridLayout(4, false);
        layout6.horizontalSpacing = 1;
        layout6.marginWidth = 1;
        layout6.verticalSpacing = 0;
        layout6.marginHeight = 0;
        applyLineColorComp.setLayout(layout6);
        labelColorChkBox = new Button(applyLineColorComp, SWT.CHECK);
        labelColorChkBox.setText("Use Main Color");
        labelColorChkBox.setData("labelColorChkBox");

        setBtnStatus(labelColorChkBox, true);
        labelColorChkBox.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                saveBtnLastStatus(labelColorChkBox);
            }
        });

        // Button to collapse/expand the label buttons
        hideLabelListBtn = new Button(applyLineColorComp, SWT.PUSH);

        // setData calls are being used to control the toggling of
        // hide/show since the SWT Push button does not have capability
        // on its own. SWT Toggle provides this capability but causes one of the
        // buttons to be shaded which is confusing visibly. This also
        // provides the ability to keep track of button status without relying
        // on the text of the button.

        boolean hideBtnStatus = getHideLabelBtnStatus();

        // Default for hideBtnstatus is true
        hideLabelListBtn.setData(hideBtnStatus);
        if (hideBtnStatus) {
            hideLabelListBtn.setText(EXPAND_LEVELS);
        } else {
            hideLabelListBtn.setText(COLLAPSE_LEVELS);
        }
        hideLabelListBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean hideLabelList = true;
                if ((boolean) hideLabelListBtn.getData()) {
                    hideLabelList = false;
                }
                if (labelBtns != null && labelBtns.size() > 0) {
                    for (Button btn : labelBtns) {
                        GridData data = (GridData) btn.getLayoutData();
                        data.exclude = hideLabelList;
                        btn.setVisible(!data.exclude);
                    }
                }

                if (hideLabelList) {
                    hideLabelListBtn.setText(EXPAND_LEVELS);
                    hideLabelListBtn.setData(true);
                } else {
                    hideLabelListBtn.setText(COLLAPSE_LEVELS);
                    hideLabelListBtn.setData(false);
                }
                saveHideLabelBtnStatus(hideLabelListBtn);
                textGrp.getShell().pack();
                textGrp.getShell().layout();
            }
        });

        // Create a composite to display contour levels created on the value of
        // CINT.
        createLabelBtns(textGrp, true);

        return top;

    }

    @Override
    public void setAttrForDlg(IAttribute ia) {
        //
    }

    /*
     * set attributes to the given attributes in an IContours
     */
    public void setAttrForDlg(IContours ic) {
        setAttributes(ic);
    }

    @Override
    public String getCint() {
        return contourCint;
    }

    @Override
    public String getLevel() {
        return contourLevel;
    }

    @Override
    public String getForecastHour() {
        return contourFcstHr;
    }

    @Override
    public String getParm() {
        return contourParm;
    }

    @Override
    public Calendar getTime1() {
        return contourTime1;
    }

    @Override
    public Calendar getTime2() {
        return contourTime2;
    }

    @Override
    public Boolean isClosedLine() {
        return lineClosedBtn.getSelection();
    }

    public boolean getToggleSnapLblChecked() {
        return toggleSnapLblChecked;
    }

    /**
     * Flag to see if we apply the line/symbol/color to its label.
     */
    public Boolean isUseMainColor() {
        return labelColorChkBox.getSelection();
    }

    /**
     * Get the text in the label text box.
     */
    public String getLabel() {

        String lbl = labelTxt.getText();
        if (lbl == null || lbl.trim().length() == 0) {
            lbl = new String("0");
            if (labelBtns != null && labelBtns.size() > 0) {
                lbl = labelBtns.get(0).getText();
                for (Button btn : labelBtns) {
                    if (btn.getSelection()) {
                        lbl = btn.getText();
                        break;
                    }
                }
            }

            setLabel(lbl);
        }

        return lbl;
    }

    /*
     * Get the number of labels.
     */
    public int getNumOfLabels() {

        int nlabels;

        nlabels = Integer.parseInt(labelNumSpinner.getText());

        return nlabels;
    }

    /*
     * Pops up the Contours information dialog
     */
    public void setNumOfLabels(int nlabels) {

        labelNumSpinner.setSelection(nlabels);

    }

    /*
     * Open the dialog to edit the contours info.
     */
    private void openContourInfoDlg() {

        if (contoursInfoDlg == null) {
            contoursInfoDlg = new ContoursInfoDlg(this.getShell());
            contoursInfoDlg.setContoursAttrDlg(this);
        }

        contoursInfoDlg.open();

    }

    /*
     * Set the dialog attributes.
     */
    public void setAttributes(IContours attr) {

        setParm(attr.getParm());
        setLevel(attr.getLevel());
        setFcstHr(attr.getForecastHour());
        setTime1(attr.getTime1());
        setTime2(attr.getTime2());
        setCint(attr.getCint());

        setInfoBtnText();
    }

    /*
     * Set the label for the contour line.
     */
    public void setLabel(String lbl) {
        labelTxt.setText(lbl);
        updateLabelBtnsSelection(lbl);
    }

    /*
     * Set parm
     */
    public void setParm(String parm) {
        contourParm = parm;
    }

    /*
     * Set level
     */
    public void setLevel(String level) {
        contourLevel = level;
    }

    /*
     * Set forecast hour
     */
    public void setFcstHr(String fcstHr) {
        contourFcstHr = fcstHr;
    }

    /*
     * Set time
     */
    public void setTime1(Calendar time) {
        contourTime1 = time;
    }

    /*
     * Set time
     */
    public void setTime2(Calendar time) {
        contourTime2 = time;
    }

    /*
     * set cint
     */
    public void setCint(String cint) {
        contourCint = cint;
        createLabelBtns(textGrp, false);
    }

    /*
     * set closed button
     */
    public void setClosed(boolean closed) {
        lineClosedBtn.setSelection(closed);
        saveBtnLastStatus(lineClosedBtn);
    }

    /*
     * Change the label text to the text of the next selected button.
     */
    private void changeLabel(boolean upArrowClicked) {

        int ii = 0;
        for (Button btn : labelBtns) {

            if (btn.getSelection()) {

                int next;
                if (upArrowClicked) {
                    next = (ii + 1) % labelBtns.size();
                } else {
                    next = ((ii - 1) + labelBtns.size()) % labelBtns.size();
                }

                btn.setSelection(false);
                labelBtns.get(next).setSelection(true);
                labelTxt.setText(labelBtns.get(next).getText());
                break;
            }

            ii++;
        }

        if (labelTemplate != null) {
            labelTemplate.setText(new String[] { getLabel() });
        }

    }

    /*
     * Select the label button that has the same value as the given text.
     */
    private void updateLabelBtnsSelection(String lbl) {

        for (Button btn : labelBtns) {
            btn.setSelection(false);
        }

        for (Button btn : labelBtns) {
            if (btn.getText().equals(lbl)) {
                btn.setSelection(true);
                break;
            }
        }

    }

    /*
     * create label buttons based on the contours' Cint value.
     */
    private void createLabelBtns(Composite comp, boolean firstTime) {

        if (firstTime) {
            labelGrp = new Composite(comp, SWT.SHADOW_ETCHED_OUT);
            GridLayout gl = new GridLayout(3, true);
            gl.marginHeight = 1;
            gl.marginWidth = 1;
            gl.horizontalSpacing = 1;
            gl.verticalSpacing = 1;
            labelGrp.setLayout(gl);
        } else {
            Control[] wids = labelGrp.getChildren();
            if (wids != null) {
                for (int jj = 0; jj < wids.length; jj++) {
                    wids[jj].dispose();
                }
            }
        }

        // pack it to force the change.
        if (!firstTime) {
            this.getShell().pack();
        }

        // recreate.
        if (labelBtns != null) {
            labelBtns.clear();
        } else {
            labelBtns = new ArrayList<>();
        }

        /*
         * Note: zoom level starts from 1, not 0.
         */
        List<String> cints = GraphToGridParamDialog.parseCints(contourCint);
        if (cints != null && cints.size() > 0) {

            // use GridData.exclude to hide/show these buttons.
            GridData gdata = new GridData();
            gdata.exclude = (boolean) hideLabelListBtn.getData();

            gdata.horizontalAlignment = SWT.FILL;

            for (String lblstr : cints) {
                Button btn = new Button(labelGrp, SWT.RADIO);
                btn.setText(lblstr);
                btn.setData(lblstr);
                btn.setLayoutData(gdata);

                btn.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        labelTxt.setText(event.widget.getData().toString());
                        if (labelTemplate != null) {
                            labelTemplate.setText(new String[] { getLabel() });
                        }
                    }
                });

                labelBtns.add(btn);
            }

            if (labelBtns.size() > 0) {
                labelBtns.get(0).setSelection(true);
                labelTxt.setText(labelBtns.get(0).getText());
            }
        }

        /*
         * Repack
         */
        if (!firstTime) {
            this.getShell().pack();
            this.getShell().layout();
        }

    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        //

    }

    @Override
    public void widgetSelected(SelectionEvent e) {

        if (e.widget instanceof Button) {

            Button b = (Button) e.widget;
            DrawableElement de = drawingLayer.getSelectedDE();

            if (de != null) {
                currentContours = (Contours) de.getParent().getParent();
            }

            /*
             * Which button?
             */
            String btnName = b.getData().toString();

            /*
             * Close the attribute editing dialogs.
             */
            if (btnName.equals(SELECT_CONTOURLINE)
                    || btnName.equals(ADD_CONTOURLINE)
                    || btnName.equals(DELETE_CONTOURLINE)) {

                closeAttrEditDialogs();
            }

            /*
             * Notify the Drawing Tool to allow new contour line to be selected
             */
            if (btnName.equals(SELECT_CONTOURLINE)) {

                drawingLayer.removeSelected();

                this.setDrawingStatus(ContourDrawingStatus.SELECT);
                tool.setPgenSelectHandler();

                setLabelFocus();
            }

            /*
             * Draw and add a new contour line to the current contours and the
             * notify the drawing tool.
             */
            else if (btnName.equals(ADD_CONTOURLINE)) {

                tool.setPgenContoursHandler();

                this.getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
                this.getButton(IDialogConstants.OK_ID).setEnabled(false);

            }

            /*
             * Remove the selected contour line or min/max from the contours and
             * the notify the drawing tool.
             */
            else if (btnName.equals(DELETE_CONTOURLINE)) {

                if (de != null
                        && (de instanceof Line
                                && de.getParent() instanceof ContourLine)
                        || (de instanceof Symbol
                                && de.getParent() instanceof ContourMinmax)
                        || (de instanceof Arc
                                && de.getParent() instanceof ContourCircle)) {

                    Contours oldContours = (Contours) de.getParent()
                            .getParent();
                    Contours newContours = new Contours();

                    if (((DECollection) oldContours).size() <= 1) {
                        // If no ContourLine left, start a new Contours.
                        drawingLayer.removeElement(oldContours);
                        newContours = null;
                    } else {

                        Iterator<AbstractDrawableComponent> iterator = oldContours
                                .getComponentIterator();

                        /*
                         * Remove the selected contour line and copy over other
                         * non-selected contour lines.
                         */
                        while (iterator.hasNext()) {

                            AbstractDrawableComponent oldAdc = iterator.next();
                            AbstractDrawableComponent newAdc = oldAdc.copy();

                            if (!(oldAdc.equals(de.getParent()))) {
                                newAdc.setParent(newContours);
                                newContours.add(newAdc);
                            }

                        }

                        /*
                         * Update the contours attributes and replace the old
                         * one with the new Contours.
                         */
                        newContours.update(this);
                        drawingLayer.replaceElement(oldContours, newContours);

                    }

                    /*
                     * Reset the current contours
                     */
                    currentContours = newContours;
                    drawingLayer.removeSelected();

                    if (tool != null) {
                        tool.setCurrentContour(newContours);
                    }

                }

                if (mapEditor != null) {
                    mapEditor.refresh();
                }

            }
        }
    }

    /*
     * Set the text on the info button based on parm, level, and time.
     */
    private void setInfoBtnText() {

        String str = Contours.getKey(this).replace('|', '\n');

        infoBtn.setText(str);
        infoComp.pack();

    }

    /**
     * Check if the contour line drawing button is selected.
     */
    public boolean drawContourLine() {

        return drawingStatus == ContourDrawingStatus.DRAW_LINE;

    }

    /**
     * Updates the selected contours and contour component, then redraws the
     * PGEN layer.
     */
    @Override
    public void okPressed() {

        /*
         * Create a new Contours as well as a new contour component
         */
        DrawableElement de = drawingLayer.getSelectedDE();
        if (de != null && de.getParent() != null
                && de.getParent().getParent() instanceof Contours) {

            DrawableElement newEl = (DrawableElement) de.copy();

            Contours oldContours = (Contours) de.getParent().getParent();

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Replace the selected contour component with a new contour
             * component and copy over other non-selected contour components.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (oldAdc.equals(de.getParent())) {

                    newEl.setParent(newAdc);

                    if (oldAdc instanceof ContourCircle) {

                        if (newEl instanceof Arc) {
                            ((DECollection) newAdc).replace(
                                    ((ContourCircle) newAdc).getCircle(),
                                    newEl);
                        }

                        else {
                            gov.noaa.nws.ncep.ui.pgen.elements.Text oldLabel = ((ContourCircle) newAdc)
                                    .getLabel();
                            ((DECollection) newAdc).replace(oldLabel, newEl);
                        }

                        ((ContourCircle) newAdc)
                                .updateLabelString(new String[] { getLabel() });
                        ((ContourCircle) newAdc).getLabel()
                                .setHide(hideCircleLabel());
                    } else if (oldAdc instanceof ContourLine) {

                        if (newEl instanceof Line) {
                            ((DECollection) newAdc).replace(
                                    ((ContourLine) newAdc).getLine(), newEl);
                        }

                        else {
                            gov.noaa.nws.ncep.ui.pgen.elements.Text oldLabel = ((ContourLine) newAdc)
                                    .getLabels().get(0);
                            Coordinate opt = ((gov.noaa.nws.ncep.ui.pgen.elements.Text) de)
                                    .getPosition();
                            double dist = oldLabel.getPosition().distance(opt);
                            for (gov.noaa.nws.ncep.ui.pgen.elements.Text lbl : ((ContourLine) newAdc)
                                    .getLabels()) {
                                if (lbl.getPosition().distance(opt) < dist) {
                                    oldLabel = lbl;
                                    dist = lbl.getPosition().distance(opt);
                                }
                            }

                            ((DECollection) newAdc).replace(oldLabel, newEl);
                        }

                        ((ContourLine) newAdc).getLine()
                                .setPgenType(getContourLineType());
                        ((ContourLine) newAdc).getLine()
                                .setClosed(lineClosedBtn.getSelection());

                        int nlabels = ((ContourLine) newAdc).getNumOfLabels();
                        if (nlabels != getNumOfLabels()) {

                            ((ContourLine) newAdc)
                                    .updateNumOfLabels(getNumOfLabels());

                            if (newEl instanceof gov.noaa.nws.ncep.ui.pgen.elements.Text) {
                                if (((ContourLine) newAdc).getLabels()
                                        .size() > 0) {
                                    newEl = ((ContourLine) newAdc).getLabels()
                                            .get(0);
                                } else {
                                    drawingLayer.removeSelected();
                                    newEl = null;
                                }
                            }
                        }

                        ((ContourLine) newAdc)
                                .updateLabelString(new String[] { getLabel() });

                    } else if (oldAdc instanceof ContourMinmax) {

                        if (newEl instanceof Symbol) {
                            ((DECollection) newAdc).replace(
                                    ((ContourMinmax) newAdc).getSymbol(),
                                    newEl);
                        } else {
                            gov.noaa.nws.ncep.ui.pgen.elements.Text oldLabel = ((ContourMinmax) newAdc)
                                    .getLabel();

                            ((DECollection) newAdc).replace(oldLabel, newEl);
                        }

                        // Check symbol-only and label-only min/max.
                        DrawableElement newSymb = ((ContourMinmax) newAdc)
                                .getSymbol();
                        gov.noaa.nws.ncep.ui.pgen.elements.Text newLabel = ((ContourMinmax) newAdc)
                                .getLabel();

                        if (newSymb != null) {
                            newSymb.setPgenCategory(getActiveSymbolClass());
                            newSymb.setPgenType(getActiveSymbolObjType());
                        }

                        if (newLabel != null) {
                            newLabel.setHide(hideSymbolLabel());
                            ((ContourMinmax) newAdc).updateLabelString(
                                    new String[] { getLabel() });
                        }
                    }

                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(this);
            drawingLayer.replaceElement(oldContours, newContours);
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            /*
             * Reset the selected contours and DE to the updated ones.
             */
            currentContours = newContours;
            drawingLayer.removeSelected();
            if (newEl != null) {
                drawingLayer.setSelected(newEl);
            }

        } else if (currentContours != null) {

            Contours oldContours = currentContours;

            if (!contourParm.equals(oldContours.getParm())
                    || !contourLevel.equals(oldContours.getLevel())
                    || !contourFcstHr.equals(oldContours.getForecastHour())
                    || !contourCint.equals(oldContours.getCint())) {

                Contours newContours = oldContours.copy();

                newContours.update(this);
                drawingLayer.replaceElement(oldContours, newContours);
                if (tool != null) {
                    tool.setCurrentContour(newContours);
                }

                currentContours = newContours;
            }

        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }

    }

    /**
     * Close all contours related dialogs.
     */
    @Override
    public boolean close() {

        closeAttrEditDialogs();

        if (g2gDlg != null) {
            g2gDlg.close();
        }

        if (contourLineBtns != null && contourLineBtns.size() > 0) {
            contourLineBtns.clear();
        }

        if (contourSymbolBtns != null && contourSymbolBtns.size() > 0) {
            contourSymbolBtns.clear();
        }

        currentContours = null;

        // Release contours-specific key bindings.
        deactivatePGENContoursContext();

        return super.close();

    }

    /**
     * Close line/label attribute editing dialogs.
     */
    public void closeAttrEditDialogs() {

        if (labelAttrDlg != null) {
            labelAttrDlg.close();
        }
        if (lineAttrDlg != null) {
            lineAttrDlg.close();
        }
        if (minmaxAttrDlg != null) {
            minmaxAttrDlg.close();
        }
        if (circleAttrDlg != null) {
            circleAttrDlg.close();
        }

    }

    /**
     * Returns the current contours.
     */
    public Contours getCurrentContours() {
        return currentContours;
    }

    /**
     * Sets the current contours.
     */
    public void setCurrentContours(Contours currentContours) {
        this.currentContours = currentContours;
        if (contoursAttrSettings != null) {
            contoursAttrSettings.put(PgenConstant.CONTOURS, currentContours);
        }
    }

    /**
     * @return the labelTemplate
     */
    public IAttribute getLabelTemplate() {

        if (labelAttrDlg != null && labelAttrDlg.getShell() != null) {
            return labelAttrDlg;
        } else {
            return (IAttribute) contoursAttrSettings.get(getLabelTempKey());
        }
    }

    /*
     * @param key to retrieve the label template
     */
    private String getLabelTempKey() {

        String tempKey = PgenConstant.TYPE_GENERAL_TEXT;
        if (drawSymbol() && activeContourSymbolBtn != null) {
            tempKey = new String(
                    activeContourSymbolBtn.getData().toString() + labelSuffix);
        } else if (drawContourLine() && activeContourLineBtn != null) {
            tempKey = new String(
                    activeContourLineBtn.getData().toString() + labelSuffix);
        } else if (drawCircle()) {
            tempKey = new String(PgenConstant.CIRCLE + labelSuffix);
        }

        return tempKey;

    }

    /*
     * @de generate a string key from a contour label.
     */
    private String getLabelTempKey(AbstractDrawableComponent de) {

        String tempKey = PgenConstant.TYPE_GENERAL_TEXT;
        AbstractDrawableComponent dp = de.getParent();
        if (dp != null) {
            if (dp instanceof ContourLine) {
                tempKey = new String(((ContourLine) dp).getLine().getPgenType()
                        + labelSuffix);
            } else if (dp instanceof ContourMinmax) {
                if (((ContourMinmax) dp).getSymbol() != null) {
                    tempKey = new String(
                            ((ContourMinmax) dp).getSymbol().getPgenType()
                                    + labelSuffix);
                }
            } else if (dp instanceof ContourCircle) {
                tempKey = new String(PgenConstant.CIRCLE + labelSuffix);
            }
        }

        return tempKey;

    }

    /**
     * @param labelTemplate
     *            the labelTemplate to set
     */
    public void setLabelTemplate(
            gov.noaa.nws.ncep.ui.pgen.elements.Text labelTemplate) {
        this.labelTemplate = labelTemplate;
    }

    /**
     * @return the lineTemplate
     */
    public IAttribute getLineTemplate() {
        if (lineAttrDlg != null && lineAttrDlg.getShell() != null) {
            return lineAttrDlg;
        } else {
            return (IAttribute) contoursAttrSettings
                    .get(activeContourLineBtn.getData().toString());
        }

    }

    /**
     * @param lineTemplate
     *            the lineTemplate to set
     */
    public void setLineTemplate(Line lineTemplate) {
        this.lineTemplate = lineTemplate;
    }

    /**
     * @return the minmaxTemplate
     */
    public IAttribute getMinmaxTemplate() {
        if (minmaxAttrDlg != null && minmaxAttrDlg.getShell() != null) {
            return minmaxAttrDlg;
        } else {
            return (IAttribute) contoursAttrSettings
                    .get(activeContourSymbolBtn.getData().toString());
        }
    }

    /**
     * @param minmaxTemplate
     *            the minmaxTemplate to set
     */
    public void setMinmaxTemplate(Symbol minmaxTemplate) {
        this.minmaxTemplate = minmaxTemplate;
    }

    /**
     * @return the circleTemplate
     */
    public IAttribute getCircleTemplate() {
        if (circleAttrDlg != null && circleAttrDlg.getShell() != null) {
            return circleAttrDlg;
        } else if (circleTemplate != null) {
            return circleTemplate;
        } else {
            circleTemplate = (Arc) contoursAttrSettings
                    .get(PgenConstant.CIRCLE);
            return circleTemplate;
        }
    }

    /**
     * @param circleTemplate
     *            the circleTemplate to set
     */
    public void setCircleTemplate(Arc circleTemplate) {
        this.circleTemplate = circleTemplate;
    }

    /**
     * Initialize and open the line and label attribute editing dialog
     *
     * @param dlg
     */
    private void openAttrDlg(AttrDlg dlg) {
        dlg.setBlockOnOpen(false);
        dlg.setDrawingLayer(drawingLayer);
        dlg.setMapEditor(mapEditor);
        dlg.open();
        dlg.enableButtons();
    }

    /**
     * Private Label Text dialog class
     *
     * @author jwu
     *
     */
    protected class LabelAttrDlg extends TextAttrDlg {

        private LabelAttrDlg(Shell parShell) throws VizException {

            super(parShell);

        }

        @Override
        public int open() {

            if (this.getShell() == null || this.getShell().isDisposed()) {
                int ret = super.open();
                /*
                 * Set dialog location
                 */
                if (this.shellLocation == null) {
                    this.getShell().setLocation(getAttrDlgLocation());
                }
                return ret;
            } else {
                return CANCEL;
            }
        }

        /**
         * Update the label attributes
         */
        @Override
        public void okPressed() {

            /*
             * Update the label template first.
             */
            labelTemplate = (gov.noaa.nws.ncep.ui.pgen.elements.Text) new DrawableElementFactory()
                    .create(DrawableType.TEXT, this, "Text", "General Text",
                            (Coordinate) null, null);

            String lblKey = new String(getLabelTempKey());
            contoursAttrSettings.put(lblKey, labelTemplate);

            labelTemplate.setText(new String[] { getLabel() });

            /*
             * Update the contours.
             */
            updateLabelAttributes();

        }

        /**
         * closes the text attribute dialog only
         */
        @Override
        public void cancelPressed() {
            attrDlgCleanUp();
            this.close();
        }

        @Override
        public void handleShellCloseEvent() {
            attrDlgCleanUp();
            this.close();
        }

        /**
         * disable un-used widgets
         */
        private void disableWidgets() {
            text.setEnabled(false);
            textLabel.setEnabled(false);
        }

        /**
         * initialize dialog
         */
        private void initDlg() {
            this.getShell().setText("Contour Label Attributes");
            setBoxText(true, DisplayType.NORMAL);
        }

    }

    /**
     * Updates the selected contours' label attributes, then redraws the PGEN
     * layer.
     */
    private void updateLabelAttributes() {
        /*
         * If no element is selected and "All" is not checked, do nothing.
         */
        DrawableElement de = drawingLayer.getSelectedDE();

        if (de == null) {
            return;
        }

        /*
         * Create a new Contours with all components in the old Contours and
         * update the label attributes if required.
         */
        DrawableElement newEl = null;
        Contours oldContours = null;

        if (de != null && de.getParent() != null
                && de.getParent().getParent() instanceof Contours) {
            newEl = (DrawableElement) de.copy();
            oldContours = (Contours) de.getParent().getParent();
        } else {
            oldContours = currentContours;
        }

        if (oldContours != null) {

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Copy all contour collections and update the label attributes.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (newAdc instanceof ContourLine) {

                    if (newEl != null && oldAdc.equals(de.getParent())) {

                        newEl.setParent(newAdc);

                        if (newEl instanceof Line) {
                            ((DECollection) newAdc).replace(
                                    ((ContourLine) newAdc).getLine(), newEl);
                        }

                        else {
                            gov.noaa.nws.ncep.ui.pgen.elements.Text oldLabel = ((ContourLine) newAdc)
                                    .getLabels().get(0);
                            Coordinate opt = ((gov.noaa.nws.ncep.ui.pgen.elements.Text) de)
                                    .getPosition();
                            double dist = oldLabel.getPosition().distance(opt);
                            for (gov.noaa.nws.ncep.ui.pgen.elements.Text lbl : ((ContourLine) newAdc)
                                    .getLabels()) {
                                if (lbl.getPosition().distance(opt) < dist) {
                                    oldLabel = lbl;
                                    dist = lbl.getPosition().distance(opt);
                                }
                            }

                            ((DECollection) newAdc).replace(oldLabel, newEl);
                        }

                        for (gov.noaa.nws.ncep.ui.pgen.elements.Text lbl : ((ContourLine) newAdc)
                                .getLabels()) {

                            boolean hide = lbl.getHide();
                            boolean auto = lbl.getAuto();
                            lbl.update(labelTemplate);
                            lbl.setHide(hide);
                            lbl.setAuto(auto);
                            lbl.setText(new String[] { getLabel() });
                        }

                    }
                } else if (newAdc instanceof ContourMinmax) {
                    if (newEl != null && oldAdc.equals(de.getParent())) {
                        newEl.setParent(newAdc);
                        if (newEl instanceof Symbol) {
                            ((DECollection) newAdc).replace(
                                    ((ContourMinmax) newAdc).getSymbol(),
                                    newEl);
                        } else {
                            ((DECollection) newAdc).replace(
                                    ((ContourMinmax) newAdc).getLabel(), newEl);
                        }

                        gov.noaa.nws.ncep.ui.pgen.elements.Text lbl = ((ContourMinmax) newAdc)
                                .getLabel();
                        if (lbl != null) {
                            boolean hide = lbl.getHide();
                            boolean auto = lbl.getAuto();
                            lbl.update(labelTemplate);
                            lbl.setHide(hide);
                            lbl.setAuto(auto);
                            lbl.setText(new String[] { getLabel() });
                        }

                    }
                } else if (newAdc instanceof ContourCircle) {
                    if (newEl != null && oldAdc.equals(de.getParent())) {
                        newEl.setParent(newAdc);
                        if (newEl instanceof Arc) {
                            ((DECollection) newAdc).replace(
                                    ((ContourCircle) newAdc).getCircle(),
                                    newEl);
                        } else {
                            ((DECollection) newAdc).replace(
                                    ((ContourCircle) newAdc).getLabel(), newEl);
                        }

                        gov.noaa.nws.ncep.ui.pgen.elements.Text lbl = ((ContourCircle) newAdc)
                                .getLabel();
                        boolean hide = lbl.getHide();
                        boolean auto = lbl.getAuto();
                        lbl.update(labelTemplate);
                        lbl.setHide(hide);
                        lbl.setAuto(auto);
                        lbl.setText(new String[] { getLabel() });
                    }
                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(oldContours);
            drawingLayer.replaceElement(oldContours, newContours);

            /*
             * Reset the selected Contours and DE to the updated ones.
             */
            currentContours = newContours;
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            if (newEl != null) {
                drawingLayer.removeSelected();
                drawingLayer.setSelected(newEl);
            }

        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }

    }

    /**
     * Private Contour Line attribute dialog class
     *
     * @author jwu
     *
     */
    protected class ContourLineAttrDlg extends LineAttrDlg {

        private ContourLineAttrDlg(Shell parShell) throws VizException {

            super(parShell);

        }

        /**
         * Update the line attributes
         */
        @Override
        public void okPressed() {
            /*
             * Update the line template first.
             */
            lineTemplate = (gov.noaa.nws.ncep.ui.pgen.elements.Line) new DrawableElementFactory()
                    .create(DrawableType.LINE, this, "Line", "LINE_SOLID",
                            (Coordinate) null, null);

            lineTemplate.setClosed(isClosedLine());

            // Need to set to contoursAttrSettings to be effective.
            contoursAttrSettings.put(getContourLineType(), lineTemplate);

            if (ContoursAttrDlg.this.drawContourLine()) {
                setButtonColor(activeContourLineBtn, defaultButtonColor,
                        lineTemplate.getColors()[0]);
            }

            /*
             * Update the Contours.
             */
            updateLineAttributes();
        }

        /**
         * Open the dialog if it doesn't exist.
         */
        @Override
        public int open() {

            if (this.getShell() == null || this.getShell().isDisposed()) {
                int ret = super.open();
                /*
                 * Set dialog location
                 */
                if (this.shellLocation == null) {
                    this.getShell().setLocation(getAttrDlgLocation());
                }
                return ret;
            } else {
                return CANCEL;
            }
        }

        /**
         * closes the line attribute dialog only
         */
        @Override
        public void cancelPressed() {
            attrDlgCleanUp();
            this.close();
        }

        @Override
        public void handleShellCloseEvent() {
            attrDlgCleanUp();
            this.close();
        }

        /**
         * disable un-used widgets
         */
        private void disableWidgets() {
            closedBtn.setEnabled(false);
        }

        /**
         * initialize dialog
         */
        private void initDlg() {
            this.getShell().setText("Contour Line Attributes");
        }

    }

    /**
     * Updates the selected contours' line attributes, then redraws the PGEN
     * layer. Note: if
     */
    private void updateLineAttributes() {

        /*
         * If no ContourLine is selected and "All" is not checked, do nothing.
         */
        DrawableElement de = drawingLayer.getSelectedDE();

        /*
         * Create a new Contours with all components in the old Contours and
         * update the line attributes if required.
         */
        DrawableElement newEl = null;

        Contours oldContours = null;

        if (de != null && de.getParent() instanceof ContourLine) {
            newEl = (DrawableElement) de.copy();
            oldContours = (Contours) de.getParent().getParent();
        } else {
            oldContours = currentContours;
        }

        if (oldContours != null) {

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Copy all contour lines and update the line attributes.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (newAdc instanceof ContourLine) {

                    if (newEl != null && oldAdc.equals(de.getParent())) {
                        newEl.setParent(newAdc);
                        if (newEl instanceof Line) {
                            ((DECollection) newAdc).replace(
                                    ((ContourLine) newAdc).getLine(), newEl);
                        }

                        ((ContourLine) newAdc).getLine().update(lineTemplate);
                        ((ContourLine) newAdc).getLine()
                                .setClosed(isClosedLine());

                    }

                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(oldContours);
            drawingLayer.replaceElement(oldContours, newContours);

            /*
             * Reset the selected contours and DE to the updated ones.
             */
            currentContours = newContours;
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            if (newEl != null) {
                drawingLayer.removeSelected();
                drawingLayer.setSelected(newEl);
            }

        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }

    }

    /**
     * Private Contour Circle attribute dialog class
     *
     * @author jwu
     *
     */
    protected class ContourCircleAttrDlg extends ArcAttrDlg {

        private ContourCircleAttrDlg(Shell parShell) throws VizException {

            super(parShell);

        }

        /**
         * Update the circle attributes
         */
        @Override
        public void okPressed() {
            if (ContoursAttrDlg.this.tool != null) {
                /*
                 * Update the circle template first.
                 */
                circleTemplate = (gov.noaa.nws.ncep.ui.pgen.elements.Arc) new DrawableElementFactory()
                        .create(DrawableType.ARC, this, "Arc",
                                PgenConstant.CIRCLE, (Coordinate) null, null);

                if (ContoursAttrDlg.this.drawCircle()) {
                    setButtonColor(circleTypeBtn, defaultButtonColor,
                            circleTemplate.getColors()[0]);
                }

                /*
                 * Update the Contours.
                 */
                updateCircleAttributes();
            }
        }

        /**
         * Open the dialog if it doesn't exist.
         */
        @Override
        public int open() {

            if (this.getShell() == null || this.getShell().isDisposed()) {
                int ret = super.open();
                /*
                 * Set dialog location
                 */
                if (this.shellLocation == null) {
                    this.getShell().setLocation(getAttrDlgLocation());
                }
                return ret;
            } else {
                return CANCEL;
            }
        }

        /**
         * closes the circle attribute dialog only
         */
        @Override
        public void cancelPressed() {
            attrDlgCleanUp();
            this.close();
        }

        @Override
        public void handleShellCloseEvent() {
            attrDlgCleanUp();
            this.close();
        }

        /**
         * disable un-used widgets
         */
        private void disableWidgets() {
            axisRatioLbl.setEnabled(false);
            axisRatioSlider.setEnabled(false);
            axisRatioText.setEnabled(false);

            startAngleLbl.setEnabled(false);
            startAngleSlider.setEnabled(false);
            startAngleText.setEnabled(false);

            endAngleLbl.setEnabled(false);
            endAngleSlider.setEnabled(false);
            endAngleText.setEnabled(false);
        }

        /**
         * initialize dialog
         */
        private void initDlg() {
            this.getShell().setText("Contour Circle Attributes");
        }

    }

    /**
     * Updates the selected contours' circle attributes, then redraws the PGEN
     * layer.
     */
    private void updateCircleAttributes() {

        /*
         * If no Contourcircle is selected and "All" is not checked, do nothing.
         */
        DrawableElement de = drawingLayer.getSelectedDE();

        /*
         * Create a new Contours with all components in the old Contours and
         * update the line attributes if required.
         */
        DrawableElement newEl = null;

        Contours oldContours = null;

        if (de != null && de.getParent() instanceof ContourCircle) {
            newEl = (DrawableElement) de.copy();
            oldContours = (Contours) de.getParent().getParent();
        } else {
            oldContours = currentContours;
        }

        if (oldContours != null) {

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Copy all contour lines and update the line attributes.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (newAdc instanceof ContourCircle) {

                    if (newEl != null && oldAdc.equals(de.getParent())) {
                        newEl.setParent(newAdc);
                        if (newEl instanceof Arc) {
                            ((DECollection) newAdc).replace(
                                    ((ContourCircle) newAdc).getCircle(),
                                    newEl);
                        }

                        ((ContourCircle) newAdc).getCircle()
                                .update(circleTemplate);

                    }

                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(oldContours);
            drawingLayer.replaceElement(oldContours, newContours);

            /*
             * Reset the selected contours and DE to the updated ones.
             */
            currentContours = newContours;
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            if (newEl != null) {
                drawingLayer.removeSelected();
                drawingLayer.setSelected(newEl);
            }

        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }

    }

    /*
     * Open the dialog to do graph-to-grid processing.
     */
    private void openG2GDlg() {

        if (g2gDlg == null) {
            try {

                g2gDlg = new GraphToGridParamDialog(PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell());
                g2gDlg.setCntAttrDlg(this);

            } catch (VizException e) {
                handler.error(
                        "Error opening dialog to do graph-to-grid processing",
                        e);
            }

        }

        if (g2gDlg != null) {
            g2gDlg.open();
        }

    }

    /**
     * Retrieve all line types, symbol and marker types defined in PGEN palette.
     */
    private void retrieveIconType() {

        PgenPaletteWindow plt = PgenSession.getInstance().getPgenPalette();

        // Get all Line types
        if (lineIconType == null) {
            lineIconType = new LinkedHashMap<>();
        }

        List<String> lineObjNames = plt.getObjectNames("Lines");

        for (String str : lineObjNames) {
            lineIconType.put(str, "Lines");
        }

        //
        lineItemMap = new LinkedHashMap<>();

        HashMap<String, IConfigurationElement> itemMap = plt.getItemMap();
        for (String str : itemMap.keySet()) {
            IConfigurationElement ifg = itemMap.get(str);
            String type = ifg.getName();
            if (type.equalsIgnoreCase("object")) {
                String cls = ifg.getAttribute("className");
                if (cls.equals("Lines")) {
                    lineItemMap.put(str, ifg);
                }
            }
        }

        // Get all Symbols
        symbolItemMap = new LinkedHashMap<>();

        for (String str : itemMap.keySet()) {
            IConfigurationElement ifg = itemMap.get(str);
            String type = ifg.getName();
            if (type.equalsIgnoreCase("object")) {
                String cls = ifg.getAttribute("className");
                if (cls.equals("Symbol") && !str.contains("TURBULENCE")) {
                    symbolItemMap.put(str, ifg);
                }
            }
        }

        // Get all Markers
        for (String str : itemMap.keySet()) {
            IConfigurationElement ifg = itemMap.get(str);
            String type = ifg.getName();
            if (type.equalsIgnoreCase("object")) {
                String cls = ifg.getAttribute("className");
                if (cls.equals("Marker")) {
                    symbolItemMap.put(str, ifg);
                }
            }
        }

        /*
         * Bet all Combo symbol's class "ComboSymbol" and symbol/marker's class
         * is "Symbol", switching between them requires more detailed handling -
         * so we do not use combos for now.
         */
    }

    /**
     * Retrieve image for an icon defined in PGEN palette.
     */
    protected Image getIcon(String iconName) {

        PgenPaletteWindow plt = PgenSession.getInstance().getPgenPalette();

        return plt.getButtonImage(iconName);

    }

    /**
     * Check if the Symbol check button is selected.
     */
    public boolean drawSymbol() {

        return drawingStatus == ContourDrawingStatus.DRAW_SYMBOL;

    }

    /**
     * Set the symbol drawing button.
     */
    public void setDrawingSymbol() {
        setDrawingStatus(ContourDrawingStatus.DRAW_SYMBOL);
    }

    /**
     * Check if the symbol-only button is selected.
     */
    public boolean isMinmaxSymbolOnly() {
        return symbolOnlyBtn.getSelection();
    }

    /**
     * Set the symbol-only button.
     */
    public void setMinmaxSymbolOnly(boolean symbolOnly) {
        symbolOnlyBtn.setSelection(symbolOnly);
        hideSymbolLabelBtn.setEnabled(true);
        if (symbolOnly) {
            labelOnlyBtn.setSelection(false);
            hideSymbolLabelBtn.setSelection(false);
            hideSymbolLabelBtn.setEnabled(false);
        }

        saveBtnLastStatus(symbolOnlyBtn);
        saveBtnLastStatus(labelOnlyBtn);
        saveBtnLastStatus(hideSymbolLabelBtn);
    }

    /**
     * Check if the label-only button is selected.
     */
    public boolean isMinmaxLabelOnly() {
        return labelOnlyBtn.getSelection();
    }

    /**
     * Set the label-only button.
     */
    public void setMinmaxLabelOnly(boolean labelOnly) {
        labelOnlyBtn.setSelection(labelOnly);
        hideSymbolLabelBtn.setEnabled(true);
        if (labelOnly) {
            symbolOnlyBtn.setSelection(false);
            hideSymbolLabelBtn.setSelection(false);
            hideSymbolLabelBtn.setEnabled(false);
        }

        saveBtnLastStatus(symbolOnlyBtn);
        saveBtnLastStatus(labelOnlyBtn);
        saveBtnLastStatus(hideSymbolLabelBtn);
    }

    /**
     * Check if the symbol label hiding button is selected.
     */
    public boolean hideSymbolLabel() {
        return hideSymbolLabelBtn.getSelection();
    }

    /**
     * Set the symbol label hiding button.
     */
    public void setHideSymbolLabel(boolean hide) {
        hideSymbolLabelBtn.setSelection(hide);
        saveBtnLastStatus(hideSymbolLabelBtn);
    }

    /**
     * Set the active symbol's type and image to a selected one. If not found in
     * the list, force to be the first one.
     */
    public void setActiveSymbol(DrawableElement elem) {

        boolean found = false;

        String symboltype = elem.getPgenType();
        Color clr = elem.getColors()[0];
        for (Button btn : contourSymbolBtns) {

            if (symboltype.equals(btn.getData().toString())) {
                btn.setToolTipText(
                        symbolItemMap.get(symboltype).getAttribute("label"));
                btn.setImage(getIcon(symboltype));
                activeContourSymbolBtn = btn;
                setButtonColor(btn, defaultButtonColor, clr);
                found = true;
            } else {
                setButtonColor(btn, activeButtonColor, defaultButtonColor);
            }
        }

        if (!found) {
            activeContourSymbolBtn = contourSymbolBtns.get(0);
            activeContourSymbolBtn.setData(symboltype);
            activeContourSymbolBtn.setImage(getIcon(symboltype));
            setButtonColor(activeContourSymbolBtn, defaultButtonColor, clr);
        }

        contoursAttrSettings.put(symboltype, elem);

    }

    /**
     * Get the active symbol's type (pgenType string).
     */
    public String getActiveSymbolObjType() {
        return activeContourSymbolBtn.getData().toString();
    }

    /**
     * Get the active symbol's class (pgenCategory string).
     */
    public String getActiveSymbolClass() {
        return symbolItemMap.get(getActiveSymbolObjType())
                .getAttribute("className");

    }

    /**
     * Set the Line Drawing button.
     */
    public void setDrawingLine() {
        setDrawingStatus(ContourDrawingStatus.DRAW_LINE);
    }

    /**
     * Set the line type and image.
     */
    public void setContourLineType(String str) {
        activeContourLineBtn.setData(str);
        activeContourLineBtn.setImage(getIcon(str));
    }

    /**
     * Get the line type (pgenType string).
     */
    public String getContourLineType() {
        return activeContourLineBtn.getData().toString();
    }

    /**
     * Check if the circle drawing button is selected.
     */
    public boolean drawCircle() {
        return drawingStatus == ContourDrawingStatus.DRAW_CIRCLE;
    }

    /**
     * Set the circle drawing button.
     */
    public void setDrawingCircle() {
        setDrawingStatus(ContourDrawingStatus.DRAW_CIRCLE);
    }

    /**
     * Check if the circle label hiding button is selected.
     */
    public boolean hideCircleLabel() {
        return hideCircleLabelBtn.getSelection();
    }

    /**
     * Set the circle label hiding button.
     */
    public void setHideCircleLabel(boolean hide) {
        hideCircleLabelBtn.setSelection(hide);
    }

    /**
     * Class for a a list of Symbols to be selected for drawing min/max.
     *
     * @author jun
     *
     */
    public class LineTypeSelectionDlg extends Dialog {

        private List<String> objNames;

        private String selectedType;

        private Button activator;

        private String title;

        private Composite top;

        /**
         * constructor
         */
        public LineTypeSelectionDlg(Shell parent, List<String> objNames,
                Button activator, String title) {

            super(parent);

            this.objNames = objNames;
            this.activator = activator;
            this.selectedType = activator.getData().toString();
            this.title = title;

        }

        /**
         * Add Accept and Cancel buttons on the dialog's button bar.
         */
        @Override
        public void createButtonsForButtonBar(Composite parent) {

            super.createButtonsForButtonBar(parent);
            this.getButton(IDialogConstants.OK_ID).setText("Accept");

        }

        /**
         * @param activator
         *            the activator to set
         */
        public void setActivator(Button activator) {
            this.activator = activator;
        }

        /**
         * Creates the dialog area
         */
        @Override
        public Control createDialogArea(Composite parent) {

            top = (Composite) super.createDialogArea(parent);
            this.getShell().setText(title);

            GridLayout mainLayout = new GridLayout(SYMBOLCOL, false);
            mainLayout.marginHeight = 3;
            mainLayout.marginWidth = 3;
            mainLayout.horizontalSpacing = 0;
            mainLayout.verticalSpacing = 0;
            top.setLayout(mainLayout);

            for (String str : objNames) {
                Button btn = new Button(top, SWT.PUSH);
                btn.setData(str);
                btn.setImage(getIcon(str));
                btn.setToolTipText(lineItemMap.get(str).getAttribute("label"));

                if (str.equals(activator.getData().toString())) {
                    Color clr = defaultButtonColor;
                    Line ade = (Line) contoursAttrSettings.get(str);
                    if (ade != null) {
                        clr = ade.getColors()[0];
                    }
                    setButtonColor(btn, clr);
                } else {
                    setButtonColor(btn, defaultButtonColor);
                }

                btn.addListener(SWT.MouseDown, new Listener() {

                    @Override
                    public void handleEvent(Event event) {

                        Control[] wids = top.getChildren();

                        if (wids != null) {
                            for (int kk = 0; kk < wids.length; kk++) {
                                setButtonColor((Button) wids[kk],
                                        defaultButtonColor);
                            }
                        }

                        String objstr = event.widget.getData().toString();
                        selectedType = objstr;

                        Color clr = defaultButtonColor;
                        Line ade = (Line) contoursAttrSettings.get(objstr);
                        if (ade != null) {
                            clr = ade.getColors()[0];
                        }

                        setButtonColor((Button) event.widget, clr);

                    }
                });

            }

            return top;
        }

        /**
         * Updates the selected type's data string and image icon.
         */
        @Override
        public void okPressed() {

            Color clr = activeButtonColor;
            Line ade = (Line) contoursAttrSettings.get(selectedType);
            if (ade != null) {
                clr = ade.getColors()[0];
                if (lineAttrDlg != null) {
                    lineAttrDlg.setAttrForDlg(ade);
                }
            }
            setButtonColor(activator, clr);

            activator.setData(selectedType);
            activator.setImage(getIcon(selectedType));
            activator.setToolTipText(
                    lineItemMap.get(selectedType).getAttribute("label"));
            close();
        }

    }

    /**
     * Class for a a list of Symbols to be selected for drawing min/max.
     *
     * @author jun
     *
     */
    public class SymbolTypeSelectionDlg extends Dialog {

        private Composite top;

        private List<String> objNames;

        private String selectedType;

        private Button activator;

        private String title;

        /**
         * constructor
         */
        public SymbolTypeSelectionDlg(Shell parent, List<String> objNames,
                Button activator, String title) {

            super(parent);

            this.objNames = objNames;
            this.activator = activator;
            this.selectedType = activator.getData().toString();
            this.title = title;

        }

        /**
         * Add Accept and Cancel buttons on the dialog's button bar.
         */
        @Override
        public void createButtonsForButtonBar(Composite parent) {

            super.createButtonsForButtonBar(parent);
            this.getButton(IDialogConstants.OK_ID).setText("Accept");

        }

        /**
         * @param activator
         *            the activator to set
         */
        public void setActivator(Button activator) {
            this.activator = activator;
        }

        /**
         * Creates the dialog area
         */
        @Override
        public Control createDialogArea(Composite parent) {

            top = (Composite) super.createDialogArea(parent);
            this.getShell().setText(title);

            GridLayout mainLayout = new GridLayout(SYMBOLCOL, false);
            mainLayout.marginHeight = 3;
            mainLayout.marginWidth = 3;
            mainLayout.horizontalSpacing = 0;
            mainLayout.verticalSpacing = 0;
            top.setLayout(mainLayout);

            for (String str : objNames) {
                Button btn = new Button(top, SWT.PUSH);
                btn.setData(str);
                btn.setImage(getIcon(str));
                btn.setToolTipText(
                        symbolItemMap.get(str).getAttribute("label"));

                if (str.equals(activator.getData().toString())) {
                    Color clr = defaultButtonColor;
                    SinglePointElement ade = (SinglePointElement) contoursAttrSettings
                            .get(str);
                    if (ade != null) {
                        clr = ade.getColors()[0];
                    }
                    setButtonColor(btn, clr);
                } else {
                    setButtonColor(btn, defaultButtonColor);
                }

                btn.addListener(SWT.MouseDown, new Listener() {

                    @Override
                    public void handleEvent(Event event) {

                        Control[] wids = top.getChildren();

                        if (wids != null) {
                            for (int kk = 0; kk < wids.length; kk++) {
                                setButtonColor((Button) wids[kk],
                                        defaultButtonColor);
                            }
                        }

                        String objstr = event.widget.getData().toString();
                        selectedType = objstr;
                        Color clr1 = activeButtonColor;

                        SinglePointElement ades = (SinglePointElement) contoursAttrSettings
                                .get(objstr);
                        if (ades != null) {
                            clr1 = ades.getColors()[0];
                        }

                        setButtonColor((Button) event.widget, clr1);

                    }
                });

            }

            return top;
        }

        /**
         * Updates the selected type's data string and image icon.
         */
        @Override
        public void okPressed() {

            Color clr = activeButtonColor;
            SinglePointElement ade = (SinglePointElement) contoursAttrSettings
                    .get(selectedType);
            if (ade != null) {
                clr = ade.getColors()[0];
            }
            setButtonColor(activator, clr);

            if (minmaxAttrDlg != null) {
                minmaxAttrDlg.setColor(clr);
            }
            activator.setData(selectedType);
            activator.setImage(getIcon(selectedType));
            activator.setToolTipText(
                    symbolItemMap.get(selectedType).getAttribute("label"));

            close();
        }

    }

    /*
     * Open the dialog to choose a symbol for drawing min/max.
     */
    private void openSymbolPanel() {

        if (symbolTypePanel == null) {
            List<String> objs = new ArrayList<>(symbolItemMap.keySet());
            Shell sh = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();

            symbolTypePanel = new SymbolTypeSelectionDlg(sh, objs,
                    activeContourSymbolBtn, "Select Contouring Button");
        }

        if (symbolTypePanel != null) {
            symbolTypePanel.setActivator(activeContourSymbolBtn);
            symbolTypePanel.open();
        }

    }

    /*
     * Open the dialog to choose a line type for drawing.
     */
    private void openLineTypePanel() {

        if (lineTypePanel == null) {
            List<String> objs = new ArrayList<>(lineIconType.keySet());
            Shell sh = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            lineTypePanel = new LineTypeSelectionDlg(sh, objs,
                    activeContourLineBtn, "Contours Line Types");
        }

        if (lineTypePanel != null) {
            lineTypePanel.setActivator(activeContourLineBtn);
            lineTypePanel.open();
        }

    }

    /**
     * Private Contour Min/Max attribute dialog class
     *
     * @author jwu
     *
     */
    protected class ContourMinmaxAttrDlg extends LabeledSymbolAttrDlg {

        private PgenContoursTool tool = null;

        private Contours prevCont = null;

        private Contours nowCont = null;

        private ContourMinmaxAttrDlg(Shell parShell) throws VizException {

            super(parShell);

        }

        @Override
        public int open() {

            if (this.getShell() == null || this.getShell().isDisposed()) {
                int ret = super.open();
                /*
                 * Set dialog location
                 */
                if (this.shellLocation == null) {
                    this.getShell().setLocation(getAttrDlgLocation());
                }
                return ret;
            } else {
                return CANCEL;
            }
        }

        /**
         * Update the min/max attributes
         */
        @Override
        public void okPressed() {

            /*
             * Update the symbol template first.
             */
            minmaxTemplate = (gov.noaa.nws.ncep.ui.pgen.elements.Symbol) new DrawableElementFactory()
                    .create(DrawableType.SYMBOL, this, "Symbol",
                            getActiveSymbolObjType(), (Coordinate) null, null);
            contoursAttrSettings.put(getActiveSymbolObjType(), minmaxTemplate);

            if (ContoursAttrDlg.this.drawSymbol()) {
                setButtonColor(activeContourSymbolBtn, defaultButtonColor,
                        this.getColors()[0]);
            }

            /*
             * Update the Contours.
             */
            updateMinmaxAttributes();

        }

        /**
         * closes the line attribute dialog only
         */
        @Override
        public void cancelPressed() {
            attrDlgCleanUp();
            this.close();
        }

        @Override
        public void handleShellCloseEvent() {
            attrDlgCleanUp();
            this.close();
        }

        /**
         * initialize dialog
         */
        private void initDlg() {
            this.getShell().setText("Contour Min/Max Attributes");
            super.setLabelChkBox(false);
            AbstractPgenTool apt = (AbstractPgenTool) VizPerspectiveListener
                    .getCurrentPerspectiveManager().getToolManager()
                    .getSelectedModalTool("gov.noaa.nws.ncep.viz.ui.modalTool");
            if (apt instanceof PgenContoursTool) {
                tool = (PgenContoursTool) apt;
            }

            if (tool != null) {
                tool.resetUndoRedoCount();
                PgenSession.getInstance().getCommandManager()
                        .addStackListener(tool);
            }

            // set the lat/lon from the current symbol.
            DrawableElement de = drawingLayer.getSelectedDE();
            if (de != null && de.getParent() instanceof ContourMinmax
                    && de instanceof Symbol) {
                super.setLatitude(((Symbol) de).getLocation().y);
                super.setLongitude(((Symbol) de).getLocation().x);
            }

            /*
             * Reset the listener.
             */
            for (Listener ls : undoBtn.getListeners(SWT.MouseDown)) {
                undoBtn.removeListener(SWT.MouseDown, ls);
            }

            undoBtn.addListener(SWT.MouseDown, new Listener() {

                @Override
                public void handleEvent(Event event) {

                    if (undoBtn.getText().equalsIgnoreCase(UNDO_SYMBOL)) {
                        undoBtn.setText(REDO_SYMBOL);
                        drawingLayer.getCommandMgr().undo();

                    } else if (undoBtn.getText()
                            .equalsIgnoreCase(REDO_SYMBOL)) {
                        undoBtn.setText(UNDO_SYMBOL);
                        drawingLayer.getCommandMgr().redo();
                    }

                    /*
                     * Reset the currentContours for the ContoursAttrDlg.
                     */
                    currentContours = prevCont;
                    prevCont = nowCont;
                    nowCont = currentContours;

                    mapEditor.refresh();

                }

            });
        }

        @Override
        public boolean close() {
            if (tool != null) {
                tool.resetUndoRedoCount();
                PgenSession.getInstance().getCommandManager()
                        .removeStackListener(tool);
            }

            prevCont = null;
            nowCont = null;

            return super.close();
        }

        /**
         * Place the symbol at location from the lat/lon text fields
         */
        @Override
        protected void placeSymbol() {

            if (tool != null) {
                if (tool.getMouseHandler() instanceof PgenContoursHandler) {
                    /*
                     * Keep a copy of currentContours for "Undo". It changes
                     * after call the "tool".
                     */
                    prevCont = currentContours;
                    ((PgenContoursHandler) tool.getMouseHandler())
                            .drawContourMinmax(new Coordinate(
                                    Double.parseDouble(longitudeText.getText()),
                                    Double.parseDouble(
                                            latitudeText.getText())));
                    placeBtn.setEnabled(false);
                    nowCont = currentContours; // Keep a copy for "Redo"
                    undoBtn.setEnabled(true);
                    undoBtn.setText("Undo Symbol");

                } else if (tool
                        .getMouseHandler() instanceof PgenSelectHandler) {
                    minmaxTemplate = (gov.noaa.nws.ncep.ui.pgen.elements.Symbol) new DrawableElementFactory()
                            .create(DrawableType.SYMBOL, this, "Symbol",
                                    getActiveSymbolObjType(), (Coordinate) null,
                                    null);
                    contoursAttrSettings.put(getActiveSymbolObjType(),
                            minmaxTemplate);

                    updateMinmaxAttributes();

                    placeBtn.setEnabled(false);

                }
            }
        }
    }

    /**
     * Updates the selected contours' Minmax attributes, then redraws the PGEN
     * layer.
     */
    private void updateMinmaxAttributes() {

        /*
         * If no ContourLine is selected and "All" is not checked, do nothing.
         */
        DrawableElement de = drawingLayer.getSelectedDE();

        /*
         * Create a new Contours with all components in the old Contours and
         * update the line attributes if required.
         */
        DrawableElement newEl = null;

        Contours oldContours = null;

        if (de != null && de.getParent() instanceof ContourMinmax) {
            newEl = (DrawableElement) de.copy();
            oldContours = (Contours) de.getParent().getParent();
        } else {
            oldContours = currentContours;
        }

        if (oldContours != null) {

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Copy all contour Minmax symbols and update the minmax attributes.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (newAdc instanceof ContourMinmax) {

                    if (newEl != null && oldAdc.equals(de.getParent())) {
                        newEl.setParent(newAdc);

                        if (newEl instanceof Symbol) {

                            if (minmaxAttrDlg != null
                                    && minmaxAttrDlg.getShell() != null) {
                                if (minmaxAttrDlg.latitudeText.isEnabled()
                                        && minmaxAttrDlg.longitudeText
                                                .isEnabled()) {
                                    ArrayList<Coordinate> loc = new ArrayList<>();
                                    double lat = ((Symbol) newEl)
                                            .getLocation().y;
                                    double lon = ((Symbol) newEl)
                                            .getLocation().x;
                                    try {
                                        lon = Double.valueOf(
                                                minmaxAttrDlg.longitudeText
                                                        .getText());
                                        lat = Double.valueOf(
                                                minmaxAttrDlg.latitudeText
                                                        .getText());
                                    } catch (Exception e) {
                                        lon = ((Symbol) newEl).getLocation().x;
                                        lat = ((Symbol) newEl).getLocation().y;
                                    }

                                    loc.add(new Coordinate(lon, lat));
                                    newEl.setPoints(loc);
                                }
                            }

                            ((DECollection) newAdc).replace(
                                    ((ContourMinmax) newAdc).getSymbol(),
                                    newEl);
                        }

                        if (((ContourMinmax) newAdc).getSymbol() != null) {
                            ((ContourMinmax) newAdc).getSymbol()
                                    .update(minmaxTemplate);
                        }

                        if (((ContourMinmax) newAdc).getLabel() != null) {
                            ((ContourMinmax) newAdc).getLabel().setAuto(true);
                        }
                    }
                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(oldContours);
            drawingLayer.replaceElement(oldContours, newContours);

            /*
             * Reset the selected contours and DE to the updated ones.
             */
            currentContours = newContours;
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            if (newEl != null) {
                drawingLayer.removeSelected();
                drawingLayer.setSelected(newEl);
            }

        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }

    }

    /**
     * Set a button's color
     */
    private void setButtonColor(Button btn, Color clr) {

        btn.setBackground(
                new org.eclipse.swt.graphics.Color(this.getShell().getDisplay(),
                        clr.getRed(), clr.getGreen(), clr.getBlue()));

    }

    /**
     * set a button's background/foreground color
     */
    private void setButtonColor(Button btn, Color fclr, Color bclr) {

        if (btn != null) {
            btn.setBackground(new org.eclipse.swt.graphics.Color(
                    this.getShell().getDisplay(), bclr.getRed(),
                    bclr.getGreen(), bclr.getBlue()));

            btn.setForeground(new org.eclipse.swt.graphics.Color(
                    this.getShell().getDisplay(), fclr.getRed(),
                    fclr.getGreen(), fclr.getBlue()));
        }
    }

    /**
     * Set the drawing status (LINE/MINMAX/CIRCLE)
     */
    public void setDrawingStatus(ContourDrawingStatus st) {

        if (tool != null) {
            tool.clearSelected();
        }

        switch (st) {

        case DRAW_SYMBOL:

            setButtonColor(activeContourLineBtn, activeButtonColor,
                    defaultButtonColor);
            setButtonColor(circleTypeBtn, activeButtonColor,
                    defaultButtonColor);

            drawingStatus = ContourDrawingStatus.DRAW_SYMBOL;
            if (!(tool.getMouseHandler() instanceof PgenContoursHandler)) {
                tool.setPgenContoursHandler();
            }
            break;

        case DRAW_CIRCLE:

            setButtonColor(activeContourLineBtn, activeButtonColor,
                    defaultButtonColor);
            setButtonColor(activeContourSymbolBtn, activeButtonColor,
                    defaultButtonColor);

            drawingStatus = ContourDrawingStatus.DRAW_CIRCLE;
            if (!(tool.getMouseHandler() instanceof PgenContoursHandler)) {
                tool.setPgenContoursHandler();
            }

            break;

        case DRAW_LINE:

            setButtonColor(activeContourSymbolBtn, activeButtonColor,
                    defaultButtonColor);
            setButtonColor(circleTypeBtn, activeButtonColor,
                    defaultButtonColor);

            drawingStatus = ContourDrawingStatus.DRAW_LINE;
            if (tool != null && !(tool
                    .getMouseHandler() instanceof PgenContoursHandler)) {
                tool.setPgenContoursHandler();
            }
            break;

        case SELECT:
            setSelectMode();
            break;

        default:

            setButtonColor(activeContourSymbolBtn, activeButtonColor,
                    defaultButtonColor);
            setButtonColor(circleTypeBtn, activeButtonColor,
                    defaultButtonColor);

            drawingStatus = ContourDrawingStatus.DRAW_LINE;

            break;
        }

        // Set keyboard focus to the label text.
        setLabelFocus();

    }

    /**
     * Set to selecting mode.
     */
    public void setSelectMode() {
        setButtonColor(activeContourSymbolBtn, activeButtonColor,
                defaultButtonColor);
        setButtonColor(circleTypeBtn, activeButtonColor, defaultButtonColor);
        setButtonColor(activeContourLineBtn, activeButtonColor,
                defaultButtonColor);

        drawingStatus = ContourDrawingStatus.SELECT;

        if (!(tool.getMouseHandler() instanceof PgenSelectHandler)) {
            tool.setPgenSelectHandler();
        }
    }

    /**
     * Get a list of contouring symbols defined in for ContoursInfo.xml If none
     * of them is selected, the "H" and "L" will be selected by default.
     *
     * @param
     * @return
     */
    private List<String> getContourSymbols() {

        List<String> lbls = new ArrayList<>();

        int selected = 0;

        HashMap<String, ContoursInfo> cntinfo = ContoursInfoDlg.readInfoTbl();

        ContoursInfo coninf = cntinfo.get(getParm());
        if (coninf != null) {
            ContourButtons btns = coninf.getButtons();

            List<ContourObject> objects = btns.getObjects();
            for (ContourObject obj : objects) {
                String className = obj.getClassName();
                String name = obj.getName();

                if ("Symbol".equals(className)) {
                    lbls.add(name);
                    selected++;
                }

            }
        }

        numOfButtons = selected;

        if (selected == 0) {
            lbls.add("FILLED_HIGH_PRESSURE_H");
            lbls.add("FILLED_LOW_PRESSURE_L");
            numOfButtons = 2;
        }

        numOfContrSymbols = numOfButtons;

        return lbls;
    }

    /**
     * Retrieve default settings from "settings.tbl" for contour line, symbol,
     * label, and circle.
     *
     * @param
     * @return
     */
    private void retrieveContoursSettings() {

        String skey = AttrSettings.getInstance().getSettingsName();

        if (contoursAttrSettingsMap == null) {
            contoursAttrSettingsMap = new HashMap<>();
            contoursAttrSettings = new HashMap<>();
        }

        // Each settings table should be only loaded once.
        if (contoursAttrSettingsMap.get(skey) != null) {
            contoursAttrSettings = contoursAttrSettingsMap.get(skey);
        } else {
            String defaultSettingsContoursKey = AttrSettings
                    .getContoursSettingsKey(getParm());
            HashMap<String, AbstractDrawableComponent> ncontoursAttrSettings = new HashMap<>();

            ncontoursAttrSettings.put(PgenConstant.CONTOURS,
                    retrieveDefaultSettings(defaultSettingsContoursKey));

            // Get all line types from "settings.tbl"
            for (String str : lineIconType.keySet()) {
                ncontoursAttrSettings.put(str, retrieveDefaultSettings(str));
            }

            // Get all symbols/markers from "settings.tbl"
            for (String str : symbolItemMap.keySet()) {
                ncontoursAttrSettings.put(str, retrieveDefaultSettings(str));
            }

            // Get Default for Circle.
            ncontoursAttrSettings.put(PgenConstant.CIRCLE,
                    retrieveDefaultSettings(PgenConstant.CIRCLE));

            // Get Default for label.
            ncontoursAttrSettings.put(PgenConstant.TYPE_GENERAL_TEXT,
                    retrieveDefaultSettings(PgenConstant.TYPE_GENERAL_TEXT));

            /*
             * Get line, text, symbols/markers, circles found in the Contours in
             * "settings.tbl"
             */
            AbstractDrawableComponent adc = retrieveDefaultSettings(
                    defaultSettingsContoursKey);
            if (adc != null && adc instanceof Contours) {
                List<ContourLine> cline = ((Contours) adc).getContourLines();
                if (cline != null && cline.size() > 0) {
                    for (ContourLine cln : cline) {
                        Line ln = cln.getLine();
                        if (ln != null) {
                            ncontoursAttrSettings.put(ln.getPgenType(),
                                    ln.copy());

                            if (cln.getLabels() != null
                                    && cln.getLabels().size() > 0) {
                                String lblKey = new String(
                                        ln.getPgenType() + labelSuffix);
                                ncontoursAttrSettings.put(lblKey,
                                        cln.getLabels().get(0).copy());
                            }
                        }
                    }
                }

                List<ContourMinmax> csymbols = ((Contours) adc)
                        .getContourMinmaxs();
                if (csymbols != null && csymbols.size() > 0) {
                    for (ContourMinmax cmx : csymbols) {
                        if (cmx.getSymbol() != null) {
                            ncontoursAttrSettings.put(
                                    cmx.getSymbol().getPgenType(),
                                    cmx.getSymbol().copy());
                        }

                        if (cmx.getLabel() != null) {
                            String lblKey = getLabelTempKey(cmx.getLabel());
                            ncontoursAttrSettings.put(lblKey,
                                    cmx.getLabel().copy());
                        }
                    }
                }

                List<ContourCircle> ccircles = ((Contours) adc)
                        .getContourCircles();
                if (ccircles != null && ccircles.size() > 0) {
                    Arc cc = (Arc) ccircles.get(0).getCircle();
                    if (cc != null) {
                        ncontoursAttrSettings.put(cc.getPgenType(), cc.copy());
                    }

                    if (ccircles.get(0).getLabel() != null) {
                        String lblKey = new String(
                                cc.getPgenType() + labelSuffix);
                        ncontoursAttrSettings.put(lblKey,
                                ccircles.get(0).getLabel().copy());
                    }
                }
            }

            contoursAttrSettingsMap.put(skey, ncontoursAttrSettings);
            contoursAttrSettings = ncontoursAttrSettings;
        }
    }

    /**
     * Retrieve default settings for a given type of DE.
     *
     * @param key
     *            key for the drawable component (typically PGEN type, but
     *            contours should use
     *            {@link AttrSettings#getContourSettingsKey})
     * @return AbstractDrawableComponent
     */
    private AbstractDrawableComponent retrieveDefaultSettings(String key) {
        return AttrSettings.getInstance().getSettings(key);
    }

    @Override
    public Coordinate[] getLinePoints() {
        //
        return null;
    }

    @Override
    public String getPatternName() {
        //
        return null;
    }

    @Override
    public int getSmoothFactor() {
        //
        return 0;
    }

    @Override
    public Boolean isFilled() {
        //
        return null;
    }

    @Override
    public FillPattern getFillPattern() {
        //
        return null;
    }

    /**
     * set the drawing tool(can be PgenSeletingTool or PgenJetDrawingTool)
     */
    public void setDrawingTool(PgenContoursTool tool) {
        this.tool = tool;
    }

    /**
     * Removes ghost line, handle bars, and closes the dialog
     */
    @Override
    public void cancelPressed() {

        PgenUtil.setSelectingMode();
        super.cancelPressed();

    }

    /**
     * Call this to keep keyboard focus on the contour label text field. So the
     * user could type a new interval value without mouse click on the dialog.
     * Also, the original text should be selected so whatever the user types
     * will replace the existing value.
     */
    public void setLabelFocus() {
        if (!labelTxt.isDisposed()) {
            labelTxt.setFocus();
            labelTxt.selectAll();

            updateLabelBtnsSelection(labelTxt.getText());
        }
    }

    /**
     * Move the label selection up to next one or or down to previous one.
     */
    public void upDownLabelSelection(String which) {

        if (labelBtns == null || labelBtns.size() == 0) {
            return;
        }

        boolean upArrowClicked = which.equals("ARROW_UP");
        int ii = 0;
        for (Button btn : labelBtns) {

            if (btn.getSelection()) {

                int next;
                if (upArrowClicked) {
                    next = (ii + 1) % labelBtns.size();
                } else {
                    next = ((ii - 1) + labelBtns.size()) % labelBtns.size();
                }

                btn.setSelection(false);
                labelBtns.get(next).setSelection(true);
                labelTxt.setText(labelBtns.get(next).getText());

                /*
                 * Remember to keep focus on the label text field so the user
                 * can edit it immediately.
                 */
                setLabelFocus();
                break;
            }

            ii++;
        }

        if (labelTemplate != null) {
            labelTemplate.setText(new String[] { getLabel() });
        }

    }

    /**
     * Get a list of contouring lines defined in the contours info xml files. If
     * none of them is selected, the "LINE_SOLID" and "LINE_DASHED_2" will be
     * selected by default.
     *
     * @param
     * @return
     */
    private List<String> getContourLineSymbols() {

        List<String> lbls = new ArrayList<>();

        int selected = 0;

        // Read information in the contours info xml files
        HashMap<String, ContoursInfo> cntinfo = ContoursInfoDlg.readInfoTbl();

        // make a list of contour line objects from contours info xml file
        ContoursInfo coninf = cntinfo.get(getParm());
        if (coninf != null) {
            ContourLines lines = coninf.getLines();

            List<ContourObject> objects = lines.getObjects();

            for (ContourObject obj : objects) {
                String className = obj.getClassName();
                String name = obj.getName();

                if (null == className || className.isEmpty()) {
                    lbls.add(name);
                }

                selected++;
            }

        }

        if (selected == 0) {
            lbls.add("LINE_SOLID");
            lbls.add("LINE_DASHED_2");
        }

        return lbls;
    }

    /**
     * Set the active line's type and image to a selected one. If not found in
     * the list, force to be the first one.
     */
    public void setActiveLine(DrawableElement elem) {

        boolean found = false;
        String lineType = elem.getPgenType();
        Color clr = elem.getColors()[0];
        for (Button btn : contourLineBtns) {

            if (lineType.equals(btn.getData().toString())) {
                btn.setToolTipText(
                        lineItemMap.get(lineType).getAttribute("label"));
                btn.setImage(getIcon(lineType));
                activeContourLineBtn = btn;
                setButtonColor(btn, defaultButtonColor, clr);
                found = true;
            } else {
                setButtonColor(btn, activeButtonColor, defaultButtonColor);
            }
        }

        if (!found) {
            activeContourLineBtn = contourLineBtns.get(0);
            activeContourLineBtn.setData(lineType);
            activeContourLineBtn.setImage(getIcon(lineType));
            setButtonColor(activeContourLineBtn, defaultButtonColor, clr);
        }

        contoursAttrSettings.put(lineType, elem);

    }

    /**
     * Change the selected contour line's type.
     */
    public boolean changeLineType() {

        /*
         * Create a new Contours as well as a new ContourLine with a new line
         * and a label.
         */
        boolean typeChanged = false;
        DrawableElement de = drawingLayer.getSelectedDE();
        if (de != null && de instanceof Line && de.getParent() != null
                && de.getParent().getParent() instanceof Contours) {

            DrawableElement newEl = (DrawableElement) de.copy();

            Contours oldContours = (Contours) de.getParent().getParent();

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Replace the selected contour line with a new contour line and
             * copy over other non-selected contour lines.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (oldAdc.equals(de.getParent())) {

                    newEl.setParent(newAdc);

                    if (oldAdc instanceof ContourLine) {

                        if (newEl instanceof Line) {
                            ((DECollection) newAdc).replace(
                                    ((ContourLine) newAdc).getLine(), newEl);
                        }

                        String ltype = getContourLineType();
                        ((ContourLine) newAdc).getLine().setPgenType(ltype);

                        Line ade = (Line) contoursAttrSettings.get(ltype);
                        if (ade != null) {
                            ((ContourLine) newAdc).getLine()
                                    .setColors(ade.getColors());

                            if (isUseMainColor()) {
                                for (gov.noaa.nws.ncep.ui.pgen.elements.Text lbl : ((ContourLine) newAdc)
                                        .getLabels()) {
                                    lbl.setColors(ade.getColors());
                                }
                            }
                        }

                    }

                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(this);
            drawingLayer.replaceElement(oldContours, newContours);
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            /*
             * Reset the selected contours and DE to the updated ones.
             */
            currentContours = newContours;
            drawingLayer.removeSelected();
            if (newEl != null) {
                drawingLayer.setSelected(newEl);
            }

            if (mapEditor != null) {
                mapEditor.refresh();
            }

            typeChanged = true;
        }

        return typeChanged;
    }

    /**
     * Update lat/lon and Undo button on SymbolAttrDlg.
     */
    public void updateSymbolAttrOnGUI(Coordinate loc) {
        if (minmaxAttrDlg != null && minmaxAttrDlg.getShell() != null) {
            minmaxAttrDlg.setLatitude(loc.y);
            minmaxAttrDlg.setLongitude(loc.x);
            minmaxAttrDlg.enableUndoBtn(true);
        }
    }

    @Override
    public int open() {
        if (drawingLayer == null ) {
            return CANCEL;
        }

        /*
         * Use defaults defined for this layer. Otherwise, use the default from
         * settings table.
         */
        if (drawingLayer.getSelectedDE() == null) {
            // Check for "One Contours Per Layer" rule.
            Contours oneContours = findExistingContours();
            if (oneContours != null) {
                contourParm = oneContours.getParm();
                contourLevel = oneContours.getLevel();
                contourFcstHr = oneContours.getForecastHour();
                contourCint = oneContours.getCint();
                contourTime1 = oneContours.getTime1();
                contourTime2 = oneContours.getTime2();
            } else {
                String parmOnLayer = getContourParmOnLayer();
                ContourDefault contourDef = ContoursInfoDlg
                        .getContourMetaDefault(parmOnLayer);

                // Use "parm" defined on the layer.
                if (parmOnLayer != PgenConstant.NONE) {
                    contourParm = parmOnLayer;
                }

                /*
                 * Get the default attributes from settings table. Need to do
                 * this after setting contourParm above since different parms
                 * can have different contour settings.
                 */
                retrieveContoursSettings();

                Contours adc = (Contours) contoursAttrSettings
                        .get(PgenConstant.CONTOURS);
                if (parmOnLayer == PgenConstant.NONE && adc != null) {
                    contourParm = adc.getParm();
                }

                /*
                 * Use defaults defined for level/fcsthr/cint defined in
                 * contourInfoXXXX.xml, which is linked with this layer, where
                 * "XXXX" is the name of the contours' parameter name.
                 */
                if (contourDef != null) {
                    contourLevel = contourDef.getLevel();
                    contourFcstHr = contourDef.getFhrs();
                    contourCint = contourDef.getCint();
                } else if (adc != null) {
                    contourLevel = adc.getLevel();
                    contourFcstHr = adc.getForecastHour();

                    String defaultCint = ContoursInfoDlg.getCints()
                            .get(contourParm + "-" + contourLevel);
                    if (defaultCint != null
                            && defaultCint.trim().length() > 0) {
                        contourCint = defaultCint;
                    } else {
                        contourCint = adc.getCint();
                    }
                }

                // Use current date/time as default.
                setDefaultTimeToCurrent();
            }
        }

        // Ensure settings are initialized
        retrieveContoursSettings();

        // Activate contours-specific hotkeys.
        activatePGENContoursContext();

        int op = super.open();

        return op;
    }

    /*
     * Update the contours components' attribute settings.
     */
    public void setSettings(AbstractDrawableComponent de) {
        if (de instanceof gov.noaa.nws.ncep.ui.pgen.elements.Text) {
            contoursAttrSettings.put(getLabelTempKey(de), de);
        } else {
            contoursAttrSettings.put(de.getPgenType(), de);
        }
    }

    /*
     * Activate pgenContoursContext for contours hotkeys.
     */
    private void activatePGENContoursContext() {
        /*
         * Don't activate context outside of NCP, key bindings will conflict
         * with D2D
         */
        IEditorPart editor = EditorUtil.getActiveEditor();
        if (pgenContoursContextActivation == null
                && PgenUtil.isNatlCntrsEditor(editor)) {
            IContextService ctxSvc = PlatformUI.getWorkbench()
                    .getService(IContextService.class);
            pgenContoursContextActivation = ctxSvc.activateContext(
                    "gov.noaa.nws.ncep.ui.pgen.pgenContoursContext");
        }
    }

    /*
     * Deactivate pgenContoursContext to unbind hotkeys.
     */
    private void deactivatePGENContoursContext() {
        if (pgenContoursContextActivation != null) {

            IContextService ctxSvc = PlatformUI.getWorkbench()
                    .getService(IContextService.class);
            ctxSvc.deactivateContext(pgenContoursContextActivation);
            pgenContoursContextActivation = null;
        }
    }

    /*
     * Retrieve the contour parameter name defined for the current layer.
     *
     * @return - name of the contour parameter.
     */
    private String getContourParmOnLayer() {

        String currentProductType = PgenSession.getInstance().getPgenResource()
                .getActiveProduct().getType();
        ProductType currentType = ProductConfigureDialog.getProductTypes()
                .get(currentProductType);
        String currentLayer = PgenSession.getInstance().getPgenResource()
                .getActiveLayer().getName();

        String contourParm = PgenConstant.NONE;
        if (currentType != null && currentType.getPgenLayer() != null) {
            for (PgenLayer layer : currentType.getPgenLayer()) {
                if (layer.getName().equals(currentLayer)) {
                    String parmOnLayer = layer.getContourParm();
                    if (parmOnLayer != null) {
                        contourParm = parmOnLayer;
                    }
                }
            }
        }

        return contourParm;
    }

    /**
     * Loop through current layer and see if there is an same type of Contours.
     *
     * If "one contour per layer" rule is forced and cannot find the same type
     * of Contours, the first Contours in the layer is used.
     *
     * @return the contours
     */
    public Contours findExistingContours() {
        Contours existingContours = null;

        // Check the Contours of same type.
        Iterator<AbstractDrawableComponent> it = drawingLayer.getActiveLayer()
                .getComponentIterator();
        while (it.hasNext()) {
            AbstractDrawableComponent adc = it.next();
            if (adc instanceof Contours && !(adc instanceof Outlook)) {
                Contours thisContour = (Contours) adc;
                if (thisContour.getKey().equals(Contours.getKey(this))) {
                    existingContours = (Contours) adc;
                    break;
                }
            }
        }

        // Check "One Contours per Layer" rule.
        if (PgenUtil.getOneContourPerLayer()) {
            Iterator<AbstractDrawableComponent> ite = drawingLayer
                    .getActiveLayer().getComponentIterator();
            while (ite.hasNext()) {
                AbstractDrawableComponent adc = ite.next();
                if (adc instanceof Contours && !(adc instanceof Outlook)) {
                    existingContours = (Contours) adc;
                    break;
                }
            }
        }

        return existingContours;
    }

    /*
     * Change the selected contour Minmax's type, including symbol-only and
     * label-only Minmax.
     */
    private boolean changeMinmaxType() {

        /*
         * Create a new Contours as well as a new ContourMinmax (symbol with
         * label, symbol-only, ot label only).
         */
        boolean typeChanged = false;
        DrawableElement de = drawingLayer.getSelectedDE();
        if (de != null && de.getParent() instanceof ContourMinmax
                && de.getParent().getParent() instanceof Contours) {

            DrawableElement newEl = (DrawableElement) de.copy();
            Contours oldContours = (Contours) de.getParent().getParent();

            Iterator<AbstractDrawableComponent> iterator = oldContours
                    .getComponentIterator();

            Contours newContours = new Contours();

            /*
             * Replace the selected ContourMinmax with a new ContourMinmax and
             * copy over other non-selected contour components.
             */
            while (iterator.hasNext()) {

                AbstractDrawableComponent oldAdc = iterator.next();
                AbstractDrawableComponent newAdc = oldAdc.copy();

                if (oldAdc.equals(de.getParent())) {
                    newEl.setParent(newAdc);

                    // Get symbol/label/location in current ContourMinMax.
                    ContourMinmax cmmAdc = ((ContourMinmax) newAdc);

                    gov.noaa.nws.ncep.ui.pgen.elements.Text cmmLabel = cmmAdc
                            .getLabel();
                    DrawableElement cmmSymbol = cmmAdc.getSymbol();

                    String symbolCls = getActiveSymbolClass();
                    String symbolObj = getActiveSymbolObjType();

                    Coordinate symbLoc;
                    String[] symbLabel = new String[] { getLabel() };
                    if (cmmSymbol != null) {
                        symbLoc = ((SinglePointElement) cmmSymbol)
                                .getLocation();
                    } else {
                        symbLoc = cmmLabel.getLocation();
                    }

                    // Create new symbol and label with right attributes.
                    ContourMinmax newCmm = new ContourMinmax(symbLoc, symbolCls,
                            symbolObj, symbLabel, hideSymbolLabel());

                    IAttribute mmTemp = getMinmaxTemplate();

                    DrawableElement newSymb = newCmm.getSymbol();

                    IAttribute lblTemp = (IAttribute) contoursAttrSettings
                            .get(getLabelTempKey(newSymb));

                    newSymb.setParent(cmmAdc);

                    gov.noaa.nws.ncep.ui.pgen.elements.Text newLbl = newCmm
                            .getLabel();
                    newLbl.setParent(cmmAdc);

                    if (newSymb != null && mmTemp != null) {
                        newSymb.update(mmTemp);
                    }

                    if (newLbl != null && lblTemp != null) {
                        newLbl.update(lblTemp);
                    }

                    newLbl.setText(symbLabel);
                    if (isUseMainColor()) {
                        newLbl.setColors(newSymb.getColors());
                    }

                    /*
                     * Change between label-only, symbol-only and regular.
                     */
                    if (cmmAdc.isLabelOnly()) { // Label-only to others.
                        newEl = cmmLabel;
                        if (isMinmaxLabelOnly()) {
                            cmmAdc.updateLabelString(
                                    new String[] { getLabel() });
                            cmmLabel.setColors(newLbl.getColors());
                        } else if (isMinmaxSymbolOnly()) {
                            newEl = newSymb;
                            cmmAdc.add(newSymb);
                            cmmAdc.remove(cmmLabel);
                        } else {
                            if (hideSymbolLabel()) {
                                newEl = newSymb;
                            } else {
                                newEl = newLbl;
                            }
                            cmmAdc.add(newSymb);

                            cmmAdc.replace(cmmLabel, newLbl);
                            newLbl.setAuto(true);
                            newLbl.setHide(hideSymbolLabel());
                        }
                    } else if (cmmAdc.isSymbolOnly()) { // Symbol-only to
                                                        // others.
                        newEl = newSymb;
                        cmmAdc.replace(cmmSymbol, newEl);

                        if (isMinmaxSymbolOnly()) {
                            // skip
                        } else if (isMinmaxLabelOnly()) {
                            newEl = newLbl;
                            cmmAdc.add(newLbl);
                            newLbl.setAuto(false);
                            newLbl.setHide(false);
                            newLbl.setLocation(symbLoc);

                            cmmAdc.remove(cmmAdc.getSymbol());
                        } else {
                            cmmAdc.add(newLbl);
                            newLbl.setAuto(true);
                        }
                    } else { // Regular to other types
                        newEl = newSymb;
                        cmmAdc.replace(cmmSymbol, newEl);
                        cmmAdc.replace(cmmLabel, newLbl);

                        if (isMinmaxLabelOnly()) {
                            newEl = newLbl;
                            newLbl.setLocation(symbLoc);
                            newLbl.setAuto(false);
                            newLbl.setHide(false);

                            cmmAdc.remove(cmmAdc.getSymbol());
                        } else if (isMinmaxSymbolOnly()) {
                            cmmAdc.remove(cmmAdc.getLabel());
                        }
                    }
                }

                newAdc.setParent(newContours);
                newContours.add(newAdc);
            }

            /*
             * Update the contours attributes and replace the old one with the
             * new Contours.
             */
            newContours.update(this);
            drawingLayer.replaceElement(oldContours, newContours);
            if (tool != null) {
                tool.setCurrentContour(newContours);
            }

            /*
             * Reset the selected contours and DE to the updated ones.
             */
            currentContours = newContours;
            drawingLayer.removeSelected();
            if (newEl != null) {
                drawingLayer.setSelected(newEl);
            }

            if (mapEditor != null) {
                mapEditor.refresh();
            }

            typeChanged = true;
        }

        return typeChanged;
    }

    /**
     * Save a button's last status to the map.
     *
     * @param btn
     *            the button to save
     */
    private void saveBtnLastStatus(Button btn) {
        btnLastStatus.put((String) btn.getData(), btn.getSelection());
    }

    /**
     * Initialize or Restore a button's status from the map.
     *
     * @param btn
     *            the button to restore
     */
    private void setBtnStatus(Button btn, boolean defStatus) {

        String btnKey = (String) btn.getData();
        if (!btnLastStatus.containsKey(btnKey)) {
            btn.setSelection(defStatus);
            saveBtnLastStatus(btn);
        } else {
            btn.setSelection(btnLastStatus.get(btnKey));
        }
    }

    /*
     * Initialize the default time to the current GMT time.
     *
     * If minutes >= ROUND_UP_ONE_HOUR (15 minutes), round to the next hour.
     *
     * @param
     */
    private void setDefaultTimeToCurrent() {
        contourTime1 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int minute = contourTime1.get(Calendar.MINUTE);
        if (minute >= ROUND_UP_ONE_HOUR) {
            contourTime1.add(Calendar.HOUR_OF_DAY, 1);
        }

        contourTime1.set(Calendar.MINUTE, 0);
        contourTime1.set(Calendar.SECOND, 0);
        contourTime1.set(Calendar.MILLISECOND, 0);

        contourTime2 = (Calendar) contourTime1.clone();
    }

    /**
     * Save the hide button's last status to the map.
     *
     * @param btn
     *            the hide label button
     */
    private void saveHideLabelBtnStatus(Button btn) {
        btnLastStatus.put(HIDE_BUTTON, (Boolean) btn.getData());
    }

    /**
     * Get the status of the hide button from the map.
     *
     * @return the state of the hide button
     */
    private boolean getHideLabelBtnStatus() {

        boolean buttonStatus = true;
        if (btnLastStatus.containsKey(HIDE_BUTTON)) {
            buttonStatus = btnLastStatus.get(HIDE_BUTTON);
        }
        return buttonStatus;
    }

    /**
     * Opens the line attribute dialog.
     */
    public void openLineAttrDlg() {
        try {
            if (lineAttrDlg == null) {
                lineAttrDlg = new ContourLineAttrDlg(PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell());
            }

            openAttrDlg(lineAttrDlg);
            lineAttrDlg.initDlg();

            // get stored attributes
            DrawableElement de = drawingLayer.getSelectedDE();

            if (de != null && de.getParent() != null
                    && de.getParent() instanceof ContourLine) {
                ContourLine pde = (ContourLine) de.getParent();
                lineAttrDlg.setAttrForDlg(pde.getLine());
            } else {
                if (lineTemplate == null) {
                    lineTemplate = (Line) contoursAttrSettings
                            .get(activeContourLineBtn.getData().toString());
                }
                lineAttrDlg.setAttrForDlg(lineTemplate);
            }

            // disable unused attributes
            lineAttrDlg.disableWidgets();

        } catch (VizException e) {
            handler.error("Error handling Line Attribute Dialog creation", e);
        }

    }

    /**
     * Opens the symbol attribute dialog.
     */
    public void openSymbolAttrDlg() {
        try {
            if (minmaxAttrDlg == null) {
                minmaxAttrDlg = new ContourMinmaxAttrDlg(PlatformUI
                        .getWorkbench().getActiveWorkbenchWindow().getShell());
                minmaxAttrDlg.setPgenCategory(PgenConstant.SYMBOL);
                minmaxAttrDlg.setPgenType(getActiveSymbolObjType());
            }

            openAttrDlg(minmaxAttrDlg);
            minmaxAttrDlg.initDlg();

            // get stored attributes
            DrawableElement de = drawingLayer.getSelectedDE();
            if (de != null && de instanceof Symbol
                    && de.getParent() instanceof ContourMinmax) {
                minmaxAttrDlg.setAttrForDlg(de);
            } else {
                minmaxTemplate = (Symbol) contoursAttrSettings
                        .get(activeContourSymbolBtn.getData().toString());
                if (minmaxTemplate == null) {
                    minmaxTemplate = new Symbol(null,
                            new Color[] { Color.green }, 2.0F, 2.0, true, null,
                            PgenConstant.SYMBOL, getActiveSymbolObjType());
                }

                minmaxAttrDlg.setAttrForDlg(minmaxTemplate);

            }
        } catch (VizException e) {
            handler.error("Error handling min-max Dialog creation", e);
        }
    }

    /**
     * Opens the circle attribute dialog
     */
    public void openCircleAttrDlg() {
        try {
            if (circleAttrDlg == null) {
                circleAttrDlg = new ContourCircleAttrDlg(PlatformUI
                        .getWorkbench().getActiveWorkbenchWindow().getShell());
            }

            openAttrDlg(circleAttrDlg);
            circleAttrDlg.initDlg();

            // get stored attributes
            DrawableElement de = drawingLayer.getSelectedDE();

            if (de != null && de.getParent() != null
                    && de.getParent() instanceof ContourCircle) {
                ContourCircle pde = (ContourCircle) de.getParent();
                circleAttrDlg.setAttrForDlg(pde.getCircle());
            } else {
                if (circleTemplate == null) {
                    circleTemplate = (Arc) contoursAttrSettings
                            .get(PgenConstant.CIRCLE);
                    circleTemplate.setPgenType(PgenConstant.CIRCLE);
                }
                circleAttrDlg.setAttrForDlg(circleTemplate);
            }

            // disable unused attributes
            circleAttrDlg.disableWidgets();
        } catch (VizException e) {
            handler.error("Error handling Circle Attribute Dialog creation", e);
        }
    }

    /**
     * Opens the label attribute dialog.
     */
    public void openLabelAttrDlg() {
        try {
            if (labelAttrDlg == null) {
                labelAttrDlg = new LabelAttrDlg(PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell());
            }

            openAttrDlg(labelAttrDlg);
            labelAttrDlg.initDlg();

            // get stored attributes
            DrawableElement de = drawingLayer.getSelectedDE();

            if (de != null) {

                if (de.getParent() instanceof ContourLine
                        && ((ContourLine) (de.getParent())).getLabels()
                                .size() > 0) {
                    labelAttrDlg.setAttrForDlg(((ContourLine) (de.getParent()))
                            .getLabels().get(0));
                } else if (de.getParent() instanceof ContourMinmax
                        && ((ContourMinmax) (de.getParent()))
                                .getLabel() != null) {
                    labelAttrDlg.setAttrForDlg(
                            ((ContourMinmax) (de.getParent())).getLabel());
                } else if (de.getParent() instanceof ContourCircle
                        && ((ContourCircle) (de.getParent()))
                                .getLabel() != null) {
                    labelAttrDlg.setAttrForDlg(
                            ((ContourCircle) (de.getParent())).getLabel());
                }
            } else {

                labelTemplate = (gov.noaa.nws.ncep.ui.pgen.elements.Text) contoursAttrSettings
                        .get(getLabelTempKey());

                labelAttrDlg.setAttrForDlg(labelTemplate);

                if (isUseMainColor()) {
                    if (drawingStatus == ContourDrawingStatus.DRAW_LINE) {
                        labelAttrDlg.setColor(lineTemplate.getColors()[0]);
                    } else if (drawingStatus == ContourDrawingStatus.DRAW_SYMBOL) {
                        labelAttrDlg.setColor(minmaxTemplate.getColors()[0]);

                    } else if (drawingStatus == ContourDrawingStatus.DRAW_CIRCLE) {
                        labelAttrDlg.setColor(circleTemplate.getColors()[0]);
                    }
                }
            }

            labelAttrDlg.setText(new String[] { getLabel() });

            // disable unused attributes
            labelAttrDlg.disableWidgets();

        } catch (VizException e) {
            handler.error("Error handling Label Attribute Dialog creation", e);
        }
    }

    /**
     * Removes ghosts and selected elements. Sets to selecting mode.
     */
    private void attrDlgCleanUp() {
        drawingLayer.removeSelected();
        drawingLayer.removeGhostLine();
        setDrawingStatus(ContoursAttrDlg.ContourDrawingStatus.SELECT);
    }

    /**
     * Calculates the location of the sub-object attribute dialog.
     *
     * @return Point - location of the attribute dialog
     */
    private Point getAttrDlgLocation() {
        return new Point(getShell().getLocation().x + getShell().getSize().x,
                getShell().getLocation().y);
    }

    /**
     * @return the shiftDownInContourDialog
     */
    public boolean isShiftDownInContourDialog() {
        return shiftDownInContourDialog;
    }

    /**
     * @param shiftDownInContourDialog
     *            the shiftDownInContourDialog to set
     */
    public void setShiftDownInContourDialog(boolean shiftDown) {
        this.shiftDownInContourDialog = shiftDown;
    }

}
