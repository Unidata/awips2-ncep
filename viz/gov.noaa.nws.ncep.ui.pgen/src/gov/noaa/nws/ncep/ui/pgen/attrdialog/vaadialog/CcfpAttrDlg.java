/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.vaaDialog.CcfpAttrDlg
 *
 * 20 September 2010
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog.vaadialog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.LabeledLine;
import gov.noaa.nws.ncep.ui.pgen.sigmet.AbstractSigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Ccfp;
import gov.noaa.nws.ncep.ui.pgen.sigmet.CcfpInfo;
import gov.noaa.nws.ncep.ui.pgen.sigmet.ICcfp;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.SigmetInfo;
import gov.noaa.nws.ncep.ui.pgen.tools.ILabeledLine;

/**
 * Singleton attribute dialog for CCFP.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 2010      #322      G. Zhang     Initial creation
 * Apr 2011                B. Yin       Re-factor IAttribute
 * Mar 2012      #625,#611 S. Gurung    Change default CCFP polygon colors: Purple for Hi
 *                                      confidence Area, Line and Line(Med)
 *                                      Added ability to change SIGMET type (from Area to
 *                                      Line/LineMed and back and forth)
 * Mar 2013      #928      B. Yin       Made the button bar smaller.
 * Mar 20, 2019  #7572     dgilling     Code cleanup.
 *
 * </pre>
 *
 * @author gzhang
 */
public class CcfpAttrDlg extends AttrDlg implements ICcfp{

    private static final int APPLY_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

    private static final int FORMAT_BUTTON_ID = IDialogConstants.CLIENT_ID + 2;

    private static CcfpAttrDlg INSTANCE = null;

    private ILabeledLine ccfpTool;

    // 20100903 type changed from AbstractSigmet to Sigmet
    private Sigmet asig = null;

    public static final String AREA = "Area", LINE = "Line",
            LINE_MED = "LineMed";

    public static final String LINE_SEPERATER = ":::";

    private String lineType = "";

    private static final String WIDTH = "10.00";

    private String width = WIDTH;

    private static final Color PURPLE = new Color(145, 44, 238);

    private static final Color LIGHT_BLUE = new Color(30, 144, 255);

    private static final String[] ITEMS_CVRG = new String[] { "75-100%",
            "40-74%", "25-39%" };

    private static final String[] ITEMS_TOPS = new String[] { "400+", "350-390",
            "300-340", "250-290" };

    private static final String[] ITEMS_CONF = new String[] { "50-100%",
            "25-49%" };

    private static final String[] ITEMS_GWTH = new String[] { "\u2191", "NC",
            "\u2193" };

    private static final String[] ITEMS_SPD = new String[] { "0", "5", "10",
            "15", "20", "25", "30", "35", "40", "45", "50", "55", "60" };

    private static final String[] ITEMS_DIR = SigmetInfo.DIRECT_ARRAY;

    private Color[] colors = new Color[] { LIGHT_BLUE, PURPLE };

    private Group top_3;

    private Button btnArea;

    private Button btnLine;

    private Button btnLineMed;

    protected Composite top = null;

    private Combo cmbCvrg;

    private Combo cmbTops;

    private Combo cmbConf;

    private Combo cmbGwth;

    private Combo cmbSpd;

    private Combo cmbDir;

    private static final int LAYOUT_WIDTH = 7;

    private static boolean NotFirstOpen = false;

    private HashMap<String, Button[]> attrButtonMap = new HashMap<>();

    private String editableAttrArea = "";

    private String editableAttrFromLine = "";

    private String editableAttrId = "EAST";

    private String editableAttrSequence = "";

    // editableAttrStartTime;
    private String ccfpIssueTime = "";

    // editableAttrEndTime;
    private String ccfpValidTime = "";

    // editableAttrPhenom;
    private String ccfpCvrg = ITEMS_CVRG[0];// "";
    // editableAttrPhenom2;

    private String ccfpTops = ITEMS_TOPS[0];// "";
    // editableAttrPhenomLat;

    private String ccfpConf = ITEMS_CONF[0];// "";
    // editableAttrPhenomLon;

    private String ccfpGrwt = ITEMS_GWTH[0];// "";
    // editableAttrPhenomSpeed;

    private String ccfpSpd = ITEMS_SPD[0];// "";
    // editableAttrPhenomDirection;

