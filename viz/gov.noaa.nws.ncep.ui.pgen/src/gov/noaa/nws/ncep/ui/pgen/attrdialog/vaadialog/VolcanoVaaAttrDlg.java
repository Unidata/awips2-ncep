/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.vaaDialog
 *
 * Janurary 2010
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog.vaadialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.viz.core.exception.VizException;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.display.FillPatternList.FillPattern;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.file.ProductConverter;
import gov.noaa.nws.ncep.ui.pgen.file.Products;
import gov.noaa.nws.ncep.ui.pgen.sigmet.ISigmet;
import gov.noaa.nws.ncep.ui.pgen.sigmet.VaaInfo;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Volcano;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;

/**
 * The class for Volcano Attribute Dialog
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 2010      #165      G. Zhang     Initial creation
 * Apr 2011                B. Yin       Re-factor IAttribute
 * Jul 2011      #450      G. Hull      NcPathManager
 * Nov 2012      #889      B. Yin       Don't save XML file before saving text file.
 * Nov 2012      #890      B. Yin       Allow lower cases and numbers in the correction text field.
 * Mar 2013      #928      B. Yin       Made the button bar smaller.
 * Apr 2013      #977      S. Gilbert   PGEN Database support
 * Nov 2013      #1067     B. Yin       Check PGEN resource in the close method.
 * Mar 20, 2019  #7572     dgilling     Code cleanup.
 *
 * </pre>
 *
 * @author G. Zhang
 */
public class VolcanoVaaAttrDlg extends AttrDlg implements ISigmet {

    /**
     * singleton instance of this class
     */
    private static VolcanoVaaAttrDlg INSTANCE;

    /**
     * id for "Format VAA" button.
     */
    private static int FORMAT_ID = IDialogConstants.CLIENT_ID + 0;

    /**
     * id for "Reset" button.
     */
    private static int RESET_ID = IDialogConstants.CLIENT_ID + 1;

    private static final String PGEN_VAA_XSLT = "xslt" + File.separator + "vaa"
            + File.separator + "vaaXml2Txt.xslt";

    /**
     * top Composite for this dialog.
     */
    protected Composite top;

    /**
     * the Volcano this dialog representing for.
     */
    private Volcano volcano;

    /**
     * Volcano location text, depends on the Volcano
     */
    private Label lblLocText;

    /**
     * Volcano area text, depends on the Volcano
     */
    private Label lblAreaText;

    /**
     * Volcano location elevation, depends on the Volcano
     */
    private Label lblElevText;

    /**
     * Combo widget for VAAC station name.
     */
    private Combo comboStn;

    /**
     * Combo widget for VAAC station id.
     */
    private Combo comboId;

    /**
     * Combo widget for VAAC station header.
     */
    private Combo comboHdr;

    /**
     * Combo widget for the VAA product type.
     */
    private Combo comboType;

    /**
     * Text field for the eruption year.
     */
    private Text txtYear;

    /**
     * Text field for the eruption advisory no.
     */
    private Text txtAdNo;

    /**
     * Text field for the correction no.
     */
    private Text txtCorr;

    /**
     * Text field for the eruption info source.
     */
    private Text txtInfoSour;

    /**
     * Text field for the eruption addition source.
     */
    private Text txtAddInfoSour;

    /**
     * Text field for the eruption details.
     */
    private Text txtErup;

    /**
     * Text field for the eruption Date.
     */
    private Text txtAshDate;

    /**
     * Text field for the eruption time.
     */
    private Text txtAshTime;

    /**
     * Text field for the remarks for the eruption.
     */
    private Text txtRemark;

    /**
     * Combo widget for aviation color code
     */
    private Combo comboAviColoCode;

    /**
     * Combo widget for next advisory time.
     */
    private Combo comboNextAdv;

    /**
     * Combo widget for forecaster name.
     */
    private Combo comboForecaster;

    /**
     * flag indicating if it is from selection.
     */
    private boolean fromSelection = true;

    /**
     * Radio Button for NIL of Date/time.
     */
    private Button btnNil;

    /**
     * product types from this dialog: NORMAL, QUICK, etc
     */
    private String[] type = VaaInfo.ProductInfo.getProduct(VaaInfo.LOCS[0]);

    /**
     * Volcano elevation text.
     */
    private String elevFootMeterTxt = "";

    /**
     * GridData for layout of this dialog parts
     */
    private GridData singleTxtGridData = new GridData(58, 16);

    /**
     * helper class for verifying input texts.
     */
    private TxtVerifyListener tvl = new TxtVerifyListener();

    /**
     * Fhr 6 Ash Cloud info of user manual inputs
     */
    private String vacInfoFhr6;

    /**
     * Fhr 12 Ash Cloud info of user manual inputs
     */
    private String vacInfoFhr12;

    /**
     * Fhr 18 Ash Cloud info of user manual inputs
     */
    private String vacInfoFhr18;

    /**
     * constructor for this class.
     *
     * @param Shell
     *            : parent Shell for this dialog.
     * @throws VizException
     */
    public VolcanoVaaAttrDlg(Shell parShell) {
        super(parShell);
    }

