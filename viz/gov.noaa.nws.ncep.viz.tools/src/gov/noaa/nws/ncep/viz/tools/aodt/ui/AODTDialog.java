package gov.noaa.nws.ncep.viz.tools.aodt.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.measure.UnitConverter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.ICloseCallbackDialog;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource;
import gov.noaa.nws.ncep.viz.tools.aodt.natives.AODTv64Native;
import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors;
import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors.CursorRef;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;
import si.uom.SI;

/**
 * AODT History file management Dialog.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 08/25/09      150        M. Li       Initial creation
 * 08/11/11                 Chin Chen   1.Fixed AODT can not retrieve IR temp issue
 *                                      2.Fixed AODT crash CAVE issue when invalid data retrieved from database
 *                                      3.Changed to retrieve IR data only when user click Run AODT button
 * 07/02/12                 S. Jacobs & 1.Fixed AODT Lat/Lon text input issue
 *                          J. Bartlett 2.Monospace font for the AODT output
 * 02/11/13      972        G. Hull     AbstractEditor instead of NCMapEditor
 * 01/19/2016    5054       randerso    Remove dummy shell
 * 11/07/18      #7552      dgilling    Allow tool to work with arbitrary
 *                                      NcSatResources.
 * Feb  1, 2019  7570       tgurney     Add close callback support
 * Feb 15, 2019  7562       tgurney     Correctly set the previous cursor
 *                                      when closing the dialog.
 *
 * </pre>
 *
 * @author M. Li
 */
public class AODTDialog extends Dialog implements ICloseCallbackDialog {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private AODTv64Native odt;

    private Shell shell;

    private String dlgTitle = null;

    private Font font;

    private Text lat_txt = null;

    private Text lon_txt = null;

    private Combo stormHisFile_combo = null;

    private Combo domain_combo = null;

    private Combo land_combo = null;

    private Combo search_combo = null;

    private Combo sceneType_combo = null;

    private Text AODTResult = null;

    private Text saveTo_txt = null;

    private float centerLat;

    private float centerLon;

    private static final int NUMX = 105;

    private float[] temps = new float[NUMX * NUMX];

    private float[] lats = new float[NUMX * NUMX];

    private float[] lons = new float[NUMX * NUMX];

    private AODTHistFileMngDialog histFileMngDlg = null;

    private String domain_options[] = { "DEF", "ATL", "PAC" };

    private String yesno[] = { "NO", "YES" };

    private String sceneTypes[] = { "COMPUTED", "CLEAR", "PINHOLE",
            "LARGE CLEAR", "LARGE RAGGED", "RAGGED", "OBSCURED", "UNIFORM CDO",
            "EMBEDDED CENTER", "IRREGULAR CDO", "CURVED BAND", "SHEAR" };

    private DataTime currentDate;

    private String history_file = null;

    private String AODT_DIR = LocalizationManager.getUserDir();

    private NcSatelliteResource satRsc = null;

    private AbstractEditor mapEditor = null;

    private UnitConverter tempUnitsConverter = null;

    private final ListenerList<ICloseCallback> closeListeners;

    private void getSatRsc() {
        if (mapEditor != null) {
            ResourceList rscs = NcEditorUtil.getDescriptor(mapEditor)
                    .getResourceList();

            for (ResourcePair r : rscs) {
                if (r.getResource() instanceof NcSatelliteResource
                        && ((NcSatelliteResource) r.getResource())
                                .isCloudHeightCompatible()) {
                    satRsc = (NcSatelliteResource) r.getResource();

                    if (satRsc.getTemperatureUnits() != SI.KELVIN) {
                        tempUnitsConverter = satRsc.getTemperatureUnits()
                                .getConverterTo(SI.KELVIN);
                    }
                    return;
                }
            }
        }
    }

    public AODTDialog(Shell parent, String title) {
        super(parent);
        this.closeListeners = new ListenerList<>(ListenerList.IDENTITY);
        dlgTitle = title;
        mapEditor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (NcEditorUtil
                .getNcDisplayType(mapEditor) == NcDisplayType.NMAP_DISPLAY) {
            getSatRsc();
        }
    }