    private String ccfpDir = ITEMS_DIR[0];// "";

    protected CcfpAttrDlg(Shell parShell) {
        super(parShell);
    }

    public static synchronized CcfpAttrDlg getInstance(Shell parShell) {
        if (INSTANCE == null) {
            INSTANCE = new CcfpAttrDlg(parShell);
        }
        return INSTANCE;
    }

    @Override
    public String getCcfpLineType(){

        if (CcfpAttrDlg.LINE_MED.equalsIgnoreCase(lineType)) {
            return "LINE_DASHED_4";
        } else {
            return "LINE_SOLID";
        }
    }

    @Override
    public FillPattern getFillPattern(){
        if (cmbCvrg == null || cmbCvrg.isDisposed()) {
            return FillPattern.FILL_PATTERN_1;
        }

        ccfpCvrg = cmbCvrg.getText().trim();

        if (ccfpCvrg == null || ccfpCvrg.length() == 0) {
            return FillPattern.FILL_PATTERN_1;
        }

        if (ITEMS_CVRG[0].equals(ccfpCvrg)) {
            return FillPattern.FILL_PATTERN_5;
        } else if(ITEMS_CVRG[1].equals(ccfpCvrg)) {
            return FillPattern.FILL_PATTERN_3;
        } else {
            return FillPattern.FILL_PATTERN_1;
        }
    }

    @Override
    public boolean close() {
        NotFirstOpen = false;

        if (getShell() != null) {
            Rectangle bounds = getShell().getBounds();
            shellLocation = new Point(bounds.x, bounds.y);
        }
        return super.close();
    }

    @Override
    public Boolean isFilled(){
        return true;
    }

    @Override
    public void createButtonsForButtonBar(Composite parent) {
        Button formatBtn = createButton(parent, FORMAT_BUTTON_ID, "Format...", false);
        formatBtn.setEnabled(false);

        Button applyBtn = createButton(parent, APPLY_BUTTON_ID, "Apply", false);
        applyBtn.setEnabled(false);

        Button cancelBtn = createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        cancelBtn.setEnabled(false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case FORMAT_BUTTON_ID:
            CcfpTimeDlg ct = null;
            try {
                ct = CcfpTimeDlg.getInstance(CcfpAttrDlg.this.getParentShell());
            } catch (Exception ee) {
                statusHandler.debug(ee.getLocalizedMessage(), ee);
            }

            if (ct != null) {
                ct.open();
            }
            break;

        case APPLY_BUTTON_ID:
            okPressed2();
            break;

        case IDialogConstants.CANCEL_ID:
            cancelPressed();
            break;

        default:
            break;
        }
    }

    @Override
    public void enableButtons() {
        this.getButton(FORMAT_BUTTON_ID).setEnabled(true);
        this.getButton(APPLY_BUTTON_ID).setEnabled(true);
        this.getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
    }

    public AbstractSigmet getAbstractSigmet() {
        return this.asig;
    }

    public void setAbstractSigmet(
            gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement de) {
        /*
         * 20100907 without this lineType wont' set BEFORE details area
         * displayed
         */
        this.setAttrForDlg(de);
    }

    @Override
    public void copyEditableAttrToAbstractSigmet(AbstractSigmet ba){

        ba.setColors(CcfpAttrDlg.this.getColors());

        ba.setType(this.getLineType());
        ba.setWidth(this.getWidth());

        Sigmet sig = (Sigmet) ba;
        sig.setEditableAttrStartTime(this.ccfpIssueTime);
        sig.setEditableAttrEndTime(this.ccfpValidTime);
        sig.setEditableAttrPhenom(this.ccfpCvrg);
        sig.setEditableAttrPhenom2(this.ccfpTops);
        sig.setEditableAttrPhenomLat(this.ccfpConf);
        sig.setEditableAttrPhenomLon(this.ccfpGrwt);
        sig.setEditableAttrPhenomSpeed(this.ccfpSpd);
        sig.setEditableAttrPhenomDirection(this.ccfpDir);

        if(ITEMS_CVRG[0].equals(ccfpCvrg)) {
            sig.setFillPattern(FillPattern.FILL_PATTERN_5);
        } else if(ITEMS_CVRG[1].equals(ccfpCvrg)) {
            sig.setFillPattern(FillPattern.FILL_PATTERN_3);
        } else {
            sig.setFillPattern(FillPattern.FILL_PATTERN_1);
        }
    }

