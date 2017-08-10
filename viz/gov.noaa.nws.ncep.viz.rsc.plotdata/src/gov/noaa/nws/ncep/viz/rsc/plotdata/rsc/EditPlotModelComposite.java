package gov.noaa.nws.ncep.viz.rsc.plotdata.rsc;

import gov.noaa.nws.ncep.viz.common.ui.Markers.MarkerType;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorMatrixSelector;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory.ResourceSelection;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.rsc.plotdata.Activator;
import gov.noaa.nws.ncep.viz.rsc.plotdata.advanced.ConditionalColorBar;
import gov.noaa.nws.ncep.viz.rsc.plotdata.advanced.EditPlotModelElementAdvancedDialog;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefn;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefns;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefnsMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.PlotModelMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModelElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * UI for editing Point data resource attributes.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/15/2009    172        M. Li       Initial creation.
 * 12/05/2009    217        Greg Hull   broke out from plot manager dialog and reworked.
 * 08/09/2010    291        G. Zhang    add support for more data resources
 * 03/07/2011               Greg Hull   remove duplicate topComposite which created a
 *                                      blank area at top of dialog.  
 * 03/31/2011    425        Greg Hull   Refactor and created PlotModelElemCenterButton
 * 07/13/2011    264        Archana     Updated initWidgets() to sort the names of the plot  
 *                                      parameters before populating the list.
 * 08/10/2011    450        Greg Hull   use PlotParameterDefns instead of PlotParameterDefnsMngr
 * 11/01/2011    482        Greg Hull   add tooTips, move font/size/style 
 * 11/03/2011    482        Greg Hull   add unimplemented Clear and Reset, move Text Attributes
 * 04/16/2012    615        S. Gurung   Adjusted size for PlotModelElemButton
 * 05/02/2012    778        Q. Zhou     Changed scale from integer to double  
 * 05/29/2012    654        S. Gurung   Added option "Apply to All" to apply text changes to all parameters;
 *                                      Added additional options to textFontOptions and textStyleOptions;
 *                                      Fixed the issue of Sky Coverage parameters not appearing in the Sky Coverage drop-down list.
 * 07/24/2012    431        S. Gurung   Added code for editing advanced settings
 * 02/26/2013    936        Archana     Removed references to the 'Standard' font.
 * 03/10/2014    921        S. Russell  TTR 921: implemented the Clear & Reset buttons
 * 06/17/2014    923        S. Russell  TTR 923: altered method initWidgets()
 * 11/03/2014  R5156        B. Hebbard  Add more textFontOptions
 * 08/14/2015  R7757        B. Hebbard  Enable (symbol) size selection for vector parameters
 * 11/17/2015  R9579        B. Hebbard  Add marker type and symbol line width selection
 * 12/17/2015  R9579        B. Hebbard  Make sure PTND (combined text & symbol) enables symbol controls
 * 11/05/2015   5070        randerso    Adjust font sizes for dpi scaling
 * 
 * 03/29/2016  R7567        A. Su       Disabled Advanced button when empty plot model element is selected.
 *                                      Fixed the title of Edit Advanced Options dialog for center plot model element.
 *                                      Disabled and enabled Advanced button at right moments to
 *                                        prevent multiple Edit Advanced Options dialog to open for the same plot model element.
 *                                      Allowed Edit Advanced Options dialogs to open for different plot model elements
 *                                        with each new dialog positioned slightly lower-left to the previous one.
 *                                      Restored the function of Reset button after searching localization info.
 *                                      Enabled Clear and Reset buttons to close its associated Advanced Options Dialogs.
 *                                      Fixed to show initial rainbow color for center element of non-SKYC and non-BRBK.
 * 11/17/2016 R26156        J. Huber    Cleaned up parameter button tooltips.
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */
public class EditPlotModelComposite extends Composite {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EditPlotModelComposite.class);

    /**
     * The label for an empty model element.
     */
    private static final String EMPTY_MODEL_ELEMENT_LABEL = "Parm";

    private PlotModel editedPlotModel = null;

    private ColorMatrixSelector cms = null;

    private List availParamsList = null;

    private Group selectMarkerTypeGroup = null;

    private ToolBar markerSelectToolBar = null;

    private ToolItem markerSelectToolItem = null;

    private Menu markerSelectMenu = null;

    private Scale symbolSizeScale = null;

    private Scale symbolWidthScale = null;

    private Combo textSizeCombo = null;

    private Combo textFontCombo = null;

    private Combo textStyleCombo = null;

    private Button applyToAllBtn = null;

    private Button advancedBtn = null;

    private final String[] textSizeOptions = { "8", "10", "12", "14", "16",
            "18", "20", "22", "28" };

    private static final String[] textFontOptions = { "Courier", "Helvetica",
            "Times", "Monospaced", // same as "DialogInput"
            "SansSerif", // same as "Dialog"
            "Liberation Serif" };

    private static final String[] textStyleOptions = { "Normal", "Italic",
            "Bold", "Bold-Italic" };

    private static final String ELEMENT_POSITION_MIDDLE_CENTER = "MC";

    private static final String[] plotModelElementPositions = { "TC", "UL",
            "UC", "UR", "ML", ELEMENT_POSITION_MIDDLE_CENTER, "MR", "LL", "LC",
            "LR", "BC" };

    private HashMap<String, PlotModelElemButton> plotModelElementsUIMap = new HashMap<String, PlotModelElemButton>();

    private HashMap<String, PlotModelElemButton> origPltMdlElmntsUIMap = new HashMap<String, PlotModelElemButton>();

    private PlotParameterDefns plotParamDefns = null;

    private PlotModelElemButton seldPlotModelElemButton = null;

    private Composite topComposite = null;

    // for getting resource related parameters
    INatlCntrsResourceData rscData = null;

    // group for center buttons
    private Group ctrGrp = null;

    private Combo comboSky = null;

    private Combo comboBrbk = null;

    private ArrayList<String> availWindBarpParams = null;

    private ArrayList<String> availSkyCoverParams = null;

    // declare here for setting the text to reflect the actual size
    private Label symbolSizeLabel = null;

    // declare here for setting the text to reflect the actual width
    private Label symbolWidthLabel = null;

    // center parameter only Buttons
    private Label skycLbl = null;

    private Label wndBrbLbl = null;

    // Make it static so it can be created once for all instances to share.
    private static final File advancedIconFile = NcPathManager.getInstance()
            .getStaticFile(NcPathConstants.ADVANCED_ICON_IMG);

    // Make it static so it can be created once for all instances to share.
    private static final Image advancedIconImage = (advancedIconFile != null
            && advancedIconFile.exists())
                    ? new Image(Display.getCurrent(),
                            advancedIconFile.getAbsolutePath())
                    : null;

    private MarkerType selectedMarkerType = MarkerType.BOX;

    private static final String MARK_PARAMETER = "MARK";

    /**
     * This set records the positions of plot model elements that are being
     * edited.
     */
    private Set<String> plotModelElementsBeingEdited;

    /**
     * This set records the shells (windows) of associated Edit Advanced Options
     * dialogs that are being edited.
     */
    private Set<Shell> childrenList = new HashSet<Shell>();

    /**
     * The marginal offset of x coordinate for Edit Advanced Options dialog.
     */
    public static final int X_OFFSET_FOR_EDIT_ADVANCED_OPTIONS_DIALOG = 30;

    /**
     * The marginal offset of y coordinate for Edit Advanced Options dialog.
     */
    public static final int Y_OFFSET_FOR_EDIT_ADVANCED_OPTIONS_DIALOG = 25;

    /**
     * This number is used to determine where to position a new Edit Advanced
     * Options dialog. The keyword static is required to account for all the
     * dialogs opened by different instances of this class.
     */
    private static int offsetCount = 0;

    /**
     * The initial x coordinate of an Edit Advanced Options Dialog.
     */
    private static int initXCoord = 0;

    /**
     * The initial y coordinate of an Edit Advanced Options Dialog.
     */
    private static int initYCoord = 0;

    /**
     * The window where this instance of EditPlotModelCpomposite is located.
     */
    private Shell shell;

    public EditPlotModelComposite(Composite parent, int style, PlotModel pm,
            INatlCntrsResourceData rscData) {
        super(parent, style);

        shell = parent.getShell();
        clearEditedList();

        editedPlotModel = pm;
        this.rscData = rscData;
        topComposite = this;
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        mainLayout.verticalSpacing = 5;
        topComposite.setLayout(mainLayout);
        plotParamDefns = PlotParameterDefnsMngr.getInstance()
                .getPlotParamDefns(editedPlotModel.getPlugin());

        createPlotModelGuiElements();

        origPltMdlElmntsUIMap = new HashMap<String, PlotModelElemButton>(
                plotModelElementsUIMap);

        createTextAttrControls();

        createCenterParamAndMarkerControls();

        createParmListControls();

        initWidgets();

    }

    /* Create text attributes -- size, font and style */
    private void createTextAttrControls() {
        Group textAttrGrp = new Group(topComposite, SWT.SHADOW_NONE);
        GridLayout gl = new GridLayout(7, false);
        gl.marginTop = 0;
        gl.marginBottom = 3;
        gl.marginRight = 0;
        textAttrGrp.setLayout(gl);
        textAttrGrp.setText("Text");

        // Text size attribute
        new Label(textAttrGrp, SWT.NONE).setText("size");
        textSizeCombo = new Combo(textAttrGrp, SWT.DROP_DOWN | SWT.READ_ONLY);
        textSizeCombo.setEnabled(false); // wait until a plot element is
                                         // selected.
        textSizeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if (seldPlotModelElemButton != null) {
                    seldPlotModelElemButton.getPlotModelElement()
                            .setTextSize(textSizeCombo.getText());
                    if (applyToAllBtn.getSelection()) {
                        applyTextChangesToAllParameters();
                    }
                }
            }
        });

        // Text font attribute
        new Label(textAttrGrp, SWT.NONE).setText(" font");
        textFontCombo = new Combo(textAttrGrp, SWT.DROP_DOWN | SWT.READ_ONLY);
        textFontCombo.setEnabled(false);
        textFontCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if (seldPlotModelElemButton != null) {
                    seldPlotModelElemButton.getPlotModelElement()
                            .setTextFont(textFontCombo.getText());
                    textStyleCombo.setEnabled(true);
                    if (applyToAllBtn.getSelection()) {
                        applyTextChangesToAllParameters();
                    }
                }
            }
        });

        // Text style attribute
        new Label(textAttrGrp, SWT.NONE).setText(" style");
        textStyleCombo = new Combo(textAttrGrp, SWT.DROP_DOWN | SWT.READ_ONLY);
        textStyleCombo.setEnabled(false);
        textStyleCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if (seldPlotModelElemButton != null) {
                    seldPlotModelElemButton.getPlotModelElement()
                            .setTextStyle(textStyleCombo.getText());
                    if (applyToAllBtn.getSelection()) {
                        applyTextChangesToAllParameters();
                    }
                }
            }
        });

        // Text attributes "Apply to All" (positions with text) button
        applyToAllBtn = new Button(textAttrGrp, SWT.CHECK);
        applyToAllBtn.setText("Apply to All");
        applyToAllBtn.setToolTipText("Apply changes to all text parameters");
        applyToAllBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (applyToAllBtn.getSelection()) {
                    applyTextChangesToAllParameters();
                }
            }
        });

    }

    /* create widgets for center-position-only and marker-type parameters */
    private void createCenterParamAndMarkerControls() {

        Composite comp = new Composite(topComposite, SWT.NONE);
        GridLayout gl8 = new GridLayout(2, false);
        comp.setLayout(gl8);

        availWindBarpParams = plotParamDefns.getWindBarbParams();
        availSkyCoverParams = plotParamDefns.getSkyCoverageParams();

        ctrGrp = new Group(comp, SWT.SHADOW_NONE);
        GridLayout gl = new GridLayout(6, false);
        gl.marginLeft = 6;
        gl.marginTop = 2;
        gl.marginBottom = 3;
        gl.marginRight = 6;
        ctrGrp.setLayout(gl);
        ctrGrp.setText("Center Only Parameters");
        GridData gd8 = new GridData(420, 44);
        ctrGrp.setLayoutData(gd8);

        skycLbl = new Label(ctrGrp, SWT.None);
        skycLbl.setText("Sky Coverage");

        comboSky = new Combo(ctrGrp, SWT.DROP_DOWN | SWT.READ_ONLY);

        comboSky.setItems(availSkyCoverParams.toArray(new String[] {}));
        comboSky.add("None", 0);

        comboSky.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String name = comboSky.getItem(comboSky.getSelectionIndex());

                if (seldPlotModelElemButton instanceof PlotModelElemCenterButton) {
                    ((PlotModelElemCenterButton) seldPlotModelElemButton)
                            .setSkyCoverageParamName(
                                    (name.equals("None") ? null : name));
                }
            }
        });

        Label sepe = new Label(ctrGrp, 0);
        sepe.setText("     ");

        wndBrbLbl = new Label(ctrGrp, SWT.None);
        wndBrbLbl.setText("Wind Barb ");

        comboBrbk = new Combo(ctrGrp, SWT.DROP_DOWN | SWT.READ_ONLY);

        comboBrbk.setItems(availWindBarpParams.toArray(new String[] {}));
        comboBrbk.add("None", 0);

        sepe = new Label(ctrGrp, 0);
        sepe.setText("          ");

        comboBrbk.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String name = comboBrbk.getItem(comboBrbk.getSelectionIndex());

                if (seldPlotModelElemButton instanceof PlotModelElemCenterButton) { // sanity
                    ((PlotModelElemCenterButton) seldPlotModelElemButton)
                            .setWindBarbParamName(
                                    (name.equals("None") ? null : name));
                }
            }
        });

        setSelectedSkyAndWindParams();

        enableCenterParamWidgets(false);

        // Marker Type group

        selectMarkerTypeGroup = new Group(comp, SWT.SHADOW_NONE);
        selectMarkerTypeGroup.setText("Marker Type");
        GridLayout markerTypeGridLayout = new GridLayout();
        markerTypeGridLayout.numColumns = 1;
        markerTypeGridLayout.marginLeft = 10;
        markerTypeGridLayout.marginRight = 30;
        markerTypeGridLayout.marginHeight = 2;
        selectMarkerTypeGroup.setLayout(markerTypeGridLayout);

        GridData gd4 = new GridData(105, 44);
        selectMarkerTypeGroup.setLayoutData(gd4);
        /*
         * Use a toolbar with a single drop-down button item that pops up a
         * menu, to simulate a combo that works for images.
         */
        markerSelectToolBar = new ToolBar(selectMarkerTypeGroup,
                SWT.HORIZONTAL);
        markerSelectToolItem = new ToolItem(markerSelectToolBar, SWT.DROP_DOWN);
        markerSelectMenu = new Menu(topComposite.getShell(), SWT.POP_UP);

        for (MarkerType markerType : MarkerType.values()) {
            MenuItem markerSelectMenuItem = new MenuItem(markerSelectMenu,
                    SWT.PUSH);
            markerSelectMenuItem.setData(markerType);
            markerSelectMenuItem.setText(markerType.getDesignator());
            // TODO: Use PGEN icons via extension points; avoid ordinal
            Integer ord1 = markerType.ordinal() + 1;
            String iconString = "icons/marker" + ord1.toString() + ".gif";
            ImageDescriptor id = Activator
                    .imageDescriptorFromPlugin(Activator.PLUGIN_ID, iconString);
            if (id != null) {
                Image icon = id.createImage();
                markerSelectMenuItem.setImage(icon);
            }
            markerSelectMenuItem.addListener(SWT.Selection, new Listener() {
                /*
                 * A new marker type has been chosen off the pop-up menu:
                 * Remember it AND set its icon back on the main button.
                 */
                public void handleEvent(Event event) {
                    selectedMarkerType = (MarkerType) event.widget.getData();
                    if (seldPlotModelElemButton != null) {
                        seldPlotModelElemButton
                                .setMarkerType(selectedMarkerType);
                    }
                    Image icon = markerSelectMenu
                            .getItem(selectedMarkerType.ordinal()).getImage();
                    markerSelectToolItem.setImage(icon);
                    markerSelectToolItem
                            .setToolTipText(selectedMarkerType.getDesignator());
                }
            });
        }
        markerSelectToolItem.addListener(SWT.Selection, new Listener() {
            /* Main button clicked: Pop up the menu showing all the symbols. */
            public void handleEvent(Event event) {
                Rectangle bounds = markerSelectToolItem.getBounds();
                Point point = markerSelectToolBar.toDisplay(bounds.x,
                        bounds.y + bounds.height);
                markerSelectMenu.setLocation(point);
                markerSelectMenu.setVisible(true);
            }
        });

        // Set initial state

        Image icon = markerSelectMenu.getItem(selectedMarkerType.ordinal())
                .getImage();
        markerSelectToolItem.setImage(icon);
        markerSelectToolItem.setToolTipText(selectedMarkerType.getDesignator());

        // Disable until MARK parameter selected

        enableMarkerTypeSelection(false);
    }

    /*
     * Create Parameter List, symbol size, line width, and color picker
     */
    private void createParmListControls() {
        Composite comp = new Composite(topComposite, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        comp.setLayout(gl);

        // Parameter List
        GridData gd = new GridData(110, 149);
        Group parmListGrp = new Group(comp, SWT.SHADOW_NONE);
        parmListGrp.setLayout(new GridLayout(1, false));
        parmListGrp.setText("Plot Parameters");
        availParamsList = new List(parmListGrp,
                SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        availParamsList.setLayoutData(gd);
        availParamsList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String selectedParm = availParamsList.getSelection()[0];
                if (seldPlotModelElemButton != null) {
                    seldPlotModelElemButton.setParmName(selectedParm);
                    updateTextAndSymbolWidgets();
                }
            }
        });

        // Symbol size
        GridData gd2 = new GridData(45, 114);
        Group symbolSizeGrp = new Group(comp, SWT.SHADOW_NONE);
        symbolSizeGrp.setLayout(new GridLayout(1, false));
        symbolSizeGrp.setText("Size");

        symbolSizeLabel = new Label(symbolSizeGrp, SWT.BOLD);
        symbolSizeLabel.setText("    0.5");

        symbolSizeScale = new Scale(symbolSizeGrp, SWT.VERTICAL);
        symbolSizeScale.setLayoutData(gd2);
        symbolSizeScale.setMinimum(5);
        symbolSizeScale.setMaximum(30);
        symbolSizeScale.setIncrement(1);
        symbolSizeScale.setPageIncrement(1);
        symbolSizeScale.setSelection(5);
        symbolSizeScale.setEnabled(false); // wait until a plot element is
                                           // selected
        symbolSizeScale.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                double selection = symbolSizeScale.getSelection();

                symbolSizeLabel.setText(String.valueOf(selection / 10));

                if (seldPlotModelElemButton != null) {
                    seldPlotModelElemButton.setSymbolSize(selection / 10);
                }
            }
        });

        // Symbol (line) width
        GridData gd3 = new GridData(45, 114);
        Group symbolWidthGrp = new Group(comp, SWT.SHADOW_NONE);
        symbolWidthGrp.setLayout(new GridLayout(1, false));
        symbolWidthGrp.setText("Width");

        symbolWidthLabel = new Label(symbolWidthGrp, SWT.BOLD);
        symbolWidthLabel.setText("    0.5");

        symbolWidthScale = new Scale(symbolWidthGrp, SWT.VERTICAL);
        symbolWidthScale.setLayoutData(gd3);
        symbolWidthScale.setMinimum(5);
        symbolWidthScale.setMaximum(50);
        symbolWidthScale.setIncrement(1);
        symbolWidthScale.setPageIncrement(1);
        symbolWidthScale.setSelection(5);
        symbolWidthScale.setEnabled(false); // wait until a plot element is
                                            // selected
        symbolWidthScale.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                double selection = symbolWidthScale.getSelection();

                symbolWidthLabel.setText(String.valueOf(selection / 10));

                if (seldPlotModelElemButton != null) {
                    seldPlotModelElemButton.setSymbolWidth(selection / 10);
                }
            }
        });

        // Color selector
        Group selColorGrp = new Group(comp, SWT.SHADOW_NONE);
        selColorGrp.setText("Color");

        cms = new ColorMatrixSelector(selColorGrp, false, true, 22, 88, 18, 22,
                28, 86, 0, 4, 5);
        cms.setColorValue(new RGB(0, 255, 0));
        cms.addListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (seldPlotModelElemButton != null) {
                    RGB rgb = cms.getColorValue();
                    seldPlotModelElemButton.setColor(rgb);
                    seldPlotModelElemButton.setButtonAsSelected();
                }
            }
        });
    }

    private void createPlotModelGuiElements() {
        // Create Top position button
        Composite comp = new Composite(topComposite, SWT.NONE);
        GridLayout gl = new GridLayout(3, true);
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;

        comp.setLayout(gl);

        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.CENTER;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;

        comp.setLayoutData(gd);

        // Create "Clear" Button
        Button clearPlotModelBtn = new Button(comp, SWT.PUSH);
        clearPlotModelBtn.setText("Clear");
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = GridData.CENTER;
        clearPlotModelBtn.setLayoutData(gd);
        clearPlotModelBtn.setEnabled(true);
        clearPlotModelBtn.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                clearPlotModel();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        PlotModelElement pme = editedPlotModel
                .getPlotModelElement(plotModelElementPositions[0]);

        // create a blank element and only add it to the editedPlotModel if the
        // user selects a parameter.
        if (pme == null) {
            pme = new PlotModelElement();
            pme.setPosition(plotModelElementPositions[0]);
        }

        // Param button 1
        PlotModelElemButton pmeBtn = new PlotModelElemButton(comp, pme);
        pmeBtn.init();

        plotModelElementsUIMap.put(plotModelElementPositions[0], pmeBtn);

        // Reset Button
        Button resetPlotModelBtn = new Button(comp, SWT.PUSH);
        resetPlotModelBtn.setText("Reset");
        gd = new GridData();
        gd.horizontalAlignment = GridData.END;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = GridData.CENTER;
        resetPlotModelBtn.setLayoutData(gd);
        resetPlotModelBtn.setEnabled(true);
        resetPlotModelBtn.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                resetPlotModel();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        // Create Upper, middle and lower row position buttons.
        comp = new Composite(topComposite, SWT.NONE);
        gl = new GridLayout(3, true);
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        comp.setLayout(gl);

        gd = new GridData();
        gd.horizontalAlignment = GridData.CENTER;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;

        comp.setLayoutData(gd);

        // 9 Param Buttons
        for (int i = 1; i <= 9; i++) {
            pme = editedPlotModel
                    .getPlotModelElement(plotModelElementPositions[i]);

            if (pme == null) {
                pme = new PlotModelElement();
                pme.setPosition(plotModelElementPositions[i]);
            }

            try {
                // center button stores the sky coverage and wind barb params
                if (plotModelElementPositions[i]
                        .equals(ELEMENT_POSITION_MIDDLE_CENTER)) {
                    PlotModelElemCenterButton cntrBtn = new PlotModelElemCenterButton(
                            comp, pme, editedPlotModel.getSkyCoverageElement(),
                            editedPlotModel.getWindBarbElement());
                    cntrBtn.init();
                    plotModelElementsUIMap.put(plotModelElementPositions[i],
                            cntrBtn);
                } else {
                    pmeBtn = new PlotModelElemButton(comp, pme);
                    pmeBtn.init();
                    plotModelElementsUIMap.put(plotModelElementPositions[i],
                            pmeBtn);
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Exception when setting up plotModelElementsUIMap in EditPlotModelComposite.createPlotModelGuiElements(): "
                                + e);
            }
        }

        // Create Bottom position button
        comp = new Composite(topComposite, SWT.NONE);
        gl = new GridLayout(3, true);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        comp.setLayout(gl);

        // create "Advanced..." button
        advancedBtn = new Button(comp, SWT.NONE);
        advancedBtn.setText("Advanced...");
        advancedBtn.setToolTipText("Edit Advanced Settings");
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        gd.grabExcessHorizontalSpace = true;
        advancedBtn.setLayoutData(gd);
        if (seldPlotModelElemButton == null)
            advancedBtn.setEnabled(false);
        else if (!seldPlotModelElemButton.isParamNameSelected())
            advancedBtn.setEnabled(false);

        advancedBtn.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                if (seldPlotModelElemButton != null) {
                    advancedBtn.setEnabled(false);
                    editAdvancedSettings();
                }
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        gd = new GridData();
        gd.horizontalAlignment = GridData.CENTER;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessHorizontalSpace = true;
        comp.setLayoutData(gd);

        pme = editedPlotModel
                .getPlotModelElement(plotModelElementPositions[10]);
        if (pme == null) {
            pme = new PlotModelElement();
            pme.setPosition(plotModelElementPositions[10]);
        }

        pmeBtn = new PlotModelElemButton(comp, pme);
        pmeBtn.init();

        plotModelElementsUIMap.put(plotModelElementPositions[10], pmeBtn);

    }

    private class PlotModelElemButton {
        protected PlotModelElement pltMdlElmt = null;

        final PlotModelElemButton thisButton = this;

        protected Group grp = null;

        protected Button parmBtn = null;;

        protected Button checkBtn = null;

        // create the widgets and add the listeners
        public PlotModelElemButton(Composite topComp,
                final PlotModelElement pme) {
            pltMdlElmt = pme;

            // if this element hasn't been set yet then set to default values
            // (the position must be set.)
            grp = new Group(topComp, SWT.SHADOW_ETCHED_OUT);
            grp.setLayout(new GridLayout(2, false));
            GridData gd = new GridData(SWT.FILL);// 90,60);
            gd.widthHint = 90;
            gd.heightHint = 60;
            gd.minimumHeight = 60;
            gd.grabExcessVerticalSpace = true;

            grp.setLayoutData(gd);

            if (advancedIconImage != null && pltMdlElmt.hasAdvancedSettings()) {
                grp.setBackgroundImage(advancedIconImage);
                grp.setToolTipText("Advanced Settings Applied.");
            } else {
                grp.setBackgroundImage(null);
                grp.setToolTipText("");
            }

            checkBtn = new Button(grp, SWT.CHECK | SWT.SHADOW_ETCHED_OUT);
            checkBtn.setLayoutData(new GridData(15, 12));

            parmBtn = new Button(grp, SWT.TOGGLE);
            parmBtn.setLayoutData(new GridData(62, 53));

            checkBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    checkButtonSelected();
                }
            });

            parmBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    paramButtonSelected();
                }
            });
        }

        public void checkButtonSelected() {

            // if unchecking then remove this element from the plotModel, else
            // if checking and the param name is set, add it back to the
            // plotModel (if checking and no param is set then the element will
            // be added to the plotModel when the param is selected.

            if (!checkBtn.getSelection()) {
                editedPlotModel.removePlotModelElement(pltMdlElmt);
            } else if (pltMdlElmt.getParamName() != null) {
                editedPlotModel.putPlotModelElement(pltMdlElmt);
            }
        }

        public void paramButtonSelected() {
            // Turn the parm button ON
            if (parmBtn.getSelection()) {

                availParamsList.deselectAll();
                availParamsList.setEnabled(true);

                checkBtn.setSelection(true);
                checkBtn.setEnabled(true);

                seldPlotModelElemButton = thisButton;

                // Select this Parameter in the Parameters List
                if (isParamNameSelected()) {

                    for (int i = 0; i < availParamsList.getItemCount(); i++) {
                        if (availParamsList.getItem(i)
                                .equals(parmBtn.getText())) {
                            availParamsList.setSelection(i);
                            break;
                        }
                    }

                    // Set color in the ColorMatrixSelector widget
                    cms.setColorValue(getColor());

                    // set the fore/background colors of the button and group
                    setButtonAsSelected();
                } else { // Parm not selected yet
                         // Set color in the ColorMatrixSelector widget
                    cms.setColorValue(getColor());
                    enableMarkerTypeSelection(true);
                }

                // unselect other buttons, change color back to original grey
                for (String pos : plotModelElementPositions) {
                    if (!pos.equals(getPosition())) {
                        plotModelElementsUIMap.get(pos).unselectButton();
                    }
                }

            } else { // Turn the parm button OFF
                seldPlotModelElemButton = null;

                unselectButton();

                availParamsList.deselectAll();
                availParamsList.setEnabled(false);
            }

            updateTextAndSymbolWidgets();
        }

        public boolean isParamNameSelected() {
            return (pltMdlElmt.getParamName() != null);
        }

        public void setSymbolSize(Double size) {
            pltMdlElmt.setSymbolSize(size);
        }

        public Double getSymbolSize() {
            if (pltMdlElmt.getParamName() != null)
                return pltMdlElmt.getSymbolSize();
            else
                return 1.0;
        }

        public void setSymbolWidth(Double width) {
            pltMdlElmt.setSymbolWidth(width);
        }

        public Double getSymbolWidth() {
            if (pltMdlElmt.getParamName() != null)
                return pltMdlElmt.getSymbolWidth();
            else
                return 1.0;
        }

        public void setMarkerType(MarkerType markerType) {
            pltMdlElmt.setMarkerType(markerType);
        }

        public MarkerType getMarkerType() {
            if (pltMdlElmt.getParamName().equals(MARK_PARAMETER))
                return pltMdlElmt.getMarkerType();
            else
                return MarkerType.BOX;
        }

        public String getButtonLabel() {
            return (isParamNameSelected() ? pltMdlElmt.getParamName()
                    : EMPTY_MODEL_ELEMENT_LABEL);
        }

        public void setButtonAsSelected() {
            RGB rgb = getColor();

            grp.setBackground(
                    new Color(getParent().getDisplay(), cms.getColorValue()));

            seldPlotModelElemButton.parmBtn
                    .setForeground(new Color(getParent().getDisplay(), rgb));
            seldPlotModelElemButton.parmBtn.setBackground(
                    Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            seldPlotModelElemButton.grp
                    .setBackground(new Color(getParent().getDisplay(), rgb));
        }

        public void unselectButton() {
            // change color back to original grey
            grp.setBackground(Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

            // unselect parm button
            parmBtn.setSelection(false);

            // unselect and disable check box for unused button
            if (!isParamNameSelected()) {
                checkBtn.setSelection(false);
                checkBtn.setEnabled(false);
            }
        }

        public boolean init() {

            parmBtn.setText(getButtonLabel());

            // Set foreground and background color.
            if (isParamNameSelected()) {
                checkBtn.setEnabled(true);
                checkBtn.setSelection(true);

                parmBtn.setForeground(new Color(getParent().getDisplay(),
                        getColor().red, getColor().green, getColor().blue));
                parmBtn.setBackground(
                        Display.getDefault().getSystemColor(SWT.COLOR_BLACK));

                PlotParameterDefn paramDefn = plotParamDefns
                        .getPlotParamDefn(pltMdlElmt.getParamName());
                parmBtn.setToolTipText(getToolTipText(paramDefn));
            } else {
                checkBtn.setEnabled(false);
                checkBtn.setSelection(false);

                // the color of the text
                parmBtn.setForeground(
                        new Color(getDisplay(), new RGB(0, 0, 0)));
                parmBtn.setBackground(Display.getDefault()
                        .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

                parmBtn.setToolTipText("");
            }

            return true;
        }

        public PlotModelElement getPlotModelElement() {
            return pltMdlElmt;
        }

        protected String getPosition() {
            return pltMdlElmt.getPosition();
        }

        protected void setParmName(String parameterName) {

            if (pltMdlElmt.getParamName() == null) {
                editedPlotModel.putPlotModelElement(pltMdlElmt);
                pltMdlElmt.setParamName(parameterName);
                updateTextAndSymbolWidgets();
                parmBtn.setToolTipText("");
            } else {
                pltMdlElmt.setParamName(parameterName);
            }

            parmBtn.setText(getButtonLabel());

            // Set foreground and background color
            parmBtn.setForeground(new Color(getParent().getDisplay(),
                    getColor().red, getColor().green, getColor().blue));
            parmBtn.setBackground(
                    Display.getDefault().getSystemColor(SWT.COLOR_BLACK));

            //
            PlotParameterDefn paramDefn = plotParamDefns
                    .getPlotParamDefn(parameterName);
            parmBtn.setToolTipText(getToolTipText(paramDefn));
        }

        protected RGB getColor() {
            return new RGB(pltMdlElmt.getColor().getRed(),
                    pltMdlElmt.getColor().getGreen(),
                    pltMdlElmt.getColor().getBlue());
        }

        protected void setColor(RGB color) {
            pltMdlElmt.setColorRGB(color);
        }
    }

    private class PlotModelElemCenterButton extends PlotModelElemButton {
        private PlotModelElement skyCovElmt = null;

        private PlotModelElement wndBrbElmt = null;

        public PlotModelElemCenterButton(Composite topComp,
                PlotModelElement pme, PlotModelElement sce,
                PlotModelElement wbe) {
            super(topComp, pme);
            // if either is null then create a new PlotModelElement but don't
            // add it to the editedPlotModel unless a param name is set.
            skyCovElmt = sce;

            if (skyCovElmt == null) {
                skyCovElmt = new PlotModelElement();
                skyCovElmt.setPosition("SC");
            }

            wndBrbElmt = wbe;

            if (wndBrbElmt == null) {
                wndBrbElmt = new PlotModelElement();
                wndBrbElmt.setPosition("WD");
            }

            // If any of wind barb, sky coverage, and regular plot model element
            // has advanced settings, set the rainbow color.
            if (advancedIconImage != null && (pme.hasAdvancedSettings()
                    || skyCovElmt.hasAdvancedSettings()
                    || wndBrbElmt.hasAdvancedSettings())) {
                grp.setBackgroundImage(advancedIconImage);
                grp.setToolTipText("Advanced Settings Applied.");
            } else {
                grp.setBackgroundImage(null);
                grp.setToolTipText("");
            }

            setColor(getColor()); // sync the color for all the elements even if
                                  // not initially set
        }

        @Override
        public void checkButtonSelected() {
            super.checkButtonSelected();

            // if unchecking then remove the wndBrb and skyCov elements from the
            // plotModel else add it back
            if (!checkBtn.getSelection()) {
                editedPlotModel.removePlotModelElement(wndBrbElmt);
                editedPlotModel.removePlotModelElement(skyCovElmt);
            } else {
                if (wndBrbElmt.getParamName() != null) {
                    editedPlotModel.putPlotModelElement(wndBrbElmt);
                }
                if (skyCovElmt.getParamName() != null) {
                    editedPlotModel.putPlotModelElement(skyCovElmt);
                }
            }

        }

        // If a 'normal' parameter is selected in the center then the SkyC and
        // WindBarb are disabled and if a skyC or WindBarb is selected then the
        // normal param is not allowed.
        @Override
        public void setParmName(String parameter) {
            super.setParmName(parameter);

            setSkyCoverageParamName(null);
            setWindBarbParamName(null);

            // update the label and combo menus with new selections
            setSelectedSkyAndWindParams();
            parmBtn.setText(getButtonLabel());
        }

        public String getWindBarbParamName() {
            return wndBrbElmt.getParamName();
        }

        public void setWindBarbParamName(String parameter) {
            // in case the parameter is set for the first time we need to add
            // the plotModelElement to the plotModel

            if (parameter != null) {
                if (wndBrbElmt.getParamName() == null) {
                    wndBrbElmt.setParamName(parameter);
                    editedPlotModel.putPlotModelElement(wndBrbElmt);

                    // we can't have a skyc and a normal parameter
                    editedPlotModel.removePlotModelElement(pltMdlElmt);
                    pltMdlElmt.setParamName(null);
                }
            } else {
                editedPlotModel.removePlotModelElement(wndBrbElmt);
            }

            wndBrbElmt.setParamName(parameter);
            parmBtn.setText(getButtonLabel());
            // This update for both wind barb and not wind barb.
            updateTextAndSymbolWidgets();
        }

        public String getSkyCovParamName() {
            return skyCovElmt.getParamName();
        }

        public void setSkyCoverageParamName(String parameter) {
            // in case the parameter is set for the first time we need to add
            // the plotModelElement to the plotModel

            if (parameter != null) {
                if (skyCovElmt.getParamName() == null) {
                    skyCovElmt.setParamName(parameter);
                    editedPlotModel.putPlotModelElement(skyCovElmt);
                }

                // we can't have a skyc and a normal parameter
                editedPlotModel.removePlotModelElement(pltMdlElmt);
                pltMdlElmt.setParamName(null);
            } else {
                editedPlotModel.removePlotModelElement(skyCovElmt);
            }

            skyCovElmt.setParamName(parameter);
            parmBtn.setText(getButtonLabel());
            // This update for both sky coverage and not sky coverage.
            updateTextAndSymbolWidgets();
        }

        @Override
        public String getButtonLabel() {
            if (skyCovElmt.getParamName() == null
                    && wndBrbElmt.getParamName() == null) {
                return super.getButtonLabel();
            } else if (skyCovElmt.getParamName() == null) {
                return wndBrbElmt.getParamName();
            } else if (wndBrbElmt.getParamName() == null) {
                return skyCovElmt.getParamName();
            } else {
                return skyCovElmt.getParamName() + "\n"
                        + wndBrbElmt.getParamName();
            }
        }

        public PlotModelElement getSkyCoveragePlotModelElement() {
            return skyCovElmt;
        }

        public PlotModelElement getWindBarbPlotModelElement() {
            return wndBrbElmt;
        }

        @Override
        public Double getSymbolSize() {
            if (pltMdlElmt.getParamName() != null) {
                return pltMdlElmt.getSymbolSize();
            } else if (skyCovElmt.getParamName() != null) {
                return skyCovElmt.getSymbolSize();
            } else if (wndBrbElmt.getParamName() != null) {
                return wndBrbElmt.getSymbolSize();
            } else {
                return 1.0;
            }
        }

        @Override
        public Double getSymbolWidth() {
            if (pltMdlElmt.getParamName() != null) {
                return pltMdlElmt.getSymbolWidth();
            } else if (skyCovElmt.getParamName() != null) {
                return skyCovElmt.getSymbolWidth();
            } else if (wndBrbElmt.getParamName() != null) {
                return wndBrbElmt.getSymbolWidth();
            } else {
                return 1.0;
            }
        }

        // the color should be the same in any/all elements
        @Override
        protected RGB getColor() {
            gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.Color col = null;

            if (pltMdlElmt.getParamName() != null) {
                col = pltMdlElmt.getColor();
            } else if (skyCovElmt.getParamName() != null) {
                col = skyCovElmt.getColor();
            } else if (wndBrbElmt.getParamName() != null) {
                col = wndBrbElmt.getColor();
            } else if (col == null) {
                return new RGB(255, 255, 255); // use a default, white
            }

            return new RGB(col.getRed(), col.getGreen(), col.getBlue());
        }

        @Override
        public void setColor(RGB color) {
            super.setColor(color);
            wndBrbElmt.setColorRGB(color);
            skyCovElmt.setColorRGB(color);
        }

        @Override
        public void unselectButton() {
            super.unselectButton();
            enableCenterParamWidgets(false);
        }

        @Override
        public void paramButtonSelected() {
            super.paramButtonSelected();

            if (parmBtn.getSelection()) {

                // If any param is selected then set the background color
                // ('normal' param selections are checked in the super.)
                if (wndBrbElmt.getParamName() != null
                        || skyCovElmt.getParamName() != null) {

                    // Set color in the ColorMatricSelector widget
                    cms.setColorValue(getColor());

                    // Set grp background
                    grp.setBackground(new Color(getParent().getDisplay(),
                            cms.getColorValue()));
                }
            }

            enableCenterParamWidgets(parmBtn.getSelection());
        }

        @Override
        public boolean isParamNameSelected() {
            return (wndBrbElmt.getParamName() != null
                    || skyCovElmt.getParamName() != null
                    || pltMdlElmt.getParamName() != null);
        }

        @Override
        public void setSymbolSize(Double size) {
            super.setSymbolSize(size);
            this.wndBrbElmt.setSymbolSize(size);
            this.skyCovElmt.setSymbolSize(size);
        }

        @Override
        public void setSymbolWidth(Double width) {
            super.setSymbolWidth(width);
            this.wndBrbElmt.setSymbolWidth(width);
            this.skyCovElmt.setSymbolWidth(width);
        }

    }

    public void initWidgets() {
        if (editedPlotModel == null) {
            statusHandler.handle(Priority.PROBLEM,
                    "Null editedPlotModel in initWidgets() of the class EditPlotModelComposite");
            return;
        }

        textSizeCombo.setItems(textSizeOptions);
        textSizeCombo.select(4);

        textFontCombo.setItems(textFontOptions);
        textFontCombo.select(0); // to be replaced by init

        textStyleCombo.setItems(textStyleOptions);
        textStyleCombo.select(0); // to be replaced by init

        String[] strArray = plotParamDefns.getAllParameterNames(false, true);

        // Remove "P03X" from the menu ( the dummy XML it comes from in
        // plotParameters_obs.xml is needed to make the code run without a
        // a significant amount of reworking
        ArrayList<String> parameterNames = new ArrayList<String>();
        Collections.addAll(parameterNames, strArray);
        parameterNames.remove("P03X");

        strArray = parameterNames.toArray(new String[parameterNames.size()]);

        Arrays.sort(strArray);

        availParamsList.setItems(strArray);
        availParamsList.setEnabled(false);
    }

    private void setSelectedSkyAndWindParams() {
        PlotModelElemCenterButton cntrBtn = (PlotModelElemCenterButton) plotModelElementsUIMap
                .get(ELEMENT_POSITION_MIDDLE_CENTER);

        String seldSkyCovParam = cntrBtn.getSkyCovParamName();

        if (seldSkyCovParam == null) {
            comboSky.select(0);
        } else {
            // 0 indx is 'None'
            comboSky.select(availSkyCoverParams.indexOf(seldSkyCovParam) + 1);
        }

        String seldWndBarbParam = cntrBtn.getWindBarbParamName();

        if (seldWndBarbParam == null) {
            comboBrbk.select(0);
        } else {
            comboBrbk.select(availWindBarpParams.indexOf(seldWndBarbParam) + 1);
        }
    }

    private void updateTextAndSymbolWidgets() {
        // Set text widgets if applicable
        PlotParameterDefn prmDefn = null;
        boolean isTextApplicable = false;
        boolean isSymbApplicable = false;
        boolean isMarkApplicable = false;

        if (!isValidSelectedPlotModelElement() || plotModelElementsBeingEdited
                .contains(seldPlotModelElemButton.getPosition())) {

            // Disable the Advanced button if the above condition holds.
            advancedBtn.setEnabled(false);
        } else {
            advancedBtn.setEnabled(true);

            prmDefn = plotParamDefns.getPlotParamDefn(seldPlotModelElemButton
                    .getPlotModelElement().getParamName());

            // Add further constraints here on the plot class if necessary
            if (prmDefn != null) {
                isTextApplicable = prmDefn.getPlotMode()
                        .equalsIgnoreCase(PlotParameterDefn.PLOT_MODE_TEXT);
                isMarkApplicable = prmDefn.getPlotMode()
                        .equalsIgnoreCase(PlotParameterDefn.PLOT_MODE_MARKER);
                isSymbApplicable = prmDefn.getPlotMode()
                        .equalsIgnoreCase(PlotParameterDefn.PLOT_MODE_TABLE)
                        // PTND includes a symbol (table) component...
                        || prmDefn.getPlotParamName().equalsIgnoreCase(
                                PlotParameterDefn.PLOT_PARAMETER_NAME_PTND)
                                // ...markers are a subset of symbols...
                        || isMarkApplicable
                        // ...and symbol controls (size and line width) apply to
                        // vectors too...
                        || prmDefn.isVectorParameter();
            }
            // If this is the center button and a 'regular' parameter isn't set
            // then check if a skyc or wind barb is set and check if the symbol
            // is applicable
            else {
                isMarkApplicable = true;
                if (seldPlotModelElemButton instanceof PlotModelElemCenterButton) {
                    PlotModelElemCenterButton cntrBtn = (PlotModelElemCenterButton) seldPlotModelElemButton;
                    if (cntrBtn.getSkyCovParamName() != null
                            || cntrBtn.getWindBarbParamName() != null) {
                        isTextApplicable = false;
                        isSymbApplicable = true;
                        isMarkApplicable = false;
                    }
                }
            }

        }

        if (isTextApplicable) {
            textFontCombo.setEnabled(true);
            textSizeCombo.setEnabled(true);
            textStyleCombo.setEnabled(true);
            applyToAllBtn.setEnabled(true);

            for (int i = 0; i < textSizeCombo.getItemCount(); i++) {
                if (textSizeCombo.getItem(i).equals(seldPlotModelElemButton
                        .getPlotModelElement().getTextSize())) {
                    textSizeCombo.select(i);
                    break;
                }
            }

            for (int i = 0; i < textFontCombo.getItemCount(); i++) {
                if (textFontCombo.getItem(i)
                        .equalsIgnoreCase(seldPlotModelElemButton
                                .getPlotModelElement().getTextFont())) {
                    textFontCombo.select(i);
                    break;
                }
            }

            for (int i = 0; i < textStyleCombo.getItemCount(); i++) {
                if (textStyleCombo.getItem(i)
                        .equalsIgnoreCase(seldPlotModelElemButton
                                .getPlotModelElement().getTextStyle())) {
                    textStyleCombo.select(i);
                    break;
                }
            }

            if (applyToAllBtn.getSelection()) {
                applyTextChangesToAllParameters();
            }
        } else {
            textFontCombo.deselectAll();
            textFontCombo.setEnabled(false);
            textSizeCombo.deselectAll();
            textSizeCombo.setEnabled(false);
            textStyleCombo.deselectAll();
            textStyleCombo.setEnabled(false);
            applyToAllBtn.setEnabled(false);
        }

        // Set symbol size if applicable
        if (isSymbApplicable) {

            symbolSizeScale.setEnabled(true);

            if (seldPlotModelElemButton.getSymbolSize() > 3) {
                symbolSizeScale.setSelection(30);
                symbolSizeLabel.setText("    " + 3.0);
            } else {
                double size = seldPlotModelElemButton.getSymbolSize();
                symbolSizeScale.setSelection((int) (size * 10));
                symbolSizeLabel.setText("    " + size);
            }
        } else {
            symbolSizeScale.setSelection(0);
            symbolSizeScale.setEnabled(false);
            symbolSizeLabel.setText("    ");
        }

        // Set symbol line width if applicable
        if (isSymbApplicable) {

            symbolWidthScale.setEnabled(true);

            if (seldPlotModelElemButton.getSymbolWidth() > 5) {
                symbolWidthScale.setSelection(50);
                symbolWidthLabel.setText("    " + 5.0);
            } else {
                double width = seldPlotModelElemButton.getSymbolWidth();
                symbolWidthScale.setSelection((int) (width * 10));
                symbolWidthLabel.setText("    " + width);
            }
        } else {
            symbolWidthScale.setSelection(0);
            symbolWidthScale.setEnabled(false);
            symbolWidthLabel.setText("    ");
        }

        // Set marker type if applicable
        if (isMarkApplicable) {
            selectedMarkerType = seldPlotModelElemButton.getMarkerType();
            enableMarkerTypeSelection(true);
        } else {
            enableMarkerTypeSelection(false);
        }

    }

    private void enableMarkerTypeSelection(boolean isEnabled) {
        if (isEnabled) {
            Image icon = markerSelectMenu.getItem(selectedMarkerType.ordinal())
                    .getImage();
            markerSelectToolItem.setImage(icon);
            markerSelectToolItem
                    .setToolTipText(selectedMarkerType.getDesignator());
        }
        markerSelectToolBar.setEnabled(isEnabled);
        markerSelectToolItem.setEnabled(isEnabled);
        markerSelectMenu.setEnabled(isEnabled);
    }

    private void enableCenterParamWidgets(boolean isEnabled) {
        skycLbl.setEnabled(isEnabled);
        comboSky.setEnabled(isEnabled);
        wndBrbLbl.setEnabled(isEnabled);
        comboBrbk.setEnabled(isEnabled);
    }

    public PlotModel getEditedPlotModel() {
        return this.editedPlotModel;
    }

    private void applyTextChangesToAllParameters() {
        for (int i = 0; i < 11; i++) {
            PlotModelElemButton pmeBtn = plotModelElementsUIMap
                    .get(plotModelElementPositions[i]);
            if (pmeBtn != null) {
                PlotParameterDefn prmDefn = plotParamDefns.getPlotParamDefn(
                        pmeBtn.getPlotModelElement().getParamName());
                if (prmDefn != null) {
                    if (prmDefn.getPlotMode().equalsIgnoreCase("text")) {
                        pmeBtn.getPlotModelElement()
                                .setTextSize(textSizeCombo.getText());
                        pmeBtn.getPlotModelElement()
                                .setTextFont(textFontCombo.getText());
                        pmeBtn.getPlotModelElement()
                                .setTextStyle(textStyleCombo.getText());
                    }
                }
            }
        }
    }

    private void editAdvancedSettings() {

        PlotModelElement pltMdlElmt = null;
        PlotModelElement skycPltMdlElmt = null;
        PlotModelElement wbPltMdlElmt = null;

        if (seldPlotModelElemButton instanceof PlotModelElemCenterButton) {
            PlotModelElemCenterButton cntrBtn = (PlotModelElemCenterButton) seldPlotModelElemButton;

            skycPltMdlElmt = cntrBtn.getSkyCoveragePlotModelElement();
            wbPltMdlElmt = cntrBtn.getWindBarbPlotModelElement();

            // Properly assign selected element so Edit Advanced Options dialog
            // can be polualted.
            if (skycPltMdlElmt.getParamName() != null) {
                pltMdlElmt = skycPltMdlElmt;

            } else if (wbPltMdlElmt.getParamName() != null) {
                pltMdlElmt = wbPltMdlElmt;

            } else {
                pltMdlElmt = seldPlotModelElemButton.pltMdlElmt;
            }
        } else {
            pltMdlElmt = seldPlotModelElemButton.pltMdlElmt;
        }

        String position = seldPlotModelElemButton.getPosition();
        plotModelElementsBeingEdited.add(position);

        if (pltMdlElmt.getConditionalColorBar() == null) {
            ConditionalColorBar newColorBar = new ConditionalColorBar();
            pltMdlElmt.setConditionalColorBar(newColorBar);
            pltMdlElmt.setConditionalParameter(pltMdlElmt.getParamName());
        }

        String prevCondParam = pltMdlElmt.getConditionalParameter();
        ConditionalColorBar prevCondColorBar = new ConditionalColorBar(
                pltMdlElmt.getConditionalColorBar());

        EditPlotModelElementAdvancedDialog editConditionalFilterDlg = new EditPlotModelElementAdvancedDialog(
                topComposite.getShell(), pltMdlElmt, plotParamDefns);

        // Open Edit Advanced Options dialog lower-right to the previous one.
        int xCoordinate, yCoordinate;
        if (offsetCount == 0) {
            initXCoord = topComposite.getShell().getLocation().x
                    + topComposite.getShell().getSize().x + 10;
            initYCoord = topComposite.getShell().getLocation().y;
        }
        xCoordinate = initXCoord
                + offsetCount * X_OFFSET_FOR_EDIT_ADVANCED_OPTIONS_DIALOG;
        yCoordinate = initYCoord
                + offsetCount * Y_OFFSET_FOR_EDIT_ADVANCED_OPTIONS_DIALOG;
        offsetCount++;

        PlotModelElement newPme = (PlotModelElement) editConditionalFilterDlg
                .open(this, xCoordinate, yCoordinate);

        // This prevents exception of "widget is disposed".
        if (shell.isDisposed()) {
            clearEditedList();
            return;
        }

        plotModelElementsBeingEdited.remove(position);

        if (newPme != null) {
            pltMdlElmt = newPme;
        } else {
            pltMdlElmt.setConditionalParameter(prevCondParam);
            pltMdlElmt.setConditionalColorBar(prevCondColorBar);
        }

        // Avoid null-pointer exceptions thrown.
        if (seldPlotModelElemButton != null
                && seldPlotModelElemButton.grp != null) {
            if (advancedIconImage != null && pltMdlElmt.hasAdvancedSettings()) {
                seldPlotModelElemButton.grp
                        .setBackgroundImage(advancedIconImage);
                seldPlotModelElemButton.grp
                        .setToolTipText("Advanced Settings Applied.");

            } else {
                seldPlotModelElemButton.grp.setBackgroundImage(null);
                seldPlotModelElemButton.grp.setToolTipText("");
            }
        }

        if (skycPltMdlElmt != null && wbPltMdlElmt != null) {
            skycPltMdlElmt.setConditionalParameter(
                    pltMdlElmt.getConditionalParameter());
            skycPltMdlElmt.setConditionalColorBar(
                    pltMdlElmt.getConditionalColorBar());
            wbPltMdlElmt.setConditionalParameter(
                    pltMdlElmt.getConditionalParameter());
            wbPltMdlElmt.setConditionalColorBar(
                    pltMdlElmt.getConditionalColorBar());
        }
    }

    private void clearPlotModel() {
        clearEditedList();

        PlotModelElemCenterButton pmecb = null;
        PlotModelElemButton pmeBtn = null;
        Color widget_background = null;
        Color widget_foreground = null;
        Display display = Display.getDefault();

        // Get the background & foreground colors of an empty button
        widget_background = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        widget_foreground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

        seldPlotModelElemButton = null;
        availParamsList.deselectAll();
        availParamsList.setEnabled(false);

        // Clear Plot Model Buttons, make them all vanilla "Parm" buttons
        for (String position : plotModelElementPositions) {

            // Clear special middle center button
            if (position.equalsIgnoreCase(ELEMENT_POSITION_MIDDLE_CENTER)) {
                pmecb = (PlotModelElemCenterButton) plotModelElementsUIMap
                        .get(ELEMENT_POSITION_MIDDLE_CENTER);
                pmecb.parmBtn.setText(EMPTY_MODEL_ELEMENT_LABEL);
                pmecb.parmBtn.setSelection(false);
                pmecb.checkBtn.setSelection(false);
                pmecb.checkBtn.setEnabled(false);
                pmecb.grp.setBackground(widget_background);
                pmecb.grp.setBackgroundImage(null); // Clear the rainbow image
                pmecb.parmBtn.setBackground(widget_background);
                pmecb.parmBtn.setForeground(widget_foreground);
                editedPlotModel
                        .removePlotModelElement(pmecb.getPlotModelElement());
                editedPlotModel.removePlotModelElement(
                        pmecb.getSkyCoveragePlotModelElement());
                editedPlotModel.removePlotModelElement(
                        pmecb.getWindBarbPlotModelElement());
                pmecb.pltMdlElmt = new PlotModelElement();
                pmecb.pltMdlElmt.setPosition(position);
                pmecb.skyCovElmt.setColorRGB(new RGB(255, 255, 255));
                pmecb.wndBrbElmt.setColorRGB(new RGB(255, 255, 255));
            } // all other positions
            else {
                pmeBtn = plotModelElementsUIMap.get(position);
                pmeBtn.parmBtn.setText(EMPTY_MODEL_ELEMENT_LABEL);
                pmeBtn.parmBtn.setSelection(false);
                pmeBtn.checkBtn.setSelection(false);
                pmeBtn.checkBtn.setEnabled(false);
                pmeBtn.grp.setBackground(widget_background);
                pmeBtn.grp.setBackgroundImage(null); // Clear the rainbow image
                pmeBtn.parmBtn.setBackground(widget_background);
                pmeBtn.parmBtn.setForeground(widget_foreground);
                editedPlotModel
                        .removePlotModelElement(pmeBtn.getPlotModelElement());
                pmeBtn.pltMdlElmt = new PlotModelElement();
                pmeBtn.pltMdlElmt.setPosition(position);
            }
        } // end for-loop
    }// end method clearPlotModel()

    private void resetPlotModel() {
        clearEditedList();

        PlotModel origPlotModel = new PlotModel(this.getOriginalPlotModel());
        PlotModelElement opme = null;
        PlotModelElement oskypme, owindpme, tpme = null;
        PlotModelElemButton pmeBtn = null;
        PlotModelElemCenterButton cntrBtn = null;
        RGB rgb = null;
        int red, green, blue = 0;

        clearPlotModel();

        for (String position : plotModelElementPositions) {

            opme = origPlotModel.getPlotModelElement(position);
            if (opme == null) {
                opme = new PlotModelElement();
                opme.setPosition(position);
            }

            // Reset the special center button
            if (position.equalsIgnoreCase(ELEMENT_POSITION_MIDDLE_CENTER)) {

                oskypme = origPlotModel.getSkyCoverageElement();
                owindpme = origPlotModel.getWindBarbElement();

                // Not all plot models have a MIDDLE_CENTER button with sky &
                // wind PMEs
                if (oskypme != null)
                    tpme = oskypme;
                else if (owindpme != null)
                    tpme = owindpme;
                else
                    tpme = opme;

                red = tpme.getColor().getRed();
                green = tpme.getColor().getGreen();
                blue = tpme.getColor().getBlue();
                rgb = new RGB(red, green, blue);

                cntrBtn = (PlotModelElemCenterButton) origPltMdlElmntsUIMap
                        .get(position);
                cntrBtn.setColor(rgb);
                cntrBtn.setParmName(opme.getParamName());
                cntrBtn.pltMdlElmt = opme;

                if (oskypme != null) {
                    cntrBtn.setSkyCoverageParamName(oskypme.getParamName());
                    cntrBtn.skyCovElmt = oskypme;
                    if (oskypme.getParamName() != null) {
                        editedPlotModel.putPlotModelElement(oskypme);
                    }
                }

                if (owindpme != null) {
                    cntrBtn.setWindBarbParamName(owindpme.getParamName());
                    cntrBtn.wndBrbElmt = owindpme;
                    if (owindpme.getParamName() != null) {
                        this.editedPlotModel.putPlotModelElement(owindpme);
                    }
                }

                if (oskypme != null || owindpme != null) {
                    cntrBtn.pltMdlElmt.setParamName(null);
                }

                if (advancedIconImage != null && tpme.hasAdvancedSettings()) {
                    cntrBtn.grp.setBackgroundImage(advancedIconImage);
                }

                cntrBtn.init();

            } // reset all other positions
            else {

                // Get the original button color
                red = opme.getColor().getRed();
                green = opme.getColor().getGreen();
                blue = opme.getColor().getBlue();
                rgb = new RGB(red, green, blue);

                pmeBtn = origPltMdlElmntsUIMap.get(position);
                pmeBtn.setColor(rgb);
                pmeBtn.setParmName(opme.getParamName());
                pmeBtn.pltMdlElmt = opme;

                if (advancedIconImage != null && opme.hasAdvancedSettings()) {
                    pmeBtn.grp.setBackgroundImage(advancedIconImage);
                }

                pmeBtn.init();

            } // end all other positions

            if (opme.getParamName() != null) {
                editedPlotModel.putPlotModelElement(opme);
            } else { // opme null paramname
                editedPlotModel.removePlotModelElement(opme);
            }
        } // end for-loop
    }

    private PlotModel getOriginalPlotModel() {
        PlotModel origPlotModel = null;

        try {
            if (rscData != null) {
                INatlCntrsResourceData origResourceData = null;
                ResourceName resource_name = null;
                ResourceSelection original_resource = null;
                RscAttrValue origPlotModelRscAttr = null;
                ResourceAttrSet origAttrSet = null;

                resource_name = rscData.getResourceName();
                original_resource = ResourceFactory
                        .createResource(resource_name);
                origResourceData = original_resource.getResourceData();
                origAttrSet = new ResourceAttrSet(
                        origResourceData.getRscAttrSet());
                origPlotModelRscAttr = origAttrSet.getRscAttr("plotModel");
                origPlotModel = (PlotModel) origPlotModelRscAttr.getAttrValue();

            } else {
                // Find original plot model based on localization info.
                String editedPlugin = editedPlotModel.getPlugin();
                String editedName = editedPlotModel.getName();
                LocalizationLevel editedLevel = editedPlotModel
                        .getLocalizationFile().getContext()
                        .getLocalizationLevel();

                HashMap<String, PlotModel> originalPlotModels = PlotModelMngr
                        .getInstance().getPlotModelsByPlugin(editedPlugin);

                for (PlotModel model : originalPlotModels.values()) {
                    LocalizationLevel level = model.getLocalizationFile()
                            .getContext().getLocalizationLevel();

                    if (model.getName().equals(editedName)
                            && level.equals(editedLevel)) {
                        origPlotModel = model;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "not able to get original plot model." + e);
        }
        return origPlotModel;
    }

    /**
     * This method allows Edit Advanced Options dialog to register its shell
     * (window).
     */
    public void addChild(Shell child) {
        childrenList.add(child);
    }

    /**
     * 
     */
    private boolean isValidSelectedPlotModelElement() {
        return !(seldPlotModelElemButton == null
                || !seldPlotModelElemButton.isParamNameSelected()
                || seldPlotModelElemButton.getButtonLabel()
                        .equals(EMPTY_MODEL_ELEMENT_LABEL));
    }

    /**
     * Clear the list of Plot Model elements that are being edited, and dispose
     * their Edit Advanced Options dialogs.
     */
    public void clearEditedList() {
        plotModelElementsBeingEdited = new HashSet<String>();

        for (Shell child : childrenList) {
            child.dispose();
        }
        childrenList = new HashSet<Shell>();
    }

    /**
     * Restart the position offsetCount for Advanced Options Dialog.
     */
    public static void clearAdvancedOptionsDialogPositionOffsetCount() {
        offsetCount = 0;
    }
    
    private String getToolTipText (PlotParameterDefn paramDefn){
        if (paramDefn == null) {
            return "";
        } else {
            if (paramDefn.getDeriveParams() != null) {
                if (paramDefn.getDeriveParams()[0].equals("all")) {
                    return "Derived Parameter";
                } else {
                    String[] derivedParameters = paramDefn.getDeriveParams();
                    String derivedList = "";
                    if (derivedParameters.length > 1){
                        for (int i = 0; i < derivedParameters.length; i++){                                
                            derivedList = derivedList + derivedParameters[i] + ", ";
                        }
                        derivedList = derivedList.replaceAll(", $", "");
                        return "Derived from " + derivedList;
                    } else {
                    return "Derived from "+ paramDefn.getDeriveParams()[0];
                    }
                }
            } else {
                return paramDefn.getMetParamName();
            }
        }
    }
}
