/*
 * This code has been developed by NCEP-SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.resources.colorBar;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.converter.ConversionException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.viz.core.imagery.ImageCombiner;
import com.raytheon.viz.core.imagery.ImageCombiner.IImageCombinerListener;
import com.raytheon.viz.ui.dialogs.colordialog.ColorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarAnchorLocation;
import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarOrientation;
import gov.noaa.nws.ncep.viz.common.ColorMapUtil;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceCategory;
import gov.noaa.nws.ncep.viz.ui.display.ColorBarFromColormap;
import gov.noaa.nws.ncep.viz.ui.display.IColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * 
 * Creates the widgets that constitute the dialogs for editing the color bar
 * from a color map
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 2/29/2012     651        Archana       Initial Creation
 * 04/02/2012               S. Gurung     Increased height for colorBarLocOptionsGroup
 * 04/06/2012    651        Archana       Added a call to refreshColorBar(), when the showLabelBtn
 *                                        is toggled
 * 07/17/2012    743        Archana       Refactored the packages for 
 *                                        ColorBarAnchorLocation and ColorBarOrientation                                              
 * 02/09/2013    972        Greg Hull     ResourceCategory class
 * 04/05/2016    R15715     dgilling      Rewrite to fix bugs with Capabilities handling.
 * 01/25/2017    R20988     K.Bugenhagen  Updated constructor to check for 
 *                                        correct colormap name.
 * </pre>
 * 
 * @author archana
 * @version 1
 */

public class ColorBarFromColorMapAttrsEditorComposite extends Composite {

    private static final ColorBarAnchorLocation[] AVAIL_ANCHOR_LOCS = new ColorBarAnchorLocation[] {
            ColorBarAnchorLocation.UpperLeft, ColorBarAnchorLocation.UpperRight,
            ColorBarAnchorLocation.LowerLeft,
            ColorBarAnchorLocation.LowerRight, };

    private static final int DEFAULT_DLG_HEIGHT = 205;

    private static final int SHELL_WIDTH = 764;

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final ColorMapCapability cMapCapability;

    private final ImagingCapability imgCapability;

    private final INatlCntrsResourceData theResourceData;

    private final AbstractEditor currEditor;

    private final List<AbstractNatlCntrsResource<?, ?>> imageResources;

    private String[] arrayOfSliderTextStrings = null;

    private float cmapMin;

    private float cmapMax;

    private float cmapWidth;

    private float cmapIncrement;

    private float currentMax;

    private float currentMin;

    private NumberFormat format = null;

    private String selectedColorMapName = null;

    private Combo orientationCombo = null;

    private Combo anchorCombo = null;

    private IColorBar editedColorBar = null;

    private ResourceCategory colorMapCategory = null;

    private String colorMapName;

    private Scale minSlider = null;

    private Scale maxSlider = null;

    private Text minValueText = null;

    private Text maxValueText = null;

    private Scale brightnessScale = null;

    private Label brightnessText = null;

    private Scale contrastScale = null;

    private Label contrastText = null;

    private Scale alphaScale = null;

    private Label alphaLabel = null;

    private Label alphaText = null;

    private Button interpolationChk = null;

    private Button combineNextImage = null;

    private Button showColorBarEditOptionsBtn = null;

    private IImageCombinerListener combineNextImageListener = new IImageCombinerListener() {
        @Override
        public void preferenceChanged(boolean newPref) {
            combineNextImage.setSelection(newPref);
        }
    };

    private Color labelColor;