    @Override
    public Color[] getColors(){
        if (!AREA.equalsIgnoreCase(lineType)) {
            // Line/LineMed ONLY use purple
            return new Color[] { PURPLE };
        } else {

            if (cmbConf == null || cmbConf.isDisposed()) {
                return colors;
            } else{
                ccfpConf = cmbConf.getText().trim();
                if (ccfpConf.contains(ITEMS_CONF[1])) {
                    return new Color[] { PURPLE };
                } else {
                    return new Color[] { LIGHT_BLUE };
                }
            }
        }
        //return colors;
    }

    private void setColor(Color clr) {
        colors[0] = clr;
    }

    public String getLineType() {
        return this.lineType;
    }

    public void setLineType(String lType) {
        this.lineType = lType;
    }

    public String getSideOfLine() {
        return "";
    }

    public void setSideOfLine(String lineSideString) {

    }

    public String getWidth() {
        return this.width;
    }

    public void setWidth(String widthString) {
        this.width = widthString;
    }

    @Override
    public Control createDialogArea(Composite parent) {

        this.top = (Composite) super.createDialogArea(parent);

        top.setLayout(new GridLayout(LAYOUT_WIDTH, false));
        getShell().setText("Collaborative Convective");

        btnArea = new Button(top, SWT.RADIO);
        btnArea.setSelection(true); // default
        btnArea.setText("Area");
        if (mouseHandlerName == null
                || mouseHandlerName.contains("PgenLabeledLineDrawingHandler")) {
            // when NOT selecting element
            setLineType(AREA);
        }

        this.fillSpaces(top, SWT.LEFT, 2, false);

        btnLine = new Button(top, SWT.RADIO);
        btnLine.setText("Line");

        this.fillSpaces(top, SWT.LEFT, 2, false);

        btnLineMed = new Button(top, SWT.RADIO);
        btnLineMed.setText("Line(Med)  ");

        attrButtonMap.put("lineType",
                new Button[] { btnArea, btnLine, btnLineMed });


        addBtnListeners();

        if (this.asig == null
                && (this.lineType == null || this.lineType.isEmpty())) {
            this.setLineType(AREA);
        }

        String lt = this.getLineType();
        if (lt != null && lt.equalsIgnoreCase(AREA)) {
this.createAreaInfo(top);
        }

        init();
        // grayOutUnselectedBtns();
        setMouseHandlerName(null);
        return top;
    }


    public String getEditableAttrArea() {
        return editableAttrArea;
    }

    public void setEditableAttrArea(String editableAttrArea) {
        this.editableAttrArea = editableAttrArea;
    }

    public String getEditableAttrFromLine() {
        return editableAttrFromLine;
    }

    public void setEditableAttrFromLine(String editableAttrFromLine) {
        this.editableAttrFromLine = editableAttrFromLine;
    }

    public String getEditableAttrId() {
        return editableAttrId;
    }

    public void setEditableAttrId(String editableAttrId) {
        this.editableAttrId = editableAttrId;
    }

    public String getEditableAttrSequence() {
        return editableAttrSequence;
    }

    public void setEditableAttrSequence(String editableAttrSequence) {
        this.editableAttrSequence = editableAttrSequence;
    }

    // private class SigmetCommAttrDlgSaveMsgDlg extends AttrDlg{ }

    public void saveApplyPressed() {
        ArrayList<AbstractDrawableComponent> adcList = null;
        ArrayList<AbstractDrawableComponent> newList = new ArrayList<>();

        if (drawingLayer != null) {
            adcList = (ArrayList<AbstractDrawableComponent>) drawingLayer
                    .getAllSelected();
        }

        if (adcList != null && !adcList.isEmpty()) {

            Sigmet newEl = null;

            for (AbstractDrawableComponent adc : adcList) {

                Sigmet el = (Sigmet) adc.getPrimaryDE();
                if (el != null) {
                    newEl = (Sigmet) el.copy();

                    attrUpdate();

                    copyEditableAttrToAbstractSigmet(newEl);

                    this.setAbstractSigmet(newEl);
                    newList.add(newEl);
                }
            }

            // if ( newEl != null ){ AttrSettings.getInstance().setSettings(
            // newEl ); }

            ArrayList<AbstractDrawableComponent> oldList = new ArrayList<>(
                    adcList);
            drawingLayer.replaceElements(oldList, newList);
        }

        drawingLayer.removeSelected();
        for (AbstractDrawableComponent adc : newList) {
            drawingLayer.addSelected(adc);
        }

        if (mapEditor != null) {
            mapEditor.refresh();
        }
    }