    /**
     * singleton creation method for this class.
     *
     * @param Shell
     *            : parent Shell for this dialog.
     * @return
     */
    public static synchronized VolcanoVaaAttrDlg getInstance(Shell parShell) {
        if (INSTANCE == null) {
            INSTANCE = new VolcanoVaaAttrDlg(parShell);
        }
        return INSTANCE;
    }

    public Map<String, Object> getAttrFromDlg() {
        return new HashMap<>();
    }

    /**
     * when the attrDlg opens after selecting, the selected elem is NOT set yet.
     *
     * This method is called after it opened, so some fields can be set here.
     *
     * @param IAttribute
     *            : the element this dialog for.
     */
    @Override
    public void setAttrForDlg(IAttribute ia) {

        // ---title

        this.getShell().setText(getDlgTitle());

        // ---location, area, elevation

        lblLocText.setText(volcano.getTxtLoc());
        lblAreaText.setText(volcano.getArea());
        lblElevText.setText(volcano.getElev());// getElevText());

        // ---year, advisorNo, correction

        txtYear.setText(getYear());
        txtAdNo.setText(getAdvisoryNo());

        // ---product type: from Edit

        comboType.setEnabled(true);
        // in legacy, only type is NOT reflected 2010-03-25
        comboType.setText(volcano.getProduct());

        // --- set text fields

        txtInfoSour.setText(Volcano.getUserInputPart(volcano.getInfoSource()));// .getNoNullTxt(volcano.getInfoSource()));
        txtAddInfoSour
                .setText(volcano.getNoNullTxt(volcano.getAddInfoSource()));
        txtErup.setText(Volcano.getUserInputPart(volcano.getErupDetails()));// .getNoNullTxt(volcano.getErupDetails()));
        txtAshDate.setText(volcano.getNoNullTxt(volcano.getObsAshDate()));
        txtAshTime.setText(volcano.getNoNullTxt(volcano.getObsAshTime()));
        txtRemark.setText(Volcano.getUserInputPart(volcano.getRemarks()));// .getNoNullTxt(volcano.getRemarks()));

        // --- set combo fields

        setComboItem(comboForecaster, volcano.getForecasters(), true);
        setComboItem(comboNextAdv, volcano.getNextAdv(), false);

        // --- TODO: obs & fcst

    }

    public void setVolcano(DrawableElement de) {
        volcano = (Volcano) de;// 2010-03-25VaaInfo.getVolcano();//2010-03-17//(Volcano)de;
    }

    public Volcano getVolcano() {
        return volcano;
    }

    /**
     * button listener method overridden from super class.
     *
     * @param int: button id for buttons.
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (IDialogConstants.OK_ID == buttonId) {
            okPressed();
        } else if (IDialogConstants.CANCEL_ID == buttonId) {
            cancelPressed();
        } else if (FORMAT_ID == buttonId) {
            formatPressed();
        } else if (RESET_ID == buttonId) {
            resetPressed();
        }

    }

    /**
     * button listener helper for "Apply", overridden from super class.
     */
    @Override
    public void okPressed() {
        // TODO: undo/redo ???
        // --- 2010-03-23
        this.copyAttr2Vol();
    }

    /**
     * button listener helper for "Cancel", overridden from super class.
     */
    @Override
    public void cancelPressed() {
        if (drawingLayer == null) {
            setReturnCode(CANCEL);
            close();
        } else {
            super.cancelPressed();
        }
    }

    /**
     * button listener helper for "Format Vaa".
     */
    public void formatPressed() {
        copyAttr2Vol();

        SaveMsgDlg smDlg = SaveMsgDlg.getInstance(this.getParentShell());
        smDlg.setVolAttrDlgInstance(VolcanoVaaAttrDlg.INSTANCE);

        smDlg.setVolcano(volcano);

        String xsltFile = PgenStaticDataProvider.getProvider()
                .getFileAbsolutePath(
                        PgenStaticDataProvider.getProvider()
                                .getPgenLocalizationRoot() + PGEN_VAA_XSLT);

        List<Product> prds = new ArrayList<>();
        Product volProd = getVaaProduct(volcano);

        String xml = "";
        if (volProd != null) {
            prds.add(volProd);
            Products filePrds = ProductConverter.convert(prds);
            try {
                xml = SerializationUtil.marshalToXml(filePrds);
            } catch (JAXBException e) {
                statusHandler.debug(e.getLocalizedMessage(), e);
            }
        }

        String textMsg = convertXml2Txt(xml, xsltFile);
        smDlg.setTxtFileContent(textMsg);
        String name = smDlg.getFileName();

        smDlg.setBlockOnOpen(true);
        smDlg.open();

        if (smDlg.getReturnCode() == OK) {
            String dataURI = saveVolcano();
            if (dataURI != null) {
                try {
                    StorageUtils.storeDerivedProduct(dataURI, name, "TEXT",
                            PgenUtil.wrap(textMsg, 51, null, false));
                } catch (PgenStorageException e) {
                    StorageUtils.showError(e);
                    return;
                }
            }
        }
    }

    /**
     * button listener helper for "Reset".
     */
    public void resetPressed() {

        txtYear.setText("");
        txtAdNo.setText("");
        txtInfoSour.setText("");
        txtAddInfoSour.setText("");
        txtErup.setText("");
        txtAshDate.setText("");
        txtAshTime.setText("");
        txtRemark.setText("");

        comboForecaster.deselectAll();
        comboHdr.deselectAll();
        comboId.deselectAll();
        comboNextAdv.deselectAll();
        comboStn.deselectAll();

    }