    public ColorBarFromColorMapAttrsEditorComposite(Composite parent, int style,
            INatlCntrsResourceData resourceData, Capabilities capabilities) {
        super(parent, style);

        this.theResourceData = resourceData;
        this.cMapCapability = (capabilities
                .hasCapability(ColorMapCapability.class))
                        ? capabilities.getCapability(
                                (AbstractResourceData) this.theResourceData,
                                ColorMapCapability.class)
                        : null;
        this.imgCapability = (capabilities
                .hasCapability(ImagingCapability.class))
                        ? capabilities.getCapability(
                                (AbstractResourceData) this.theResourceData,
                                ImagingCapability.class)
                        : null;

        imageResources = new ArrayList<>();
        ResourceAttrSet resAttrSet = theResourceData.getRscAttrSet();
        colorMapCategory = theResourceData.getResourceName().getRscCategory();

        colorMapName = resAttrSet.getRscAttr("colorMapName").getAttrValue()
                .toString();

        String colorMapNameFromCapabilities = capabilities
                .getCapability((AbstractResourceData) resourceData,
                        ColorMapCapability.class)
                .getColorMapParameters().getColorMapName();

        /*
         * For some resources (e.g. Gini satellite), the colormap name obtained
         * from the ResourceAttrSet does not reflect the correct name. Compare
         * it to the colormap name found in the capabilities and use that if
         * they differ.
         */
        if (!colorMapNameFromCapabilities.equals(colorMapName)) {
            colorMapName = colorMapNameFromCapabilities;
        }

        editedColorBar = (ColorBarFromColormap) resAttrSet
                .getRscAttr("colorBar").getAttrValue();

        labelColor = new Color(getDisplay(), editedColorBar.getLabelColor());
        currEditor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (currEditor != null) {
            ResourceList resList = currEditor.getActiveDisplayPane()
                    .getRenderableDisplay().getDescriptor().getResourceList();
            if (resList != null && resList.size() > 0) {
                for (ResourcePair rp : resList) {
                    AbstractVizResource<?, ?> resource = rp.getResource();
                    if (resource instanceof AbstractNatlCntrsResource<?, ?>) {
                        if (resource.hasCapability(ImagingCapability.class)) {
                            imageResources.add(
                                    (AbstractNatlCntrsResource<?, ?>) resource);
                        }
                    }
                }
            }
        }

        Composite topForm = this;
        FormData fd = new FormData(SHELL_WIDTH, DEFAULT_DLG_HEIGHT);
        fd.top = new FormAttachment(0, 0);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        topForm.setLayoutData(fd);
        topForm.setLayout(new FormLayout());

        topForm.setLayout(new GridLayout(1, true));
        final Composite colorMapComp = new Composite(topForm, SWT.NONE);
        createColorMapComboAndEditButton(colorMapComp);

        fd = new FormData(163, 30);
        fd.left = new FormAttachment(colorMapComp, 525, SWT.RIGHT);
        fd.top = new FormAttachment(colorMapComp, 15, SWT.BOTTOM);
        showColorBarEditOptionsBtn = new Button(colorMapComp, SWT.PUSH);
        showColorBarEditOptionsBtn.setLayoutData(fd);
        showColorBarEditOptionsBtn.setText("Edit Color Bar Options ...");

        Group imagingGrp = new Group(colorMapComp, SWT.NONE);
        imagingGrp.setText("Image Properties");
        fd = new FormData(350, 120);
        fd.left = new FormAttachment(colorMapComp, 10, SWT.RIGHT);
        fd.top = new FormAttachment(colorMapComp, 60, SWT.BOTTOM);

        imagingGrp.setLayout(new FormLayout());
        imagingGrp.setLayoutData(fd);

        // Create the slider controls for Alpha, Brightness and Contrast
        initializeABCControls(imagingGrp);

        Group minMaxValuesGroup = new Group(colorMapComp, SWT.NONE);
        minMaxValuesGroup.setText("Colormap Range:");
        fd = new FormData(360, 75);
        fd.left = new FormAttachment(imagingGrp, 20, SWT.RIGHT);
        fd.top = new FormAttachment(colorMapComp, 65, SWT.BOTTOM);
        minMaxValuesGroup.setLayout(new FormLayout());
        minMaxValuesGroup.setLayoutData(fd);
        createColorMapRangeControlSliders(minMaxValuesGroup);

        final Group colorBarLocOptionsGroup = new Group(colorMapComp,
                SWT.SHADOW_ETCHED_OUT);
        colorBarLocOptionsGroup.setText("Edit Color Bar Options");

        colorBarLocOptionsGroup.setLayout(new FormLayout());
        createColorBarLocationDimensionAndLabelDisplayControls(
                colorBarLocOptionsGroup);
        showColorBarEditOptionsBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Shell shell = getShell();

                if (((Button) e.widget).getText().startsWith("Edit")) {
                    ((Button) e.widget).setText("Hide ...");
                    shell.setSize(new Point(shell.getSize().x, shell.getSize().y
                            + colorBarLocOptionsGroup.getSize().y + 30));
                    colorBarLocOptionsGroup.setVisible(true);
                } else {
                    ((Button) e.widget).setText("Edit Color Bar Options ...");
                    colorBarLocOptionsGroup.setVisible(false);
                    shell.setSize(new Point(shell.getSize().x, shell.getSize().y
                            - colorBarLocOptionsGroup.getSize().y - 30));
                }

            }
        });

        fd = new FormData(730, 95);

        fd.left = new FormAttachment(colorMapComp, 10, SWT.RIGHT);
        fd.top = new FormAttachment(imagingGrp, 20, SWT.BOTTOM);
        colorBarLocOptionsGroup.setLayoutData(fd);
        colorBarLocOptionsGroup.setVisible(false);

    }

    private void createColorBarLocationDimensionAndLabelDisplayControls(
            Composite colorBarLocOptionsGroup) {

        Composite newComp = new Composite(colorBarLocOptionsGroup, SWT.NONE);
        newComp.setLayout(new FormLayout());

        Label orLabel = new Label(newComp, SWT.NONE);
        orLabel.setText("Orientation");
        FormData fd = new FormData();
        fd.left = new FormAttachment(newComp, 8, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        orLabel.setLayoutData(fd);

        orientationCombo = new Combo(newComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd = new FormData();
        fd.left = new FormAttachment(orLabel, 20, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        orientationCombo.setLayoutData(fd);

        Label anchorLabel = new Label(newComp, SWT.NONE);
        anchorLabel.setText("Anchor");
        fd = new FormData();
        fd.left = new FormAttachment(newComp, 8, SWT.RIGHT);
        fd.top = new FormAttachment(orientationCombo, 20, SWT.BOTTOM);
        anchorLabel.setLayoutData(fd);

        anchorCombo = new Combo(newComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd = new FormData();
        fd.left = new FormAttachment(anchorLabel, 45, SWT.RIGHT);
        fd.top = new FormAttachment(orientationCombo, 20, SWT.BOTTOM);
        anchorCombo.setLayoutData(fd);

        Label lengthLabel = new Label(newComp, SWT.NONE);
        lengthLabel.setText("Length");
        fd = new FormData();
        fd.left = new FormAttachment(anchorCombo, 40, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        lengthLabel.setLayoutData(fd);

        final Spinner lenSpinner = new Spinner(newComp, SWT.BORDER);
        fd = new FormData();
        fd.left = new FormAttachment(lengthLabel, 10, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        lenSpinner.setLayoutData(fd);
        lenSpinner.setToolTipText(
                "ColorBar length as a percentage of the screen size");

        lenSpinner.setMinimum(10);
        lenSpinner.setMaximum(100);
        lenSpinner.setIncrement(5);
        lenSpinner
                .setSelection((int) (editedColorBar.getLengthAsRatio() * 100));
        lenSpinner.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                editedColorBar
                        .setLengthAsRatio((lenSpinner.getSelection()) / 100f);
                refreshColorBar();

            }
        });

        Label percentLabel = new Label(newComp, SWT.NONE);
        percentLabel.setText("%");
        fd = new FormData();
        fd.left = new FormAttachment(lenSpinner, 7, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        percentLabel.setLayoutData(fd);

        Label widthLabel = new Label(newComp, SWT.NONE);
        widthLabel.setText("Width");
        fd = new FormData();
        fd.left = new FormAttachment(anchorCombo, 40, SWT.RIGHT);
        fd.top = new FormAttachment(lengthLabel, 20, SWT.BOTTOM);
        widthLabel.setLayoutData(fd);

        final Spinner widthSpinner = new Spinner(newComp, SWT.BORDER);

        widthSpinner.setMinimum(2);
        widthSpinner.setMaximum(50);
        widthSpinner.setIncrement(1);
        widthSpinner.setSelection(editedColorBar.getWidthInPixels());
        widthSpinner.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                editedColorBar.setWidthInPixels(widthSpinner.getSelection());
                refreshColorBar();
            }
        });

        fd = new FormData();
        fd.left = new FormAttachment(widthLabel, 20, SWT.RIGHT);
        fd.top = new FormAttachment(lengthLabel, 20, SWT.BOTTOM);
        widthSpinner.setLayoutData(fd);

        Label pixelLabel = new Label(newComp, SWT.NONE);
        pixelLabel.setText("pixels");
        fd = new FormData();
        fd.left = new FormAttachment(widthSpinner, 7, SWT.RIGHT);
        fd.top = new FormAttachment(lengthLabel, 20, SWT.BOTTOM);
        pixelLabel.setLayoutData(fd);

        final Button showLabelsBtn = new Button(newComp, SWT.CHECK);
        showLabelsBtn.setText("Show Label");
        fd = new FormData();
        fd.left = new FormAttachment(pixelLabel, 40, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        showLabelsBtn.setLayoutData(fd);
        showLabelsBtn.setSelection(editedColorBar.getShowLabels());

        showLabelsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editedColorBar.setShowLabels(showLabelsBtn.getSelection());
                refreshColorBar();
            }
        });

        Composite labelColorComp = new Composite(newComp, SWT.NONE);
        labelColorComp.setLayout(new FormLayout());
        fd = new FormData();
        fd.left = new FormAttachment(pixelLabel, 25, SWT.RIGHT);
        fd.top = new FormAttachment(showLabelsBtn, 20, SWT.BOTTOM);
        labelColorComp.setLayoutData(fd);
        final ColorButtonSelector labelColorSelector = new ColorButtonSelector(
                labelColorComp, 50, 25);
        labelColorSelector.setColorValue(labelColor.getRGB());
        labelColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (labelColor != null) {
                    labelColor.dispose();
                }
                labelColor = new Color(getDisplay(),
                        labelColorSelector.getColorValue());
                editedColorBar.setLabelColor(labelColor.getRGB());
                refreshColorBar();

            }
        });

        Label labelColorLbl = new Label(newComp, SWT.NONE);
        labelColorLbl.setText("Label Color");
        fd = new FormData();
        fd.left = new FormAttachment(pixelLabel, 80, SWT.RIGHT);
        fd.top = new FormAttachment(showLabelsBtn, 20, SWT.BOTTOM);
        labelColorLbl.setLayoutData(fd);

        orientationCombo.add(ColorBarOrientation.Vertical.name());
        orientationCombo.add(ColorBarOrientation.Horizontal.name());

        orientationCombo.select(
                editedColorBar.getOrientation() == ColorBarOrientation.Vertical
                        ? 0 : 1);

        orientationCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editedColorBar.setOrientation(
                        (orientationCombo.getSelectionIndex() == 0
                                ? ColorBarOrientation.Vertical
                                : ColorBarOrientation.Horizontal));
                refreshColorBar();
            }
        });

        for (ColorBarAnchorLocation anchorLoc : AVAIL_ANCHOR_LOCS) {
            anchorCombo.add(anchorLoc.name());
        }
        //
        for (int a = 0; a < AVAIL_ANCHOR_LOCS.length; a++) {
            if (editedColorBar.getAnchorLoc() == AVAIL_ANCHOR_LOCS[a]) {
                anchorCombo.select(a);
            }
        }

        anchorCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editedColorBar.setAnchorLoc(
                        AVAIL_ANCHOR_LOCS[anchorCombo.getSelectionIndex()]);
                refreshColorBar();
            }
        });

        final Button reverseColorsBtn = new Button(newComp, SWT.CHECK);
        reverseColorsBtn.setText("Reverse colors");
        fd = new FormData();
        fd.left = new FormAttachment(showLabelsBtn, 40, SWT.RIGHT);
        fd.top = new FormAttachment(newComp, 20, SWT.BOTTOM);
        reverseColorsBtn.setLayoutData(fd);
        reverseColorsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (editedColorBar.getReverseOrder()) {
                    editedColorBar.setReverseOrder(false);
                } else {
                    editedColorBar.setReverseOrder(true);
                }
                refreshColorBar();
            }
        });

    }

    private void createColorMapRangeControlSliders(
            Composite theColorRangeGroup) {
        buildColorMapData();

        Composite minMaxValues = new Composite(theColorRangeGroup, SWT.NONE);
        minMaxValues.setLayout(new GridLayout(3, false));
        Label maxLabel = new Label(minMaxValues, SWT.None);
        maxLabel.setText("Max:");

        maxSlider = new Scale(minMaxValues, SWT.HORIZONTAL);
        maxSlider.setMaximum(255);
        maxSlider.setMinimum(0);
        maxSlider.setIncrement(1);
        maxSlider.setSelection(cmapToSelection(currentMax));
        GridData layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.minimumWidth = 250;
        maxSlider.setLayoutData(layoutData);

        GridData labelLayoutData = new GridData(SWT.FILL, SWT.DEFAULT, false,
                false);
        maxValueText = new Text(minMaxValues,
                SWT.SINGLE | SWT.BORDER | SWT.RIGHT);
        maxValueText.setLayoutData(labelLayoutData);
        maxValueText.setText(cmapToText(currentMax));

        Label minLabel = new Label(minMaxValues, SWT.None);
        minLabel.setText("Min:");

        minSlider = new Scale(minMaxValues, SWT.HORIZONTAL);
        minSlider.setMaximum(255);
        minSlider.setMinimum(0);
        minSlider.setIncrement(1);
        minSlider.setSelection(cmapToSelection(currentMin));
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.minimumWidth = 250;
        minSlider.setLayoutData(layoutData);

        labelLayoutData = new GridData(SWT.FILL, SWT.DEFAULT, false, false);

        minValueText = new Text(minMaxValues,
                SWT.SINGLE | SWT.BORDER | SWT.RIGHT);
        minValueText.setLayoutData(labelLayoutData);
        minValueText.setText(cmapToText(currentMin));

        maxSlider.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (maxSlider.getSelection() <= minSlider.getSelection()) {
                    maxSlider.setSelection(minSlider.getSelection() + 1);
                }

                changeMax(maxSlider.getSelection());
            }
        });

        minSlider.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (minSlider.getSelection() >= maxSlider.getSelection()) {
                    minSlider.setSelection(maxSlider.getSelection() - 1);
                }

                changeMin(minSlider.getSelection());
            }
        });

        maxValueText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR) {
                    maxTextChanged();
                }
            }
        });

        minValueText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR) {
                    minTextChanged();
                }
            }
        });
    }

    private void createColorMapComboAndEditButton(Composite colorMapComp) {
        FormData fd = new FormData();
        colorMapComp.setLayout(new FormLayout());
        Label selectColorMapLabel = new Label(colorMapComp, SWT.NONE);
        selectColorMapLabel.setText("Select ColorMap");
        fd = new FormData();
        fd.left = new FormAttachment(colorMapComp, 17, SWT.RIGHT);
        fd.top = new FormAttachment(colorMapComp, 20, SWT.BOTTOM);
        selectColorMapLabel.setLayoutData(fd);
        selectColorMapLabel.setVisible(true);
        final Combo colorMapNamesCombo = new Combo(colorMapComp,
                SWT.READ_ONLY | SWT.DROP_DOWN);
        fd = new FormData();
        fd.left = new FormAttachment(selectColorMapLabel, 20, SWT.RIGHT);
        fd.top = new FormAttachment(colorMapComp, 17, SWT.BOTTOM);
        colorMapNamesCombo.setLayoutData(fd);

        if (colorMapCategory != null
                && colorMapCategory != ResourceCategory.NullCategory) {
            final String[] listOfColorMapNames = ColorMapUtil
                    .listColorMaps(colorMapCategory.getCategoryName());
            if (listOfColorMapNames != null && listOfColorMapNames.length > 0) {
                colorMapNamesCombo.setItems(listOfColorMapNames);

                int seldColorMapIndx = -1;
                if (colorMapName != null) {
                    for (int c = 0; c < listOfColorMapNames.length; c++) {
                        if (listOfColorMapNames[c]
                                .compareTo(colorMapName) == 0) {
                            colorMapNamesCombo.select(c);
                            selectedColorMapName = listOfColorMapNames[c];
                            seldColorMapIndx = c;
                        }
                    }

                    if (seldColorMapIndx == -1) {
                        statusHandler.warn("The current colorMap '"
                                + colorMapName + "' was not found.");
                        seldColorMapIndx = 0;
                        colorMapNamesCombo.select(0);
                    }
                }

                colorMapNamesCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        // update the selected color map name in the attrSet and
                        // in the colorbar editor.
                        selectedColorMapName = new String(
                                listOfColorMapNames[colorMapNamesCombo
                                        .getSelectionIndex()]);
                        if ((StringUtils.isNotEmpty(selectedColorMapName))
                                && (colorMapName != null)) {
                            ResourceAttrSet resAttrSet = theResourceData
                                    .getRscAttrSet();
                            resAttrSet.getRscAttr("colorMapName")
                                    .setAttrValue(selectedColorMapName);
                            theResourceData.setRscAttrSet(resAttrSet);
                            setColorBarFromColorMap(selectedColorMapName);
                            refreshColorBar();
                        }
                    }
                });
            }
        }

        Button editColorsBtn = new Button(colorMapComp, SWT.PUSH);
        editColorsBtn.setText("Edit Colors...");
        fd = new FormData();
        fd.left = new FormAttachment(colorMapNamesCombo, 20, SWT.RIGHT);
        fd.top = new FormAttachment(colorMapComp, 17, SWT.BOTTOM);
        editColorsBtn.setLayoutData(fd);
        editColorsBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (currEditor != null) {
                    String cmapEditorCmdStr = "gov.noaa.nws.ncep.viz.colorMapEditor";

                    ICommandService service = (ICommandService) currEditor
                            .getSite().getService(ICommandService.class);
                    Command cmd = service.getCommand(cmapEditorCmdStr);
                    if (cmd != null) {
                        try {
                            Map<String, Object> params = new HashMap<>();
                            ExecutionEvent exec = new ExecutionEvent(cmd,
                                    params, null, null);
                            cmd.executeWithChecks(exec);
                        } catch (Exception ex) {
                            statusHandler.error(
                                    "Error executing cmd: " + cmapEditorCmdStr,
                                    ex);
                        }
                    }

                    if (currEditor != null) {
                        currEditor.refresh();
                    }
                }
            }

        });

    }

    /**
     * Creates the sliders to control the brightness and contrast of the image.
     * Refactored from Raytheon's ImagingDialog class
     * 
     * @param mainComp
     */
    private void initializeABCControls(Composite mainComp) {
        Composite body = new Composite(mainComp, SWT.NONE);
        body.setLayout(new GridLayout(3, false));
        Label label = new Label(body, SWT.BOLD);
        label.setText("Brightness: ");

        brightnessScale = new Scale(body, SWT.NONE);
        brightnessScale.setLayoutData(new GridData(200, SWT.DEFAULT));

        brightnessText = new Label(body, SWT.NONE);
        brightnessText.setLayoutData(new GridData(50, SWT.DEFAULT));

        brightnessScale.setMinimum(0);
        brightnessScale.setMaximum(100);
        brightnessScale.setIncrement(1);
        brightnessScale.setPageIncrement(5);

        brightnessScale.setSelection(((Number) theResourceData.getRscAttrSet()
                .getRscAttr("brightness").getAttrValue()).intValue() * 100);
        brightnessText.setText(brightnessScale.getSelection() + "%");

        Label label2 = new Label(body, SWT.BOLD);
        label2.setText("Contrast: ");

        contrastScale = new Scale(body, SWT.NONE);
        contrastScale.setLayoutData(new GridData(200, SWT.DEFAULT));

        contrastText = new Label(body, SWT.NONE);
        contrastText.setLayoutData(new GridData(50, SWT.DEFAULT));

        contrastScale.setMinimum(0);
        contrastScale.setMaximum(100);
        contrastScale.setIncrement(1);
        contrastScale.setPageIncrement(5);
        int contrastVal = ((Number) theResourceData.getRscAttrSet()
                .getRscAttr("contrast").getAttrValue()).intValue() * 100;
        contrastScale.setSelection(contrastVal);
        contrastText.setText(contrastScale.getSelection() + "%");
        initializeAlphaScale(body);

        Composite checkBoxComp = new Composite(body, SWT.NONE);
        checkBoxComp.setLayoutData(
                new GridData(SWT.CENTER, SWT.CENTER, true, true, 3, 1));
        checkBoxComp.setLayout(new GridLayout(2, false));

        interpolationChk = new Button(checkBoxComp, SWT.CHECK);
        interpolationChk.setText("Interpolate");
        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, true);
        interpolationChk.setLayoutData(gd);
        interpolationChk.setEnabled(false);
        interpolationChk.setGrayed(true);
        combineNextImage = new Button(checkBoxComp, SWT.CHECK);
        combineNextImage.setText("Combine Next Image Load");
        gd = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        combineNextImage.setLayoutData(gd);
        combineNextImage.setSelection(ImageCombiner.isCombineImages());
        combineNextImage.setEnabled(false);
        combineNextImage.setGrayed(true);
        combineNextImage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Lets call our command
                IHandlerService handlerService = (IHandlerService) PlatformUI
                        .getWorkbench().getActiveWorkbenchWindow()
                        .getService(IHandlerService.class);
                try {
                    handlerService.executeCommand(
                            "com.raytheon.uf.viz.d2d.ui.actions.imageCombination",
                            null);
                } catch (Exception ex) {
                    statusHandler.error(
                            "Error changing setting for 'Combine Next Image Load'",
                            ex);
                }
                combineNextImage.setSelection(ImageCombiner.isCombineImages());
                if (currEditor != null) {
                    currEditor.refresh();
                }
            }
        });

        ImageCombiner.addListener(combineNextImageListener);

        brightnessScale.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                brightnessText.setText(brightnessScale.getSelection() + "%");

                if ((imgCapability != null) && (theResourceData != null)) {
                    float brightnessValue = brightnessScale.getSelection()
                            / 100.0f;
                    ResourceAttrSet resAttrSet = theResourceData
                            .getRscAttrSet();
                    resAttrSet.getRscAttr("brightness")
                            .setAttrValue(brightnessValue);
                    theResourceData.setRscAttrSet(resAttrSet);

                    imgCapability
                            .setContrast(
                                    ((Number) resAttrSet.getRscAttr("contrast")
                                            .getAttrValue()).floatValue(),
                                    false);
                    imgCapability
                            .setAlpha(
                                    ((Number) resAttrSet.getRscAttr("alpha")
                                            .getAttrValue()).floatValue(),
                                    false);
                    imgCapability.setBrightness(brightnessValue);
                }
            }
        });

        contrastScale.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                contrastText.setText(contrastScale.getSelection() + "%");

                if ((imgCapability != null) && (theResourceData != null)) {
                    float cValue = contrastScale.getSelection() / 100.0f;
                    ResourceAttrSet resAttrSet = theResourceData
                            .getRscAttrSet();
                    resAttrSet.getRscAttr("contrast").setAttrValue(cValue);
                    theResourceData.setRscAttrSet(resAttrSet);

                    imgCapability
                            .setBrightness(
                                    ((Number) resAttrSet
                                            .getRscAttr("brightness")
                                            .getAttrValue()).floatValue(),
                                    false);
                    imgCapability
                            .setAlpha(
                                    ((Number) resAttrSet.getRscAttr("alpha")
                                            .getAttrValue()).floatValue(),
                                    false);
                    imgCapability.setContrast(cValue);
                }
            }
        });

        interpolationChk.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (imageResources != null) {
                    for (AbstractNatlCntrsResource<?, ?> rsc : imageResources) {
                        ResourceCategory rscCat = rsc.getResourceData()
                                .getResourceName().getRscCategory();
                        if (rscCat == colorMapCategory) {
                            ImagingCapability imgcap = rsc
                                    .getCapability(ImagingCapability.class);
                            imgcap.setInterpolationState(
                                    interpolationChk.getSelection());
                        }
                    }

                    if (currEditor != null) {
                        currEditor.refresh();
                    }
                }
            }

        });
    }

    /***
     * Creates the slider for changing Alpha
     * 
     * @param parent
     */

    private void initializeAlphaScale(Composite parent) {
        alphaLabel = new Label(parent, SWT.BOLD);
        alphaLabel.setText("Alpha: ");
        alphaLabel.setLayoutData(new GridData());

        alphaScale = new Scale(parent, SWT.NONE);
        alphaScale.setLayoutData(new GridData(200, SWT.DEFAULT));

        alphaText = new Label(parent, SWT.NONE);
        alphaText.setLayoutData(new GridData(50, SWT.DEFAULT));

        alphaScale.setMinimum(0);
        alphaScale.setMaximum(100);
        alphaScale.setIncrement(1);
        alphaScale.setPageIncrement(5);

        RscAttrValue alphaRscAttrValue = theResourceData.getRscAttrSet()
                .getRscAttr("alpha");
        int alphaVal = ((Float) alphaRscAttrValue.getAttrValue()).intValue();
        alphaScale.setSelection(alphaVal * 100);
        alphaText.setText(alphaScale.getSelection() + "%");
        alphaScale.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                alphaText.setText(alphaScale.getSelection() + "%");

                if ((imgCapability != null) && (theResourceData != null)) {
                    float alphaValue = alphaScale.getSelection() / 100.0f;
                    ResourceAttrSet resAttrSet = theResourceData
                            .getRscAttrSet();
                    resAttrSet.getRscAttr("alpha").setAttrValue(alphaValue);
                    theResourceData.setRscAttrSet(resAttrSet);

                    imgCapability
                            .setBrightness(
                                    ((Number) resAttrSet
                                            .getRscAttr("brightness")
                                            .getAttrValue()).floatValue(),
                                    false);
                    imgCapability
                            .setContrast(
                                    ((Number) resAttrSet.getRscAttr("contrast")
                                            .getAttrValue()).floatValue(),
                                    false);
                    imgCapability.setAlpha(alphaValue);
                }
            }
        });
    }

    private void setColorBarFromColorMap(String cmapName) {
        if ((colorMapCategory != null)
                && (colorMapCategory != ResourceCategory.NullCategory)
                && (StringUtils.isNotEmpty(cmapName))) {
            try {
                IColorMap selectedCmap = ColorMapUtil.loadColorMap(
                        colorMapCategory.getCategoryName(), cmapName);

                ((ColorBarFromColormap) editedColorBar)
                        .setColorMap((ColorMap) selectedCmap);
                theResourceData.getRscAttrSet().getRscAttr("colorMapName")
                        .setAttrValue(cmapName);

                ColorMapParameters cmapParams = cMapCapability
                        .getColorMapParameters();
                cmapParams.setColorMap(selectedCmap);
                setColorMapMax(255f);
                setColorMapMin(0f);
                cMapCapability.notifyResources();
            } catch (VizException e) {
                statusHandler.error("Could not load color map: " + cmapName, e);
                return;
            }
        }
    }

    private void refreshColorBar() {
        if (theResourceData != null && editedColorBar != null) {
            ResourceAttrSet resAttrSet = theResourceData.getRscAttrSet();
            resAttrSet.setAttrValue("colorBar", editedColorBar);
            theResourceData.setRscAttrSet(resAttrSet);
        }
    }

    private void buildColorMapData() {
        arrayOfSliderTextStrings = new String[256];

        if (cMapCapability != null) {
            ColorMapParameters cMapParam = cMapCapability
                    .getColorMapParameters();
            cmapMin = 0f;
            cmapMax = 255f;
            cmapWidth = cmapMax - cmapMin;
            cmapIncrement = cmapWidth / ColorUtil.MAX_VALUE;
            initializeAlphaMaskInColorMapParam();

            currentMax = 255f;
            currentMin = 0f;

            float start = cmapMin;
            String units = StringUtils.EMPTY;

            double lastVal = Double.NaN;

            for (int i = 0; i < arrayOfSliderTextStrings.length; ++i) {
                double value = start;

                // handle precision errors
                if (value > cmapMax) {
                    /*
                     * if the difference is .1 the increment between steps
                     * assume that cmapMax is ok
                     */
                    if ((value - cmapMax) < (.1 * cmapIncrement)) {
                        value = cmapMax;
                    }
                }

                String textStr = StringUtils.EMPTY;

                if (cMapParam.isLogarithmic()) {
                    // TODO: Handle case where min/max go from neg to pos
                    if (cMapParam.getColorMapMax() >= 0
                            && cMapParam.getColorMapMin() >= 0) {
                        double index = (i) / ColorUtil.MAX_VALUE;
                        value = Math.pow(Math.E,
                                (Math.log(cMapParam
                                        .getColorMapMin())
                                + (index * (Math.log(cMapParam.getColorMapMax())
                                        - Math.log(
                                                cMapParam.getColorMapMin())))));
                    }
                    if (format == null) {
                        format = new DecimalFormat("0.000");
                    }
                }

                if (!Double.isNaN(value)) {
                    int zeros = 0;
                    String val = Double.toString(value);
                    char[] vals = val.substring(val.indexOf(".") + 1)
                            .toCharArray();
                    for (int j = 0; j < vals.length; ++j) {
                        if (vals[j] == '0') {
                            ++zeros;
                        } else {
                            ++zeros;
                            break;
                        }
                    }
                    zeros = Math.min(3, zeros);

                    String f = "0.";
                    for (int j = 0; j < zeros; ++j) {
                        f += "0";
                    }

                    if (format == null) {
                        format = new DecimalFormat(f);
                    }
                }

                String txt;
                if (textStr.length() == 0) {
                    txt = format.format(value);
                } else {
                    txt = textStr;
                }

                if (units != null && units.length() != 0) {
                    txt += " " + units;
                }

                arrayOfSliderTextStrings[i] = txt;
                start += cmapIncrement;

            }

        }

    }

    private void initializeAlphaMaskInColorMapParam() {
        if (cMapCapability != null) {
            ColorMapParameters cMapParam = cMapCapability
                    .getColorMapParameters();
            int rangeEnd = (int) cMapParam.getColorMapMax();
            byte[] mask = cMapParam.getAlphaMask();
            if ((mask == null || mask.length == 0) && rangeEnd > 0) {
                mask = new byte[rangeEnd];
                for (int i = 0; i < rangeEnd; ++i) {
                    mask[i] = 1;
                }
            } else {
                for (int i = 0; i < mask.length; ++i) {
                    mask[i] = 1;
                }
            }
            cMapParam.setAlphaMask(mask);
        }
    }

    private float parseTextField(String userEntry) {
        userEntry = userEntry.trim().split(" ")[0];
        try {
            return Float.valueOf(userEntry);
        } catch (NumberFormatException ex) {
            statusHandler.error(
                    "Cannot parse new colormap range value: " + userEntry, ex);
        } catch (ConversionException ex) {
            statusHandler.error("Unit converter error.", ex);
        }
        return Float.NaN;
    }

    private float selectionToCmap(int selection) {
        float percentOffset = selection / 255.0f;
        float value = percentOffset * cmapWidth + cmapMin;
        return value;
    }

    private int cmapToSelection(float value) {
        int selection = (int) ((value - cmapMin) * 255.0f / cmapWidth);
        return selection;
    }

    private void changeMax(int position) {
        /*
         * slider min and max is based on the color map, so position is the new
         * color map max
         */
        float newMax = selectionToCmap(position);
        setColorMapMax(newMax);
    }

    private void changeMin(int position) {
        /*
         * slider min and max is based on the color map, so position is the new
         * color map min
         */
        float newMin = selectionToCmap(position);
        setColorMapMin(newMin);
    }

    private void setColorMapMax(float f) {
        if ((currentMax != f) && (cMapCapability != null)) {
            currentMax = f;
            cMapCapability.getColorMapParameters().setColorMapMax(f, true);
            cMapCapability.notifyResources();
        }
        maxSlider.setSelection(cmapToSelection(f));
        maxValueText.setText(cmapToText(f));
    }

    private void setColorMapMin(float f) {
        if ((currentMin != f) && (cMapCapability != null)) {
            currentMin = f;
            cMapCapability.getColorMapParameters().setColorMapMin(f, true);
            cMapCapability.notifyResources();
        }
        minSlider.setSelection(cmapToSelection(f));
        minValueText.setText(cmapToText(f));
    }

    private String cmapToText(float value) {
        String txt = StringUtils.EMPTY;

        if (Float.isNaN(value)) {
            txt = "NO DATA";
        } else {
            txt = format.format(value);
        }
        return txt;
    }

    private void maxTextChanged() {
        float newMaxValue = parseTextField(maxValueText.getText());

        if (Float.isNaN(newMaxValue)) {
            newMaxValue = currentMax;
        } else if (currentMin >= newMaxValue) {
            newMaxValue = currentMax;
            statusHandler.error(
                    "Maximum of colormap range cannot be below the minimum.");
        } else if (newMaxValue >= cmapMax) {
            newMaxValue = cmapMax;
        }

        setColorMapMax(newMaxValue);
    }

    private void minTextChanged() {
        float newMinValue = parseTextField(minValueText.getText());

        if (Float.isNaN(newMinValue)) {
            newMinValue = currentMin;
        } else if (newMinValue >= currentMax) {
            newMinValue = currentMin;
            statusHandler.error(
                    "Minimum of colormap range cannot exceed the maximum.");
        } else if (cmapMin >= newMinValue) {
            newMinValue = cmapMin;
        }

        setColorMapMin(newMinValue);
    }

    @Override
    public void dispose() {
        if (labelColor != null) {
            labelColor.dispose();
        }
        editedColorBar.dispose();
        super.dispose();
    }

}