    private void attrUpdate() {
        if (cmbConf == null || cmbConf.isDisposed()) {
            return;
        }
        this.ccfpConf = this.cmbConf.getText().trim();
        this.ccfpCvrg = this.cmbCvrg.getText().trim();
        this.ccfpDir = this.cmbDir.getText().trim();
        this.ccfpGrwt = this.cmbGwth.getText().trim();
        // this.ccfpIssueTime = this.cmbIssTime.getText().trim();
        this.ccfpSpd = this.cmbSpd.getText().trim();
        this.ccfpTops = this.cmbTops.getText().trim();
        // this.ccfpValidTime = this.cmbVaTime.getText().trim();

    }

    static Color getDefaultColor(String pType) {
        return LIGHT_BLUE;
    }

    /*
     * take the empty spaces
     */
    private void fillSpaces(Composite gp, int dir, int num, boolean empty) {
        for (int i = 0; i < num; i++) {
            Label lbl = new Label(gp, dir);
            lbl.setText(empty ? "" : " ");
        }
    }

    @Override
    public int open() {

        if (this.getShell() == null) {
            this.create();
        }
        if (shellLocation == null) {
            this.getShell()
                    .setLocation(this.getShell().getParent().getLocation());
        } else {
            getShell().setLocation(shellLocation);
        }

        int i = super.open();
        Point shellSize = this.getShell().getSize();
        shellSize.x += 20;
        this.getShell().setSize(shellSize);
        return i;

    }

    public String getCcfpIssueTime() {
        return ccfpIssueTime;
    }

    public void setCcfpIssueTime(String ccfpIssueTime) {
        this.ccfpIssueTime = ccfpIssueTime;
    }

    public String getCcfpValidTime() {
        return ccfpValidTime;
    }

    public void setCcfpValidTime(String ccfpValidTime) {
        this.ccfpValidTime = ccfpValidTime;
    }

    public String getCcfpCvrg() {
        return ccfpCvrg;
    }

    public void setCcfpCvrg(String ccfpCvrg) {
        this.ccfpCvrg = ccfpCvrg;
    }

    public String getCcfpTops() {
        return ccfpTops;
    }

    public void setCcfpTops(String ccfpTops) {
        this.ccfpTops = ccfpTops;
    }

    public String getCcfpConf() {
        return ccfpConf;
    }

    public void setCcfpConf(String ccfpConf) {
        this.ccfpConf = ccfpConf;
    }

    public String getCcfpGrwt() {
        return ccfpGrwt;
    }

    public void setCcfpGrwt(String ccfpGrwt) {
        this.ccfpGrwt = ccfpGrwt;
    }

    public String getCcfpSpd() {
        return ccfpSpd;
    }

    public void setCcfpSpd(String ccfpSpd) {
        this.ccfpSpd = ccfpSpd;
    }

    public String getCcfpDir() {
        return ccfpDir;
    }

    public void setCcfpDir(String ccfpDir) {
        this.ccfpDir = ccfpDir;
    }

    @Override
    public void setAttrForDlg(IAttribute attr) {
        if (attr == null) {
            return;
        }

        this.asig = (Sigmet) attr;

        Color clr = attr.getColors()[0];
        if (clr != null) {
            this.setColor(clr);
        }

        this.lineType = asig.getType();
        this.ccfpIssueTime = asig.getEditableAttrStartTime();
        this.ccfpValidTime = asig.getEditableAttrEndTime();
        this.ccfpCvrg = asig.getEditableAttrPhenom();
        this.ccfpTops = asig.getEditableAttrPhenom2();
        this.ccfpConf = asig.getEditableAttrPhenomLat();
        this.ccfpGrwt = asig.getEditableAttrPhenomLon();
        this.ccfpSpd = asig.getEditableAttrPhenomSpeed();
        this.ccfpDir = asig.getEditableAttrPhenomDirection();
        /*
         * createDialogArea() calls init() first when open() executed,
         * setAttrForDlg() after open(), so redo it
         */
        init();
    }

