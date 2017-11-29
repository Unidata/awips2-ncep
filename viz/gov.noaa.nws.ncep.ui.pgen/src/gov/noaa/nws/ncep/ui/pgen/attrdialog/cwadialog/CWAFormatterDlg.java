/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.SigmetCommAttrDlg;

/**
 * This class displays the CWA formatter dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#  Engineer    Description
 * ----------- -------- ----------- --------------------------
 * 11/21/2016  17469    wkwock      Initial creation
 * 
 * </pre>
 * 
 * @author wkwock
 */
public class CWAFormatterDlg extends SigmetCommAttrDlg {
    /** logger */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(CWAFormatterDlg.class);

    /** product text field */
    private StyledText productTxt;

    /** tab folder */
    private TabFolder tabFolder;

    /** Thunderstorm composite */
    private ThunderstormComp thunderComp;

    /** IFR/LIFR composite */
    private IfrLifrComp ifrComp;

    /** turbulence composite */
    private TurbLlwsComp turbulenceComp;

    /** icing composite */
    private IcingFrzaComp icingComp;

    /** Blowing dust/blowing sand composite */
    private BlduBlsaComp blduComp;

    /** volcano composite */
    private VolcanoComp volcanoComp;

    /** cancel/manual composite */
    private CancelManualComp cancelComp;

    /** routine radio button */
    private Button routineRdo;

    /** correction radio button */
    private Button corRdo;

    /** site */
    private String site;

    /** product ID */
    private String productId;

    /** CWA configurations */
    private CWAGeneratorConfigXML cwaConfigs;

    /** current tab index */
    private int currentTabIndex = 0;

    /** allow the product text modification */
    private boolean allowMod = false;

    /** ttaaii for CWA products */
    private String ttaaii = "FAUS2";

    /**
     * Constructor
     * 
     * @param parShell
     * @throws VizException
     */
    public CWAFormatterDlg(Shell parShell) throws VizException {
        super(parShell);
    }

    /**
     * get the CWAFormatterDlg instance
     * 
     * @param parShell
     * @return
     */
    public static CWAFormatterDlg getInstance(Shell parShell) {
        CWAFormatterDlg cwaFormatterDlg = null;
        try {
            cwaFormatterDlg = new CWAFormatterDlg(parShell);
        } catch (VizException e) {
            logger.error("Failed to instantiate CWAFormatterDlg.", e);
        }
        return cwaFormatterDlg;
    }

    /**
     * Set parameters
     * 
     * @param site
     * @param productId
     * @param cwaConfigs
     */
    public void setParameters(String site, String productId,
            CWAGeneratorConfigXML cwaConfigs) {
        this.site = site;
        this.productId = productId;
        this.cwaConfigs = cwaConfigs;
        volcanoComp.updateVolcano(cwaConfigs);
        cancelComp.updateCwsuId(site);
        getShell().setText("CWA Formatter - Site: " + cwaConfigs.getCwsuId()
                + " - Product: " + productId);
        productTxt.setEditable(cwaConfigs.isEditable());

        if (cwaConfigs.getCwsuId().equalsIgnoreCase("ZAN")) {
            ttaaii = "FAAK2";
        }
    }

