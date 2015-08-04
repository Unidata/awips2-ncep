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
import gov.noaa.nws.ncep.viz.rsc.ghcd.util.GhcdUtil;

import java.util.HashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * An interface to edit Ghcd graph resource attributes.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Oct 16, 2014   R5097     sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class EditGhcdGraphAttrsDialog extends Dialog {

    protected Shell shell;

    protected String dlgTitle = "Edit Graph Attributes";

    protected static EditGhcdGraphAttrsDialog INSTANCE = null;

    protected GraphAttributes graphAttrs = null;

    protected final String[] scaleTypeOptions = { GhcdUtil.YSCALE_TYPE_LOG,
            GhcdUtil.YSCALE_TYPE_LINEAR };

    protected final String[] yesNoOptions = { "Yes", "No" };

    protected final String[] textSizeOptions = { "10", "12", "14", "16", "18",
            "20", "22", "24", "32" };

    protected final String[] textFontOptions = { "Courier", "Helvetica",
            "Times" };

    protected final String[] textFontStyleOptions = { "Normal", "Bold" };

    protected Combo yScaleTypeCombo = null;

    protected Combo displayLastDataDateCombo = null;

    protected Combo displayxLabelsCombo = null;

    protected Combo xScaleCombo = null;

    protected Combo unitsFontSizeCombo = null;

    protected Combo unitsFontCombo = null;

    protected Combo unitsStyleCombo = null;

    protected Combo titleFontSizeCombo = null;

    protected Combo titleFontCombo = null;

    protected Combo titleStyleCombo = null;

    // protected Text graphKeyText;

    protected Text yScaleMinText;

    protected Text yScaleMaxText;

    protected Text yIntervalText;

    protected Text yNumTicksText;

    protected Text xTitleText;

    protected Text graphTopTitleText;

    protected boolean ok = false;

    public EditGhcdGraphAttrsDialog(Shell parentShell, String dialogTitle,
            GraphAttributes graphAttrs) {
        super(parentShell);
        this.graphAttrs = graphAttrs;
    }

    /**
     * Creates the dialog if the dialog does not exist and returns the instance.
     * If the dialog exists, return the instance.
     * 
     * @param parShell
     * @return
     */
    public static EditGhcdGraphAttrsDialog getInstance(Shell parShell,
            String dialogTitle, GraphAttributes graphAttrs) {

        if (INSTANCE == null) {
            INSTANCE = new EditGhcdGraphAttrsDialog(parShell, dialogTitle,
                    graphAttrs);
        }
        return INSTANCE;

    }

    public Composite createDialog(Composite composite) {
        Composite topComp = composite;

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

        // graphTopTitle
        Label graphTopTitleLabel = new Label(comp, SWT.NONE);
        graphTopTitleLabel.setText("Top Title:");
        graphTopTitleText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        graphTopTitleText.setLayoutData(new GridData(230, SWT.DEFAULT));
        graphTopTitleText.setText(graphAttrs.getGraphTopTitle());
        graphTopTitleText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                graphAttrs.setGraphTopTitle((String) graphTopTitleText
                        .getText().trim());
            }
        });

        // xAxisScale
        Label xScaleLabel = new Label(comp, SWT.NONE);
        xScaleLabel.setText("X-axis Scale:");
        xScaleCombo = new Combo(comp, SWT.DROP_DOWN);
        xScaleCombo.setLayoutData(new GridData(230, SWT.DEFAULT));
        xScaleCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {

                XAxisScale sc = XAxisScaleMngr.getInstance().getXAxisScale(
                        xScaleCombo.getText().trim());
                if (sc == null) {
                    sc = XAxisScaleMngr.getInstance().getDefaultxAxisScale();
                } else {
                    sc = sc.clone();
                }
                graphAttrs.setxAxisScale(sc);
            }
        });

        // displayxLabel
        Label displayxLabelsLabel = new Label(comp, SWT.NONE);
        displayxLabelsLabel.setText("Display X-axis Labels:");
        displayxLabelsCombo = new Combo(comp, SWT.DROP_DOWN);
        displayxLabelsCombo.setLayoutData(new GridData(75, SWT.DEFAULT));
        displayxLabelsCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if ("Yes".equalsIgnoreCase(displayxLabelsCombo.getText())) {
                    graphAttrs.setDisplayXLabels((Boolean) true);
                } else {
                    graphAttrs.setDisplayXLabels((Boolean) false);
                }
            }
        });

        // xTitle
        Label xTitleLabel = new Label(comp, SWT.NONE);
        xTitleLabel.setText("X-axis Title:");
        xTitleText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        xTitleText.setLayoutData(new GridData(230, SWT.DEFAULT));
        xTitleText.setText(graphAttrs.getxTitle());
        xTitleText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                graphAttrs.setxTitle((String) xTitleText.getText().trim());
            }
        });

        // yScaleType
        Label yScaleTypeLabel = new Label(comp, SWT.NONE);
        yScaleTypeLabel.setText("Y-axis Scale Type:");
        yScaleTypeCombo = new Combo(comp, SWT.DROP_DOWN);
        yScaleTypeCombo.setLayoutData(new GridData(90, SWT.DEFAULT));
        yScaleTypeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setyScaleType(yScaleTypeCombo.getText());

                if (GhcdUtil.YSCALE_TYPE_LOG.equalsIgnoreCase(yScaleTypeCombo
                        .getText())) {
                    // For log scale type, disable input for yInterval and
                    // yNumticks
                    yScaleMinText.setText("0.1");
                    yIntervalText.setText("10.0");
                    yIntervalText.setEditable(false);
                    yNumTicksText.setText("-1");
                    yNumTicksText.setEditable(false);
                    // validateYScaleMinMax();
                }

            }
        });

        // yScaleMin
        Label yScaleMinLabel = new Label(comp, SWT.NONE);
        yScaleMinLabel.setText("Y-axis Scale Min:");
        yScaleMinText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yScaleMinText.setLayoutData(new GridData(100, SWT.DEFAULT));
        yScaleMinText.setText(String.valueOf(graphAttrs.getyScaleMin()));
        yScaleMinText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!yScaleMinText.getText().isEmpty()) {
                    try {
                        float fval = Float.parseFloat(yScaleMinText.getText());
                        graphAttrs.setyScaleMin(Float.valueOf(fval));
                        validateYScaleMinMax();
                    } catch (NumberFormatException exc) {
                        float fval = graphAttrs.getyScaleMin().floatValue();
                        yScaleMinText.setText(Float.toString(fval));
                    }
                }
            }

        });

        // yScaleMax
        Label yScaleMaxLabel = new Label(comp, SWT.NONE);
        yScaleMaxLabel.setText("Y-axis Scale Max:");
        yScaleMaxText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yScaleMaxText.setLayoutData(new GridData(100, SWT.DEFAULT));
        yScaleMaxText.setText(String.valueOf(graphAttrs.getyScaleMax()));
        yScaleMaxText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!yScaleMaxText.getText().isEmpty()) {
                    try {
                        float fval = Float.parseFloat(yScaleMaxText.getText());
                        graphAttrs.setyScaleMax(Float.valueOf(fval));
                        validateYScaleMinMax();
                    } catch (NumberFormatException exc) {
                        float fval = graphAttrs.getyScaleMax().floatValue();
                        yScaleMaxText.setText(Float.toString(fval));
                    }
                }
            }

        });

        // yInterval
        Label yIntervalLabel = new Label(comp, SWT.NONE);
        yIntervalLabel.setText("Y-axis Interval:");
        yIntervalText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yIntervalText.setLayoutData(new GridData(100, SWT.DEFAULT));
        yIntervalText.setText(String.valueOf(graphAttrs.getyInterval()));
        yIntervalText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!yIntervalText.getText().isEmpty()) {
                    try {
                        float fval = Float.parseFloat(yIntervalText.getText());
                        graphAttrs.setyInterval(Float.valueOf(fval));
                    } catch (NumberFormatException exc) {
                        float fval = graphAttrs.getyInterval().floatValue();
                        yIntervalText.setText(Float.toString(fval));
                    }
                }
            }

        });

        // yNumTicks
        Label yNumTicksLabel = new Label(comp, SWT.NONE);
        yNumTicksLabel.setText("Y-axis Number of Ticks:");
        yNumTicksText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        yNumTicksText.setLayoutData(new GridData(100, SWT.DEFAULT));
        yNumTicksText.setText(String.valueOf(graphAttrs.getyNumTicks()));
        yNumTicksText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!yNumTicksText.getText().isEmpty()) {
                    try {
                        int ival = Integer.parseInt(yNumTicksText.getText());
                        graphAttrs.setyNumTicks(Integer.valueOf(ival));
                    } catch (NumberFormatException exc) {
                        int ival = graphAttrs.getyNumTicks().intValue();
                        yNumTicksText.setText(Integer.toString(ival));
                    }
                }
            }
        });

        // displayLastDataDate
        Label displayLastDataDateLabel = new Label(comp, SWT.NONE);
        displayLastDataDateLabel.setText("Display Last Data Date:");
        displayLastDataDateCombo = new Combo(comp, SWT.DROP_DOWN);
        displayLastDataDateCombo.setLayoutData(new GridData(75, SWT.DEFAULT));
        displayLastDataDateCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if ("Yes".equalsIgnoreCase(displayLastDataDateCombo.getText())) {
                    graphAttrs.setDisplayLastDataDate((Boolean) true);
                } else {
                    graphAttrs.setDisplayLastDataDate((Boolean) false);
                }
            }
        });

        // titleFontSize
        Label titleFontSizeLabel = new Label(comp, SWT.NONE);
        titleFontSizeLabel.setText("Title Text Size: ");
        titleFontSizeCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        titleFontSizeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setTitleFontSize(titleFontSizeCombo.getText());
            }
        });

        // titleFont
        Label titleFontLabel = new Label(comp, SWT.NONE);
        titleFontLabel.setText("Title Text Font: ");
        titleFontCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        titleFontCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setTitleFont(titleFontCombo.getText());
            }
        });

        // titleFontStyle
        Label titleStyleLabel = new Label(comp, SWT.NONE);
        titleStyleLabel.setText("Title Text Style: ");
        titleStyleCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        titleStyleCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setTitleStyle(titleStyleCombo.getText());
            }
        });

        // unitsFontSize
        Label unitsFontSizeLabel = new Label(comp, SWT.NONE);
        unitsFontSizeLabel.setText("Units Text Size: ");
        unitsFontSizeCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        unitsFontSizeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setUnitsFontSize(unitsFontSizeCombo.getText());
            }
        });

        // unitsFont
        Label unitsFontLabel = new Label(comp, SWT.NONE);
        unitsFontLabel.setText("Units Text Font: ");
        unitsFontCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        unitsFontCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setUnitsFont(unitsFontCombo.getText());
            }
        });

        // unitsFontStyle
        Label unitsStyleLabel = new Label(comp, SWT.NONE);
        unitsStyleLabel.setText("Units Text Style: ");
        unitsStyleCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        unitsStyleCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                graphAttrs.setUnitsStyle(unitsStyleCombo.getText());
            }
        });

        GridData gd = new GridData();

        Composite okCanComp = new Composite(shell, SWT.NONE);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        okCanComp.setLayoutData(gd);

        okCanComp.setLayout(new FormLayout());

        Button canBtn = new Button(okCanComp, SWT.PUSH);
        canBtn.setText(" Cancel ");
        FormData fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(45, -40);
        canBtn.setLayoutData(fd);

        canBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ok = false;
                shell.dispose();
            }
        });

        Button applyBtn = new Button(okCanComp, SWT.PUSH);
        applyBtn.setText("  OK  ");
        fd = new FormData();
        fd.width = 80;
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(55, -40);
        applyBtn.setLayoutData(fd);

        applyBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ok = true;
                shell.dispose();
            }
        });

        return topComp;
    }

    public void initWidgets() {
        HashMap<String, XAxisScale> xScalesMap = XAxisScaleMngr.getInstance()
                .getXAxisScales();

        if (xScalesMap != null) {
            String[] xScalesArray = new String[xScalesMap.size()];
            int i = 0;
            for (XAxisScale cf : xScalesMap.values()) {
                xScalesArray[i] = cf.getName();
                i++;
            }
            if (xScalesArray != null && xScalesArray.length > 0) {
                xScaleCombo.setItems(xScalesArray);

                for (i = 0; i < xScalesArray.length; i++) {
                    if (graphAttrs.getxAxisScale() != null
                            && xScalesArray[i].equals(graphAttrs
                                    .getxAxisScale().getName())) {
                        xScaleCombo.select(i);
                        break;
                    }
                }
            }
        }

        yScaleTypeCombo.setItems(scaleTypeOptions);
        for (int i = 0; i < scaleTypeOptions.length; i++) {
            if (graphAttrs.getyScaleType() != null) {
                String scaleType = graphAttrs.getyScaleType();
                if (scaleTypeOptions[i].equalsIgnoreCase(scaleType)) {
                    yScaleTypeCombo.select(i);
                }
            }
        }

        displayLastDataDateCombo.setItems(yesNoOptions);
        for (int i = 0; i < yesNoOptions.length; i++) {
            Boolean val = graphAttrs.getDisplayLastDataDate();
            if (val == true && yesNoOptions[i].equalsIgnoreCase("Yes")) {
                displayLastDataDateCombo.select(i);
            } else if (val == false && yesNoOptions[i].equalsIgnoreCase("No")) {
                displayLastDataDateCombo.select(i);
            }
        }

        displayxLabelsCombo.setItems(yesNoOptions);
        for (int i = 0; i < yesNoOptions.length; i++) {
            Boolean val = graphAttrs.getDisplayXLabels();
            if (val == true && yesNoOptions[i].equalsIgnoreCase("Yes")) {
                displayxLabelsCombo.select(i);
            } else if (val == false && yesNoOptions[i].equalsIgnoreCase("No")) {
                displayxLabelsCombo.select(i);
            }
        }

        unitsFontSizeCombo.setItems(textSizeOptions);
        for (int i = 0; i < textSizeOptions.length; i++) {
            String sz = graphAttrs.getUnitsFontSize();
            if (textSizeOptions[i].equals(sz.trim())) {
                unitsFontSizeCombo.select(i);
            }
        }

        unitsFontCombo.setItems(textFontOptions);
        for (int i = 0; i < textFontOptions.length; i++) {
            String fnt = graphAttrs.getUnitsFont();
            if (textFontOptions[i].equalsIgnoreCase(fnt)) {
                unitsFontCombo.select(i);
            }
        }

        unitsStyleCombo.setItems(textFontStyleOptions);
        for (int i = 0; i < textFontStyleOptions.length; i++) {
            String style = graphAttrs.getUnitsStyle();
            if (textFontStyleOptions[i].equalsIgnoreCase(style)) {
                unitsStyleCombo.select(i);
            }
        }

        titleFontSizeCombo.setItems(textSizeOptions);
        for (int i = 0; i < textSizeOptions.length; i++) {
            String sz = graphAttrs.getTitleFontSize();
            if (textSizeOptions[i].equals(sz.trim())) {
                titleFontSizeCombo.select(i);
            }
        }

        titleFontCombo.setItems(textFontOptions);
        for (int i = 0; i < textFontOptions.length; i++) {
            String fnt = graphAttrs.getTitleFont();
            if (textFontOptions[i].equalsIgnoreCase(fnt)) {
                titleFontCombo.select(i);
            }
        }

        titleStyleCombo.setItems(textFontStyleOptions);
        for (int i = 0; i < textFontStyleOptions.length; i++) {
            String style = graphAttrs.getTitleStyle();
            if (textFontStyleOptions[i].equalsIgnoreCase(style)) {
                titleStyleCombo.select(i);
            }
        }

    }

    public void validateYScaleMinMax() {
        if (GhcdUtil.YSCALE_TYPE_LOG
                .equalsIgnoreCase(yScaleTypeCombo.getText())) {

            // For log scale type, validate yScaleMin and yScaleMax
            // values
            try {

                float fval1 = Float.parseFloat(yScaleMinText.getText());
                float fval2 = Float.parseFloat(yScaleMaxText.getText());
                float logval1 = (float) Math.log(fval1);
                float logval2 = (float) Math.log(fval2);

                if (Float.isNaN(logval1) || Float.isNaN(logval2)) {

                    MessageDialog infoDlg = new MessageDialog(
                            shell,
                            "Error",
                            null,
                            "Cannot perform log calculation. Please double check the values of yScaleMin and/or yScaleMax.",
                            MessageDialog.ERROR, new String[] { "OK" }, 0);
                    infoDlg.open();
                }
            } catch (NumberFormatException exc) {
                float fval1 = graphAttrs.getyScaleMin().floatValue();
                yScaleMinText.setText(Float.toString(fval1));

                float fval2 = graphAttrs.getyScaleMax().floatValue();
                yScaleMaxText.setText(Float.toString(fval2));
            }
        }
    }

    public void createShell(int x, int y) {

        shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
        shell.setText(dlgTitle);
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);
        shell.setLocation(x, y);

        createDialog(shell);
    }

    public void open() {
        open(getParent().getLocation().x + 10, getParent().getLocation().y + 10);
    }

    public GraphAttributes open(int x, int y) {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        createShell(x, y);
        initWidgets();

        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        return (ok ? graphAttrs : null);
    }
}