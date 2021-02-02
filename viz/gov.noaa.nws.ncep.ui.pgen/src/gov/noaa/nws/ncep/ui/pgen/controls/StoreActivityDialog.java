/*
 * gov.noaa.nws.ncep.ui.pgen.controls.StoreActivityeDialog
 *
 * 27 March 2013
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.controls;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.viz.core.mode.CAVEMode;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

import gov.noaa.nws.ncep.common.dataplugin.pgen.ActivityInfo;
import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductConfigureDialog;
import gov.noaa.nws.ncep.ui.pgen.producttypes.ProductType;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;

/**
 * Create a dialog to Store PGEN products to EDEX.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * --------------------------------------------------------------------------------------
 * 03/13        #977        S. Gilbert  Initial creation
 * 01/14        #1105       J. Wu       Pre-fill for each activity info.
 * 05/14        TTR 963     J. Wu       Change activity Info to Activity Label.
 * 01/7/2016    R13162      J. Lopez    Added a combo box to save the file using the Default name
 * 04/14/2016   R13245      B. Yin      Changed reference time to 24 hour format.
 *                                      Added a drop down list for reference time.
 * 05/02/2016   R16076      J. Wu       change type/subtype/site/desk to pulldown menu.
 * 07/28/2016   R17954      B. Yin      return CANCEL when Cancel button is pressed.
 * 01/19/2021   86162       S. Russell  Updated storeProducts() to save child
 *                                      Product objects. Updated setDialogFields()
 *                                      so Ref Time menu uses the GMT time zone.
 *
 * </pre>
 *
 * @author
 * @version 1
 */
public class StoreActivityDialog extends CaveJFACEDialog {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(StoreActivityDialog.class);

    private static final int SAVE_ID = IDialogConstants.CLIENT_ID + 3841;

    private static final String SAVE_LABEL = "Save";

    private static final int CANCEL_ID = IDialogConstants.CLIENT_ID + 3842;

    private static final String CANCEL_LABEL = "Cancel";

    private static final int SHELL_MINIMUM_WIDTH = 375;

    private static final int SHELL_MINIMUM_LENGTH = 400;

    private static final int PREFERRED_WIDTH = 400;

    private static final int REF_TIME_LIMIT = 4;

    private static final int REF_TIME_WIDTH = 90;

    private static final String DEFAULT_LABEL_PREFIX = "("
            + PgenConstant.GENERAL_DEFAULT + ") ";

    private String title = null;

    private String defaultFileName;

    private Combo labelCombo = null;

    private Text nameText = null;

    private Combo typeCombo = null;

    private Combo subtypeCombo = null;

    private Combo siteCombo = null;

    private Combo deskCombo = null;

    private Text forecasterText = null;

    private Combo modeCombo;

    private Text statusText;

    private DateTime validDate;

    private Combo validTime;

    private Button autoSaveOffBtn;

    private Button autoSaveOnBtn;

    private PgenResource rsc;

    private Product activity;

    private LinkedHashMap<String, ProductType> prdTyps = null;

    private ActivityCollection actCollection;