    /**
     * method for creating the Button bar for this dialog; overridden form the
     * super class.
     *
     * @param Composite
     *            : parent of the dialog.
     */
    @Override
    public void createButtonsForButtonBar(Composite parent) {
        Button applyBtn = createButton(parent, IDialogConstants.OK_ID, "Apply",
                true);
        applyBtn.setEnabled(true);

        Button cancelBtn = createButton(parent, IDialogConstants.CANCEL_ID,
                "Cancel", true);
        cancelBtn.setEnabled(true);

        Button formatBtn = createButton(parent, FORMAT_ID, "Format VAA", true);
        formatBtn.setEnabled(true);

        Button resetBtn = createButton(parent, RESET_ID, "Reset Form", true);
        resetBtn.setEnabled(true);

        mouseHandlerName = null;
    }

    /**
     * method for creating the dialog area, overridden from the super class.
     *
     * @param Composite
     *            : parent Composite of the dialog.
     * @return:
     */
    @Override
    public Control createDialogArea(Composite parent) {
        this.top = (Composite) super.createDialogArea(parent);

        this.getShell().setText(getDlgTitle());

        GridLayout mainLayout = new GridLayout(8, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        Group top1 = new Group(top, SWT.LEFT);
        top1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top1.setLayout(new GridLayout(8, false));
        createArea1(top1);

        Group top2 = new Group(top, SWT.LEFT);
        top2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top2.setLayout(new GridLayout(8, false));
        createArea2(top2);

        Group top3 = new Group(top, SWT.LEFT);
        top3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top3.setLayout(new GridLayout(8, false));
        createArea3(top3);

        Group top4 = new Group(top, SWT.LEFT);
        top4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 8, 1));
        top4.setLayout(new GridLayout(8, false));
        createArea4(top4);