    @Override
    public Control createDialogArea(Composite parent) {
        super.createDialogArea(parent);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 5;
        parent.setLayout(layout);

        tabFolder = new TabFolder(top, SWT.TOP | SWT.BORDER);
        tabFolder.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, false, false, 9, 1));
        tabFolder.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(
                    org.eclipse.swt.events.SelectionEvent event) {
                changeTab();
            }
        });

        TabItem thunderTab = new TabItem(tabFolder, SWT.NONE);
        thunderTab.setText("Thunderstorm");
        thunderComp = new ThunderstormComp(tabFolder);
        thunderTab.setControl(thunderComp);

        TabItem ifrTab = new TabItem(tabFolder, SWT.NONE);
        ifrTab.setText("IFR/LIFR");
        ifrComp = new IfrLifrComp(tabFolder);
        ifrTab.setControl(ifrComp);

        TabItem turbulenceTab = new TabItem(tabFolder, SWT.NONE);
        turbulenceTab.setText("Turb/LLWS");
        turbulenceComp = new TurbLlwsComp(tabFolder);
        turbulenceTab.setControl(turbulenceComp);

        TabItem icingTab = new TabItem(tabFolder, SWT.NONE);
        icingTab.setText("Icing/FRZA");
        icingComp = new IcingFrzaComp(tabFolder);
        icingTab.setControl(icingComp);

        TabItem blduTab = new TabItem(tabFolder, SWT.NONE);
        blduTab.setText("BLDU/BLSA");
        blduComp = new BlduBlsaComp(tabFolder);
        blduTab.setControl(blduComp);

        TabItem volcanoTab = new TabItem(tabFolder, SWT.NONE);
        volcanoTab.setText("Volcano");
        volcanoComp = new VolcanoComp(tabFolder);
        volcanoTab.setControl(volcanoComp);

        TabItem cancelTab = new TabItem(tabFolder, SWT.NONE);
        cancelTab.setText("Can/Man");
        cancelComp = new CancelManualComp(tabFolder);
        cancelTab.setControl(cancelComp);

        createProductTextBox();

        return top;
    }

    /**
     * Change tab action
     */
    private void changeTab() {
        int newIndex = tabFolder.getSelectionIndex();
        if (currentTabIndex != newIndex) {
            if (!productTxt.getText().isEmpty()) {
                MessageBox messageBox = new MessageBox(top.getShell(),
                        SWT.YES | SWT.NO);
                messageBox.setText(
                        "Moving to " + tabFolder.getItem(newIndex).getText());
                messageBox.setMessage(
                        "Move to " + tabFolder.getItem(newIndex).getText()
                                + "?\nProduct will be lost.");
                int buttonId = messageBox.open();
                if (buttonId == SWT.YES) {
                    setProductText("");
                    currentTabIndex = newIndex;
                } else {
                    tabFolder.setSelection(currentTabIndex);
                }
            }
        }
    }

    /**
     * create the product text box
     */
    private void createProductTextBox() {
        productTxt = new StyledText(top,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.CENTER, SWT.FILL, false, false, 9, 1);
        gd.heightHint = 200;
        gd.widthHint = 600;
        productTxt.setLayoutData(gd);

        productTxt.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                // lock the 1st four lines
                if (!allowMod && (productTxt.getLineAtOffset(e.start) < 4
                        || productTxt.getLineAtOffset(e.end) < 4)) {
                    e.doit = false;
                }
            }
        });
    }

    /**
     * Create bottom buttons
     */
    @Override
    public void createButtonsForButtonBar(Composite parent) {
        parent.setLayout(new GridLayout(6, false));

        routineRdo = new Button(parent, SWT.RADIO);
        routineRdo.setText("Rtn");
        routineRdo.setSelection(true);

        corRdo = new Button(parent, SWT.RADIO);
        corRdo.setText("COR");
        corRdo.setSelection(false);

        Button newBtn = new Button(parent, SWT.PUSH);
        newBtn.setText("Create New Text");
        newBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewText();
            }
        });

        Button previousBtn = new Button(parent, SWT.PUSH);
        previousBtn.setText("Use Previous CWA Text");
        previousBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createPreviousText();
            }
        });

        Button sendBtn = new Button(parent, SWT.PUSH);
        sendBtn.setText("Send Text");
        sendBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendProduct();
            }
        });

        Button exitBtn = new Button(parent, SWT.PUSH);
        exitBtn.setText("Exit");
        exitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
    }

    @Override
    public Control createButtonBar(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        composite.setLayoutData(gd);
        composite.setFont(parent.getFont());

        // Add the buttons to the button bar.
        createButtonsForButtonBar(composite);
        return composite;
    }

    /**
     * create new product text
     */
    private void createNewText() {
        if (getEditableAttrFromLine().isEmpty()) {
            MessageBox msgBox = new MessageBox(this.getShell(), SWT.OK);
            msgBox.setText("Empty EditableAttrFromLine");
            msgBox.setMessage(
                    "No VORs Drawn. Please draw the VORs and try again.");
            msgBox.open();
        }

        String wmoHeader = ttaaii + productId.substring(productId.length() - 1)
                + " " + cwaConfigs.getKcwsuId();
        String header = cwaConfigs.getCwsuId()
                + productId.substring(productId.length() - 1) + " CWA";

        String fromline = "FROM " + getEditableAttrFromLine();
        String body = "AREA OF ";
        if (getLineType().equals(SigmetCommAttrDlg.LINE)) {
            body = "AREA..." + (int) getWidth() + " NM WIDE...";
        } else if (getLineType().equals(SigmetCommAttrDlg.ISOLATED)) {
            // Remove FROM text with ISOL IFA point
            fromline = getEditableAttrFromLine();
            body = "ISOL...DIAM " + (int) getWidth() + "NM...";
        }

        String productStr = "";
        Control control = tabFolder.getItem(tabFolder.getSelectionIndex())
                .getControl();
        if (control instanceof AbstractCWAComp) {
            AbstractCWAComp cwaComp = (AbstractCWAComp) control;
            productStr = cwaComp.createText(wmoHeader, header, fromline, body,
                    cwaConfigs.getCwsuId(), productId, corRdo.getSelection(),
                    cwaConfigs.isOperational());
        }

        setProductText(productStr);

    }

    /**
     * create previous product text
     */
    private void createPreviousText() {
        Control control = tabFolder.getItem(tabFolder.getSelectionIndex())
                .getControl();
        if (!(control instanceof AbstractCWAComp)) {
            return;
        }

        AbstractCWAComp cwaComp = (AbstractCWAComp) control;

        String startDateTime = cwaComp.getStartTime();
        String endDateTime = cwaComp.getEndTime();

        StringBuilder output = new StringBuilder();

        String wmoHeader = ttaaii + productId.substring(productId.length() - 1)
                + " " + cwaConfigs.getKcwsuId();
        String header = cwaConfigs.getCwsuId()
                + productId.substring(productId.length() - 1) + " CWA";

        CWAProduct cwaProduct = new CWAProduct(productId,
                cwaConfigs.getCwsuId(), cwaConfigs.isOperational());
        String[] lines = cwaProduct.getProductTxt().split("\n");

        // Extract Site ID and CWA number. Re-use previous number
        // if this is a correction.
        int seriesId = 1;
        if (lines.length > 2) {
            String[] items = lines[2].split("\\s+");
            if (items.length > 3) {
                try {
                    seriesId = Integer.parseInt(items[2]);
                } catch (NumberFormatException nfe) {
                    logger.error("Failed to parse " + lines[2], nfe);
                }
            }
        }

        StringBuilder body = new StringBuilder();
        for (int index = 3; index < lines.length; index++) {
            body.append(lines[index]).append("\n");
        }

        output.append(wmoHeader).append(" ").append(startDateTime).append("\n");
        output.append(header).append(" ").append(startDateTime);
        if (corRdo.getSelection()) {
            output.append(" COR");
        }
        output.append("\n");
        output.append(cwaConfigs.getCwsuId()).append(" CWA ").append(seriesId)
                .append(" VALID UNTIL ").append(endDateTime).append("Z\n");
        output.append(body);

        setProductText(output.toString());
    }

    /**
     * Set product text
     * 
     * @param text
     */
    void setProductText(String text) {
        allowMod = true;
        try {
            productTxt.setText(text);
        } finally {
            allowMod = false;
        }
    }

    /**
     * save and send product
     */
    private void sendProduct() {
        String product = productTxt.getText();
        if (product.trim().isEmpty()) {
            MessageBox messageBox = new MessageBox(top.getShell(), SWT.OK);
            messageBox.setText("No Product to Save");
            messageBox.setMessage("There's no product to save.");
            messageBox.open();
            return;
        } else {
            MessageBox messageBox = new MessageBox(top.getShell(),
                    SWT.YES | SWT.NO);
            messageBox.setText("Save Product");
            String message = "Save product " + productId + " to textdb?";
            if (cwaConfigs.isOperational()) {
                message += "\nAnd distribute to DEFAULTNCF?";
            }
            messageBox.setMessage(message);
            int buttonId = messageBox.open();
            if (buttonId != SWT.YES) {
                return;
            }
        }

        CWAProduct cwaProduct = new CWAProduct(productId,
                cwaConfigs.isOperational());
        cwaProduct.setProductTxt(product);
        boolean success = cwaProduct.sendText(site);

        if (cwaConfigs.isOperational()) {
            MessageBox messageBox = new MessageBox(top.getShell(), SWT.OK);
            messageBox.setText("Product Distribution");
            if (success) {
                messageBox.setMessage(
                        "Product " + productId + " successfully distributed.");
            } else {
                messageBox.setMessage(
                        "Failed to distribute product " + productId + ".");
            }
            messageBox.open();
        }
    }
}
