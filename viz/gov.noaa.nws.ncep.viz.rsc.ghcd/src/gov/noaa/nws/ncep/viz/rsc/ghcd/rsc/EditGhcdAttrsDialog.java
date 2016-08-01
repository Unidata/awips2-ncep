/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.rsc.ghcd.rsc;

import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScale;
import gov.noaa.nws.ncep.viz.common.tsScaleMngr.XAxisScaleMngr;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsInteractiveDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;
import gov.noaa.nws.ncep.viz.rsc.ghcd.util.GhcdUtil;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * An interface to edit Ghcd resource attributes.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Oct 16, 2014   R5097     sgurung     Initial creation
 * 04/05/2016     R15715    dgilling    Refactored for new AbstractEditResourceAttrsDialog constructor.
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class EditGhcdAttrsDialog extends
        AbstractEditResourceAttrsInteractiveDialog {

    private RscAttrValue yScaletypeAttr = null;

    private RscAttrValue yTitlePosAttr = null;

    private RscAttrValue yUnitsPosAttr = null;

    private RscAttrValue yDataAttr = null;

    private RscAttrValue graphKeyAttr = null;

    private RscAttrValue yScaleMinAttr = null;

    private RscAttrValue yScaleMaxAttr = null;

    private RscAttrValue yIntervalAttr = null;

    private RscAttrValue yNumTicksAttr = null;

    private RscAttrValue plotColorAttr = null;

    private RscAttrValue lineStyleAttr = null;

    private RscAttrValue lineWidthAttr = null;

    private RscAttrValue yTitleAttr = null;

    private RscAttrValue xTitleAttr = null;

    private RscAttrValue yUnitsAttr = null;

    private RscAttrValue graphTopTitleAttr = null;

    private RscAttrValue displayLastDataDateAttr = null;

    private RscAttrValue displayxLabelsAttr = null;

    private RscAttrValue xAxisScaleAttr = null;

    private RscAttrValue unitsFontAttr = null;

    private RscAttrValue unitsFontSizeAttr = null;

    private RscAttrValue unitsStyleAttr = null;

    private RscAttrValue titleFontAttr = null;

    private RscAttrValue titleFontSizeAttr = null;

    private RscAttrValue titleStyleAttr = null;

    private Combo yTitlePosCombo = null;

    private Combo yUnitsPosCombo = null;

    private Combo lineStyleCombo = null;

    private Combo lineWidthCombo = null;

    private Text graphKeyText;

    private Text yDataText;

    private Text yTitleText;

    private Text yUnitsText;

    private Button graphAttrsBtn = null;

    private ColorButtonSelector plotColorCms = null;

    protected ResourceAttrSet prevEditedRscAttrSet = null;

    protected GraphAttributes graphAttrs = null;

    protected final String[] positionOptions = { GhcdUtil.TITLE_POSITION_LEFT,
            GhcdUtil.TITLE_POSITION_RIGHT };

    protected final String[] lineWidthOptions = { "1", "2", "3", "4" };

    protected final LineStyle[] lineStyleOptions = new LineStyle[] {
            LineStyle.DEFAULT, LineStyle.SOLID, LineStyle.DASHED,
            LineStyle.DASHED_LARGE, LineStyle.DOTTED, LineStyle.DASH_DOTTED };

    public EditGhcdAttrsDialog(Shell parentShell, INatlCntrsResourceData r,
            Capabilities capabilities, Boolean apply) {
        super(parentShell, r, capabilities, apply);
    }

    @Override
    public Composite createDialog(Composite topComp) {

        prevEditedRscAttrSet = new ResourceAttrSet(editedRscAttrSet);

        yScaletypeAttr = editedRscAttrSet.getRscAttr("yScaleType");
        yTitlePosAttr = editedRscAttrSet.getRscAttr("yTitlePosition");
        yUnitsPosAttr = editedRscAttrSet.getRscAttr("yUnitsPosition");
        yDataAttr = editedRscAttrSet.getRscAttr("yData");
        graphKeyAttr = editedRscAttrSet.getRscAttr("graphKey");
        yScaleMinAttr = editedRscAttrSet.getRscAttr("yScaleMin");
        yScaleMaxAttr = editedRscAttrSet.getRscAttr("yScaleMax");
        yIntervalAttr = editedRscAttrSet.getRscAttr("yInterval");
        yNumTicksAttr = editedRscAttrSet.getRscAttr("yNumTicks");
        plotColorAttr = editedRscAttrSet.getRscAttr("dataColor");
        lineStyleAttr = editedRscAttrSet.getRscAttr("lineStyle");
        lineWidthAttr = editedRscAttrSet.getRscAttr("lineWidth");
        yTitleAttr = editedRscAttrSet.getRscAttr("yTitle");
        xTitleAttr = editedRscAttrSet.getRscAttr("xTitle");
        yUnitsAttr = editedRscAttrSet.getRscAttr("yUnits");
        graphTopTitleAttr = editedRscAttrSet.getRscAttr("graphTopTitle");
        displayLastDataDateAttr = editedRscAttrSet
                .getRscAttr("displayLastDataDate");
        displayxLabelsAttr = editedRscAttrSet.getRscAttr("displayXLabels");
        xAxisScaleAttr = editedRscAttrSet.getRscAttr("xAxisScale");
        unitsFontAttr = editedRscAttrSet.getRscAttr("unitsFont");
        unitsFontSizeAttr = editedRscAttrSet.getRscAttr("unitsFontSize");
        unitsStyleAttr = editedRscAttrSet.getRscAttr("unitsStyle");
        titleFontAttr = editedRscAttrSet.getRscAttr("titleFont");
        titleFontSizeAttr = editedRscAttrSet.getRscAttr("titleFontSize");
        titleStyleAttr = editedRscAttrSet.getRscAttr("titleStyle");

        if (yScaletypeAttr == null
                || ((String) yScaletypeAttr.getAttrValue()).trim().length() <= 0) {
            yScaletypeAttr.setAttrValue("");
        }

        if (yTitlePosAttr == null
                || ((String) yTitlePosAttr.getAttrValue()).trim().length() <= 0) {
            yTitlePosAttr.setAttrValue("");
        }

        if (yUnitsPosAttr == null
                || ((String) yUnitsPosAttr.getAttrValue()).trim().length() <= 0) {
            yUnitsPosAttr.setAttrValue("");
        }

        if (yDataAttr == null
                || ((String) yDataAttr.getAttrValue()).trim().length() <= 0) {
            yDataAttr.setAttrValue("");
        }

        if (graphKeyAttr == null
                || ((String) graphKeyAttr.getAttrValue()).trim().length() <= 0) {
            graphKeyAttr.setAttrValue("");
        }

        if (yScaleMinAttr == null) {
            yScaleMinAttr.setAttrValue(-1.0);
        }

        if (yScaleMaxAttr == null) {
            yScaleMaxAttr.setAttrValue(-1.0);
        }

        if (yIntervalAttr == null) {
            yIntervalAttr.setAttrValue(-1.0);
        }

        if (yNumTicksAttr == null) {
            yNumTicksAttr.setAttrValue(-1);
        }

        if (plotColorAttr == null) {
            plotColorAttr.setAttrValue(new RGB(255, 255, 255));
        }

        if (yTitleAttr == null
                || ((String) yTitleAttr.getAttrValue()).trim().length() <= 0) {
            yTitleAttr.setAttrValue("");
        }

        if (xTitleAttr == null
                || ((String) xTitleAttr.getAttrValue()).trim().length() <= 0) {
            xTitleAttr.setAttrValue("");
        }

        if (displayxLabelsAttr == null) {
            displayxLabelsAttr.setAttrValue(true);
        }

        if (xAxisScaleAttr == null
                || xAxisScaleAttr.getAttrClass() != XAxisScale.class) {
            System.out
                    .println("xAxisScaleAttr is null or not of expected class GraphAttributes?");
            return topComp;
        }

        if (lineStyleAttr == null
                || lineStyleAttr.getAttrClass() != LineStyle.class) {
            System.out.println("lineStyle is null or not of expected class? "
                    + lineStyleAttr.getAttrClass().toString());
        }

        else if (lineWidthAttr == null
                || lineWidthAttr.getAttrClass() != Integer.class) {
            System.out.println("lineWidth is null or not of expected class? "
                    + lineWidthAttr.getAttrClass().toString());
        }

        if (unitsFontAttr == null
                || ((String) unitsFontAttr.getAttrValue()).trim().length() <= 0) {
            unitsFontAttr.setAttrValue("");
        }

        if (unitsFontSizeAttr == null
                || ((String) unitsFontSizeAttr.getAttrValue()).trim().length() <= 0) {
            unitsFontSizeAttr.setAttrValue("");
        }

        if (unitsStyleAttr == null
                || ((String) unitsStyleAttr.getAttrValue()).trim().length() <= 0) {
            unitsStyleAttr.setAttrValue("");
        }

        if (titleFontAttr == null
                || ((String) titleFontAttr.getAttrValue()).trim().length() <= 0) {
            titleFontAttr.setAttrValue("");
        }

        if (titleFontSizeAttr == null
                || ((String) titleFontSizeAttr.getAttrValue()).trim().length() <= 0) {
            titleFontSizeAttr.setAttrValue("");
        }

        if (titleStyleAttr == null
                || ((String) titleStyleAttr.getAttrValue()).trim().length() <= 0) {
            titleStyleAttr.setAttrValue("");
        }

        copyRscAttrValuesToGraphAttrs();

        // ghcd attributes editing
        Group ghcdAttributesGroup = new Group(topComp, SWT.SHADOW_NONE);
        GridLayout ghcdAttrGridLayout = new GridLayout();
        ghcdAttrGridLayout.numColumns = 1;
        ghcdAttrGridLayout.marginHeight = 8;
        ghcdAttrGridLayout.marginWidth = 2;
        ghcdAttrGridLayout.horizontalSpacing = 20;
        ghcdAttrGridLayout.verticalSpacing = 8;
        ghcdAttributesGroup.setLayout(ghcdAttrGridLayout);

        Composite comp = new Composite(ghcdAttributesGroup, SWT.SHADOW_NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 4;
        comp.setLayout(gridLayout);

        // y data
        Label yDataLabel = new Label(comp, SWT.NONE);
        yDataLabel.setText("Y-axis Data:");

        yDataText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yDataText.setLayoutData(new GridData(230, SWT.DEFAULT));
        yDataText.setText((String) yDataAttr.getAttrValue());
        yDataText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                yDataAttr.setAttrValue(yDataText.getText().trim());
            }

        });

        // plotColor
        Label plotColorLabel = new Label(comp, SWT.NONE);
        plotColorLabel.setText("Plot Color:");
        plotColorCms = new ColorButtonSelector(comp, 85, 25);
        plotColorCms.setColorValue((RGB) plotColorAttr.getAttrValue());
        plotColorCms.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                plotColorAttr.setAttrValue(plotColorCms.getColorValue());
            }
        });

        // yTitle
        Label yTitleLabel = new Label(comp, SWT.NONE);
        yTitleLabel.setText("Y-axis Title:");
        yTitleText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yTitleText.setLayoutData(new GridData(230, SWT.DEFAULT));
        yTitleText.setText((String) yTitleAttr.getAttrValue());
        yTitleText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                yTitleAttr.setAttrValue(yTitleText.getText().trim());
            }
        });

        // yUnits
        Label yUnitsLabel = new Label(comp, SWT.NONE);
        yUnitsLabel.setText("Y-axis Units:");
        yUnitsText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yUnitsText.setLayoutData(new GridData(230, SWT.DEFAULT));
        yUnitsText.setText((String) yUnitsAttr.getAttrValue());
        yUnitsText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                yUnitsAttr.setAttrValue(yUnitsText.getText().trim());
            }
        });

        // yTitlePosition
        Label yTitlePosLabel = new Label(comp, SWT.NONE);
        yTitlePosLabel.setText("Y-axis Title Position:");
        yTitlePosCombo = new Combo(comp, SWT.DROP_DOWN);
        yTitlePosCombo.setLayoutData(new GridData(75, SWT.DEFAULT));
        yTitlePosCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                yTitlePosAttr.setAttrValue(yTitlePosCombo.getText());
            }
        });

        // yUnitsPosition
        Label yUnitsPosLabel = new Label(comp, SWT.NONE);
        yUnitsPosLabel.setText("Y-axis Units Position:");
        yUnitsPosCombo = new Combo(comp, SWT.DROP_DOWN);
        yUnitsPosCombo.setLayoutData(new GridData(75, SWT.DEFAULT));
        yUnitsPosCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                yUnitsPosAttr.setAttrValue(yUnitsPosCombo.getText());
            }
        });

        // lineStyle
        Label lineStyleLabel = new Label(comp, SWT.NONE);
        lineStyleLabel.setText("Line Style:");
        lineStyleCombo = new Combo(comp, SWT.DROP_DOWN);
        lineStyleCombo.setLayoutData(new GridData(150, SWT.DEFAULT));
        lineStyleCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if ("DEFAULT".equals(lineStyleCombo.getText())) {
                    lineStyleAttr.setAttrValue(LineStyle.DEFAULT);
                } else if ("SOLID".equals(lineStyleCombo.getText())) {
                    lineStyleAttr.setAttrValue(LineStyle.SOLID);
                } else if ("DASHED".equals(lineStyleCombo.getText())) {
                    lineStyleAttr.setAttrValue(LineStyle.DASHED);
                } else if ("DASHED_LARGE".equals(lineStyleCombo.getText())) {
                    lineStyleAttr.setAttrValue(LineStyle.DASHED_LARGE);
                } else if ("DOTTED".equals(lineStyleCombo.getText())) {
                    lineStyleAttr.setAttrValue(LineStyle.DOTTED);
                } else if ("DASH_DOTTED".equals(lineStyleCombo.getText())) {
                    lineStyleAttr.setAttrValue(LineStyle.DASH_DOTTED);
                }
            }
        });

        // lineWidth
        Label lineWidthLabel = new Label(comp, SWT.NONE);
        lineWidthLabel.setText("Line Width:");
        lineWidthCombo = new Combo(comp, SWT.DROP_DOWN);
        lineWidthCombo.setLayoutData(new GridData(75, SWT.DEFAULT));
        lineWidthCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                lineWidthAttr.setAttrValue(Integer.parseInt(lineWidthCombo
                        .getText()));
            }
        });

        // graph key
        Label graphKeyLabel = new Label(comp, SWT.NONE);
        graphKeyLabel.setText("Graph Key:");

        graphKeyText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        graphKeyText.setLayoutData(new GridData(200, SWT.DEFAULT));
        graphKeyText.setText((String) graphKeyAttr.getAttrValue());
        graphKeyText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                graphKeyAttr.setAttrValue(graphKeyText.getText().trim());
            }

        });

        // graph attributes
        // Label graphAttrLabel = new Label(comp, SWT.NONE);
        // graphAttrLabel.setText("");
        graphAttrsBtn = new Button(comp, SWT.PUSH);
        graphAttrsBtn.setLayoutData(new GridData(150, SWT.DEFAULT));
        graphAttrsBtn.setText("Edit Graph...");
        if (!hasApplyBtn) {
            graphAttrsBtn.setEnabled(false);
            graphAttrsBtn
                    .setToolTipText("Graph attributes can be modified by using the 'Edit Attributes' option from the right-click legend menu.");
        }
        graphAttrsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EditGhcdGraphAttrsDialog pd = new EditGhcdGraphAttrsDialog(
                        shell, "Edit Graph Attributes for  '"
                                + (String) graphKeyAttr.getAttrValue() + "'",
                        graphAttrs);

                GraphAttributes result = pd.open(shell.getLocation().x + 150,
                        shell.getLocation().y - 100);

                if (result != null) {
                    graphTopTitleAttr.setAttrValue(result.getGraphTopTitle()
                            .trim());
                    xAxisScaleAttr.setAttrValue(result.getxAxisScale());
                    displayxLabelsAttr.setAttrValue(result.getDisplayXLabels());
                    xTitleAttr.setAttrValue(result.getxTitle());
                    yScaletypeAttr.setAttrValue(result.getyScaleType());
                    yScaleMinAttr.setAttrValue(result.getyScaleMin());
                    yScaleMaxAttr.setAttrValue(result.getyScaleMax());
                    yIntervalAttr.setAttrValue(result.getyInterval());
                    yNumTicksAttr.setAttrValue(result.getyNumTicks());
                    displayLastDataDateAttr.setAttrValue(result
                            .getDisplayLastDataDate());
                    titleFontSizeAttr.setAttrValue(result.getTitleFontSize());
                    titleFontAttr.setAttrValue(result.getTitleFont());
                    titleStyleAttr.setAttrValue(result.getTitleStyle());
                    unitsFontSizeAttr.setAttrValue(result.getUnitsFontSize());
                    unitsFontAttr.setAttrValue(result.getUnitsFont());
                    unitsStyleAttr.setAttrValue(result.getUnitsStyle());
                }

            }
        });

        return topComp;
    }

    @Override
    public void initWidgets() {
        HashMap<String, XAxisScale> xScalesMap = XAxisScaleMngr.getInstance()
                .getXAxisScales();

        yTitlePosCombo.setItems(positionOptions);
        for (int i = 0; i < positionOptions.length; i++) {
            String position = (String) yTitlePosAttr.getAttrValue();
            if (positionOptions[i].equalsIgnoreCase(position)) {
                yTitlePosCombo.select(i);
            }
        }

        yUnitsPosCombo.setItems(positionOptions);
        for (int i = 0; i < positionOptions.length; i++) {
            String position = (String) yUnitsPosAttr.getAttrValue();
            if (positionOptions[i].equalsIgnoreCase(position)) {
                yUnitsPosCombo.select(i);
            }
        }

        String[] lineStyleOptionsStr = new String[lineStyleOptions.length];
        for (int i = 0; i < lineStyleOptions.length; i++) {
            lineStyleOptionsStr[i] = lineStyleOptions[i].toString();
        }

        lineStyleCombo.setItems(lineStyleOptionsStr);
        for (int i = 0; i < lineStyleOptionsStr.length; i++) {
            LineStyle ls = (LineStyle) lineStyleAttr.getAttrValue();
            String lineStyle = ls.toString();
            if (lineStyleOptionsStr[i].equalsIgnoreCase(lineStyle)) {
                lineStyleCombo.select(i);
            }
        }

        lineWidthCombo.setItems(lineWidthOptions);
        for (int i = 0; i < lineWidthOptions.length; i++) {
            String position = "" + lineWidthAttr.getAttrValue();
            if (lineWidthOptions[i].equalsIgnoreCase(position)) {
                lineWidthCombo.select(i);
            }
        }

    }

    public void copyRscAttrValuesToGraphAttrs() {
        graphAttrs = new GraphAttributes();
        graphAttrs.setGraphKey((String) graphKeyAttr.getAttrValue());
        graphAttrs.setyScaleType((String) yScaletypeAttr.getAttrValue());
        graphAttrs.setyScaleMin((Float) yScaleMinAttr.getAttrValue());
        graphAttrs.setyScaleMax((Float) yScaleMaxAttr.getAttrValue());
        graphAttrs.setyInterval((Float) yIntervalAttr.getAttrValue());
        graphAttrs.setyNumTicks((Integer) yNumTicksAttr.getAttrValue());
        graphAttrs.setGraphTopTitle((String) graphTopTitleAttr.getAttrValue());
        graphAttrs.setDisplayLastDataDate((Boolean) displayLastDataDateAttr
                .getAttrValue());
        graphAttrs.setDisplayXLabels((Boolean) displayxLabelsAttr
                .getAttrValue());
        graphAttrs.setxTitle((String) xTitleAttr.getAttrValue());
        graphAttrs.setTitleFont((String) titleFontAttr.getAttrValue());
        graphAttrs.setTitleFontSize((String) titleFontSizeAttr.getAttrValue());
        graphAttrs.setTitleStyle((String) titleStyleAttr.getAttrValue());
        graphAttrs.setUnitsFont((String) unitsFontAttr.getAttrValue());
        graphAttrs.setUnitsFontSize((String) unitsFontSizeAttr.getAttrValue());
        graphAttrs.setUnitsStyle((String) unitsStyleAttr.getAttrValue());
        graphAttrs.setxAxisScale((XAxisScale) xAxisScaleAttr.getAttrValue());
    }

    @Override
    public Object open() {
        Display display = getParent().getDisplay();

        // copy the attrSet
        editedRscAttrSet = new ResourceAttrSet(rscData.getRscAttrSet());

        createShell();

        initWidgets();

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        // Uses Java Bean utils to set the attributes on the resource
        if (ok) {
            rscData.setRscAttrSet(editedRscAttrSet);
            rscData.setIsEdited(true);

            GraphAttributes graphAttrs = new GraphAttributes(
                    (GhcdResourceData) rscData);

            // set graphAttributes to resources with same graphKey
            AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
            IDisplayPane activePane = editor.getActiveDisplayPane();

            List<GhcdResource> ghcdRscs = activePane.getDescriptor()
                    .getResourceList()
                    .getResourcesByTypeAsType(GhcdResource.class);

            for (GhcdResource rsc : ghcdRscs) {
                GhcdResourceData rscData = rsc.getResourceData();
                String graphKey = rscData.getGraphKey();

                if (graphAttrs.getGraphKey().equals(graphKey)) {
                    rscData.setGraphAttr(graphAttrs);
                }

            }
            ok = false;
        } else {
            rscData.setRscAttrSet(prevEditedRscAttrSet);
            rscData.setIsEdited(false);
        }

        dispose();

        return null;
    }

    @Override
    protected void dispose() {
        super.dispose();
    }
}