    /*
     * Constructor
     */
    public StoreActivityDialog(Shell parShell, String btnName)
            throws VizException {

        super(parShell);
        setStoreMode(btnName);
        rsc = PgenSession.getInstance().getPgenResource();
        activity = rsc.getActiveProduct();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    /*
     * Set up the file mode.
     */
    private void setStoreMode(String btnName) {

        if (btnName.equals("Open")) {
            title = "Open a PGEN Activity file";
        } else if (btnName.equals("Save") || btnName.equals("Save All")) {
            title = "Save the PGEN Activity";
        } else if (btnName.equals("Save As")) {
            title = "Save the PGEN Activity as";
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
     * .Shell)
     */
    @Override
    protected void configureShell(Shell shell) {
        this.setShellStyle(SWT.RESIZE | SWT.PRIMARY_MODAL);
        super.configureShell(shell);
        shell.setMinimumSize(SHELL_MINIMUM_WIDTH, SHELL_MINIMUM_LENGTH);

        if (title != null) {
            shell.setText(title);
        }
    }

    /**
     * (non-Javadoc) Create all of the widgets on the Dialog
     *
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createDialogArea(Composite parent) {

        Composite dlgAreaForm = (Composite) super.createDialogArea(parent);
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        dlgAreaForm.setLayout(mainLayout);

        /*
         * Initialize all of the Storm Information Section
         */
        Composite g1 = new Composite(dlgAreaForm, SWT.NONE);
        g1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createActivityInfoArea(g1);

        /*
         * Initialize all of the Reference Time Section
         */
        Composite g2 = new Composite(dlgAreaForm, SWT.NONE);
        g2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createRefTimeArea(g2);

        /*
         * Initialize all of the Reference Time Section
         */
        Composite g3 = new Composite(dlgAreaForm, SWT.NONE);
        g3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createAutoSaveArea(g3);

        setDialogFields();

        return dlgAreaForm;
    }

    private void createActivityInfoArea(Composite g1) {

        g1.setLayout(new GridLayout(2, false));
        GridData gdata = new GridData(SWT.FILL, SWT.CENTER, true, false);

        Label infoLabel = new Label(g1, SWT.NONE);
        infoLabel.setText("Activity Label*:");

        labelCombo = new Combo(g1, SWT.DROP_DOWN);
        GridData layout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        layout.widthHint = PREFERRED_WIDTH;
        labelCombo.setLayoutData(layout);

        labelCombo.setToolTipText("Input or select a file name - required.");
        Label nameLabel = new Label(g1, SWT.NONE);
        nameLabel.setText("Activity Name*:");

        nameText = new Text(g1, SWT.NONE);
        nameText.setLayoutData(gdata);
        nameText.setToolTipText(
                "Name for this activity, just like your first name while activity "
                        + "type/subtype is the last name.");
        nameText.setEnabled(false);

        Label typeLabel = new Label(g1, SWT.NONE);
        typeLabel.setText("Activity Type*:");
        typeCombo = new Combo(g1, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setToolTipText("Activity type as defined");

        Label subtypeLabel = new Label(g1, SWT.NONE);
        subtypeLabel.setText("Activity Subtype*:");
        subtypeCombo = new Combo(g1, SWT.DROP_DOWN | SWT.READ_ONLY);
        subtypeCombo.setToolTipText("Activity subtype as defined.");

        Label siteLabel = new Label(g1, SWT.NONE);
        siteLabel.setText("Site*:");
        siteCombo = new Combo(g1, SWT.DROP_DOWN | SWT.MULTI);
        siteCombo.setToolTipText("Sites available");

        Label deskLabel = new Label(g1, SWT.NONE);
        deskLabel.setText("Desk:");
        deskCombo = new Combo(g1, SWT.DROP_DOWN | SWT.MULTI);
        deskCombo.setToolTipText("Desks available");

        Label forecasterLabel = new Label(g1, SWT.NONE);
        forecasterLabel.setText("Forecaster:");
        forecasterText = new Text(g1, SWT.NONE);
        forecasterText.setLayoutData(gdata);
        forecasterText.setToolTipText(
                "Forecaster's name, default is your user name, not required");

        Label modeLabel = new Label(g1, SWT.NONE);
        modeLabel.setText("Operating Mode:");

        modeCombo = new Combo(g1, SWT.DROP_DOWN | SWT.READ_ONLY);

        for (CAVEMode cm : CAVEMode.values()) {
            modeCombo.add(cm.name());
        }

        modeCombo.select(0);
        modeCombo
                .setToolTipText("CAVE mode set at starting, or pick one here.");

        Label statusLabel = new Label(g1, SWT.NONE);
        statusLabel.setText("Activity Status:");

        statusText = new Text(g1, SWT.NONE);
        statusText.setLayoutData(gdata);
        statusText.setToolTipText("Activity status, not in use yet.");

    }

    private void createRefTimeArea(Composite g2) {

        g2.setLayout(new GridLayout(4, false));

        Label refTimeLabel = new Label(g2, SWT.NONE);
        refTimeLabel.setText("Ref Time*:            ");

        validDate = new DateTime(g2, SWT.BORDER | SWT.DATE);

        validDate.setToolTipText(
                "Activity's reference date, changing it and saving the "
                        + "activity will save the current activity as a new entry in PGEN DB.");

        validTime = new Combo(g2, SWT.DROP_DOWN);

        validTime.setToolTipText(
                "Activity's reference time, changing it and saving the "
                        + "activity will save the current activity as a new entry in PGEN DB.");

        validTime.setTextLimit(REF_TIME_LIMIT);
        validTime.setLayoutData(new GridData(REF_TIME_WIDTH, -1));

        Calendar curTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        validTime.setText(
                String.format("%02d%2d", curTime.get(Calendar.HOUR_OF_DAY),
                        curTime.get(Calendar.MINUTE)));

        validTime.addVerifyListener(new VerifyListener() {
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

        validTime.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!validTime.getText().isEmpty()) {
                    if (PgenUtil.validateUTCTime(validTime.getText())) {
                        validTime.setBackground(Display.getCurrent()
                                .getSystemColor(SWT.COLOR_WHITE));
                    } else {
                        validTime.setBackground(Display.getCurrent()
                                .getSystemColor(SWT.COLOR_RED));
                    }
                }
            }
        });

        Label utcLabel = new Label(g2, SWT.NONE);
        utcLabel.setText("UTC");

    }

