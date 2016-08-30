/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.ContoursInfoDlg
 * 
 * Date created: October 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourDefault;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourFiles;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourLabel;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourLevel;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourRoot;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContoursInfo;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.FcstHrs;
import gov.noaa.nws.ncep.ui.pgen.contours.IContours;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

/**
 * Class for creating a dialog to edit the contours' attribute information.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#      Engineer     Description
 * -------------------------------------------------------------------
 * 10/09        #167        J. Wu         Initial Creation.
 * 07/11        #450        G. Hull       NcPathManager
 * 12/13        1084        J. Wu         Add table-control for Cint in contoursInfo.xml
 * 08/01/2015   8213        P.            CAVE>PGEN 
 *                          Chowdhuri      - Refinements to contoursInfo.xml
 * 09/29/2015   R8163       J. Wu         Prevent exception when contour type changes.
 * 11/18/2015   R12829      J. Wu         Link "Contours Parameter" to the one defined on layer.
 * 04/14/2016   R13245      B. Yin        Changed time fields to 24 hour format.
 * 08/23/2016   R11434      J. Wu         All each parameter has its own list of level/fcsthr
 * 
 * </pre>
 * 
 * @author J. Wu
 */

public class ContoursInfoDlg extends CaveJFACEDialog implements IContours {

    // Status handling
    private static final IUFStatusHandler handler = UFStatus
            .getHandler(ContoursInfoDlg.class);

    //Time field width
    private static final int TIME_FILED_WIDTH = 50;

    //Time text limit 
    private static final int TIME_TEXT_LIMIT = 4;

    // Contours information files
    private static List<String> contoursInfoParamFilelist;

    // Contours information
    private static HashMap<String, ContoursInfo> contoursInfoTables;

    private static List<String> contourParameters;

    private static HashMap<String, List<String>> contourLevels;

    private static HashMap<String, List<String>> contourFcstHours;

    // The JAXB Manager
    private static SingleTypeJAXBManager<ContourRoot> cntrInfoManager;

    private Composite top = null;

    private Combo parmCombo = null;

    private Text parmTxt = null;

    private Combo levelCombo1 = null;

    private Text levelValueTxt1 = null;

    private Combo levelCombo2 = null;

    private Text levelValueTxt2 = null;

    private Combo fcsthrCombo = null;

    private Text fcsthrTxt = null;

    private Text cintTxt = null;

    private AttrDlg contoursAttrDlg = null;

    private DateTime date1 = null;

    private Text time1 = null;

    private DateTime date2 = null;

    private Text time2 = null;

    /*
     * Constructor
     */
    protected ContoursInfoDlg(Shell parentShell) {
        super(parentShell);
    }