    /*
     * add Listeners to the Button widgets
     */
    private void addBtnListeners() {
        btnArea.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!btnArea.getSelection()) {
                    /*
                     * if this is selected when another is selecting, this
                     * method gets called first for de-selecting
                     */
                    return;
                }

                if (top_3 == null || top_3.isDisposed()) {
                    NotFirstOpen = true;
                    createAreaInfo(top);
                }

                CcfpAttrDlg.this.setLineType(AREA);

                if (cmbConf == null || cmbConf.isDisposed()) {
                    return;
                }

                ccfpConf = cmbConf.getText().trim();
                if (ccfpConf.contains(ITEMS_CONF[1])) {
                    CcfpAttrDlg.this.setColor(PURPLE);
                } else {
                    CcfpAttrDlg.this.setColor(LIGHT_BLUE);
                }
            }
        });

        btnLine.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!btnLine.getSelection()) {
                    return;
                }

                if (top_3 != null) {
                    disposeAreaInfo();
                }

                CcfpAttrDlg.this.setLineType(LINE);
            }
        });


        btnLineMed.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!btnLineMed.getSelection()) {
                    return;
                }

                if (top_3 != null) {
                    disposeAreaInfo();
                }

                CcfpAttrDlg.this.setLineType(LINE_MED);
            }
        });
    }

    /*
     * add Listeners to the Combo widgets
     */
    private void addCmbListeners() {
        cmbCvrg.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccfpCvrg = cmbCvrg.getText().trim();

                if (asig == null) {
                    return;
                }

                if (ITEMS_CVRG[0].equals(ccfpCvrg)) {
                    asig.setFillPattern(FillPattern.FILL_PATTERN_5);
                } else if(ITEMS_CVRG[1].equals(ccfpCvrg)) {
                    asig.setFillPattern(FillPattern.FILL_PATTERN_3);
                } else {
                    asig.setFillPattern(FillPattern.FILL_PATTERN_1);
                }
            }
        });

        cmbTops.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccfpTops = cmbTops.getText().trim();
            }
        });

        cmbConf.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccfpConf = cmbConf.getText().trim();
                if (ccfpConf.contains(ITEMS_CONF[1])) {
                    CcfpAttrDlg.this.setColor(PURPLE);
                } else {
                    CcfpAttrDlg.this.setColor(LIGHT_BLUE);
                }
            }
        });

        cmbGwth.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccfpGrwt = cmbGwth.getText().trim();
            }
        });

        cmbSpd.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccfpSpd = cmbSpd.getText().trim();
            }
        });

        cmbDir.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccfpDir = cmbDir.getText().trim();
            }
        });
    }

    /*
     * initialize the widgets
     */
    private void init() {
        if (this.asig == null) {
            return;
        }

        Button[] btns = attrButtonMap.get("lineType");

        if (btns == null) {
            return;
        }

        for (Button btn : btns) {
            if (btn == null || btn.isDisposed()) {
                return;
            }
        }

        if (lineType != null) {
            if (lineType.equals(AREA)) {
                btns[0].setSelection(true);
                btns[1].setSelection(false);
                btns[2].setSelection(false);
            } else if (lineType.equals(LINE)) {
                btns[0].setSelection(false);
                btns[1].setSelection(true);
                btns[2].setSelection(false);

                // attrControlMap.get("lineType").setEnabled(true);
                // attrControlMap.get("width").setEnabled(true);
            } else if (lineType.equals(LINE_MED)) {
                btns[0].setSelection(false);
                btns[1].setSelection(false);
                btns[2].setSelection(true);

                // attrControlMap.get("width").setEnabled(true);
            }
        }

        initCombos();

    }

    private void createAreaInfo(Composite comp) {

        top_3 = new Group(comp, SWT.LEFT);
        top_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true,
                LAYOUT_WIDTH, 1));
        top_3.setLayout(new GridLayout(LAYOUT_WIDTH, false));

        // Coverage

        Label lblCvrg = new Label(top_3, SWT.LEFT);
        lblCvrg.setText("Coverage: ");

        fillSpaces(top_3, SWT.LEFT, 2, true);

        cmbCvrg = new Combo(top_3, SWT.LEFT | SWT.READ_ONLY);
        cmbCvrg.setItems(ITEMS_CVRG);
        cmbCvrg.select(0);

        fillSpaces(top_3, SWT.LEFT, 3, true);

        // Echo Tops

        Label lblTops = new Label(top_3, SWT.LEFT);
        lblTops.setText("Echo Tops:");

        fillSpaces(top_3, SWT.LEFT, 2, true);

        cmbTops = new Combo(top_3, SWT.LEFT | SWT.READ_ONLY);
        cmbTops.setItems(ITEMS_TOPS);
        cmbTops.select(0);

        fillSpaces(top_3, SWT.LEFT, 3, true);

        // Confidence

        Label lblConf = new Label(top_3, SWT.LEFT);
        lblConf.setText("Confidence:");

        fillSpaces(top_3, SWT.LEFT, 2, true);

        cmbConf = new Combo(top_3, SWT.LEFT | SWT.READ_ONLY);
        cmbConf.setItems(ITEMS_CONF);
        cmbConf.select(0);

        fillSpaces(top_3, SWT.LEFT, 3, true);

        // Growth

        Label lblGwth = new Label(top_3, SWT.LEFT);
        lblGwth.setText("Growth:");

        fillSpaces(top_3, SWT.LEFT, 2, true);

        cmbGwth = new Combo(top_3, SWT.LEFT | SWT.READ_ONLY);
        cmbGwth.setItems(ITEMS_GWTH);
        cmbGwth.select(0);

        fillSpaces(top_3, SWT.LEFT, 3, true);

        // Speed

        Label lblSpd = new Label(top_3, SWT.LEFT);
        lblSpd.setText("Speed(kts):");

        fillSpaces(top_3, SWT.LEFT, 2, true);

        cmbSpd = new Combo(top_3, SWT.LEFT | SWT.READ_ONLY);
        cmbSpd.setItems(ITEMS_SPD);
        cmbSpd.select(0);

        fillSpaces(top_3, SWT.LEFT, 3, true);

        // Direction

        Label lblDir = new Label(top_3, SWT.LEFT);
        lblDir.setText("Direction:");

        fillSpaces(top_3, SWT.LEFT, 2, true);

        cmbDir = new Combo(top_3, SWT.LEFT | SWT.READ_ONLY);
        cmbDir.setItems(ITEMS_DIR);
        cmbDir.select(0);

        fillSpaces(top_3, SWT.LEFT, 3, true);

        addCmbListeners();
        // first time NO need
        if (NotFirstOpen) {
            this.getShell().pack();
            this.getShell().layout();
        }

        NotFirstOpen = true;// toggleExFlag();

        initCombos();// 20101005
    }

    private void disposeAreaInfo() {

        if (top_3 != null && !top_3.isDisposed()) {

            Control[] wids = top_3.getChildren();

            for (int kk = 0; wids != null && kk < wids.length; kk++) {
                wids[kk].dispose();
            }

            top_3.dispose();
            top_3 = null;

        }

        this.getShell().pack();
        this.getShell().layout();
        // */// toggleExFlag();
    }

    private void initCombos() {

        if (cmbCvrg != null && (!cmbCvrg.isDisposed()) && ccfpCvrg != null
                && ccfpCvrg.length() > 0) {
            cmbCvrg.setText(ccfpCvrg);
        }
        if (cmbTops != null && (!cmbTops.isDisposed()) && ccfpTops != null
                && ccfpTops.length() > 0) {
            cmbTops.setText(ccfpTops);
        }
        if (cmbConf != null && (!cmbConf.isDisposed()) && ccfpConf != null
                && ccfpConf.length() > 0) {
            cmbConf.setText(ccfpConf);
        }
        if (cmbGwth != null && (!cmbGwth.isDisposed()) && ccfpGrwt != null
                && ccfpGrwt.length() > 0) {
            cmbGwth.setText(ccfpGrwt);
        }
        if (cmbSpd != null && (!cmbSpd.isDisposed()) && ccfpSpd != null
                && ccfpSpd.length() > 0) {
            cmbSpd.setText(ccfpSpd);
        }
        if (cmbDir != null && (!cmbDir.isDisposed()) && ccfpDir != null
                && ccfpDir.length() > 0) {
            cmbDir.setText(ccfpDir);
        }
    }

    @Override
    public Coordinate[] getLinePoints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPatternName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSmoothFactor() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Boolean isClosedLine() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copyEditableAttrToAbstractSigmet2(AbstractSigmet ba, LabeledLine ll){

        ba.setColors(CcfpAttrDlg.this.getColors());

        ba.setType(this.getLineType());
        ba.setWidth(this.getWidth());

        Sigmet sig = (Sigmet) ba;
        sig.setEditableAttrStartTime(this.ccfpIssueTime);
        sig.setEditableAttrEndTime(this.ccfpValidTime);
        sig.setEditableAttrPhenom(this.ccfpCvrg);
        sig.setEditableAttrPhenom2(this.ccfpTops);
        sig.setEditableAttrPhenomLat(this.ccfpConf);
        sig.setEditableAttrPhenomLon(this.ccfpGrwt);
        sig.setEditableAttrPhenomSpeed(this.ccfpSpd);
        sig.setEditableAttrPhenomDirection(this.ccfpDir);

        if(ITEMS_CVRG[0].equals(ccfpCvrg)) {
            sig.setFillPattern(FillPattern.FILL_PATTERN_5);
        } else if(ITEMS_CVRG[1].equals(ccfpCvrg)) {
            sig.setFillPattern(FillPattern.FILL_PATTERN_3);
        } else {
            sig.setFillPattern(FillPattern.FILL_PATTERN_1);
        }

       // copiedToSigmet = true;

        StringBuilder sb = new StringBuilder("CCFP_SIGMET");

        sb.append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getEditableAttrPhenomSpeed())
                .append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getEditableAttrPhenomDirection())
                .append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getStartTime()).append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getEndtime()).append(CcfpInfo.TEXT_SEPERATOR);

        sb.append(sig.getEditableAttrPhenom()).append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getEditableAttrPhenom2()).append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getEditableAttrPhenomLat())
                .append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(sig.getEditableAttrPhenomLon())
                .append(CcfpInfo.TEXT_SEPERATOR);
        sb.append(this.lineType);

        ((Ccfp) ll).setCollectionName(sb.toString());
    }

    public void setLine(Line sig) {

        sig.setColors(CcfpAttrDlg.this.getColors());

        if (ITEMS_CVRG[0].equals(ccfpCvrg)) {
            sig.setFillPattern(FillPattern.FILL_PATTERN_5);
        } else if(ITEMS_CVRG[1].equals(ccfpCvrg)) {
            sig.setFillPattern(FillPattern.FILL_PATTERN_3);
        } else {
            sig.setFillPattern(FillPattern.FILL_PATTERN_1);
        }

        // copiedToSigmet = true;
    }

    public void setText(Text txt) {
        txt.setColors(CcfpAttrDlg.this.getColors());
        // copiedToSigmet = true;
    }

    public void setCcfpDrawingTool(ILabeledLine pgenTool) {
        this.ccfpTool = pgenTool;
    }

    public void okPressed2() {
        if (ccfpTool != null && ccfpTool.getLabeledLine() != null) {

            String origLineType = (asig != null) ? asig.getType() : "";
            String newLineType = CcfpAttrDlg.this.getLineType();

            if (!newLineType.equals(origLineType)) {
                convertType();
            }

            LabeledLine ll = ccfpTool.getLabeledLine();

            Line pln = (Line) ll.getPrimaryDE();

            // Line & LineMed NO attributes changes and NOT from them converting
            // to Area
            if (!pln.isClosedLine()) {
                return;
            }

            // NOT from Area converting to Line, LineMed
            if (pln.isClosedLine()
                    && (LINE.equals(lineType) || LINE_MED.equals(lineType))) {
                return;
            }

            LabeledLine newll = ll.copy();
            attrUpdate();
            Sigmet sig = ((Ccfp) newll).getSigmet();
            copyEditableAttrToAbstractSigmet2(sig, newll);
            setAbstractSigmet(sig);

            Iterator<DrawableElement> it = newll.createDEIterator();
            while (it.hasNext()) {
                DrawableElement de = it.next();

                if (de instanceof Text) {

                    de.setColors(getColors());
                    Text txt = (Text) de;
                    txt.setText(CcfpInfo.getCcfpTxt2(sig));
                } else if (de instanceof Line) {

                    if ("LINE_SOLID".equals(de.getPgenType())) {
                        this.setLine((Line) de);
                    }

                    if (!"POINTED_ARROW".equals(de.getPgenType())) {
                        de.setColors(this.getColors());
                    }
                }
            }

            drawingLayer.replaceElement(ll, newll);
            ccfpTool.setLabeledLine(newll);

            // reset handle bar
            drawingLayer.removeSelected();
            Iterator<DrawableElement> iterator = newll.createDEIterator();
            while (iterator.hasNext()) {
                drawingLayer.addSelected(iterator.next());
            }

            // make sure the arrow line won't go through the text box.
            if (newll instanceof Ccfp) {
                ((Ccfp)newll).moveText2Last();
            }

            mapEditor.refresh();
        }

    }

    @Override
    public void setType(String type) {
        if (type != null) {
            this.setLineType(type);
        }
    }

    @Override
    public boolean isAreaType(){
        return AREA.equalsIgnoreCase(lineType);
    }

    public void grayOutUnselectedBtns() {
        if (mouseHandlerName == null
                || mouseHandlerName.contains("PgenLabeledLineDrawingHandler")) {
            // NOT in selecting Mode, do nothing
            return;
        }

        if (AREA.equalsIgnoreCase(lineType)) {
            btnLine.setEnabled(false);
            btnLineMed.setEnabled(false);
            return;
        }

        if (LINE.equalsIgnoreCase(lineType)) {
            btnLineMed.setEnabled(false);
            btnArea.setEnabled(false);
            return;
        }

        if (LINE_MED.equalsIgnoreCase(lineType)) {
            btnArea.setEnabled(false);
            btnLine.setEnabled(false);
            return;
        }
    }

    public void convertType() {
        if (ccfpTool != null && ccfpTool.getLabeledLine() != null) {
            LabeledLine ll = ccfpTool.getLabeledLine();
            LabeledLine newll = ll.copy();
            attrUpdate();

            Sigmet sig = ((Ccfp) newll).getSigmet();
            copyEditableAttrToAbstractSigmet2(sig, newll);
            setAbstractSigmet(sig);

            newll = createLabeledLine(newll);

            drawingLayer.replaceElement(ll, newll);
            ccfpTool.setLabeledLine(newll);

            // reset handle bar
            drawingLayer.removeSelected();
            Iterator<DrawableElement> iterator = newll.createDEIterator();
            while (iterator.hasNext()) {
                drawingLayer.addSelected(iterator.next());
            }
            mapEditor.refresh();
        }

    }

    public LabeledLine createLabeledLine(LabeledLine ll) {

        CcfpAttrDlg ccdlg = this;

        List<Coordinate> newPoints = ll.getPoints();

        /* if converting from Area, remove points for Text and Arrow, if any. */
        Iterator<DrawableElement> it = ll.createDEIterator();
        while (it.hasNext()) {
            DrawableElement de = it.next();

            if (de instanceof Text) {
                newPoints.remove(newPoints.size() - 1);
            } else if (de instanceof Line) {

                if ("POINTED_ARROW".equals(de.getPgenType())) {
                    newPoints.remove(newPoints.size() - 1);
                    newPoints.remove(newPoints.size() - 1);
                }
            }
        }
        Sigmet sig = new Sigmet();
        sig.setType(ccdlg.getCcfpLineType());

        Line ln = new Line();
        ln.update(ccdlg);
        ln.setLinePoints(newPoints);
        ln.setPgenCategory("Lines");
        ln.setPgenType(ccdlg.getCcfpLineType());
        ln.setColors(ccdlg.getColors());
        ln.setClosed(ccdlg.isAreaType());
        ln.setFilled(ccdlg.isAreaType());
        if (!ccdlg.isAreaType()) {
            ln.setLineWidth(3.0f);
        } else {
            ln.setLineWidth(2.0f);
        }

        LabeledLine newll = new Ccfp("CCFP_SIGMET");
        newll.setPgenCategory("SIGMET");
        newll.setPgenType("CCFP_SIGMET");
        newll.setParent(ll.getParent());

        ((Ccfp) newll).setSigmet(sig);
        ((Ccfp) newll).setAreaLine(ln);
        ((Ccfp) newll).setAttributes(ccdlg);

        newll.addLine(ln);
        return newll;
    }

}