    private void createAutoSaveArea(Composite g3) {
        g3.setLayout(new GridLayout(3, false));

        Label autoSaveLbl = new Label(g3, SWT.NONE);
        autoSaveLbl.setText("Auto Save:           ");

        autoSaveOffBtn = new Button(g3, SWT.RADIO);
        autoSaveOffBtn.setText("Off");
        autoSaveOffBtn.setSelection(true);

        autoSaveOnBtn = new Button(g3, SWT.RADIO);
        autoSaveOnBtn.setText("On");
        autoSaveOnBtn.setSelection(false);

    }

    /**
     * Save/Cancel button for "Save" a product file.
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {

        createButton(parent, SAVE_ID, SAVE_LABEL, true);

        createButton(parent, CANCEL_ID, CANCEL_LABEL, true);

    }

    @Override
    protected void buttonPressed(int buttonId) {

        if (buttonId == SAVE_ID) {
            storeProducts();
        } else if (buttonId == CANCEL_ID) {
            setReturnCode(Window.CANCEL);
            close();
        }
    }

    /*
     * Initialize and set up fields in the dialog
     */
    private void setDialogFields() {

        // Retrieve activity definitions & available activities
        prdTyps = ProductConfigureDialog.getProductTypes();
        actCollection = new ActivityCollection();

        // Creates the default file name
        defaultFileName = PgenUtil.buildPrdFileName(activity, prdTyps);
        labelCombo.add(DEFAULT_LABEL_PREFIX + defaultFileName);

        labelCombo.setText(defaultFileName);

        // Add the original file name if exist and select it
        if (activity.getOutputFile() != null
                && !activity.getOutputFile().equals(defaultFileName)) {
            labelCombo.add(activity.getOutputFile());
            labelCombo.select(1);
        }

        labelCombo.pack();

        // If the default name is selected, remove the "(Default)" label
        labelCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (((Combo) e.widget).getSelectionIndex() == 0) {
                    String labelStr = labelCombo.getText()
                            .substring(DEFAULT_LABEL_PREFIX.length());
                    labelCombo.setText(labelStr);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }

        });

        // Sets Activity Name
        if (activity.getName() != null) {
            nameText.setText(activity.getName());
        }

        /*
         * Type - add all types and select the current activity's type
         *
         * Note that Activity type/subtype is stored in Product as
         * "type(subtype)".
         */
        String type = activity.getType();
        String curActType = PgenConstant.DEFAULT_ACTIVITY_TYPE;
        String curActSubtype = PgenConstant.DEFAULT_SUBTYPE;
        ProductType currentAct = prdTyps.get(type);
        if (currentAct != null) {
            if (currentAct.getName() != null
                    && currentAct.getName().trim().length() > 0) {
                curActType = currentAct.getName();
            } else {
                curActType = currentAct.getType();
                curActSubtype = currentAct.getSubtype();
            }
        }

        typeCombo.removeAll();
        ArrayList<String> types = new ArrayList<>();
        for (ProductType ptype : prdTyps.values()) {
            String typStr = ptype.getType();
            if (ptype.getName() != null
                    && ptype.getName().trim().length() > 0) {
                typStr = ptype.getName();
            }

            if (!types.contains(typStr)) {
                types.add(typStr);
            }
        }

        int selectInd = 0;
        int jj = 0;
        typeCombo.add(PgenConstant.DEFAULT_ACTIVITY_TYPE);
        for (String typ : types) {
            jj++;
            if (typ.equals(curActType)) {
                selectInd = jj;
            }
            typeCombo.add(typ);
        }
        typeCombo.select(selectInd);

        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                subtypeCombo.removeAll();
                ArrayList<String> subtypes = getSubtypesForType(
                        typeCombo.getText(), prdTyps.values());
                for (String styp : subtypes) {
                    subtypeCombo.add(styp);
                }
                subtypeCombo.select(0);

                typeCombo.pack();

                resetLabelAndName();
            }
        });

        /*
         * Subtype - add all subtypes for the selected activity.
         */
        subtypeCombo.removeAll();
        ArrayList<String> subtypes = getSubtypesForType(curActType,
                prdTyps.values());

        selectInd = 0;
        jj = 0;
        for (String styp : subtypes) {
            if (styp.equals(curActSubtype)) {
                selectInd = jj;
            }
            subtypeCombo.add(styp);
            jj++;
        }
        subtypeCombo.select(selectInd);

        subtypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                subtypeCombo.pack();

                resetLabelAndName();
            }
        });

        /*
         * Site - add current site & other available. Select current site as
         * default.
         */
        siteCombo.removeAll();

        String currentSite = PgenUtil.getCurrentOffice();
        if (currentSite != null) {
            siteCombo.add(currentSite);
            siteCombo.select(0);
        }

        for (String site : actCollection.getCurrentSiteList()) {
            if (site != null && site.trim().length() > 0
                    && !site.equalsIgnoreCase(PgenConstant.OPTION_ALL)
                    && !site.equals(currentSite)) {
                siteCombo.add(site);
            }
        }

        /*
         * Desk - add current desk & other available. Select current desk as
         * default.
         */
        deskCombo.removeAll();
        String currentDesk = LocalizationManager
                .getContextName(LocalizationLevel.valueOf(PgenConstant.DESK));
        if (currentDesk != null
                && !currentDesk.equalsIgnoreCase(PgenConstant.NONE)) {
            deskCombo.add(currentDesk);
            deskCombo.select(0);
        }

        for (String desk : actCollection.getCurrentDeskList()) {
            if (desk != null && desk.trim().length() > 0
                    && !desk.equalsIgnoreCase(PgenConstant.OPTION_ALL)
                    && !desk.equals(currentSite)) {
                deskCombo.add(desk);
            }
        }

        // Forecaster.
        forecasterText.setText(System.getProperty("user.name"));

        // Select CAVE mode.
        String mode = CAVEMode.getMode().name();
        int index = 0;
        int ii = 0;
        for (String md : modeCombo.getItems()) {
            if (md.equals(mode)) {
                index = ii;
                break;
            }
            ii++;
        }
        modeCombo.select(index);

        statusText.setText("Unknown");

        // Reference time
        Calendar datetime = activity.getTime().getStartTime();
        datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (datetime != null) {
            validDate.setYear(datetime.get(Calendar.YEAR));
            validDate.setMonth(datetime.get(Calendar.MONTH));
            validDate.setDay(datetime.get(Calendar.DAY_OF_MONTH));
            validTime.setText(String.format("%02d%02d",
                    datetime.get(Calendar.HOUR_OF_DAY),
                    datetime.get(Calendar.MINUTE)));
        }

        /*
         * Creates a drop down list for reference time
         */
        ProductType pType = ProductConfigureDialog.getProductTypes().get(type);

        if ((pType != null) && (pType.getPgenSave() != null)
                && (pType.getPgenSave().getRefTimeList() != null)) {
            for (String refTime : pType.getPgenSave().getRefTimeList()
                    .split(";")) {
                validTime.add(refTime);
            }
        } else {
            for (int kk = 0; kk < 24; kk++) {
                validTime.add(String.format("%02d00", kk));
            }
        }
    }

    /*
     * Store the products to EDEX.
     */
    private void storeProducts() {

        String activityLabel = labelCombo.getText();

        if (activityLabel == null || activityLabel.isEmpty()) {
            MessageDialog confirmDlg = new MessageDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                    "Need More Information", null,
                    "Activity Label field is required.\nPlease enter an appropriate string and then try saving!",
                    MessageDialog.WARNING, new String[] { "OK" }, 0);

            confirmDlg.open();
            labelCombo.forceFocus();
            return;
        }

        ActivityInfo info = getActivityInfo();

        // Save child Product Objects, If Any
        if (activity.getChildProducts().size() > 0) {
            for (Product childProduct : activity.getChildProducts()) {

                ActivityInfo childInfo = new ActivityInfo();
                childInfo.setActivityName(info.getActivityName());
                childInfo.setActivitySubtype(info.getActivitySubtype());
                childInfo.setActivityLabel(childProduct.getOutputFile());
                childInfo.setActivityType(childProduct.getType());
                childInfo.setSite(info.getSite());
                Calendar childRefTime = childProduct.getTime().getStartTime();
                childRefTime.set(Calendar.MILLISECOND, 0);
                childInfo.setRefTime(childRefTime);

                try {
                    StorageUtils.storeProduct(childInfo, childProduct, true);
                } catch (PgenStorageException e) {
                    StorageUtils.showError(e);
                }
            }
        }

        // Save Product
        rsc.setAutosave(autoSaveOnBtn.getSelection());
        rsc.setAutoSaveFilename(activityLabel);
        activity.setInputFile(info.getActivityLabel());
        activity.setOutputFile(info.getActivityLabel());
        activity.setCenter(info.getSite());
        activity.setForecaster(info.getForecaster());

        String prevName = activity.getName();
        String prevType = activity.getType();
        activity.setName(info.getActivityName());
        activity.setType(
                getFullType(info.getActivityType(), info.getActivitySubtype()));

        // Save the current product
        try {
            StorageUtils.storeProduct(info, activity, true);
        } catch (PgenStorageException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error storing the products to EDEX", e);
            return;
        }

        close();
        PgenFileNameDisplay.getInstance().setFileName(activityLabel);
        PgenUtil.setSelectingMode();
        rsc.getResourceData().setNeedsSaving(false);

        if (!activity.getName().equals(prevName)
                || !activity.getType().equals(prevType)) {
            rsc.getResourceData().startProductManage();
        }
    }

    /*
     * Retrieve the current activity information on dialog.
     *
     * @return a ActivityInfo
     */
    private ActivityInfo getActivityInfo() {

        ActivityInfo info = new ActivityInfo();
        String lbl = labelCombo.getText();
        if (!lbl.endsWith(".xml")) {
            lbl += ".xml";
        }

        info.setActivityLabel(lbl);
        info.setActivityName(nameText.getText());
        info.setActivityType(typeCombo.getText());
        String stype = subtypeCombo.getText();
        if (stype.equals(PgenConstant.DEFAULT_SUBTYPE)) {
            stype = "";
        }
        info.setActivitySubtype(stype);
        info.setSite(siteCombo.getText());
        info.setDesk(deskCombo.getText());
        info.setForecaster(forecasterText.getText());
        info.setMode(modeCombo.getText());
        info.setStatus(statusText.getText());

        Calendar refTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        int hours = 0;
        int minutes = 0;

        try {
            hours = Integer.valueOf(validTime.getText().substring(0, 2));
            minutes = Integer.valueOf(validTime.getText().substring(2, 4));
        } catch (Exception e) {
        }

        refTime.set(validDate.getYear(), validDate.getMonth(),
                validDate.getDay(), hours, minutes, 0);
        refTime.set(Calendar.MILLISECOND, 0);

        info.setRefTime(refTime);

        return info;
    }

    /*
     * Get subtypes defined for an activity type
     *
     * Note: an activity without "subtype" uses DEFAULT_SUBTYPE "none".
     *
     * @param type activity type
     *
     * @param prdTyps a collection of ProductType
     *
     * @return a list of subtypes
     */
    private ArrayList<String> getSubtypesForType(String type,
            Collection<ProductType> prdTyps) {

        ArrayList<String> subtypes = new ArrayList<>();
        subtypes.add(PgenConstant.DEFAULT_SUBTYPE);
        if (type != null && type.trim().length() > 0) {
            for (ProductType ptype : prdTyps) {
                if (ptype.getType().equals(type)) {
                    String stp = ptype.getSubtype();
                    if (stp != null && stp.trim().length() > 0
                            && !subtypes.contains(stp)) {
                        subtypes.add(stp);
                    }
                }
            }
        }

        return subtypes;

    }

    /*
     * Get full type name from an activity type and subtype
     *
     * @param type activity type
     *
     * @param subtype activity subtype
     *
     * @return full type string
     */
    private String getFullType(String type, String subtype) {

        String fullType = PgenConstant.DEFAULT_ACTIVITY_TYPE;

        if (type != null && type.trim().length() > 0) {
            fullType = type;

            if (subtype != null && subtype.trim().length() > 0 && !subtype
                    .trim().equalsIgnoreCase(PgenConstant.DEFAULT_SUBTYPE)) {
                fullType += ("(" + subtype + ")");
            }
        }

        return fullType;
    }

    /*
     * Reset activity label and name from an activity type and subtype
     */
    private void resetLabelAndName() {

        labelCombo.removeAll();

        String fullType = getFullType(typeCombo.getText(),
                subtypeCombo.getText());

        // Construct the new default and set as the first option
        Product newAct = activity.copy();
        newAct.setType(fullType);
        newAct.setName(fullType);
        String newDefault = PgenUtil.buildPrdFileName(newAct, prdTyps);
        labelCombo.add(DEFAULT_LABEL_PREFIX + newDefault);

        // Add the original default as an second option
        if (!defaultFileName.equals(newDefault)) {
            labelCombo.add(defaultFileName);
        }

        /*
         * Add the original file name if exist and select it. Otherwise, set to
         * the new default.
         */
        if (activity.getOutputFile() != null
                && !activity.getOutputFile().equals(defaultFileName)) {
            labelCombo.add(activity.getOutputFile());
            labelCombo.select(labelCombo.getItemCount() - 1);
        } else {
            labelCombo.setText(newDefault);
        }

        // Set name
        nameText.setText(fullType);

    }

}
