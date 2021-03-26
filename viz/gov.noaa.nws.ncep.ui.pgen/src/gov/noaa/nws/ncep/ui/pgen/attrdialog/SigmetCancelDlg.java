/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.SigmetInfo;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;
import java.util.TimeZone;

/**
 *
 * Dialog for displaying the Cancellation information of a selected active
 * Sigmet.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 01, 2020  78215    smanoj   Initial creation
 * Jun 16, 2020  79243    smanoj   Added Caribbean and South American FIRs.
 * Mar 15, 2021  88217    smanoj   Added capability to SAVE CANCEL file.
 * 
 * </pre>
 *
 * @author smanoj
 */
public class SigmetCancelDlg extends AttrDlg {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SigmetAttrDlg.class);

    private Sigmet sigmet;

    private String firID;

    private String area;

    private String qualifier;

    private String attrId;

    private String seqNum;

    private String startTime;

    private String endTime;

    private String currentTime;

    private String sigToCancel;

    private String firName;

    private String seriesName;

    private int seqNumInt;

    private int seriesNumber;

    private String seriesNum;

    private String correctionMsg;

    private String corrHeader;

    private boolean addCorrInfo = false;

    private String corrInfo;

    private Composite top;

    private Text cancelText;

    private boolean validAttr = true;

    private String errMsg = "All Valid....";

    private String qualType;

    private String selectedQualType;

    private static final int CHECK_ID = IDialogConstants.CLIENT_ID + 1;

    private static final int TEST_XML_ID = IDialogConstants.CLIENT_ID + 2;

    private static final int SEND_ID =IDialogConstants.CLIENT_ID + 3;

    private static final int SAVE_ID = IDialogConstants.OK_ID;

    private static final int CANCEL_ID = IDialogConstants.CANCEL_ID;

    private static final String INTL_SIGMET = "INTL_SIGMET";

    private static final String STATUS_CANCEL = "2";

    private SigmetAttrDlg parentDlg = null;

    public SigmetCancelDlg(SigmetAttrDlg parentDlg, Shell parShell, Sigmet sigmet) {
        super(parShell);
        this.sigmet = sigmet;
        this.parentDlg = parentDlg;
    }

    @Override
    public Control createDialogArea(Composite parent) {
        top = (Composite) super.createDialogArea(parent);

        GridLayout mainLayout = new GridLayout(8, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        this.getShell().setText("SIGMET Cancellation Information");

        firID = sigmet.getEditableAttrFir();

        area = sigmet.getEditableAttrArea();

        qualifier = sigmet.getEditableAttrPhenom();

        attrId = sigmet.getEditableAttrId();

        seqNum = sigmet.getEditableAttrSeqNum();

        startTime = sigmet.getEditableAttrStartTime();

        endTime = sigmet.getEditableAttrEndTime();

        sigToCancel = attrId + " " + seqNum + " - " + firID;

        seriesName = attrId;

        seqNumInt = Integer.parseInt(seqNum);

        if (sigmet.getEditableAttrStatus() != null) {
            if (STATUS_CANCEL.equals(sigmet.getEditableAttrStatus())) {
                // In the case of opening a cancelled SIGMET (existing *.xml)
                // so the Cancel Dialog is populated with the correct Series
                // Number.
                String fileName = parentDlg.drawingLayer.getActiveProduct()
                        .getInputFile();
                if (fileName != null && fileName.startsWith(INTL_SIGMET)
                        && fileName.endsWith(".xml")) {
                    char serNum = fileName.charAt(fileName.length() - 5);
                    seriesNumber = Integer.parseInt(Character.toString(serNum));
                }
            }
        } else {
            // Incremented series number to reference the SIGMET to be canceled.
            seriesNumber = seqNumInt + 1;
        }

        // Series Number for Cancel should be higher than Sequence Number
        if (seriesNumber <= seqNumInt) {
            seriesNumber = seqNumInt + 1;
        }

        seriesNum = Integer.toString(seriesNumber);

        firName = "";
        for (String s : SigmetInfo.FIR_ARRAY) {
            if (firID.contains(s.substring(0, 4))) {
                firName = s.substring(5, s.length());
            }
        }

        createFirRegionArea(top);

        createSeriesTimeWMOArea(top);

        createCorrectionMessageArea(top);

        createTypeQualArea(top);

        createCancelTextArea(top);

        return top;
    }

    private void createFirRegionArea(Composite topComposite) {
        Group firRegGrp = new Group(topComposite, SWT.LEFT);
        firRegGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        firRegGrp.setLayout(new GridLayout(8, false));
        firRegGrp.setText(SigmetConstant.FIR_REGION);

        Group firPacificGrp = new Group(firRegGrp, SWT.LEFT);
        firPacificGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        firPacificGrp.setLayout(new GridLayout(8, false));
        firPacificGrp.setText(SigmetConstant.PACIFIC);
        for (String s : SigmetInfo.FIR_PACIFIC) {
            final Button btn = new Button(firPacificGrp, SWT.CHECK);
            btn.setText(s);

            if (firID != null) {
                if (firID.contains(s)) {
                    btn.setSelection(true);
                }
            }
        }

        Group firAtlanticcGrp = new Group(firRegGrp, SWT.TOP);
        firAtlanticcGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        firAtlanticcGrp.setLayout(new GridLayout(8, false));
        firAtlanticcGrp.setText(SigmetConstant.ATLANTIC);
        for (String s : SigmetInfo.FIR_ATLANTIC) {
            final Button btn = new Button(firAtlanticcGrp, SWT.CHECK);
            btn.setText(s);
            if (firID != null) {
                if (firID.contains(s)) {
                    btn.setSelection(true);
                }
            }
        }
        
        Group firCarSAmericanGrp = new Group(firRegGrp, SWT.LEFT);
        firCarSAmericanGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        firCarSAmericanGrp.setLayout(new GridLayout(8, false));

        Group firMexicoGrp = new Group(firCarSAmericanGrp, SWT.LEFT);
        firMexicoGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        firMexicoGrp.setLayout(new GridLayout(8, false));
        firMexicoGrp.setText(SigmetConstant.MEXICO);
        for (String s : SigmetInfo.FIR_MEXICO) {
            final Button btn = new Button(firMexicoGrp, SWT.CHECK);
            btn.setText(s);
            if (firID != null) {
                if (firID.contains(s)) {
                    btn.setSelection(true);
                }
            }
        }
        
        Group firOtherGrp = new Group(firCarSAmericanGrp, SWT.TOP);
        firOtherGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        firOtherGrp.setLayout(new GridLayout(8, false));
        firOtherGrp.setText(SigmetConstant.OTHER_SITES);
        for (String s : SigmetInfo.FIR_OTHER) {
            final Button btn = new Button(firOtherGrp, SWT.CHECK);
            btn.setText(s);
            if (firID != null) {
                if (firID.contains(s)) {
                    btn.setSelection(true);
                }
            }
        }

    }

    private void createSeriesTimeWMOArea(Composite topComposite) {
        Group topGrp = new Group(topComposite, SWT.LEFT);
        topGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        topGrp.setLayout(new GridLayout(8, false));
        topGrp.setText("Series/Valid Time/ WMO Office");

        Group seriesGrp = new Group(topGrp, SWT.LEFT);
        seriesGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        seriesGrp.setLayout(new GridLayout(8, false));
        seriesGrp.setText("Series");

        Label lblName = new Label(seriesGrp, SWT.LEFT);
        lblName.setText("Name:");

        Text txtName = new Text(seriesGrp,
                SWT.LEFT | SWT.BORDER | SWT.READ_ONLY);
        txtName.setText(seriesName);

        Label lblNumber = new Label(seriesGrp, SWT.LEFT);
        lblNumber.setText("Number:");

        Spinner spiSeq = new Spinner(seriesGrp, SWT.BORDER);
        spiSeq.setMinimum(1);
        spiSeq.setMaximum(300);
        spiSeq.setSelection(Integer.parseInt(seriesNum));
        spiSeq.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                seriesNumber = spiSeq.getSelection();
                seriesNum = Integer.toString(seriesNumber);
                updateCancelText();
            }
        });

        Group timeGrp = new Group(topGrp, SWT.TOP);
        timeGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        timeGrp.setLayout(new GridLayout(8, false));
        timeGrp.setText("Valid Time");
        Text timeTxt = new Text(timeGrp,
                SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
        currentTime = getTimeStringPlusHourInHMS(0);
        timeTxt.setText(currentTime + "/" + endTime);

        Group wmoGrp = new Group(topGrp, SWT.TOP);
        wmoGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        wmoGrp.setLayout(new GridLayout(8, false));
        wmoGrp.setText("WMO Office");
        Text wmoTxt = new Text(wmoGrp,
                SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
        wmoTxt.setText(area);
    }

    private void createCorrectionMessageArea(Composite topComposite) {
        Group correctArea = new Group(topComposite, SWT.LEFT);
        correctArea.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        correctArea.setLayout(new GridLayout(8, false));
        correctArea.setText("Correction Message");

        final Button btn = new Button(correctArea, SWT.CHECK);
        btn.setText("Correction...");
        btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                corrHeader = "";
                addCorrInfo = false;
                if (btn.getSelection()) {
                    addCorrInfo = true;
                    corrHeader = area;
                }
                updateCancelText();
            }
        });

        int style = SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL;
        Text corrMessage = new Text(correctArea, style);
        GridData gData = new GridData(SWT.FILL, SWT.CENTER, true, true, 7, 1);
        gData.heightHint = 32;
        corrMessage.setLayoutData(gData);

        corrMessage.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event e) {
                correctionMsg = corrMessage.getText();
                updateCancelText();
            }
        });
    }

    private void createTypeQualArea(Composite topComposite) {
        Group typeQualGrp = new Group(topComposite, SWT.LEFT);
        typeQualGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        typeQualGrp.setLayout(new GridLayout(8, false));
        typeQualGrp.setText("SIGMET Type/Qualifier");

        Group typeGrp = new Group(typeQualGrp, SWT.LEFT);
        typeGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        typeGrp.setLayout(new GridLayout(8, false));
        typeGrp.setText("Type");
        final Button btn = new Button(typeGrp, SWT.RADIO);
        btn.setText("CNL");
        btn.setSelection(true);

        Group qualGrp = new Group(typeQualGrp, SWT.TOP);
        qualGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        qualGrp.setLayout(new GridLayout(8, false));
        qualGrp.setText("Qualifier");

        final Combo comboQual = new Combo(qualGrp, SWT.READ_ONLY);
        String[] qualArray = qualifier.split("_");
        comboQual.setItems(qualArray[0]);
        comboQual.select(0);
        qualType = qualArray[1];
        if (SigmetConstant.ASH.contains(qualType)) {
            qualType = SigmetConstant.VA;
        } else if (SigmetConstant.CYCLONE.contains(qualType)) {
            qualType = SigmetConstant.TC;
        } else if (SigmetConstant.CLD.contains(qualType)) {
            qualType = SigmetConstant.RC;
        }

        Group activeSigGrp = new Group(typeQualGrp, SWT.LEFT);
        activeSigGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        activeSigGrp.setLayout(new GridLayout(8, false));
        activeSigGrp.setText("Active Sigmet to Cancel");

        Group mapArea = new Group(activeSigGrp, SWT.LEFT);
        mapArea.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        mapArea.setLayout(new GridLayout(8, false));
        mapArea.setText("Include Hawaii or Alaska");

        final Button hawaiiBtn = new Button(mapArea, SWT.CHECK);
        hawaiiBtn.setText(SigmetConstant.HAWAII);

        final Button alaskaBtn = new Button(mapArea, SWT.CHECK);
        alaskaBtn.setText(SigmetConstant.ALASKA);

        if (area.contains(SigmetConstant.PHFO)) {
            hawaiiBtn.setSelection(true);
        } else if (area.contains(SigmetConstant.PAWU)) {
            alaskaBtn.setSelection(true);
        }

        final Button sigBtn = new Button(activeSigGrp, SWT.RADIO);
        sigBtn.setText(sigToCancel);
        sigBtn.setSelection(true);

        Group cnlInfoGrp = new Group(typeQualGrp, SWT.LEFT);
        cnlInfoGrp.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        cnlInfoGrp.setLayout(new GridLayout(8, false));
        cnlInfoGrp.setText("Cancellation Information");

        Group cnlTypeGrp = new Group(cnlInfoGrp, SWT.LEFT);
        cnlTypeGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        cnlTypeGrp.setLayout(new GridLayout(8, false));
        cnlTypeGrp.setText("Type");

        String[] typesArrray = new String[] { "TS/ICE/TURB/RC", "TC", "VA" };
        for (String s : typesArrray) {
            final Button typBtn = new Button(cnlTypeGrp, SWT.RADIO);
            typBtn.setText(s);
            if (s.contains(qualType)) {
                typBtn.setSelection(true);
                selectedQualType = qualType;
            }
            typBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    selectedQualType = typBtn.getText();
                }
            });
        }

        Group orgTimeGrp = new Group(cnlInfoGrp, SWT.TOP);
        orgTimeGrp.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
        orgTimeGrp.setLayout(new GridLayout(8, false));
        orgTimeGrp.setText("Original Valid Time");
        Text timeTxt = new Text(orgTimeGrp,
                SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
        timeTxt.setText(startTime + "/" + endTime);
    }

    private void createCancelTextArea(Composite topComposite) {
        cancelText = new Text(topComposite,
                SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
        cancelText.setText(getFileContent());
    }

    private boolean checkAttrValues() {
        validAttr = false;
        if (checkTime() && checkQualTypeValues()) {
            validAttr = true;
        }
        return validAttr;
    }

    private boolean checkTime() {
        boolean isValid = true;
        int timediff = (Integer.parseInt(endTime) - Integer.parseInt(startTime))
                / 100;
        if ((qualType.contains(SigmetConstant.VA))
                || (qualType.contains(SigmetConstant.TC))) {
            if (timediff < 6) {
                isValid = false;
                errMsg = "Time Difference should be 6 hours Tropical Cyclone and Volcanic Ash";
            }
        } else {
            if (timediff > 4) {
                isValid = false;
                errMsg = "Time Difference should be 4 hours for Thunderstorm, Icing and Radio Active Cloud";
            }
        }
        return isValid;
    }

    private boolean checkQualTypeValues() {
        boolean isValid = true;
        if (!selectedQualType.contains(qualType)) {
            errMsg = "When cancelling a saved Active Sigmet, Type should match";
            isValid = false;
        }
        return isValid;
    }

    private void updateCancelText() {
        cancelText.clearSelection();
        String updatedTxt = getFileContent();
        if (updatedTxt != null) {
            cancelText.setText(getFileContent());
        }
    }

    private String getFileContent() {
        StringBuilder sb = new StringBuilder();

        sb.append(getWmo());
        sb.append("\n");

        sb.append(getAfospil());
        sb.append("\n");

        sb.append(firID);
        sb.append(" ").append(SigmetConstant.SIGMET);
        sb.append(" ").append(attrId);
        sb.append(" ").append(seriesNumber);
        sb.append(" ").append(SigmetConstant.VALID).append(" ");
        sb.append(getTimeStringPlusHourInHMS(0)).append("/").append(endTime);
        sb.append(" ").append(area).append("-");
        sb.append("\n");

        sb.append(firName.replace('_', ' ')).append(" ")
                .append(SigmetConstant.FIR).append(" ");
        sb.append(qualifier.replace('_', ' '));
        sb.append(" ").append(SigmetConstant.CNL);

        sb.append(" ").append(SigmetConstant.SIGMET);
        sb.append(" ").append(attrId);
        sb.append(" ").append(seqNum);
        sb.append(" ").append(startTime).append("/").append(endTime);
        sb.append(".\n");

        if (addCorrInfo) {
            sb.append(corrHeader);
            if (correctionMsg != null) {
                sb.append(" ").append(correctionMsg);
            }
            corrInfo = corrHeader + " " + correctionMsg;
        }

        return sb.toString();
    }

    private String getTimeStringPlusHourInHMS(int plusHour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + plusHour);

        // round to 5 min
        c.set(Calendar.MINUTE, (c.get(Calendar.MINUTE) / 5) * 5);

        DateFormat dateFormat = new SimpleDateFormat("ddHHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(c.getTime());
    }

    @Override
    public void createButtonsForButtonBar(Composite parent) {
        createButton(parent, CHECK_ID, "CHECK", true);

        // TODO Keeping it disabled for now according to direction from site.
        // Will update as soon as we get more information from site.
        Button testXMLbtn = createButton(parent, TEST_XML_ID, "TEST XML", true);
        testXMLbtn.setEnabled(false);
        Button testSendbtn = createButton(parent, SEND_ID, "SEND", true);
        testSendbtn.setEnabled(false);

        createButton(parent, SAVE_ID, "SAVE", true);

        createButton(parent, CANCEL_ID, "CLOSE", true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);

        switch (buttonId) {
        case CHECK_ID:
            if (!checkAttrValues()) {
                (new SigmetCancelErrorDlg(getShell(), errMsg)).open();
                break;
            }
            break;

        case TEST_XML_ID:
            break;

        case SEND_ID:
            break;

        default:
            break;
        }
    }

    @Override
    public void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    public void okPressed() {
        //Invoke the same Save Dialog from the SigmetAttrDlg
        int buttonId= IDialogConstants.CLIENT_ID + 2;
        parentDlg.buttonPressed(buttonId);
        close();
    }

    @Override
    public void handleShellCloseEvent() {
        this.close();
    }

    @Override
    public void setAttrForDlg(IAttribute ia) {
    }

    public String getSeriesNumber() {
        return seriesNum;
    }

    public boolean getValidationCheck() {
        return validAttr;
    }

    public String getCorrInfo() {
        if (addCorrInfo) {
            return corrInfo;
        } else {
            return null;
        }
    }

    private String getWmo() {
        StringBuilder sb = new StringBuilder();
        sb.append("W");
        sb.append(getWmoPhen());
        sb.append(getOcnWmoAwpHeaders()[1]);
        sb.append(getInum());
        sb.append(" ").append(parentDlg.getEditableAttrArea());
        sb.append(" ").append(getTimeStringPlusHourInHMS(0));

        return sb.toString();
    }

    private String getAfospil() {
        StringBuilder sb = new StringBuilder();

        if ("PAWU".equals(parentDlg.getEditableAttrArea())) {
            sb.append(getAwpPhen());
            sb.append(getOcnWmoAwpHeaders()[2]);
            sb.append(getInum());
            sb.append("\n").append(getIdnode());
            sb.append(parentDlg.getEditableAttrId().charAt(0));
            sb.append(" WS ").append(getTimeStringPlusHourInHMS(0));
        } else {
            sb.append(getAwpPhen());
            sb.append(getOcnWmoAwpHeaders()[2]);
            sb.append(parentDlg.getEditableAttrId().substring(0, 1));
        }

        return sb.toString();
    }

    private String getWmoPhen() {
        String phen = parentDlg.getEditableAttrPhenom();
        if (phen != null) {
            phen = phen.trim();
        }
        if (PgenConstant.TYPE_VOLCANIC_ASH.equals(phen)) {
            return "V";
        }
        if (PgenConstant.TYPE_TROPICAL_CYCLONE.equals(phen)) {
            return "C";
        }
        return "S";
    }

    private String getAwpPhen() {
        String wmophen = getWmoPhen();
        if ("S".equals(wmophen)) {
            return "SIG";
        }
        if ("V".equals(wmophen)) {
            return "WSV";
        }
        return "WST";
    }

    private String getInum() {
        char firstIdChar = parentDlg.getEditableAttrId().charAt(0);

        if ("PHFO".equals(parentDlg.getEditableAttrArea())) {
            int inum = firstIdChar - 77;
            return inum < 0 || inum > 9 ? Integer.toString(inum) : "0" + inum;
        } else if ("PAWU".equals(parentDlg.getEditableAttrArea())) {
            int inum = firstIdChar - 72;
            return inum < 0 || inum > 9 ? Integer.toString(inum) : "0" + inum;
        }
        int inum = firstIdChar - 64;
        return inum < 0 || inum > 9 ? Integer.toString(inum) : "0" + inum;
    }

    private String getIdnode() {
        String area = parentDlg.getEditableAttrArea();

        if ("KKCI".equals(area)) {
            return "MKC";
        }
        if ("KNHC".equals(area)) {
            return "NHC";
        }
        if ("PHFO".equals(area)) {
            return "HFO";
        }
        // PAWU
        return "ANC";
    }

    private String[] getOcnWmoAwpHeaders() {
        String area = parentDlg.getEditableAttrArea();
        // 0:hdrocn, 1:hdrwmo, 2:hdrawp
        String[] headers = new String[3];
        headers[0] = headers[1] = headers[2] = "";

        if ("PAWU".equals(area)) {
            headers[1] = "AK";
            headers[2] = "AK";
        } else if ("PHFO".equals(area)) {
            headers[1] = "PA";
            headers[2] = "PA";
        } else {
            // area is KKCI or KNHC
            String fir = parentDlg.getFirs();
            if (!(fir == null || fir.length() == 0)) {
                if (fir.contains("KZHU") || fir.contains("KZMA")
                        || fir.contains("KZWY") || fir.contains("TJZS")) {
                    headers[0] = "NT";
                    headers[1] = "NT";
                    headers[2] = "A0";
                } else if (fir.contains("KZAK") || fir.contains("PAZA")) {
                    headers[0] = "PN";
                    headers[1] = "PN";
                    headers[2] = "P0";
                }
            }
        }

        return headers;
    }

    private class SigmetCancelErrorDlg extends AttrDlg {

        private Text txtError;

        private String errorMsg;

        SigmetCancelErrorDlg(Shell parShell, String errorMsg) {
            super(parShell);
            this.errorMsg = errorMsg;
        }

        @Override
        public Control createDialogArea(Composite parent) {
            Composite top = (Composite) super.createDialogArea(parent);
            GridLayout mainLayout = new GridLayout(1, false);
            mainLayout.marginHeight = 3;
            mainLayout.marginWidth = 3;
            top.setLayout(mainLayout);

            this.getShell().setText("Cancel Sigmet Validation Error");

            txtError = new Text(top,
                    SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
            txtError.setText(errorMsg);

            GC gc = new GC(txtError);
            int charWidth = gc.getFontMetrics().getAverageCharWidth();
            int charHeight = txtError.getLineHeight();
            Rectangle size = txtError.computeTrim(0, 0, charWidth * 70,
                    charHeight * 2);
            gc.dispose();
            txtError.setLayoutData(GridDataFactory.defaultsFor(txtError)
                    .span(3, 1).hint(size.width, size.height).create());

            return top;
        }

        @Override
        public void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, "Close", true);

        }

        @Override
        public void enableButtons() {
            this.getButton(IDialogConstants.OK_ID).setEnabled(true);
        }

        @Override
        public void okPressed() {
            setReturnCode(OK);
            close();

        }

        @Override
        public void handleShellCloseEvent() {
            this.close();
        }

        @Override
        public void setAttrForDlg(IAttribute ia) {
        }

    }
}