    /**
     * Add Accept and Cancel buttons on the dialog's button bar.
     */
    @Override
    public void createButtonsForButtonBar(Composite parent) {

        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, true);

    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Contours Information");
    }

    /**
     * Creates the dialog area
     * 
     */
    @Override
    public Control createDialogArea(Composite parent) {

        top = (Composite) super.createDialogArea(parent);

        GridLayout mainLayout = new GridLayout(2, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        mainLayout.horizontalSpacing = 3;
        top.setLayout(mainLayout);

        initializeComponents(top);

        return top;
    }

    /**
     * create components for the dialog
     * 
     */
    private void initializeComponents(Composite comp) {

        GridLayout layout1 = new GridLayout(2, false);
        layout1.marginHeight = 1;
        layout1.marginWidth = 1;
        layout1.horizontalSpacing = 3;

        // Contours parameter name
        Label parmLbl = new Label(comp, SWT.NONE);
        parmLbl.setText("PARM:");

        Composite parmComp = new Composite(comp, SWT.NONE);
        parmComp.setLayout(layout1);

        parmCombo = new Combo(parmComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String st : getContourParms()) {
            parmCombo.add(st);
        }
        parmCombo.add(PgenConstant.EVENT_OTHER);
        parmCombo.select(0);

        parmCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateComboText(parmCombo, parmTxt, parmCombo.getText());
                updateLevelAndFcstHr();
                updateCintText();
            }
        });

        parmTxt = new Text(parmComp, SWT.SINGLE | SWT.BORDER);
        parmTxt.setLayoutData(new GridData(45, 15));
        parmTxt.setEditable(true);
        parmTxt.setText(parmCombo.getText());

        // Contours level 1
        Label levelLbl = new Label(comp, SWT.NONE);
        levelLbl.setText("Level 1:");

        Composite lvl1Comp = new Composite(comp, SWT.NONE);
        lvl1Comp.setLayout(layout1);

        levelCombo1 = new Combo(lvl1Comp, SWT.DROP_DOWN | SWT.READ_ONLY);

        List<String> levels = getContourLevels(getParm());
        for (String st : levels) {
            levelCombo1.add(st);
        }
        levelCombo1.add(PgenConstant.EVENT_OTHER);
        levelCombo1.select(0);
        levelCombo1.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateComboText(levelCombo1, levelValueTxt1,
                        levelCombo1.getText());
                updateCintText();
            }
        });

        levelValueTxt1 = new Text(lvl1Comp, SWT.SINGLE | SWT.BORDER);
        levelValueTxt1.setLayoutData(new GridData(45, 15));
        levelValueTxt1.setEditable(true);
        levelValueTxt1.setText(levelCombo1.getText());

        // Contours level 2
        Label levelLbl2 = new Label(comp, SWT.NONE);
        levelLbl2.setText("Level 2:");

        Composite lvl2Comp = new Composite(comp, SWT.NONE);
        lvl2Comp.setLayout(layout1);

        levelCombo2 = new Combo(lvl2Comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String st : levels) {
            levelCombo2.add(st);
        }
        levelCombo2.add(PgenConstant.EVENT_OTHER);
        levelCombo2.select(0);
        levelCombo2.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateComboText(levelCombo2, levelValueTxt2,
                        levelCombo2.getText());
            }
        });

        levelValueTxt2 = new Text(lvl2Comp, SWT.SINGLE | SWT.BORDER);
        levelValueTxt2.setLayoutData(new GridData(45, 15));
        levelValueTxt2.setEditable(true);
        levelValueTxt2.setText("");

        // Contours forecast hour
        Label fcsthrLbl = new Label(comp, SWT.NONE);
        fcsthrLbl.setText("Fcst Hour:");

        Composite fhrComp = new Composite(comp, SWT.NONE);
        fhrComp.setLayout(layout1);

        List<String> fhrs = getContourFcstHrs(getParm());
        fcsthrCombo = new Combo(fhrComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String st : fhrs) {
            fcsthrCombo.add(st);
        }
        fcsthrCombo.add(PgenConstant.EVENT_OTHER);
        fcsthrCombo.select(0);

        fcsthrCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateComboText(fcsthrCombo, fcsthrTxt, fcsthrCombo.getText());
            }
        });

        fcsthrTxt = new Text(fhrComp, SWT.SINGLE | SWT.BORDER);
        fcsthrTxt.setLayoutData(new GridData(45, 15));
        fcsthrTxt.setEditable(true);
        fcsthrTxt.setText(fcsthrCombo.getText());

        // Contours date/time 1
        Label dateLbl = new Label(comp, SWT.NONE);
        dateLbl.setText("Time 1:");

        Composite dtComp = new Composite(comp, SWT.NONE);
        dtComp.setLayout(layout1);

        date1 = new DateTime(dtComp, SWT.BORDER | SWT.DATE);

        time1 = new Text(dtComp, SWT.SINGLE);
        addTimeField(time1);

        // Contours date/time 2
        Label dateLbl2 = new Label(comp, SWT.NONE);
        dateLbl2.setText("Time 2:");

        Composite dtComp2 = new Composite(comp, SWT.NONE);
        dtComp2.setLayout(layout1);

        date2 = new DateTime(dtComp2, SWT.BORDER | SWT.DATE | SWT.TIME);

        time2 = new Text(dtComp2, SWT.SINGLE);
        addTimeField(time2);

        Label cintLbl = new Label(comp, SWT.NONE);
        cintLbl.setText("Cint:");

        // Contours intervals
        cintTxt = new Text(comp, SWT.SINGLE | SWT.BORDER);
        cintTxt.setLayoutData(new GridData(100, 15));
        cintTxt.setEditable(true);
        cintTxt.setText("");
        updateCintText();

        updateContourInfoSelection((IContours) contoursAttrDlg);

    }

    /**
     * Sets layout data and limit for the time fields. Adds UTC time validation
     * listeners.
     * 
     * @param timeField
     */
    private void addTimeField(final Text timeField) {
        timeField.setTextLimit(TIME_TEXT_LIMIT);
        timeField.setLayoutData(new GridData(TIME_FILED_WIDTH, -1));
        timeField.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent ve) {
                if (PgenUtil.validateDigitInput(ve)) {
                    ve.doit = true;
                } else {
                    ve.doit = false;
                    Display.getCurrent().beep();
                }
            }
        });
        timeField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!timeField.getText().isEmpty()) {
                    if (PgenUtil.validateUTCTime(timeField.getText())) {
                        timeField.setBackground(Display.getCurrent()
                                .getSystemColor(SWT.COLOR_WHITE));
                    } else {
                        timeField.setBackground(Display.getCurrent()
                                .getSystemColor(SWT.COLOR_RED));
                    }
                }
            }
        });
    }

    /**
     * Set the location for the dialog
     */
    public int open() {

        if (this.getShell() == null) {
            this.create();
        }

        Point pt = this.getShell().getParent().getLocation();

        this.getShell().setLocation(pt.x + 350, pt.y + 50);

        return super.open();

    }

    /**
     * get Contours parameter name
     */
    public String getParm() {

        String parm = parmTxt.getText();
        if (parm == null) {
            parm = "";
        }

        return parm;
    }

    /**
     * get level
     */
    public String getLevel() {

        String level = levelValueTxt1.getText();
        if (level == null) {
            level = "";
        }

        String level2 = levelValueTxt2.getText();
        if (level2 == null) {
            level2 = "";
        }

        if (level2.trim().length() > 0) {
            level = new String(level + ":" + level2);
        }

        return level;
    }

    /**
     * get Contours forecast hour
     */
    public String getForecastHour() {

        String hr = fcsthrTxt.getText();
        if (hr == null) {
            hr = "";
        }

        return hr;
    }

    /**
     * get cint
     */
    public String getCint() {
        return cintTxt.getText();
    }

    /**
     * get time
     */
    public Calendar getTime1() {

        Calendar myTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        myTime.set(date1.getYear(), date1.getMonth(), date1.getDay(), 0, 0, 0);
        setHourMinute(myTime, time1);

        return myTime;

    }

    /**
     * Sets the hour field and minute field of a calendar object from the input
     * text field.
     * 
     * @param cal
     *            - Calendar object
     * @param text
     *            - Time input field
     */
    private void setHourMinute(Calendar cal, Text text) {
        int hours = 0;
        int minutes = 0;

        try {
            hours = Integer.valueOf(text.getText().substring(0, 2));
            minutes = Integer.valueOf(text.getText().substring(2, 4));
        } catch (Exception e) {
        }

        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);

    }

    /**
     * set time
     */
    private void setTime1(Calendar time) {
        date1.setYear(time.get(Calendar.YEAR));
        date1.setMonth(time.get(Calendar.MONTH));
        date1.setDay(time.get(Calendar.DAY_OF_MONTH));
        time1.setText(String.format("%02d%02d", time.get(Calendar.HOUR_OF_DAY),
                time.get(Calendar.MINUTE)));
    }

    /**
     * get time
     */
    public Calendar getTime2() {

        Calendar myTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        myTime.set(date2.getYear(), date2.getMonth(), date2.getDay(), 0, 0, 0);
        setHourMinute(myTime, time2);

        return myTime;

    }

    /**
     * set time
     */
    private void setTime2(Calendar time) {
        if (time != null) {
            date2.setYear(time.get(Calendar.YEAR));
            date2.setMonth(time.get(Calendar.MONTH));
            date2.setDay(time.get(Calendar.DAY_OF_MONTH));
            time2.setText(String.format("%02d%02d",
                    time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));
        }
    }

    /**
     * set the associated ContoursAttrDlg.
     */
    public void setContoursAttrDlg(AttrDlg contoursAttrDlg) {
        this.contoursAttrDlg = contoursAttrDlg;
    }

    /**
     * get the associated ContoursAttrDlg.
     */
    public AttrDlg getContoursAttrDlg() {
        return contoursAttrDlg;
    }

    /**
     * update the attributes of the associated ContoursAttrDlg.
     */
    private void updateContoursAttrDlg() {

        if (contoursAttrDlg instanceof ContoursAttrDlg) {
            ((ContoursAttrDlg) contoursAttrDlg).setAttributes(this);
        } else if (contoursAttrDlg instanceof OutlookAttrDlg) {
            ((OutlookAttrDlg) contoursAttrDlg).setAttributes(this);
        }

    }

    /**
     * update the attribute selections in this dialog.
     */
    private void updateContourInfoSelection(IContours attr) {

        String prevParm = getParm();
        updateComboText(parmCombo, parmTxt, attr.getParm());
        if (!prevParm.equals(attr.getParm())) {
            updateLevelAndFcstHr();
        }

        String lvl = attr.getLevel();
        int spi = lvl.indexOf(":");
        String lvl1 = new String(lvl);
        if (spi > 0) {
            lvl1 = lvl.substring(0, spi);
        }

        updateComboText(levelCombo1, levelValueTxt1, lvl1);

        String lvl2 = new String("");
        if (spi > 0 && spi < lvl.length()) {
            lvl2 = lvl.substring(spi + 1, lvl.length());
        }

        updateComboText(levelCombo2, levelValueTxt2, lvl2);

        updateComboText(fcsthrCombo, fcsthrTxt, attr.getForecastHour());

        // Update cint if it is valid.
        if (attr.getCint() != null && attr.getCint().trim().length() > 0) {
            cintTxt.setText(attr.getCint());
        }

        setTime1(attr.getTime1());
        setTime2(attr.getTime2());

    }

    /**
     * Updates the attributes in the ContoursAttrDlg and closes this dialog.
     */
    public void okPressed() {
        updateContoursAttrDlg();
        super.okPressed();
    }

    /**
     * Reads a list of contours Info xml files by Parm #R8213
     * 
     * @return - list of contour information xml files
     */
    public static final List<String> readInfoFilelistTbl() {

        // reads and parses contoursInfo.xml file

        if (null == contoursInfoParamFilelist) {

            contoursInfoParamFilelist = new ArrayList<String>();

            try {

                String contoursInfoRoot = PgenStaticDataProvider.getProvider()
                        .getPgenLocalizationRoot() + "/contoursInfo.xml";

                String contoursInfoFile = PgenStaticDataProvider.getProvider()
                        .getFileAbsolutePath(contoursInfoRoot);

                // See if the contours info xml files are well-formed

                SAXParserFactory saxfactory = SAXParserFactory.newInstance();
                saxfactory.setValidating(false);
                saxfactory.setNamespaceAware(true);

                SAXParser cntrInfoparser = saxfactory.newSAXParser();

                XMLReader cntrInforeader = cntrInfoparser.getXMLReader();
                cntrInforeader.setErrorHandler(new SimpleHandler());
                InputSource cntrInfoSrc = new InputSource(contoursInfoFile);
                cntrInforeader.parse(cntrInfoSrc);

                cntrInfoSrc = null;

                String cntrInfoSrcRoot = "";

                if (null == cntrInfoManager) {
                    cntrInfoManager = new SingleTypeJAXBManager<ContourRoot>(
                            ContourRoot.class);
                }

                ContourRoot cntrInfoRoot = cntrInfoManager
                        .unmarshalFromXmlFile(contoursInfoFile);

                String cntrInfoFile = null;

                for (ContoursInfo cntrinfo : cntrInfoRoot.getCntrList()) {
                    if (null != cntrinfo.getName()
                            && !cntrinfo.getName().isEmpty()
                            && null != cntrinfo.getParm()
                            && !cntrinfo.getParm().isEmpty()
                            && "path".equals(cntrinfo.getParm())
                            && "cfiles".equals(cntrinfo.getName())) {

                        ContourFiles cntrfile = cntrinfo.getCfiles();

                        /*
                         * Checks for non-existing files and invalid files
                         */
                        if (null != cntrfile && cntrfile.getPaths() != null) {
                            for (String filepath : cntrfile.getPaths()) {
                                if (null != filepath && !filepath.isEmpty()) {

                                    cntrInfoSrcRoot = PgenStaticDataProvider
                                            .getProvider()
                                            .getPgenLocalizationRoot()
                                            + "/" + filepath;

                                    if (PgenStaticDataProvider.getProvider()
                                            .getFile(cntrInfoSrcRoot) != null) {
                                        cntrInfoFile = PgenStaticDataProvider
                                                .getProvider()
                                                .getFileAbsolutePath(
                                                        cntrInfoSrcRoot);
                                        cntrInfoSrc = new InputSource(
                                                cntrInfoFile);
                                        try {
                                            cntrInforeader
                                                    .setErrorHandler(null);
                                            cntrInforeader.parse(cntrInfoSrc);
                                            contoursInfoParamFilelist
                                                    .add(filepath);
                                        } catch (Exception e) {
                                            handler.handle(Priority.WARN,
                                                    "ContoursInfoDlg: Invalid contourInfo xml: "
                                                            + filepath);
                                        }

                                        cntrInfoSrc = null;
                                    }

                                    cntrInfoFile = null;
                                }
                            }
                        }
                    }
                }

                cntrInfoSrcRoot = null;
                cntrInfoSrc = null;
                cntrInfoparser = null;
                saxfactory = null;
                cntrInforeader = null;

                contoursInfoRoot = null;
                contoursInfoFile = null;

            } catch (Exception e) {

                handler.handle(
                        Priority.ERROR,
                        "ContoursInfoDlg: exception reading contourInfo xml in readInfoFilelistTbl .",
                        e);

            }

        } // contoursInfoParamFileList null.

        cntrInfoManager = null;

        return contoursInfoParamFilelist;

    }

    /**
     * Read contours information xml files by Parm
     * 
     * @return - list of contours info objects
     */
    public static final HashMap<String, ContoursInfo> readInfoTbl() {

        // Read contoursInfo.xml file and list the entries for individual
        // contoursInfo xml files in it

        if (null == contoursInfoTables) {

            contoursInfoTables = new HashMap<String, ContoursInfo>();

            if (null == contoursInfoParamFilelist) {

                contoursInfoParamFilelist = ContoursInfoDlg
                        .readInfoFilelistTbl();

                // Read from the contours info xml files in the list
                try {

                    if (null == cntrInfoManager) {
                        cntrInfoManager = new SingleTypeJAXBManager<ContourRoot>(
                                ContourRoot.class);
                    }

                    for (String path : contoursInfoParamFilelist) {

                        String cntrInfoParmFileRoot = PgenStaticDataProvider
                                .getProvider().getPgenLocalizationRoot()
                                + "/"
                                + path;

                        String cntrInfoParmFile = PgenStaticDataProvider
                                .getProvider().getFileAbsolutePath(
                                        cntrInfoParmFileRoot);

                        ContourRoot cntrInfoRoot = cntrInfoManager
                                .unmarshalFromXmlFile(cntrInfoParmFile);

                        List<ContoursInfo> cntrInfo = cntrInfoRoot
                                .getCntrList();

                        for (ContoursInfo cinfo : cntrInfo) {
                            if (null != cinfo.getName()
                                    && !cinfo.getName().isEmpty()
                                    && null != cinfo.getParm()
                                    && !cinfo.getParm().isEmpty()) {

                                contoursInfoTables.put(cinfo.getParm(), cinfo);
                            }
                        }

                    } // end foreach

                } catch (Exception e) {

                    handler.handle(
                            Priority.ERROR,
                            "ContoursInfoDlg: exception reading contourInfo xml in readInfoTbl .",
                            e);
                }

            } // contoursInfoParamFileList end .

        } // contoursInfoTables end .

        cntrInfoManager = null;

        return contoursInfoTables;

    }

    /**
     * Updates the Combo/text to a selected item.
     */
    private void updateComboText(Combo cmb, Text txt, String sel) {

        // Use the current selection on the Combo if no selection is provided.
        if (sel == null) {
            sel = cmb.getText();
        }

        // Update the Text.
        txt.setText(sel);

        // Update the Combo selection.
        int index = -1;
        boolean found = false;
        for (String str : cmb.getItems()) {
            if (str.equals(sel)) {
                found = true;
                break;
            }

            index++;
        }

        if (found) {
            cmb.select(index + 1);
            if (sel.equalsIgnoreCase(PgenConstant.EVENT_OTHER)) {
                txt.setText("");
                txt.setEnabled(true);
            } else {
                txt.setEnabled(false);
            }
        } else {
            cmb.select(cmb.getItemCount() - 1);
            txt.setEnabled(true);
        }

    }

    /**
     * Get a list of cint values for different parms and/or levels.
     * 
     * @param
     * @return LinkedHashMap<String, String>
     */
    public static LinkedHashMap<String, String> getCints() {

        LinkedHashMap<String, String> cInts = new LinkedHashMap<String, String>();

        readInfoTbl();

        Collection<ContoursInfo> cntrsInfoObjects = contoursInfoTables.values();

        for (ContoursInfo cntrsInfo : cntrsInfoObjects) {

            if (cntrsInfo.getLevels() != null) {
                List<ContourLevel> levels = cntrsInfo.getLevels();

                // Iterating through level elements
                for (ContourLevel lv : levels) {

                    String levelValue = lv.getValue();
                    String cint = lv.getCint();
                    String coord = lv.getCoord();
                    String ckey = "";

                    if (null != coord && 0 < coord.trim().length()) {
                        ckey += coord;
                        if (null != levelValue
                                && 0 < levelValue.trim().length()) {
                            ckey += "-" + levelValue;
                        }

                        if (null != cint && 0 < cint.trim().length()) {
                            cInts.put(ckey, cint);
                        }
                    }
                } // end foreach

            }

        } // end foreach

        return cInts;
    }

    /**
     * Updates the cint based on the selection of parm and/or level.
     * 
     * First try to find a value for "parm-level"; if not, try to find a value
     * for "parm".
     * 
     */
    private void updateCintText() {

        String parm = getParm();
        String levelValue = levelValueTxt1.getText();

        String key = "";
        if (parm != null && parm.trim().length() > 0) {
            key += parm;
            if (levelValue != null && levelValue.trim().length() > 0) {
                key += "-" + levelValue;
            }
        }

        String cint = getCints().get(key);
        if (cint != null && cint.trim().length() > 0) {
            cintTxt.setText(cint);
        }
    }

    /**
     * Get the list of contour parameters.
     * 
     * @return
     * 
     */
    public static List<String> getContourParms() {

        // Load "contourParameters" list from the contours info.
        readInfoTbl();

        if (contourParameters == null) {

            contourParameters = new ArrayList<String>();

            Set<String> contoursInfoTblKeys = contoursInfoTables.keySet();

            String[] keySetArray = contoursInfoTblKeys.toArray(new String[0]);

            List<String> keySetArrList = Arrays.asList(keySetArray);

            Collections.reverse(keySetArrList);

            contourParameters.addAll(keySetArrList);
        }

        return contourParameters;
    }

    /*
     * Get the list of contour levels for a contour parameter.
     * 
     * @param parm - name for contour parameter
     * 
     * @return - a list of levels
     */
    private static List<String> getContourLevels(String parm) {

        // Load "contourLevels" HashMap from all ContoursInfo.
        if (contourLevels == null) {

            contourLevels = new HashMap<String, List<String>>();

            readInfoTbl();

            Collection<ContoursInfo> cntrsInfoValues = contoursInfoTables
                    .values();

            for (ContoursInfo cntrsInfo : cntrsInfoValues) {
                List<String> retList = new ArrayList<String>();
                String level = "";
                if (cntrsInfo.getLevels() != null) {
                    List<ContourLevel> levels = cntrsInfo.getLevels();
                    for (ContourLevel lv : levels) {
                        level = lv.getValue();
                        if (level != null && !level.isEmpty()) {
                            retList.add(level);
                        }
                    }
                }

                contourLevels.put(cntrsInfo.getParm(), retList);
            }
        }

        // Return levels associated with the "parm", or an empty list.
        List<String> levels = contourLevels.get(parm);
        if (levels == null) {
            levels = new ArrayList<String>();
        }

        return levels;

    }

    /*
     * Get the list of forecast hours for a contour parameter.
     * 
     * @param parm - name for contour parameter
     * 
     * @return - a list of forecast hours
     */
    private static List<String> getContourFcstHrs(String parm) {

        // Load "contourFcstHours" HashMap from all ContoursInfo.
        if (contourFcstHours == null) {

            contourFcstHours = new HashMap<String, List<String>>();

            readInfoTbl();

            Collection<ContoursInfo> cntrsInfoValues = contoursInfoTables
                    .values();

            for (ContoursInfo cntrsInfo : cntrsInfoValues) {
                List<String> retList = new ArrayList<String>();
                String text = "";
                if (cntrsInfo.getFhrs() != null) {
                    FcstHrs fcsthrs = cntrsInfo.getFhrs();
                    List<ContourLabel> labels = fcsthrs.getClabels();
                    for (ContourLabel lbl : labels) {
                        text = lbl.getText();
                        if (text != null && !text.isEmpty()) {
                            retList.add(text);
                        }
                    }
                }

                contourFcstHours.put(cntrsInfo.getParm(), retList);
            }
        }

        // Return hours associated with the "parm" or an empty list.
        List<String> fcsthrs = contourFcstHours.get(parm);
        if (fcsthrs == null) {
            fcsthrs = new ArrayList<String>();
        }

        return fcsthrs;

    }

    /**
     * Get the default meta info for a contour parameter
     * 
     * @param parm
     *            - name for contour parameter
     * @return - a ContourDefault
     * 
     */
    public static ContourDefault getContourMetaDefault(String parm) {
        // Read the contours info xml files if needed.
        readInfoTbl();

        ContourDefault def = null;
        for (String parmKey : contoursInfoTables.keySet()) {
            if (parmKey.equals(parm)
                    && contoursInfoTables.get(parmKey).getDefaults() != null) {
                def = contoursInfoTables.get(parmKey).getDefaults();
            }
        }

        return def;
    }

    /*
     * Updates contour levels & forecast hours for a contour parameter.
     */
    private void updateLevelAndFcstHr() {

        String parm = getParm();

        // Levels
        List<String> levels = getContourLevels(parm);
        levelCombo1.removeAll();
        levelCombo2.removeAll();

        for (String lvl : levels) {
            levelCombo1.add(lvl);
            levelCombo2.add(lvl);
        }

        levelCombo1.add(PgenConstant.EVENT_OTHER);
        levelCombo2.add(PgenConstant.EVENT_OTHER);
        updateComboText(levelCombo1, levelValueTxt1, levelValueTxt1.getText());
        updateComboText(levelCombo2, levelValueTxt2, levelValueTxt2.getText());

        // Forecast hours
        List<String> fcsthrs = getContourFcstHrs(parm);
        fcsthrCombo.removeAll();

        for (String fhrs : fcsthrs) {
            fcsthrCombo.add(fhrs);
        }

        fcsthrCombo.add(PgenConstant.EVENT_OTHER);
        updateComboText(fcsthrCombo, fcsthrTxt, fcsthrTxt.getText());

    }
}