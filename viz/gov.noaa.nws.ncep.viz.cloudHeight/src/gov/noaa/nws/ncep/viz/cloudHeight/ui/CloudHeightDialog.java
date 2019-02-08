package gov.noaa.nws.ncep.viz.cloudHeight.ui;

import javax.measure.converter.ConversionException;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.ICloseCallbackDialog;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors;

/**
 * Cloud Height Dialog.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 04/30/09     #106        Greg Hull   Initial creation
 * 06/23/09     *109        M. Li       Use NCCursor POINT_SELECT cursor
 * 11/24/09                 Greg Hull   migrate to to11d6; add commented out code for
 *                                      STATION_DATA (partially implemented)
 * 11/18/10      327        M. Li       add isAlreadyOpen
 * 03/01/12      524        B. Hebbard  - Add 'Take Control' button (and related changes to
 *                                        allow mutual operation with other Modal Tools) (TTR#11)
 *                                      - Make 'Sounding Source Distance' display "N/A" vs. "NaN" (TTR#13)
 *                                      - Make 'Status Messages' display scrollable (TTR#15)
 *                                      - Widen fields to display full Sounding Source Time
 * 03/28/12      N/A        B. Hebbard  Modify 'Take Control' refactor above, to fix error when reopening
 *                                      Cloud Height tool after it had been closed
 * 09/16/2016    R15716     S. Russell  Updated the open() method.
 *                                      Updated createDialog() method, left
 *                                      justify field names, widen fields
 * Feb  1, 2019  7570       tgurney     Add close callback support
 * Feb  8, 2018  7579       tgurney     Fix "Take Control" to actually activate
 *                                      the tool
 * </pre>
 *
 */
public class CloudHeightDialog extends Dialog implements ICloseCallbackDialog {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(CloudHeightDialog.class);

    private Shell shell;

    private String dlgTitle = null;

    private boolean isOpen = false;

    private Text lat_txt = null;

    private Text lon_txt = null;

    private Text snd_src_txt = null;

    private Text snd_time_txt = null;

    private Text dist_txt = null;

    private Text pixel_txt = null;

    private Text temp_txt = null;

    // the primary cloud height level
    private Text hght1_txt = null;

    private Text pres1_txt = null;

    private Combo dist_units_combo = null;

    private Combo temp_units_combo = null;

    private Combo hght_units_combo = null;

    private List alt_cloud_lvls_list = null;

    private Text sts_txt = null;

    private CloudHeightAction associatedCloudHeightAction;

    private static CloudHeightDialog INSTANCE = null;

    private boolean closedOnce = false;

    public enum SoundingDataSourceType {
        STANDARD_ATM, STATION_DATA
    }

    public enum ComputationalMethod {
        STANDARD, MOIST_ADIABATIC
    }

    public enum PixelValueMethod {
        MAX_VALUE, MOST_FREQUENT
    }

    private CloudHeightOptionsDialog optsDlg = null;

    // NonSI.MILE;CHECK this
    private Unit<? extends Length> distWorkingUnits = SI.METER;

    // from SatResource
    private Unit<? extends Temperature> tempWorkingUnits = SI.CELSIUS;

    // from souding models and data
    private Unit<? extends Length> hghtWorkingUnits = SI.METER;

    private UnitConverter distUnitsConverter = null;

    private UnitConverter tempUnitsConverter = null;

    private UnitConverter hghtUnitsConverter = null;

    private final ListenerList<ICloseCallback> closeListeners;

    public CloudHeightDialog(Shell parent, String title) {
        super(parent);
        this.closeListeners = new ListenerList<>(ListenerList.IDENTITY);
        dlgTitle = title;

        // create the Dialog before opening it since the error
        // msg text may be set before opening the dialog.
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
        shell.setText(dlgTitle);
        // pack later
        shell.setSize(600, 800);
        init();
    }

    public static CloudHeightDialog getInstance(Shell parShell,
            CloudHeightAction anAction) {
        synchronized (CloudHeightDialog.class) {
            if (INSTANCE == null) {
                try {
                    INSTANCE = new CloudHeightDialog(parShell, "Cloud Height");
                    INSTANCE.associatedCloudHeightAction = anAction;
                } catch (Exception e) {
                    statusHandler.warn("Failed to create cloud height dialog",
                            e);
                }
            }
        }
        return INSTANCE;
    }