        return top;
    }

    /**
     * helper method for creating dialog area.
     *
     * @param Group
     *            : top container for the widgets.
     */
    private void createArea1(Group top) {
        Label lblLoc = new Label(top, SWT.LEFT);
        lblLoc.setText("Location: ");

        lblLocText = new Label(top, SWT.LEFT);
        lblLocText.setText(getLocText());

        Label lblArea = new Label(top, SWT.LEFT);
        lblArea.setText("Area: ");

        lblAreaText = new Label(top, SWT.LEFT);
        lblAreaText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 5, 1));
        lblAreaText.setText(getAreaText());

        Label lblElev = new Label(top, SWT.LEFT);
        lblElev.setText("Elevation: ");

        lblElevText = new Label(top, SWT.LEFT);
        lblElevText.setText(getElevText());
    }

    /**
     * helper method for creating dialog area.
     *
     * @param Group
     *            : top container for the widgets.
     */
    private void createArea2(Group top) {
        Label lblOrig = new Label(top, SWT.LEFT);
        lblOrig.setText("Orig Stn/VAAC: ");

        comboStn = new Combo(top, SWT.READ_ONLY);
        comboStn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
                5, 1));
        comboStn.setItems(this.getStnIdNumArray(null, null));
        comboStn.select(1);

        Label lblDummy1 = new Label(top, SWT.LEFT), lblDummy2 = new Label(top,
                SWT.LEFT);

        Label lblId = new Label(top, SWT.LEFT);
        lblId.setText("WMO ID: ");

        comboId = new Combo(top, SWT.READ_ONLY);
        comboId.setItems(this.getStnIdNumArray(true, comboStn.getText().trim()));
        comboId.select(0);

        Label lblHdr = new Label(top, SWT.LEFT);
        lblHdr.setText("Hdr Number: ");

        comboHdr = new Combo(top, SWT.READ_ONLY);
        comboHdr.setItems(this.getStnIdNumArray(false, comboStn.getText()
                .trim()));
        comboHdr.select(0);

        comboType = new Combo(top, SWT.READ_ONLY);
        comboType.setItems(type);
        comboType.select(0);
        comboType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true,
                false, 2, 1));
        comboType.setEnabled(isFromSelection());

        comboStn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String stn = comboStn.getText().trim();

                comboId.setItems(getStnIdNumArray(true, stn));
                comboId.select(0);

                comboHdr.setItems(getStnIdNumArray(false, stn));
                comboHdr.select(0);
            }
        });

    }

    /**
     * helper method for creating dialog area.
     *
     * @param Group
     *            : top container for the widgets.
     */
    private void createArea3(Group top) {

        Label lblYear = new Label(top, SWT.LEFT);
        lblYear.setText("Year: ");

        txtYear = new Text(top, SWT.LEFT | SWT.BORDER);
        txtYear.setLayoutData(singleTxtGridData);

        Label lblAdNo = new Label(top, SWT.LEFT);
        lblAdNo.setText("Advisory No: ");

        txtAdNo = new Text(top, SWT.LEFT | SWT.BORDER);
        txtAdNo.setLayoutData(singleTxtGridData);

        Button btnCorr = new Button(top, SWT.CHECK);
        btnCorr.setText("Correction: ");

        txtCorr = new Text(top, SWT.LEFT | SWT.BORDER);
        txtCorr.setEnabled(false);
        txtCorr.addVerifyListener(tvl);

        btnCorr.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                boolean flag = ((Button) e.widget).getSelection();
                if (!flag) {
                    String corr = txtCorr.getText();
                    if (corr != null && corr.length() > 0) {
                        setTxtChange(txtAdNo, true);

                        // listener removed to allow reset the field
                        txtCorr.removeVerifyListener(tvl);
                        txtCorr.setText("");
                        txtCorr.addVerifyListener(tvl);
                    }

                }
                txtCorr.setEnabled(flag);
            }
        });

        // always set year and advisory no
        txtYear.setText(getYear());
        txtAdNo.setText("001");
    }

    /**
     * helper method for creating dialog area.
     *
     * @param Group
     *            : top container for the widgets.
     */
    public void createArea4(Group top) {
        // ------------------ Info Source

        Button btnInfoSour = new Button(top, SWT.PUSH);
        btnInfoSour.setText("Information Source:");
        btnInfoSour.setLayoutData(new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING));

        txtInfoSour = new Text(top, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        GC gc = new GC(txtInfoSour);
        int charWidth = gc.getFontMetrics().getAverageCharWidth();
        int charHeight = txtInfoSour.getLineHeight();
        Rectangle size = txtInfoSour.computeTrim(0, 0, charWidth * 40,
                charHeight * 6);
        gc.dispose();
        GridData gData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gData.horizontalSpan = 7;
        gData.heightHint = size.height;
        gData.widthHint = size.width;
        txtInfoSour.setLayoutData(gData);
        // only get info from the dialog
        txtInfoSour.setEditable(false);

        btnInfoSour.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                AshCloudInfoSourceDlg isDlg = AshCloudInfoSourceDlg
                        .getInstance(VolcanoVaaAttrDlg.this.getParentShell());

                isDlg.open();
            }
        });

        // ------------------ Addition

        Label lblAddInfoSour = new Label(top, SWT.LEFT);
        lblAddInfoSour.setText("Add'l Info Source: ");
        lblAddInfoSour.setLayoutData(new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING));

        txtAddInfoSour = new Text(top, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        gData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gData.horizontalSpan = 7;
        gData.heightHint = size.height;
        gData.widthHint = size.width;
        txtAddInfoSour.setLayoutData(gData);
        txtAddInfoSour.addModifyListener(new TxtModifyListener());

        // ------------------ Aviation

        Label lblAviColoCode = new Label(top, SWT.LEFT);
        lblAviColoCode.setText("Aviation Color Code: ");

        comboAviColoCode = new Combo(top, SWT.READ_ONLY);
        comboAviColoCode.setItems(getAviColoCode());
        comboAviColoCode.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 7, 1));
        // TODO: ONLY for Washington VAAC ???
        comboAviColoCode.setEnabled(false);

        // ------------------ Eruption

        Label lblErup = new Label(top, SWT.LEFT);
        lblErup.setText("Eruption Details: ");
        lblErup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        txtErup = new Text(top, SWT.BORDER | SWT.MULTI);
        gData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gData.horizontalSpan = 7;
        gData.heightHint = size.height;
        gData.widthHint = size.width;
        txtErup.setLayoutData(gData);
        txtErup.addModifyListener(new TxtModifyListener());

        // ------------------ Ash Date / Time

        Label lblAshDate = new Label(top, SWT.LEFT);
        lblAshDate.setText("Obs Ash Date(DD): ");

        txtAshDate = new Text(top, SWT.BORDER);
        txtAshDate.setLayoutData(singleTxtGridData);

        Label lblAshTime = new Label(top, SWT.LEFT);
        lblAshTime.setText("Time(HHHH):");

        txtAshTime = new Text(top, SWT.BORDER);
        txtAshTime.setLayoutData(singleTxtGridData);

        Label lblZ = new Label(top, SWT.LEFT);
        lblZ.setText("Z");

        btnNil = new Button(top, SWT.CHECK);
        btnNil.setText("NIL");
        btnNil.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                String nil = volcano.NIL_STRING, clear = "";

                if (((Button) e.widget).getSelection()) {
                    txtAshDate.setText(nil);
                    txtAshTime.setText(nil);
                    txtAshDate.setEditable(false);
                    txtAshTime.setEditable(false);
                } else {
                    txtAshDate.setText(clear);
                    txtAshTime.setText(clear);
                    txtAshDate.setEditable(true);
                    txtAshTime.setEditable(true);
                }
            }
        });

        Label lblDummyNil = new Label(top, SWT.LEFT), lblDummyNil2 = new Label(
                top, SWT.LEFT);

        // ------------------ Cloud Info

        Button btnCloudInfo = new Button(top, SWT.PUSH);
        btnCloudInfo.setText("Observed and Forecast Ash Cloud Inormation...");
        btnCloudInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 8, 1));
        btnCloudInfo.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {

                AshCloudInfoDlg aciDlg = AshCloudInfoDlg
                        .getInstance(VolcanoVaaAttrDlg.this.getParentShell());

                aciDlg.setDateTimeForDlg(txtAshDate.getText().trim(),
                        txtAshTime.getText().trim());
                aciDlg.setVaaAttrDlg(VolcanoVaaAttrDlg.this);
                aciDlg.open();
                // TODO: check vacList, get FHR00/06/12/18 info, then put in
                // related fields of aciDlg
            }
        });
        // ------------------ Remarks

        Label lblRemarks = new Label(top, SWT.LEFT);
        lblRemarks.setText("Remarks: ");
        lblRemarks
                .setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        txtRemark = new Text(top, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        gData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gData.horizontalSpan = 7;
        gData.heightHint = size.height;
        gData.widthHint = size.width;
        txtRemark.setLayoutData(gData);
        txtRemark.addModifyListener(new TxtModifyListener());

        // ------------------ Advisory

        Label lblNextAdv = new Label(top, SWT.LEFT);
        lblNextAdv.setText("Next Advisory: ");

        comboNextAdv = new Combo(top, SWT.DROP_DOWN);
        comboNextAdv.setItems(getNextAdvText());
        comboNextAdv.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 7, 1));
        comboNextAdv.addModifyListener(new TxtModifyListener());

        // ------------------ Forecasters
        Label lblForecaster = new Label(top, SWT.LEFT);
        lblForecaster.setText("Forecaster(s): ");

        comboForecaster = new Combo(top, SWT.READ_ONLY | SWT.DROP_DOWN);
        comboForecaster.setItems(getForecastersName());
        comboForecaster.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 7, 1));

        // --- set the contents after finishing layout
        // --- TEST/RESUME have rmks without a volcano
        txtRemark.setText(getRemarks());

        /*
         * TEST/RESUME/BACKUP: set the Date/Time to NIL
         */

        if (VaaInfo.isNonDrawableVol(volcano)) {
            btnNil.setSelection(true);
            txtAshDate.setText(volcano.NIL_STRING);
            txtAshTime.setText(volcano.NIL_STRING);
            txtAshDate.setEditable(false);
            txtAshTime.setEditable(false);
        }

    }

    /**
     * getter for the dialog's title, depending on the Volcano this dialog
     * representing for.
     *
     * @return String: this dialog's title.
     */
    private String getDlgTitle() {
        StringBuilder sb = new StringBuilder("VAA - ");

        String num = volcano.getNumber();
        sb.append(volcano.getName() + "(" + (num == null ? "" : num) + ")");

        return sb.toString().toUpperCase();
    }

    private String getLocText() {
        // initial value, to be replaced by setAttrForDlg(IAttribute)
        return volcano.getTxtLoc();// "N9999W99999";
    }

    private String getAreaText() {

        return volcano.getArea();// "TEST";
    }

    /**
     * getter for Volcano elevation with foot or meter.
     *
     * @return String: text of elevation of the Volcano.
     */
    private String getElevText() {
        if (volcano == null) {
            return "";
        }
        // return volcano.getElev();/*---2010-03-26 move to VolcanoCreateDlg
        // TODO: clean up code
        StringBuilder sb = new StringBuilder();

        int dMeter = 0;
        String sMeter = volcano.getElev();
        /*
         * try{ dMeter = Double.parseDouble(sMeter); }catch(Exception e){ }
         * String feet = VaaInfo.getFootTxtFromMeter(dMeter,0);
         */
        if (sMeter == null || sMeter.length() == 0) {
            sb.append(dMeter).append("  ft (").append(dMeter).append("  m)");
            // reset elev. for Txt Msg
            volcano.setElev(sb.toString());
        }
        elevFootMeterTxt = volcano.getElev();
        // TODO: elevFootMeterTxt needs to be assigned a value ??
        return elevFootMeterTxt;
    }

    public String getElevFootMeterTxt() {
        return elevFootMeterTxt;
    }

    /**
     * Function for get Stn/VAAC, WMO ID, and Hdr No.
     *
     * @param isId
     *            , a Boolean used as a 3-way flag: null:Stn; true:ID; false:Hdr
     *            No.
     * @param stn
     *            , a String as the key to STN map.
     */
    public static String[] getStnIdNumArray(Boolean isId, String stn) {
        Map<String, String[]> map = VaaInfo.VAA_INFO_STN_MAP;

        if (isId == null) {
            return map.keySet().toArray(new String[] {});
        } else if (isId) {
            return stn == null ? new String[] {} : map.get(stn)[0].substring(1)
                    .split(";");
        } else {
            return stn == null ? new String[] {} : map.get(stn)[1].substring(1)
                    .split(";");
        }
    }

    /**
     * static method using VaaInfo maps for info source.
     *
     * @return String[]: info source items.
     */
    public static String[] getInfoSourItems() {
        String[] s = VaaInfo.VAA_INFO_SINGLE_MAP.get("information-source");

        return s;
    }

    /**
     * static method using VaaInfo maps for forecaster names.
     *
     * @return String[]: forecaster names.
     */
    public static String[] getForecastersName() {
        String[] s = VaaInfo.VAA_INFO_SINGLE_MAP.get("forecasters");

        return s;
    }

    /**
     * static method using VaaInfo maps for aviation color codes.
     *
     * @return String[]: aviation color codes.
     */
    public static String[] getAviColoCode() {
        String[] s = VaaInfo.VAA_INFO_SINGLE_MAP.get("aviation-color-code");

        return s;
    }

    /**
     * static method using VaaInfo maps for next advisory.
     *
     * @return String[]: next advisory items.
     */
    public static String[] getNextAdvText() {
        String[] s = VaaInfo.VAA_INFO_SINGLE_MAP.get("next-advisory");

        return s;
    }

    /**
     * getter for remarks for the Volcano; some with hard-wired text, others
     * user input.
     *
     * @return String: remarks in text.
     */
    public String getRemarks() {
        String rmk = Volcano.getNoNullTxt(volcano.getRemarks());
        String[] words = rmk.split(Volcano.WORD_SPLITTER);
        if (VaaInfo.isNonDrawableVol(volcano) && words.length > 1) {
            // TEST, RESUME, BACKUP
            return words[1];
        }
        return words[0];
    }

    /**
     * getter for current year.
     *
     * @return String: current year text.
     */
    public String getYear() {
        return Integer.toString(LocalDate.now().getYear());
    }

    /**
     * The Advisory No for ALL TEST/RESUME: 001
     *
     * for others, it's 1 + the advisory no of the latest text product of the
     * same volcano.
     *
     * 1. Get the latest text product of the volcano (use the file name:
     * zao_20100219_1741.txt
     *
     * 2. If no such file exist use 000. else Get the advisory no
     *
     * 3. return the advisory no + 1
     *
     * NOTE: 2010-02-19 NO txt products any more! xslt will be used to translate
     * the element xml file to required format.
     */
    public String getAdvisoryNo() {
        // TODO: see @5418 of nmap_pgvolw.c

        if (VaaInfo.isNonDrawableVol(volcano)) {
            return "001";
        }

        String no = VaaInfo.getLatestAdvNo(volcano.getName());
        int noInt = Integer.parseInt(no) + 1;

        return new DecimalFormat("000").format(noInt);
    }

    public boolean isFromSelection() {
        return fromSelection;
    }

    public void setFromSelection(boolean fromSelection) {
        this.fromSelection = fromSelection;
    }

    /**
     * method for copying dialog attributes to the Volcano.
     */
    public void copyAttrToVolcano() {

        volcano.setOrigStnVAAC(comboStn.getText());
        volcano.setWmoId(comboId.getText());
        volcano.setHdrNum(comboHdr.getText());
        if (this.fromSelection) {
            // TEST/RESUME can be set already
            volcano.setProduct(comboType.getText());
        }

        volcano.setYear(txtYear.getText());
        volcano.setAdvNum(txtAdNo.getText());
        volcano.setCorr(txtCorr.getText());

        volcano.setInfoSource(this.txtInfoSour.getText());

        volcano.setAddInfoSource(getTxtNoRsrvWord(txtAddInfoSour.getText()));
        volcano.setAviColorCode(comboAviColoCode.getText());
        volcano.setErupDetails(getTxtNoRsrvWord(txtErup.getText()));

        volcano.setObsAshDate(txtAshDate.getText());
        volcano.setObsAshTime(txtAshTime.getText());
        volcano.setNil("" + btnNil.isEnabled());

        // TODO: obs & fcst 00,6,12,18
        volcano.setObsFcstAshCloudInfo(VaaInfo.getAshCloudInfo("00")); // VaaInfo.LAYERS[1]
                                                                       // "OBS"
                                                                       // NOT
                                                                       // F00
        String[] fhrDT = VaaInfo.getFhrTimes(txtAshDate.getText().trim(),
                txtAshTime.getText().trim());
        volcano.setObsFcstAshCloudInfo6(getVacInfoFhr6(fhrDT));// VaaInfo.getAshCloudInfo("06"));
        volcano.setObsFcstAshCloudInfo12(getVacInfoFhr12(fhrDT));// VaaInfo.getAshCloudInfo("12"));
        volcano.setObsFcstAshCloudInfo18(getVacInfoFhr18(fhrDT));// VaaInfo.getAshCloudInfo("18"));

        // volcano.setObsFcstAshCloudInfo()

        volcano.setRemarks(getTxtNoRsrvWord(txtRemark.getText()));
        volcano.setNextAdv(getTxtNoRsrvWord(getNxtAdvTxt(comboNextAdv.getText())));
        volcano.setForecasters(comboForecaster.getText());

    }

    /**
     * set the Volcano element's fields with different types:
     *
     * TEST/RESUME, NORMAL, END/QUICK/NEAR
     *
     */
    private void copyAttr2Vol() {

        copyFixedAttr2Vol();

        if (VaaInfo.isNonDrawableVol(volcano)) {
            /*
             * for TEST/RESUME, all fields set already in VolcanoCreateDlg btnGo
             * listener and copyFixedAttr2Vol()
             */
            return;

        } else {

            copyAttrToVolcano();

            /*
             * reset certain fields using values from the Vaa.xml file
             */
            VaaInfo.setVolcanoFields(volcano, volcano.getProduct(), true);

        }

    }

    /**
     * these attributes are needed for all product types
     */
    private void copyFixedAttr2Vol() {

        volcano.setWmoId(comboId.getText());
        volcano.setHdrNum(comboHdr.getText());
        volcano.setOrigStnVAAC(comboStn.getText());
        volcano.setCorr(txtCorr.getText());

        if (StringUtil.isEmptyString(volcano.getProduct())) {
            // TEST/RESUME can be set already
            volcano.setProduct(comboType.getText());
        }

        if ("BACKUP".equals(volcano.getProduct())) {
            volcano.setRemarks(getTxtNoRsrvWord(txtRemark.getText()));
        }
    }

    /**
     * set info source Text field.
     *
     * @param String
     *            []: infor source items.
     */
    public void setInfoSource(String[] source) {

        if (source == null || source.length == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (String s : source) {
            sb.append(s).append(". ");
        }
        // set focus, or some lines will NOT display completely. TODO:
        // investigate this
        txtInfoSour.setFocus();

        txtInfoSour.setText(sb.toString());
    }

    /**
     * control a Text field's number increment/decrement by 1
     *
     * @param txtAdNo
     *            : the field that the number in it changes
     * @param increFlag
     *            : increment true, decrement false.
     */
    private void setTxtChange(Text txtAdNo, boolean increFlag) {
        if (VaaInfo.isNonDrawableVol(volcano)) {
            return;
        }

        if (txtAdNo == null || txtAdNo.isDisposed()) {
            return;
        }

        String adNo = txtAdNo.getText();
        if (adNo == null || adNo.length() == 0) {
            return;
        }

        int ano = 0, orig = 0;
        try {
            ano = Integer.parseInt(txtAdNo.getText());
            orig = Integer.parseInt(getAdvisoryNo());
        } catch (Exception e) {
            statusHandler.debug(e.getLocalizedMessage(), e);
            return;
        }

        // ano > 1
        if (orig > 1) {
            if (increFlag) {
                ano++;
            } else {
                ano--;
            }

            txtAdNo.setText(new DecimalFormat("000").format(ano));
        }
    }

    /**
     * This class verifys the txtCorr content
     *
     * @author gzhang
     */
    private class TxtVerifyListener implements VerifyListener {

        /**
         * only A-Z allowed for Correction field
         */
        // TTR632: Numbers and lower cases are also allowed cor correction field

        @Override
        public void verifyText(VerifyEvent event) {
            // Assume we don't allow it
            event.doit = false;

            // Get the character typed
            int myChar = event.character;
            String text = ((Text) event.widget).getText();

            // Allow A-Z as first char and decrement advNo
            // Allow a-z and numbers for TTR632
            if (((myChar >= 65 && myChar <= 90)
                    || (myChar >= 97 && myChar <= 122) || Character
                        .isDigit(myChar)) && text.length() == 0) {
                event.doit = true;
                setTxtChange(txtAdNo, false);
            }

            // 0-9? Grace Swanson: ONLY Upper Case letters
            // //if(Character.isDigit(myChar)&&text.length()==0) event.doit =
            // true;

            // Backspace: 8 and delete: 127 and increment advNo
            if ((myChar == 8 || myChar == 127) && text.length() == 1) {
                event.doit = true;
                setTxtChange(txtAdNo, true);
            }

        }
    }

    /**
     * a safe way setting text for Combo Widget
     *
     * @param Combo
     *            Widget
     * @param text
     *            to be with the Combo
     * @param isReadOnly
     *            flag
     */
    public void setComboItem(Combo c, String s, boolean isReadOnly) {
        if (isReadOnly && Arrays.asList(c.getItems()).contains(s)) {
            c.setText(s);
        }
        if (!isReadOnly && s != null && s.length() > 0) {
            c.setText(Volcano.getUserInputPart(s));
        }

    }

    /**
     * a null field means AshCloudInfoDlg not opened so set it with drawing
     * layer's clouds info same for fhr12/18
     */
    public String getVacInfoFhr6(String[] fhrDT) {
        return vacInfoFhr6 == null ? fhrDT[0] + "  "
                + VaaInfo.getAshCloudInfo(VaaInfo.LAYERS[2]) : vacInfoFhr6;
    }

    public void setVacInfoFhr6(String vacInfoFhr6) {
        this.vacInfoFhr6 = vacInfoFhr6;
    }

    public String getVacInfoFhr12(String[] fhrDT) {
        return vacInfoFhr12 == null ? fhrDT[1] + "  "
                + VaaInfo.getAshCloudInfo(VaaInfo.LAYERS[3]) : vacInfoFhr12;
    }

    public void setVacInfoFhr12(String vacInfoFhr12) {
        this.vacInfoFhr12 = vacInfoFhr12;
    }

    public String getVacInfoFhr18(String[] fhrDT) {
        return vacInfoFhr18 == null ? fhrDT[2] + "  "
                + VaaInfo.getAshCloudInfo(VaaInfo.LAYERS[4]) : vacInfoFhr18;
    }

    public void setVacInfoFhr18(String vacInfoFhr18) {
        this.vacInfoFhr18 = vacInfoFhr18;
    }

    /**
     * 2010-04-07/12 it has been decided to save Volcano into XML files when
     * Format VAA is clicked, then using XSLT to transform the XML into text
     *
     * @return Product of Volcanos and VolcanoAshClouds
     */
    private Product getVaaProduct(Volcano vol) {
        return vol != null ? VaaInfo.VOL_PROD_MAP.get(vol) : null;// volProd;
    }

    /**
     * getter for the to be saved file name.
     *
     * @return String: file name
     */
    private String getFileName() {
        String connector = "_";

        StringBuilder sb = new StringBuilder();
        sb.append(volcano.getName());
        sb.append(connector);
        sb.append(VaaInfo.getDateTime("yyyyMMdd"));
        sb.append(connector);
        sb.append(VaaInfo.getDateTime("HHmm"));

        sb.append(".xml");
        return sb.toString();
    }

    /**
     * method for saving the Volcano in xml file. *
     *
     * @return boolean: flag indicating if saving successful.
     */
    private String saveVolcano() {

        String dataURI;
        String label = getFileName();

        Product volProd = getVaaProduct(volcano);

        if (volProd != null) {
            volProd.setOutputFile(label);
            volProd.setName(volcano.getName());
            volProd.setForecaster(volcano.getForecasters());
            try {
                dataURI = StorageUtils.storeProduct(volProd);
            } catch (PgenStorageException e) {
                StorageUtils.showError(e);
                return null;
            }
            return dataURI;
        }

        return null;
    }

    /***
     * Creates a formatted string comprising of the contents of the XML string,
     * to which formatting information is applied from the style-sheet.
     *
     * @param xmlString
     *            - XML string
     * @param xltFileName
     *            - Name of the style-sheet
     * @return A <tt>String</tt> with the formatted contents of the XML file.
     */
    private String convertXml2Txt(String xmlString, String xltFileName) {
        String res = "";

        Source xmlSource = new StreamSource(new StringReader(xmlString));
        Source xsltSource = new StreamSource(xltFileName);

        TransformerFactory transFact = TransformerFactory.newInstance();

        try {
            Transformer trans = transFact.newTransformer(xsltSource);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            trans.transform(xmlSource, new StreamResult(baos));

            res = new String(baos.toByteArray());
        } catch (TransformerException e) {
            statusHandler.warn("Error: File is corrupt.", e);
        }

        return getNormalizedTxt(res);
    }

    /**
     * use the current time for Next Advisory if it requires time and Not
     * entered by the user.
     *
     * @param String
     *            : Next Advisory String
     * @return String: String containing time if needed.
     */
    private String getNxtAdvTxt(String nxtAdv) {
        // TODO: replace the hard-code
        String word = "YYYYMMMDD/HHNNZ";

        if (nxtAdv != null && nxtAdv.trim().length() > 0
                && nxtAdv.contains(word)) {
            String dateTimeStr = VaaInfo.getDateTime("yyyyMMdd/HHmmZ");
            return nxtAdv.replaceFirst(word, dateTimeStr);
        }

        return nxtAdv;
    }

    /**
     * Prohibiting use of Volcano.WORD_SPLITTER in some Text fields from user
     * inputs.
     *
     * @author gzhang
     *
     */
    private class TxtModifyListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent e) {
            String txt = "";
            if (e.widget instanceof Text) {
                txt = ((Text) e.widget).getText();
            } else if (e.widget instanceof Combo) {
                txt = ((Combo) e.widget).getText();
            }

            if (txt != null && txt.length() > 0
                    && txt.contains(Volcano.WORD_SPLITTER)) {

                MessageDialog confirmDlg = new MessageDialog(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell(),
                        "Warning",
                        null,
                        " ::: IS A RESERVED WORD AND WILL BE REMOVED FROM THE TEXT!",
                        MessageDialog.WARNING, new String[] { "OK" }, 0);

                confirmDlg.open();
            }
        }
    }

    /**
     * take off the reserved word from the text.
     *
     * @param String
     *            : to be checked with reserved word.
     * @return String: empty text or text without reserved words.
     */
    private String getTxtNoRsrvWord(String txt) {
        if (StringUtil.isEmptyString(txt)) {
            return "";
        }

        if (txt.contains(Volcano.WORD_SPLITTER)) {
            return txt.replaceAll(Volcano.WORD_SPLITTER, "");
        }

        return txt;
    }

    /**
     * 20100818 workshop issue: PGEN text NOT to be added to the final text
     * product. *
     */
    private String getNormalizedTxt(String s) {
        StringBuilder sb = new StringBuilder("");

        String[] txt = s.split("\n");
        // VOLCANO Layer text added on top, others bottom
        int start = 0, end = 0;
        for (int i = 0; i < txt.length; i++) {
            // TODO: remove the hard coded words, see vaaAllHeader.xslt
            if (txt.length > 3) {
                if (txt[i].startsWith("FV")
                        && txt[i + 1].startsWith("VA ADVISORY")
                        && txt[i + 2].startsWith("DTG")) {

                    start = i;
                }

                if ("NNNN".equals(txt[i]) && (i > start)) {
                    end = i;
                    break;
                }
            }
        }

        for (int j = start; j <= end; j++) {
            sb.append(txt[j]).append("\n");
        }

        return sb.toString();
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
    public Boolean isFilled() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FillPattern getFillPattern() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLineType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getWidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean close() {
        if ( drawingLayer != null ) {
            drawingLayer.removeSelected();
        }
        SaveMsgDlg.getInstance(this.getParentShell()).close();
        return super.close();
    }
}
