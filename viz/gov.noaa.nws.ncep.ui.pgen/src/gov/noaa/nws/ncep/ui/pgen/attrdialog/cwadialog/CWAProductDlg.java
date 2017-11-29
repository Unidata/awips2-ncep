/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenStaticDataProvider;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenMultiPointDrawingTool;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;

import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * The class for CWA generator dialog
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ---------   ----------- --------------------------
 * 12/19/2016   17469       wkwock      Initial Creation.
 * 
 * </pre>
 * 
 * @author wkwock
 */

public class CWAProductDlg extends AttrDlg {
    /** the top Composite of this dialog. */
    private Composite top = null;

    /** logger */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(CWAProductDlg.class);

    /** CWA generator configurations */
    private CWAGeneratorConfigXML cwaConfigs = null;

    /** Text box width */
    private static final int TEXT_WIDTH = 160;

    /** button width */
    private static final int BUTTON_WIDTH = 90;

    /** Maximum number of CWA widgets */
    private static final int MAX_CWA = 6;

    /** CWSU sites */
    private final static String[] sites = { "ZAB", "ZAN", "ZAU", "ZBW", "ZDC",
            "ZDV", "ZFW", "ZHU", "ZID", "ZJX", "ZKC", "ZLA", "ZLC", "ZMA",
            "ZME", "ZMP", "ZNY", "ZOA", "ZOB", "ZSE", "ZTL" };

    /** time label */
    private Label timeLbl;

    /** CSIG composite */
    private Composite csigComp;

    /** normal background color */
    private Color normalBgColor;

    /** practice background color */
    private Color praticeBgColor;

    /** CWA PI Lid */
    private String cwaPiLid = "CWA";

    /** CWS PI Lid */
    private String cwsPiLid = "CWS";

    /** CWA composite */
    private Composite cwaComposite;

    /** CWAs label */
    private Label cwaLbl;

    /** series label */
    private Label seriesLbl;

    /** CWA buttons */
    private Button[] cwaButtons;

    /** CWA texts */
    private Text[] cwaTexts;

    /** MIS composite */
    private Composite misComposite;

    /** MIS label */
    private Label misLbl;

    /** MIS Series Label */
    private Label misSeriesLbl;

    /** MIS button */
    private Button misBtn;

    /** MIS text */
    private Text misTxt;

    /** product view listener */
    private MouseTrackAdapter productViewListener;

    /** formatter dialog listener */
    private SelectionAdapter formaterListener;

    /** CWA formatter dialog */
    private CWAFormatterDlg cwaDlg;

    /** CWS formatter dialog */
    private CWSFormatterDlg cwsDlg;

    /** CSIG dialog */
    private CSIGDlg csigDlg;

    /** Report dialog */
    private ReportDlg reportDlg;

    /** bottom composite */
    private Composite bottomComp;

    /** timer for timeLbl */
    private Timer timeLblTimer;

    /** timer for auto refresh */
    private Timer refreshTimer;

    /** update interval in minutes */
    private int updateInterval = 4;

    private String originalCwsuID = null;

    /**
     * constructor for this class.
     * 
     * @param Shell:
     *            parent Shell of this dialog.
     * @throws VizException
     */
    public CWAProductDlg(Shell parShell) throws VizException {
        super(parShell);
        cwaButtons = new Button[MAX_CWA];

        cwaTexts = new Text[MAX_CWA];

    }

    /**
     * singleton creation method for this class.
     * 
     * @param Shell:
     *            parent Shell of this dialog.
     * @return
     */
    public static CWAProductDlg getInstance(Shell parShell) {
        CWAProductDlg cwaProductDlg = null;

        try {
            cwaProductDlg = new CWAProductDlg(parShell);
        } catch (VizException e) {
            logger.error("Failed to instanciate CWAGenerator", e);
        }
        return cwaProductDlg;
    }

    /**
     * button listener overridden for "OK" button
     */
    @Override
    public void okPressed() {
    }

    /**
     * method overridden from super class for this dialog
     */
    @Override
    public void enableButtons() {
    }