    public Object open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        // Set cursor to cross
        CursorRef prevCursorRef = NCCursors.getInstance()
                .getCursorRef(parent.getCursor()).orElse(null);
        NCCursors.getInstance().setCursor(parent,
                NCCursors.CursorRef.POINT_SELECT);

        shell = new Shell(parent, SWT.DIALOG_TRIM);
        shell.setText(dlgTitle);

        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);
        shell.setLocation(0, 0);

        font = new Font(shell.getDisplay(), "Monospace", 8, SWT.BOLD);

        // Initialize all of the controls and layouts
        initializeComponents();

        shell.pack();

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        font.dispose();
        NCCursors.getInstance().setCursor(parent, prevCursorRef);

        for (ICloseCallback callback : closeListeners) {
            callback.dialogClosed(null);
        }

        return null;
    }

    /**
     * closes the AODT Dialog
     */
    public void close() {
        if (shell != null) {
            shell.dispose();
        }
    }

    /**
     * Initialize the dialog components.
     */
    private void initializeComponents() {
        odt = AODTv64Native.getInstance();

        createCenterLocationControls();
        addSeparator();
        createStormHistoryFileControls();
        setHistoryFileCombo();
        createDomainControls();
        createSceneTypeControls();
        createAODTResultControls();
        createPrintSaveControls();
        addSeparator();
        createCloseButton();

    }

    private void createCenterLocationControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 10;
        comp.setLayout(gl);

        Label centerLoc = new Label(comp, SWT.LEFT);
        centerLoc.setText("Center Location:");

        lat_txt = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        lat_txt.setLayoutData(new GridData(90, SWT.DEFAULT));
        lat_txt.setText("");

        lon_txt = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        lon_txt.setLayoutData(new GridData(90, SWT.DEFAULT));
        lon_txt.setText("");
    }

    private void createStormHistoryFileControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 10;
        comp.setLayout(gl);

        Label stormHisFile = new Label(comp, SWT.LEFT);
        stormHisFile.setText("Storm/History File:");

        stormHisFile_combo = new Combo(comp, SWT.DROP_DOWN);
        stormHisFile_combo.setLayoutData(new GridData(200, SWT.DEFAULT));
        stormHisFile_combo.setText("");
        stormHisFile_combo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                history_file = stormHisFile_combo.getText().trim();
                if (history_file != null && history_file.length() > 0
                        && !history_file.endsWith(".hst_64")) {
                    history_file = history_file + ".hst_64";
                    stormHisFile_combo.setText(history_file);
                }
            }
        });

    }

    private void createDomainControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(6, false);
        gl.marginWidth = 10;
        comp.setLayout(gl);

        new Label(comp, SWT.LEFT).setText("Domain ");
        domain_combo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        domain_combo.setItems(domain_options);
        domain_combo.select(0);

        new Label(comp, SWT.LEFT).setText("   Land  ");
        land_combo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        land_combo.setItems(yesno);
        land_combo.select(0);

        new Label(comp, SWT.LEFT).setText("   Search  ");
        search_combo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        search_combo.setItems(yesno);
        search_combo.select(0);

    }

    private void createSceneTypeControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 10;
        comp.setLayout(gl);

        new Label(comp, SWT.LEFT).setText("Scene Type  ");
        sceneType_combo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        sceneType_combo.setItems(sceneTypes);
        sceneType_combo.select(0);

        Button runAODT = new Button(comp, SWT.PUSH);
        runAODT.setText("   Run AODT   ");
        runAODT.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                run_AODT();
            }
        });

    }

    protected void run_AODT() {
        if (satRsc == null) {
            AODTResult.setText("Please load an IR image first!!");
            return;
        }

        // CHIN Begin
        // Update center location

        // Calculate temperatures for a tileset of 105 by 105 pixels
        // Retrieve raw data from centerLat and centerLon
        centerLat = Float.valueOf(lat_txt.getText().trim()).floatValue();
        centerLon = Float.valueOf(lon_txt.getText().trim()).floatValue();
        IDescriptor descr = NcEditorUtil.getDescriptor(mapEditor);

        double[] p1 = descr.worldToPixel(new double[] { centerLon, centerLat });
        int rad = NUMX / 2;
        for (int i = -rad; i <= rad; i++) {
            for (int j = -rad; j <= rad; j++) {
                double[] ll = descr
                        .pixelToWorld(new double[] { p1[0] + i, p1[1] + j });
                int indx = (i + rad) * NUMX + (j + rad);
                lats[indx] = (float) ll[1];
                lons[indx] = (float) ll[0];
                Double tmpC = satRsc.getSatIRTemperature(
                        new Coordinate(lons[indx], lats[indx]));
                // chin, AODT native library has reverse sign on longitude with
                // Sat resource used
                lons[indx] = lons[indx] * -1.0F;

                if (tmpC == null || tmpC.isNaN()) {
                    AODTResult.setText("IR data retrieval failed!!");
                    return;
                    // Chin: do not input bad number to native AODT lib,
                    // otherwise will crash CAVE

                } else if (tempUnitsConverter != null) {
                    temps[indx] = tempUnitsConverter.convert(tmpC).floatValue();
                } else {
                    temps[indx] = tmpC.floatValue();
                }
            }
        }
        // Update current date
        setCurrentDate(satRsc.getCurrentFrameTime());

        odt.setCenterLatLon(centerLat, centerLon);
        odt.setHistoryFile(history_file);
        odt.setCurrentDate(currentDate);
        odt.setIdxDomain(domain_combo.getSelectionIndex());
        odt.setIdxLand(land_combo.getSelectionIndex());
        odt.setIdxSearch(search_combo.getSelectionIndex());
        odt.setIdxSceneType(sceneType_combo.getSelectionIndex());
        odt.setIRImageInfo(lats, lons, temps);

        String output = null;
        int ier = odt.run_aodt();
        if (ier < 0) {
            output = "Calculation aborted, error occured somewhere!";
        } else {
            output = odt.getOuput();
        }
        if (output != null) {
            font = new Font(AODTResult.getShell().getDisplay(), "Monospace", 10,
                    SWT.NORMAL);
            AODTResult.setFont(font);
            AODTResult.setText(output);
        }

        if (verifyHistoryFile()) {
            setHistoryFileCombo();
        }
    }

    private void createAODTResultControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        comp.setLayout(gl);

        Group dspGroup = new Group(comp, SWT.NONE);
        dspGroup.setLayout(new GridLayout(1, false));

        AODTResult = new Text(dspGroup, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
        AODTResult.setLayoutData(new GridData(420, 560));
        AODTResult.setText("");

    }

    private void createPrintSaveControls() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = 20;
        comp.setLayout(gl);

        Button print_button = new Button(comp, SWT.PUSH);
        print_button.setText("Print...");

        print_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                String result = AODTResult.getText();
                if (result == null || result.length() <= 0) {
                    MessageBox messageBox = new MessageBox(shell,
                            SWT.ICON_WARNING | SWT.OK);
                    messageBox.setText("Warning");
                    messageBox.setMessage("No results to print!   ");
                    messageBox.open();
                    return;
                }

                PrintDialog printDialog = new PrintDialog(shell, SWT.NONE);
                printDialog.setText("Print");
                PrinterData printerData = printDialog.open();
                if (!(printerData == null)) {
                    Printer p = new Printer(printerData);
                    p.startJob("PrintJob");
                    p.startPage();
                    Rectangle trim = p.computeTrim(0, 0, 0, 0);
                    Point dpi = p.getDPI();
                    int leftMargin = dpi.x + trim.x;
                    int topMargin = dpi.y / 2 + trim.y;
                    GC gc = new GC(p);
                    Font font = gc.getFont();

                    String[] resultArray = result.split("\n");

                    int heightOffset = 0;
                    for (String printText : resultArray) {
                        printText = printText.trim() + "\n";
                        heightOffset += 2 * font.getFontData()[0].getHeight();
                        gc.drawString(printText, leftMargin,
                                topMargin + heightOffset);
                    }

                    p.endPage();
                    gc.dispose();
                    p.endJob();
                    p.dispose();
                }
            }
        });

        Button saveTo_button = new Button(comp, SWT.PUSH);
        saveTo_button.setText(" Save to  ");
        saveTo_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                saveTo_file();
            }
        });

        saveTo_txt = new Text(comp, SWT.SINGLE | SWT.BORDER);
        saveTo_txt.setLayoutData(new GridData(150, SWT.DEFAULT));
        saveTo_txt.setText("");

        Button clear_button = new Button(comp, SWT.PUSH);
        clear_button.setText("  Clear  ");
        clear_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                stormHisFile_combo.setText("");
                AODTResult.setText("");
                saveTo_txt.setText("");
            }
        });
    }

    private void createCloseButton() {
        Composite comp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(2, true);
        gl.marginWidth = 20;
        comp.setLayout(gl);

        Button histFileMng_button = new Button(comp, SWT.PUSH);
        histFileMng_button.setText("History File Management...");
        histFileMng_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                if (histFileMngDlg == null) {
                    histFileMngDlg = new AODTHistFileMngDialog(shell);
                }

                if (histFileMngDlg != null && !histFileMngDlg.isOpen()) {
                    history_file = stormHisFile_combo.getText().trim();
                    if (verifyHistoryFile()) {
                        odt.setHistoryFile(history_file);
                    }
                    histFileMngDlg.open();
                }
            }
        });

        Button close_button = new Button(comp, SWT.PUSH);
        close_button.setText("   Close   ");
        close_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                shell.dispose();
            }
        });

    }

    private void addSeparator() {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        Label sepLbl = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);
    }

    public void setLatLon(double lat, double lon) {
        lat_txt.setText(String.format("%8.4f", lat));
        lon_txt.setText(String.format("%9.4f", lon));
        this.centerLat = (float) lat;
        this.centerLon = (float) lon;
    }

    public boolean isOpen() {
        return shell != null && !shell.isDisposed();
    }

    public NcSatelliteResource getSatResource() {
        return satRsc;
    }

    public DataTime getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(DataTime currentDate) {
        this.currentDate = currentDate;
    }

    public void setIRImageInfo(float[] lat, float[] lon, float[] temp) {
        this.lats = lat;
        this.lons = lon;
        this.temps = temp;
    }

    private void setHistoryFileCombo() {
        File dir = new File(AODT_DIR);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".hst_64");
            }
        };

        String[] files = dir.list(filter);
        if (files != null) {
            stormHisFile_combo.setItems(files);
        }
        if (verifyHistoryFile()) {
            stormHisFile_combo.setText(history_file);
        }
    }

    private boolean verifyHistoryFile() {
        if (history_file == null || history_file.length() <= 0) {
            return false;
        } else {
            return true;
        }
    }

    private void saveTo_file() {
        String file = saveTo_txt.getText().trim();
        if (file != null && !file.isEmpty()) {
            file = AODT_DIR + file;
            try (BufferedWriter output = new BufferedWriter(
                    new FileWriter(file))) {

                output.write(AODTResult.getText());
                output.write("\n");
                output.close();
            } catch (IOException e) {
                statusHandler.warn("Failed to write file " + file, e);
            }
        } else {
            MessageBox messageBox = new MessageBox(shell,
                    SWT.ICON_WARNING | SWT.OK);
            messageBox.setText("Message");
            messageBox.setMessage("Please specify a file name!");
            messageBox.open();
        }
    }

    @Override
    public void addCloseCallback(ICloseCallback callback) {
        closeListeners.add(callback);
    }

}