    public void createDialog(Composite parent) {
        Composite top_form = parent;

        FormLayout mainFormLayout = new FormLayout();

        top_form.setLayout(mainFormLayout);

        lat_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        lat_txt.setText("");
        // Set the width,height here to set the width,height of the dialog
        FormData fd = new FormData(200, 20);
        fd.top = new FormAttachment(0, 15);

        // This will shift all the text boxes and labels
        // This is where to change values to make the dialog box wider
        fd.left = new FormAttachment(0, 250);

        fd.right = new FormAttachment(100, -105);
        lat_txt.setLayoutData(fd);

        Label lat_lbl = new Label(top_form, SWT.NONE);
        lat_lbl.setText("Latitude");
        FormData fd1 = new FormData();
        fd1.bottom = new FormAttachment(lat_txt, -4, SWT.BOTTOM);

        // This statement, repeated with each field label, attaches the label
        // to the left side of the Composite (dialog) with an offset of 20 from
        // that edge
        fd1.left = new FormAttachment(0, 20);
        lat_lbl.setLayoutData(fd1);

        lon_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        lon_txt.setText("");
        fd = new FormData(105, 20);
        fd.top = new FormAttachment(lat_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(lat_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(lat_txt, 0, SWT.RIGHT);
        lon_txt.setLayoutData(fd);

        Label lon_lbl = new Label(top_form, SWT.NONE);
        lon_lbl.setText("Longitude");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(lon_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        lon_lbl.setLayoutData(fd1);

        snd_src_txt = new Text(top_form,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        snd_src_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(lon_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(lon_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(lon_txt, 0, SWT.RIGHT);
        snd_src_txt.setLayoutData(fd);

        Label snd_src_lbl = new Label(top_form, SWT.NONE);
        snd_src_lbl.setText("Sounding Data Source");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(snd_src_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        snd_src_lbl.setLayoutData(fd1);

        snd_time_txt = new Text(top_form,
                SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        snd_time_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(snd_src_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(snd_src_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(snd_src_txt, 0, SWT.RIGHT);
        snd_time_txt.setLayoutData(fd);

        Label snd_time_lbl = new Label(top_form, SWT.NONE);
        snd_time_lbl.setText("Sounding Source Time");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(snd_time_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        snd_time_lbl.setLayoutData(fd1);

        dist_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        dist_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(snd_time_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(snd_time_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(snd_time_txt, 0, SWT.RIGHT);
        dist_txt.setLayoutData(fd);

        Label dist_lbl = new Label(top_form, SWT.NONE);
        dist_lbl.setText("Sounding Source Distance");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(dist_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        dist_lbl.setLayoutData(fd1);

        dist_units_combo = new Combo(top_form, SWT.READ_ONLY);
        fd = new FormData();
        fd.bottom = new FormAttachment(dist_txt, 0, SWT.BOTTOM);
        fd.left = new FormAttachment(dist_txt, 10, SWT.RIGHT);
        dist_units_combo.setLayoutData(fd);

        dist_units_combo.add(NonSI.NAUTICAL_MILE.toString());
        dist_units_combo.add(SI.KILOMETER.toString());
        dist_units_combo.add(NonSI.MILE.toString());

        dist_units_combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Unit<? extends Length> prevUnits = optsDlg.getSndDistUnits();

                if (dist_units_combo.getSelectionIndex() == 0) {
                    optsDlg.setSndDistUnits(NonSI.NAUTICAL_MILE);
                } else if (dist_units_combo.getSelectionIndex() == 1) {
                    optsDlg.setSndDistUnits(SI.KILOMETER);
                } else if (dist_units_combo.getSelectionIndex() == 2) {
                    optsDlg.setSndDistUnits(NonSI.MILE);
                }

                distUnitsConverter = distWorkingUnits
                        .getConverterTo(optsDlg.getSndDistUnits());

                // convert the value currently displayed
                // (Or we could convert back to the working units and then call
                // setSoundingDataDistance
                convertDisplayedValue(dist_txt, prevUnits,
                        optsDlg.getSndDistUnits());
            }
        });

        pixel_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        pixel_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(dist_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(dist_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(dist_txt, 0, SWT.RIGHT);
        pixel_txt.setLayoutData(fd);

        Label pixel_lbl = new Label(top_form, SWT.NONE);
        pixel_lbl.setText("Pixel Value");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(pixel_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        pixel_lbl.setLayoutData(fd1);

        temp_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        temp_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(pixel_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(pixel_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(pixel_txt, 0, SWT.RIGHT);
        temp_txt.setLayoutData(fd);

        Label temp_lbl = new Label(top_form, SWT.NONE);
        temp_lbl.setText("Temperature");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(temp_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        temp_lbl.setLayoutData(fd1);

        temp_units_combo = new Combo(top_form, SWT.READ_ONLY);
        fd = new FormData();
        fd.bottom = new FormAttachment(temp_txt, 0, SWT.BOTTOM);
        fd.left = new FormAttachment(temp_txt, 10, SWT.RIGHT);
        temp_units_combo.setLayoutData(fd);

        temp_units_combo.add(SI.CELSIUS.toString());
        temp_units_combo.add(SI.KELVIN.toString());

        temp_units_combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Unit<? extends Temperature> prevUnits = optsDlg
                        .getTemperatureUnits();

                if (temp_units_combo.getSelectionIndex() == 0) {
                    optsDlg.setTemperatureUnits(SI.CELSIUS);
                } else if (temp_units_combo.getSelectionIndex() == 1) {
                    optsDlg.setTemperatureUnits(SI.KELVIN);
                }

                tempUnitsConverter = tempWorkingUnits
                        .getConverterTo(optsDlg.getTemperatureUnits());

                // convert the value currently displayed
                // (Or we could convert back to the working units and then call
                // setSoundingDataDistance
                convertDisplayedValue(temp_txt, prevUnits,
                        optsDlg.getTemperatureUnits());
            }
        });

        hght1_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        hght1_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(temp_txt, 15, SWT.BOTTOM);
        fd.left = new FormAttachment(temp_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(temp_txt, 0, SWT.RIGHT);
        hght1_txt.setLayoutData(fd);

        Label hght1_lbl = new Label(top_form, SWT.NONE);
        hght1_lbl.setText("Primary Cloud Height");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(hght1_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        hght1_lbl.setLayoutData(fd1);

        hght_units_combo = new Combo(top_form, SWT.READ_ONLY);
        fd = new FormData();
        fd.bottom = new FormAttachment(hght1_txt, 0, SWT.BOTTOM);
        fd.left = new FormAttachment(hght1_txt, 10, SWT.RIGHT);
        hght_units_combo.setLayoutData(fd);

        // if this order changes then need to change code that saves selection
        // to optsDlg
        hght_units_combo.add(NonSI.FOOT.toString());
        hght_units_combo.add(SI.METER.toString());

        hght_units_combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Unit<? extends Length> prevUnits = optsDlg
                        .getCloudHeightUnits();

                if (hght_units_combo.getSelectionIndex() == 0) {
                    optsDlg.setCloudHeightUnits(NonSI.FOOT);
                } else if (hght_units_combo.getSelectionIndex() == 1) {
                    optsDlg.setCloudHeightUnits(SI.METER);
                }

                hghtUnitsConverter = hghtWorkingUnits
                        .getConverterTo(optsDlg.getCloudHeightUnits());

                // convert the value currently displayed
                // (Or we could convert back to the working units and then call
                // setSoundingDataDistance
                convertDisplayedValue(hght1_txt, prevUnits,
                        optsDlg.getCloudHeightUnits());

                // if there are alternate cloud heights then parse the previous
                // values and
                // add them to the alt cloud heights list
                try {
                    UnitConverter unitCnvtr = prevUnits
                            .getConverterTo(hghtWorkingUnits);

                    String[] old_alt_cld_lvls = alt_cloud_lvls_list.getItems();

                    clearAltCloudHeights();

                    for (String old_alt_cld_lvl : old_alt_cld_lvls) {
                        String alt_str = old_alt_cld_lvl.trim();

                        try {
                            double hght_val = Double.parseDouble(
                                    alt_str.substring(0, alt_str.indexOf(' ')));
                            hght_val = unitCnvtr.convert(hght_val);

                            double prs_val = Double.parseDouble(
                                    alt_str.substring(alt_str.indexOf('/') + 1,
                                            alt_str.indexOf("mb") - 1));

                            addAltCloudHeight(hght_val, prs_val);
                        } catch (NumberFormatException nfe) {
                            statusHandler.debug("Failed to parse double value",
                                    nfe);
                            return;
                        }
                    }
                } catch (ConversionException ce) {
                    statusHandler.debug("Height unit conversion failed", ce);
                    return;
                }
            }
        });

        pres1_txt = new Text(top_form, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        pres1_txt.setText("");
        fd = new FormData(100, 20);
        fd.top = new FormAttachment(hght1_txt, 5, SWT.BOTTOM);
        fd.left = new FormAttachment(hght1_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(hght1_txt, 0, SWT.RIGHT);
        pres1_txt.setLayoutData(fd);

        Label pres1_lbl = new Label(top_form, SWT.NONE);
        pres1_lbl.setText("Primary Cloud Pressure");
        fd1 = new FormData();
        fd1.bottom = new FormAttachment(pres1_txt, -4, SWT.BOTTOM);
        fd1.left = new FormAttachment(0, 20);
        pres1_lbl.setLayoutData(fd1);

        Label pres_units_label = new Label(top_form, SWT.NONE);
        pres_units_label.setText("mb");
        fd = new FormData();
        fd.bottom = new FormAttachment(pres1_txt, -4, SWT.BOTTOM);
        fd.left = new FormAttachment(pres1_txt, 10, SWT.RIGHT);
        pres_units_label.setLayoutData(fd);

        alt_cloud_lvls_list = new List(top_form,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);

        // 44 is minimum to show 2 lines without scrollbars
        fd = new FormData(100, 44);
        fd.top = new FormAttachment(pres1_txt, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(pres1_txt, 0, SWT.LEFT);
        fd.right = new FormAttachment(pres1_txt, 30, SWT.RIGHT);

        alt_cloud_lvls_list.setLayoutData(fd);
        alt_cloud_lvls_list.deselectAll();

        Label alt_cl_lbl = new Label(top_form, SWT.NONE);
        alt_cl_lbl.setText("Alternate Cloud Levels");
        fd1 = new FormData();
        fd1.top = new FormAttachment(alt_cloud_lvls_list, 5, SWT.TOP);
        fd1.left = new FormAttachment(0, 20);
        alt_cl_lbl.setLayoutData(fd1);

        sts_txt = new Text(top_form,
                SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
        fd = new FormData(180, 44);
        fd.top = new FormAttachment(alt_cloud_lvls_list, 25, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 20);
        fd.right = new FormAttachment(100, -20);
        fd.bottom = new FormAttachment(100, -58);
        sts_txt.setLayoutData(fd);

        Label sts_lbl = new Label(top_form, SWT.NONE);
        sts_lbl.setText("Status Messages");
        fd = new FormData();
        fd.bottom = new FormAttachment(sts_txt, -2, SWT.TOP);
        fd.left = new FormAttachment(sts_txt, 0, SWT.LEFT);
        sts_lbl.setLayoutData(fd);

        Label sep_lbl = new Label(top_form, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd = new FormData();
        fd.top = new FormAttachment(sts_txt, 10, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -5);
        sep_lbl.setLayoutData(fd);

        Button close_btn = new Button(top_form, SWT.PUSH);
        fd = new FormData();
        close_btn.setText("  Close  ");
        fd.top = new FormAttachment(sep_lbl, 10, SWT.BOTTOM);
        fd.right = new FormAttachment(100, -20);
        close_btn.setLayoutData(fd);

        close_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                close();
            }
        });

        Button opts_btn = new Button(top_form, SWT.PUSH);
        fd = new FormData();
        opts_btn.setText("Options...");
        fd.top = new FormAttachment(close_btn, 0, SWT.TOP);
        fd.right = new FormAttachment(close_btn, -25, SWT.LEFT);
        opts_btn.setLayoutData(fd);

        opts_btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                //
                if (closedOnce) {
                    optsDlg = null;
                    init();
                }
                if (optsDlg != null && !optsDlg.isOpen()) {
                    optsDlg.open();
                }
            }
        });

        Button take_control_btn = new Button(top_form, SWT.PUSH);
        fd = new FormData();
        take_control_btn.setText(" Take Control ");
        fd.top = new FormAttachment(close_btn, 0, SWT.TOP);
        fd.right = new FormAttachment(opts_btn, -25, SWT.LEFT);
        take_control_btn.setLayoutData(fd);

        take_control_btn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (associatedCloudHeightAction != null) {
                    AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                            .getCurrentPerspectiveManager();
                    if (mgr != null) {
                        mgr.getToolManager()
                                .selectModalTool(associatedCloudHeightAction);
                        associatedCloudHeightAction.activate();
                    }
                }
            }
        });

        setUnitComboBoxes();
    }

    // for the text boxes that have units combo boxes this will update the value
    // in the text box when the units are changed.
    public boolean convertDisplayedValue(Text txt_wid, Unit<?> oldUnits,
            Unit<?> newUnits) {
        if (txt_wid.getText().isEmpty()) {
            return false;
        }
        try {
            UnitConverter unitCnvtr = oldUnits.getConverterTo(newUnits);

            try {
                double val = Double.parseDouble(txt_wid.getText());
                double cnvt_val = unitCnvtr.convert(val);
                txt_wid.setText(String.format("%.2f", cnvt_val));
            } catch (NumberFormatException e) {
                statusHandler.debug("Failed to format double value", e);
                txt_wid.setText("Error");
            }
        } catch (ConversionException e) {
            statusHandler.debug("Failed to convert value", e);
            txt_wid.setText("Error");
        }
        return true;
    }

    public void setLatLon(double lat, double lon) {
        lat_txt.setText(String.format("%6.2f", lat));
        lon_txt.setText(String.format("%7.2f", lon));
    }

    public boolean isOpen() {
        return isOpen;
    }

    private void init() {
        // create this first since it also stores the default values needed here
        if (optsDlg == null) {
            optsDlg = new CloudHeightOptionsDialog(shell,
                    "Cloud Height Options", this);
        }
    }

    public Object open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        Cursor prevCursor = parent.getCursor();
        Cursor crossCursor = NCCursors.getCursor(display,
                NCCursors.CursorRef.POINT_SELECT);

        parent.setCursor(crossCursor);

        if (shell == null || shell.isDisposed()) {
            shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
            shell.setText(dlgTitle);
            // pack later
            shell.setSize(600, 800);
        }

        shell.addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event event) {
                close();
            }
        });

        init();

        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 1;
        mainLayout.marginWidth = 1;
        shell.setLayout(mainLayout);
        shell.setLocation(0, 0);

        createDialog(shell);

        shell.pack();
        shell.open();
        isOpen = true;

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        // Closure: Reset cursor and execute modal tool shutdown sequence

        parent.setCursor(prevCursor);

        for (ICloseCallback callback : closeListeners) {
            callback.dialogClosed(null);
        }

        AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                .getCurrentPerspectiveManager();
        if (mgr != null) {
            mgr.getToolManager().deselectModalTool(associatedCloudHeightAction);
        }

        isOpen = false;

        return null;
    }

    public SoundingDataSourceType getSoundingDataSourceType() {
        return optsDlg.getSoundingDataSourceType();
    }

    public double getMaxSoundingDist() {
        return optsDlg.getMaxSoundingDist();
    }

    public ComputationalMethod getComputationalMethod() {
        return optsDlg.getComputationalMethod();
    }

    public PixelValueMethod getPixelValueMethod() {
        return optsDlg.getPixelValueMethod();
    }

    public int getPixelAreaDimension() {
        return optsDlg.getPixelArea();
    }

    public void setSoundingDataSource(String srcStr) {
        snd_src_txt.setText(srcStr);
    }

    public void setSoundingDataTime(String timeStr) {
        snd_time_txt.setText(timeStr);
    }

    // always in converted from user selected units.
    public void setSoundingDataDistance(double workingDist) {
        if (Double.isNaN(workingDist)) {
            dist_txt.setText("N/A");
        } else {
            try {
                double distVal = distUnitsConverter.convert(workingDist);
                dist_txt.setText(String.format("%.1f", distVal));
            } catch (ConversionException e) {
                temp_txt.setText("Conversion Error");
                statusHandler.debug("Distance unit conversion failed", e);
            }
        }
    }

    public void setPixelValue(String pixValStr) {
        pixel_txt.setText(pixValStr);
    }

    public void setTemperature(double workingTemp) {
        try {
            double tempVal = tempUnitsConverter.convert(workingTemp);
            temp_txt.setText(String.format("%.2f", tempVal));
        } catch (ConversionException e) {
            temp_txt.setText("Conversion Error");
            statusHandler.debug("Temperature unit conversion failed", e);
        }
    }

    // hght is in meters but will be converted if needed
    public void setPrimaryCloudHeight(double workingHght, double pres) {
        if (Double.isNaN(workingHght)) {
            hght1_txt.setText("N/A");
        } else {
            try {
                double cldHght = hghtUnitsConverter.convert(workingHght);
                hght1_txt.setText(String.format("%.2f", cldHght));
            } catch (ConversionException e) {
                hght1_txt.setText("Conversion Error");
                statusHandler.debug("Cloud height unit conversion failed", e);
            }
        }

        if (Double.isNaN(pres)) {
            pres1_txt.setText("N/A");
        } else {
            pres1_txt.setText(String.format("%.2f", pres));
        }
    }

    public void clearAltCloudHeights() {
        alt_cloud_lvls_list.removeAll();
    }

    /**
     * input values are in Ft and mb. The height will be converted to the units
     * selected for the primary cloud height.
     *
     * @param workingHght
     * @param pres
     */
    public void addAltCloudHeight(double workingHght, double pres) {
        try {
            double cldHght = hghtUnitsConverter.convert(workingHght);

            String altLvlStr = String.format("%-6.0f %s / %-4.0f mb", cldHght,
                    optsDlg.getCloudHeightUnits().toString(), pres);

            alt_cloud_lvls_list.add(altLvlStr);

            // make sure the first lvl is showing in the list
            alt_cloud_lvls_list.setTopIndex(0);
        } catch (ConversionException e) {
            alt_cloud_lvls_list
                    .add(String.format("%-6s / %-6.1f mb", "Error", pres));
            statusHandler.debug("Cloud height unit conversion failed", e);
        }
    }

    public void displayStatusMsg(String msg) {
        sts_txt.setText(msg);
    }

    public void appendStatusMsg(String msg) {
        sts_txt.setText(sts_txt.getText() + msg);
    }

    public void clearFields() {
        lat_txt.setText("");
        lon_txt.setText("");
        snd_src_txt.setText("");
        snd_time_txt.setText("");
        dist_txt.setText("");
        pixel_txt.setText("");
        temp_txt.setText("");
        hght1_txt.setText("");
        pres1_txt.setText("");
        alt_cloud_lvls_list.removeAll();
        sts_txt.setText("");
    }

    // set the combo widgets from the values set in the Options Dialog
    // Note that this is dependent on the order of the items in the lists.
    public void setUnitComboBoxes() {
        if (optsDlg.getSndDistUnits() == NonSI.NAUTICAL_MILE) {
            dist_units_combo.select(0);
        } else if (optsDlg.getSndDistUnits() == SI.KILOMETER) {
            dist_units_combo.select(1);
        } else if (optsDlg.getSndDistUnits() == NonSI.MILE) {
            dist_units_combo.select(2);
        }

        if (optsDlg.getTemperatureUnits() == SI.CELSIUS) {
            temp_units_combo.select(0);
        } else if (optsDlg.getTemperatureUnits() == SI.KELVIN) {
            temp_units_combo.select(1);
        }

        if (optsDlg.getCloudHeightUnits() == NonSI.FOOT) {
            hght_units_combo.select(0);
        } else if (optsDlg.getCloudHeightUnits() == SI.METER) {
            hght_units_combo.select(1);
        }

        distUnitsConverter = distWorkingUnits
                .getConverterTo(optsDlg.getSndDistUnits());

        tempUnitsConverter = tempWorkingUnits
                .getConverterTo(optsDlg.getTemperatureUnits());

        hghtUnitsConverter = hghtWorkingUnits
                .getConverterTo(optsDlg.getCloudHeightUnits());

        dist_txt.setText("");
        temp_txt.setText("");
        hght1_txt.setText("");
        pixel_txt.setText("");
        pres1_txt.setText("");
        clearAltCloudHeights();
    }

    // the units that the data is in for use in the UnitConverters
    //
    public void setWorkingUnits(Unit<? extends Length> distUnits,
            Unit<? extends Temperature> tempUnits,
            Unit<? extends Length> hghtUnits) {
        distWorkingUnits = distUnits;
        optsDlg.setDistWorkingUnits(distWorkingUnits);
        tempWorkingUnits = tempUnits;
        hghtWorkingUnits = hghtUnits;
    }

    /**
     * @return the distWorkingUnits
     */
    public final Unit<? extends Length> getDistWorkingUnits() {
        return distWorkingUnits;
    }

    /**
     * @return the tempWorkingUnits
     */
    public final Unit<? extends Temperature> getTempWorkingUnits() {
        return tempWorkingUnits;
    }

    /**
     * @return the hghtWorkingUnits
     */
    public final Unit<? extends Length> getHghtWorkingUnits() {
        return hghtWorkingUnits;
    }

    /**
     * closes the Cloud Height Dialog
     */
    public void close() {
        if (shell != null) {
            shell.dispose();
        }
        isOpen = false;
        closedOnce = true;
    }

    public int getMaxValidIntervalInHoursForStationData() {
        return CloudHeightOptionsDialog.getMaxIntervalForStationDataTime();
    }

    public boolean isPixelValueFromSinglePixel() {
        return CloudHeightOptionsDialog.isPixelValueFromSinglePixel();
    }

    @Override
    public void addCloseCallback(ICloseCallback callback) {
        closeListeners.add(callback);
    }
}