    /**
     * method overridden for creating this dialog area.
     * 
     * @param Composite:
     *            parent for this dialog
     */
    @Override
    public Control createDialogArea(Composite parent) {
        this.top = (Composite) super.createDialogArea(parent);
        this.getShell().setText("CWA Generator");

        prepareColors();
        buildButtonListeners();
        getCWAConfigs();

        top.setLayout(new GridLayout(1, false));

        createTopControls();
        createCWAsControls();
        createMISControls();

        createCSIGButtons();

        createBottomButtons();
        createTimeControl();

        changeModeGui();
        updateProductStatus();

        return top;
    }

    /**
     * Prepare colors
     */
    private void prepareColors() {
        normalBgColor = top.getBackground();

        praticeBgColor = new Color(top.getDisplay(), 255, 140, 0);

        getShell().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (praticeBgColor != null) {
                    praticeBgColor.dispose();
                }
            }
        });
    }

    /**
     * Build button listeners
     */
    private void buildButtonListeners() {
        productViewListener = new MouseTrackAdapter() {
            @Override
            public void mouseHover(MouseEvent e) {
                if (e.widget instanceof Button) {
                    Button button = (Button) e.widget;
                    quickViewProduct(button);
                }
            }
        };

        formaterListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = (Button) e.widget;
                openCWAFormatterDlg(button.getText());
            }
        };
    }

    /**
     * Open CWA formatter dialog
     * 
     * @param productId
     */
    private void openCWAFormatterDlg(String productId) {
        if (cwaDlg != null && cwaDlg.getShell() != null
                && !cwaDlg.getShell().isDisposed()) {
            cwaDlg.getShell().setFocus();
            return;
        }

        PgenMultiPointDrawingTool pm = new PgenMultiPointDrawingTool();
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("name", PgenConstant.CWA_FORMATTER);
        parameterMap.put("className", PgenConstant.SIGMET);

        @SuppressWarnings("deprecation")
        ExecutionEvent event = new ExecutionEvent(parameterMap, null, null);
        try {
            pm.execute(event);
            AttrDlg attrDlg = pm.getAttrDlg();
            if (attrDlg instanceof CWAFormatterDlg) {
                cwaDlg = (CWAFormatterDlg) attrDlg;
                String site = productId.substring(productId.length() - 3);
                cwaDlg.setParameters(site,
                        cwaConfigs.getKcwsuId() + productId, cwaConfigs);
            }
        } catch (ExecutionException e) {
            logger.error("Failed to run CWA Formatter", e);
        }
    }

    /**
     * open CWS formatter dialog
     */
    private void openCWSFormatterDlg() {
        if (cwaDlg != null && cwaDlg.getShell() != null
                && !cwaDlg.getShell().isDisposed()) {
            cwaDlg.close();
        }

        if (cwsDlg != null && cwsDlg.isDisposed()) {
            cwsDlg.close();
        }

        if (csigDlg != null && !csigDlg.isDisposed()) {
            csigDlg.close();
        }

        cwsDlg = new CWSFormatterDlg(getShell(),
                cwaConfigs.getKcwsuId() + misBtn.getText(), cwaConfigs);
        cwsDlg.open();
    }

    /**
     * make a quick view on a product
     * 
     * @param product
     */
    private void quickViewProduct(Button button) {
        String productId = cwaConfigs.getAwipsNode() + button.getText();
        CWAProduct cwaProduct = new CWAProduct(productId, "",
                cwaConfigs.isOperational());
        String product = cwaProduct.getProductTxt();
        if (product == null || product.isEmpty()) {
            button.setToolTipText(productId);
        } else {
            button.setToolTipText("\tProduct: " + productId + "\n\n"
                    + cwaProduct.getProductTxt());
        }
    }

    /**
     * Create top controls
     */
    private void createTopControls() {
        Composite topComp = new Composite(top, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        topComp.setLayout(gl);

        Label siteLbl = new Label(topComp, SWT.NONE);
        siteLbl.setText("Site:");
        Combo siteCbo = new Combo(topComp, SWT.DROP_DOWN);
        siteCbo.setItems(sites);
        siteCbo.select(0);
        for (int i = 0; i < sites.length; i++) {
            if (sites[i].equalsIgnoreCase(cwaConfigs.getCwsuId())) {
                siteCbo.select(i);
            }
        }

        Label modeLbl = new Label(topComp, SWT.NONE);
        modeLbl.setText("Mode:");
        Combo modeCbo = new Combo(topComp, SWT.DROP_DOWN);
        modeCbo.add("Normal Operational");
        modeCbo.add("Practice");
        if (cwaConfigs.isOperational()) {
            modeCbo.select(0);
        } else {
            modeCbo.select(1);
        }
        modeCbo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                cwaConfigs.setOperational(modeCbo.getSelectionIndex() == 0);
                changeModeGui();
                updateProductStatus();
            }
        });

        siteCbo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                cwaConfigs.setCwsuId(siteCbo.getText());
                cwaConfigs.setKcwsuId("K" + siteCbo.getText());
                int index = modeCbo.getSelectionIndex();
                if (siteCbo.getText().equalsIgnoreCase(originalCwsuID)) {
                    modeCbo.setItem(0, "Normal Operational");
                } else {
                    modeCbo.setItem(0, "Backup Operational");
                }
                // Refresh the text
                modeCbo.select(index);
                changeModeGui();
                updateProductStatus();
            }
        });
    }

    /**
     * Set the PI LIDs
     */
    private void setPiLids() {
        if (cwaConfigs.isOperational()) {
            cwaPiLid = "CWA";
            cwsPiLid = "CWS";
        } else {
            cwaPiLid = "WRK";
            cwsPiLid = "WRK";
        }
    }

    /**
     * Change color and button names base on the mode
     * 
     */
    private void changeModeGui() {
        Color bgColor = praticeBgColor;
        if (cwaConfigs.isOperational()) {
            bgColor = normalBgColor;
        }

        setPiLids();

        top.setBackground(bgColor);
        cwaComposite.setBackground(bgColor);
        cwaLbl.setBackground(bgColor);
        seriesLbl.setBackground(bgColor);
        misComposite.setBackground(bgColor);
        misLbl.setBackground(bgColor);
        misSeriesLbl.setBackground(bgColor);
        csigComp.setBackground(bgColor);
        bottomComp.setBackground(bgColor);

        String site = cwaConfigs.getCwsuId().substring(1);
        for (int i = 0; i < MAX_CWA; i++) {
            cwaButtons[i].setText(cwaPiLid + site + (i + 1));
        }

        misBtn.setText(cwsPiLid + cwaConfigs.getCwsuId());
    }

    /**
     * Create CWAs controls
     */
    private void createCWAsControls() {
        cwaComposite = new Composite(top, SWT.NONE);
        GridLayout cwaGl = new GridLayout(2, false);
        cwaComposite.setLayout(cwaGl);
        GridData cwaGd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 2,
                1);
        cwaComposite.setLayoutData(cwaGd);

        cwaLbl = new Label(cwaComposite, SWT.CENTER);
        GridData cwaLblGd = new GridData();
        cwaLblGd.widthHint = BUTTON_WIDTH;
        cwaLbl.setLayoutData(cwaLblGd);
        cwaLbl.setText("CWAs");

        seriesLbl = new Label(cwaComposite, SWT.CENTER);
        seriesLbl.setText("series      expire");

        for (int i = 0; i < MAX_CWA; i++) {
            cwaButtons[i] = new Button(cwaComposite, SWT.PUSH);
            GridData cwaBtnGd = new GridData();
            cwaBtnGd.widthHint = BUTTON_WIDTH;
            cwaButtons[i].setLayoutData(cwaBtnGd);
            cwaButtons[i].addMouseTrackListener(productViewListener);
            cwaButtons[i].addSelectionListener(formaterListener);

            cwaTexts[i] = new Text(cwaComposite, SWT.BORDER);
            GridData cwa1Gd = new GridData();
            cwa1Gd.widthHint = TEXT_WIDTH;
            cwaTexts[i].setLayoutData(cwa1Gd);
            cwaTexts[i].setEditable(false);
        }

    }

    /**
     * create MIS controls
     */
    private void createMISControls() {
        misComposite = new Composite(top, SWT.NONE);
        misComposite.setLayout(new GridLayout(2, false));
        GridData misGd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1,
                1);
        misComposite.setLayoutData(misGd);

        misLbl = new Label(misComposite, SWT.CENTER);
        GridData misLblGd = new GridData();
        misLblGd.widthHint = BUTTON_WIDTH;
        misLbl.setLayoutData(misLblGd);
        misLbl.setText("MIS");

        misSeriesLbl = new Label(misComposite, SWT.CENTER);
        misSeriesLbl.setText("series      expire");

        misBtn = new Button(misComposite, SWT.PUSH);
        GridData misBtnGd = new GridData();
        misBtnGd.widthHint = BUTTON_WIDTH;
        misBtn.setLayoutData(misBtnGd);
        misBtn.addMouseTrackListener(productViewListener);
        misBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openCWSFormatterDlg();
            }
        });

        misTxt = new Text(misComposite, SWT.BORDER);
        misGd = new GridData();
        misGd.widthHint = TEXT_WIDTH;
        misTxt.setLayoutData(misGd);
        misTxt.setEditable(false);
    }

    /**
     * create CSIG buttons
     */
    private void createCSIGButtons() {
        csigComp = new Composite(top, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginTop = 10;
        gl.marginWidth = 20;
        csigComp.setLayout(gl);
        GridData csigGd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 2,
                1);
        csigComp.setLayoutData(csigGd);

        Button csigwBtn = new Button(csigComp, SWT.PUSH);
        csigwBtn.setText("CSIG-W");
        csigwBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openCSIGDlg(cwaConfigs.getAwipsNode() + "SIGW");
            }
        });

        Button csigcBtn = new Button(csigComp, SWT.PUSH);
        csigcBtn.setText("CSIG-C");
        csigcBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openCSIGDlg(cwaConfigs.getAwipsNode() + "SIGC");
            }
        });

        Button csigeBtn = new Button(csigComp, SWT.PUSH);
        csigeBtn.setText("CSIG-E");
        csigeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openCSIGDlg(cwaConfigs.getAwipsNode() + "SIGE");
            }
        });
    }

    /**
     * Open CSIG dialog
     * 
     * @param productId
     */
    private void openCSIGDlg(String productId) {
        if (csigDlg != null && !csigDlg.isDisposed()) {
            if (csigDlg.getText().equals(productId)) {
                csigDlg.bringToTop();
                return;
            }

            csigDlg.close();
        }

        csigDlg = new CSIGDlg(top.getShell(), productId,
                cwaConfigs.isOperational());
        csigDlg.open();
    }

    /**
     * create bottom buttons
     */
    private void createBottomButtons() {
        bottomComp = new Composite(top, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginTop = 10;
        gl.marginBottom = 10;
        gl.marginWidth = 20;
        bottomComp.setLayout(gl);
        GridData bottomCompGd = new GridData(SWT.CENTER, SWT.CENTER, false,
                false, 2, 1);
        bottomComp.setLayoutData(bottomCompGd);
        bottomComp.setBackground(top.getBackground());

        Button refreshBtn = new Button(bottomComp, SWT.PUSH);
        refreshBtn.setText("Refresh");
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                updateProductStatus();
            }
        });

        Button reportBtn = new Button(bottomComp, SWT.PUSH);
        reportBtn.setText("Report");
        reportBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                openReportDlg();
            }
        });

        Button exitBtn = new Button(bottomComp, SWT.PUSH);
        exitBtn.setText("Exit");
        exitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });

    }

    /**
     * Open report dialog
     * 
     */
    private void openReportDlg() {
        if (reportDlg != null && !reportDlg.isDisposed()) {
            reportDlg.bringToTop();
            return;
        }

        reportDlg = new ReportDlg(top.getShell(), cwaConfigs.getAwipsNode(),
                cwaConfigs.getCwsuId(), cwaConfigs.isOperational());
        reportDlg.open();
    }

    /**
     * Get CWA product configurations
     */
    private void getCWAConfigs() {
        String pgenPath = PgenStaticDataProvider.getProvider()
                .getPgenLocalizationRoot();
        String fileLoczlizationPath = pgenPath + "cwaGeneratorConfig.xml";

        String cwaPath = NcPathManager.getInstance()
                .getStaticFile(fileLoczlizationPath).getAbsolutePath();
        SingleTypeJAXBManager<CWAGeneratorConfigXML> jaxb = SingleTypeJAXBManager
                .createWithoutException(CWAGeneratorConfigXML.class);
        try {
            cwaConfigs = jaxb.unmarshalFromXmlFile(cwaPath);
        } catch (SerializationException e) {
            logger.error("Failed to get localization for CWA Generator.", e);
        }
        cwaConfigs.filterVolcanoStations();
        originalCwsuID = cwaConfigs.getCwsuId();

        boolean found = false;
        String badID = originalCwsuID;
        for (String site : sites) {
            if (site.equalsIgnoreCase(originalCwsuID)) {
                found = true;
                break;
            }
        }

        if (found) {
            if (!cwaConfigs.getKcwsuId()
                    .equalsIgnoreCase("K" + originalCwsuID)) {
                found = false;
                badID = cwaConfigs.getKcwsuId();
            }
        }
        if (!found) {
            MessageBox messageBox = new MessageBox(getShell(),
                    SWT.YES | SWT.NO);
            messageBox.setText("Invalid CWSU SITE Name");
            String message = "CWSU site name " + badID
                    + " is invalid. Continue?";
            messageBox.setMessage(message);
            int buttonId = messageBox.open();
            if (buttonId == SWT.NO) {
                close();
            }
        }
    }

    /**
     * Create Time Control
     */
    private void createTimeControl() {
        timeLbl = new Label(top, SWT.CENTER);
        GridData timeGd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 2,
                1);
        timeGd.widthHint = 200;
        timeLbl.setLayoutData(timeGd);
        updateTimeLbl();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                top.getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        updateTimeLbl();
                    }
                });
            }
        };

        timeLblTimer = new Timer();
        timeLblTimer.schedule(timerTask, 0, 1000);

        timerTask = new TimerTask() {
            @Override
            public void run() {
                top.getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!cwaTexts[0].getParent().isDisposed()) {
                            updateProductStatus();
                        }
                    }
                });
            }
        };

        refreshTimer = new Timer();
        refreshTimer.schedule(timerTask, 0, updateInterval * 60 * 1000);

        top.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                timeLblTimer.cancel();
                refreshTimer.cancel();
            }
        });
    }

    /**
     * update time label
     */
    private void updateTimeLbl() {
        if (!timeLbl.isDisposed()) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss");
            Calendar cal = TimeUtil.newGmtCalendar();
            timeLbl.setText(sdf.format(cal.getTime()) + "Z");
        }
    }

    /**
     * update Product status
     */
    private void updateProductStatus() {
        for (int i = 0; i < MAX_CWA; i++) {
            CWAProduct cwaProduct = new CWAProduct(
                    cwaConfigs.getAwipsNode() + cwaButtons[i].getText(),
                    cwaConfigs.getCwsuId(), cwaConfigs.isOperational());
            cwaTexts[i].setText(
                    cwaProduct.getSeries() + "      " + cwaProduct.getExpire());
            cwaTexts[i].setBackground(cwaProduct.getColor());
        }

        CWAProduct cwaProduct = new CWAProduct(
                cwaConfigs.getAwipsNode() + misBtn.getText(),
                cwaConfigs.getCwsuId(), cwaConfigs.isOperational());
        misTxt.setText(
                cwaProduct.getSeries() + "      " + cwaProduct.getExpire());
        misTxt.setBackground(cwaProduct.getColor());

    }

    @Override
    public void setMouseHandlerName(String mhName) {
        mouseHandlerName = mhName;
    }

    @Override
    public void setAttrForDlg(IAttribute ia) {
    }
}
