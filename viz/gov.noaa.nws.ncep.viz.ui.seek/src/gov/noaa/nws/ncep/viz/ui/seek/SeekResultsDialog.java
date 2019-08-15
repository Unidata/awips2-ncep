package gov.noaa.nws.ncep.viz.ui.seek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;

import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.viz.common.LocatorUtil;
import gov.noaa.nws.ncep.viz.tools.cursor.NCCursors;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * This class displays the SEEK results in National Centers perspective.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * March 2009    86       M. Li     Initial creation.
 * June  2009    109      M. Li     Add POINT_SELECT cursor
 * Sept  2009    169      G. Hull   NCMapEditor
 * Nov   2010    337      G. Zhang  TTRs fixing.
 * Dec   2010    351      Archana   Added a reference to the SeekResultsAction
 *                                  object that created the dialog. Altered the
 *                                  base class to Dialog instead of
 *                                  CaveSWTDialog. Added a DisposeListener to
 *                                  the close button to remove the seek layer.
 *                                  Added functionality to the "Take Control"
 *                                  button to match legacy.
 * March  2011   351      Archana   Changed the path to save the CPF file - it
 *                                  is now under the user's home directory.
 * Jan    2012   561      G. Hull   make independent of Locator project.
 * Feb 22, 2012  644      Q.Zhou    Added unit NM. Fixed point1 text(double
 *                                  units).
 * Apr 12, 2019  7803     K. Sunil  changes to make SeekTool work on D2D
 *                                  perspective
 * May 08, 2019  63530    tjensen   Fix Take Control button to resolve editable
 *                                  conflicts
 * Aug 14, 2019  65755    tjensen   Update open to not block as this doesn't
 *                                  listen for events
 *
 * </pre>
 *
 * @author mli
 *
 */
public class SeekResultsDialog extends Dialog {

    private SeekResultsAction associatedSeekAction;

    private List resultList;

    // The expandable List Widget.
    private List resultList2;

    /**
     * Dialog shell.
     */
    private Shell shell;

    protected Shell getShell() {
        return shell;
    }

    /**
     * Font used for the list controls.
     */
    private Font font;

    // Result list title
    private Label title = null;

    private Label title2 = null;

    private Button button1 = null;

    private Button button2 = null;

    private Text text1 = null;

    private Text text2 = null;

    private Combo combo1 = null;

    private int combo1Index = 0;

    private Combo combo2 = null;

    private int combo2Index = 0;

    private Button clickPoint1 = null;

    private Label point1 = null;

    private Label point2 = null;

    private Text distance = null;

    private Combo distCombo = null;

    private Combo dirCombo = null;

    private int distUnitIndex = 0;

    private int dirUnitIndex = 0;

    private SeekPointData[] closePoints = null;

    private Coordinate coordinate = null;

    private int currentPointID = 0;

    private final ClickPointData[] clickPtData = new ClickPointData[] {
            new ClickPointData(), new ClickPointData() };

    private boolean isClicked = false;

    private double pointDistance = -999.99;

    // 0 = point 1->2, 1 = point 2->1;
    private int pointOrder = 0;

    // Key is the Shell's toString() value. We don't want the same dialog to be
    // shared across D2D and NCP for example.
    private static Map<String, SeekResultsDialog> seekDialogInstanceMap = new HashMap<>();

    private int limitNo = 1;

    // Fields for keeping information in Widgets.
    private String txt1txt = "", txt2txt = "";

    private String point1Txt = "", point2Txt = "", distanceTxt = "";

    private SeekPointData[] closePoints1 = null, closePoints2 = null;

    // Composites for the expandable List Widget
    private Composite resultsComp = null;

    private Composite distComp = null;

    private Composite centeredComp = null;

    // Flag for opening the expandable List Widget
    private static boolean NotFirstOpen = false;

    // Flags for setting inputs in the two Text fields
    private static boolean txtNotSet1 = true;

    private static boolean txtNotSet2 = true;

    protected static final String DISTANCEUNIT_OPTIONS[] = { "NM", "SM", "KM" };

    protected static final String DIRECTIONUNIT_OPTIONS[] = { "16 point",
            "degrees" };

    /**
     * Constructor.
     *
     * @param parent
     *            Parent shell.
     */
    public SeekResultsDialog(Shell parent) {
        super(parent);
    }

    public static SeekResultsDialog getInstance(Shell parShell,
            SeekResultsAction anAction) {

        if (seekDialogInstanceMap.get(parShell.toString()) == null) {
            try {
                SeekResultsDialog sd = new SeekResultsDialog(parShell);
                sd.associatedSeekAction = anAction;
                seekDialogInstanceMap.put(parShell.toString(), sd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return seekDialogInstanceMap.get(parShell.toString());
    }

    /**
     * Open method used to display the Seek Tool dialog.
     */
    public void open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();
        Cursor prevCursor = parent.getCursor();
        Cursor crossCursor = NCCursors.getCursor(display,
                NCCursors.CursorRef.POINT_SELECT);

        shell = new Shell(parent, SWT.DIALOG_TRIM);
        shell.setText("Seek Results");
        parent.setCursor(crossCursor);

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

        font.dispose();
        crossCursor.dispose();

        // copy with Widget disposed Exception
        try {
            parent.setCursor(prevCursor);
        } catch (Exception e) {
            System.out.println("___ SeekResultsDialog open(): Exception: "
                    + e.getMessage());
        }

        AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                .getCurrentPerspectiveManager();
        if (mgr != null) {
            mgr.getToolManager().deselectModalTool(associatedSeekAction);
        }
    }

    /**
     * Initialize the dialog components.
     */
    private void initializeComponents() {

        createSeekResultsControls();
        createDisplayOptionControls();
        createClickPointControls();

        if (NotFirstOpen) {
            createResultList2();
        }

        createPointsDistanceControls();
        addSeparator();
        createButtons();

        setResults();
        setClickPtText();
        setDrawing();
    }

    private void createSeekResultsControls() {
        Composite resultsComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        resultsComp.setLayout(gl);
        Composite titleComp = new Composite(resultsComp, SWT.NONE);
        GridLayout titleGd = new GridLayout(2, false);
        titleComp.setLayout(titleGd);
        GridData tgd1 = new GridData(160, SWT.DEFAULT);
        title = new Label(titleComp, SWT.LEFT);
        title.setLayoutData(tgd1);
        title.setText("Station name");

        GridData tgd2 = new GridData(205, SWT.DEFAULT);
        title2 = new Label(titleComp, SWT.LEFT);
        title2.setLayoutData(tgd2);
        title2.setText("LAT      LON             Distance ");

        GridData gd = new GridData(365, 80);
        resultList = new List(resultsComp,
                SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        resultList.setLayoutData(gd);
        resultList.setFont(font);
        resultList.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // no operation
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = resultList.getSelectionIndex();
                if (closePoints != null) {
                    Coordinate c = new Coordinate(closePoints[index].getLon(),
                            closePoints[index].getLat());
                    clickPtData[currentPointID].setCoord(c);
                    setResults();
                    drawLine(clickPtData[0].getCoord(),
                            clickPtData[1].getCoord());
                }

            }

        });

    }

    private void createDisplayOptionControls() {
        Composite displayOptionComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        displayOptionComp.setLayout(gl);

        Group dspGroup = new Group(displayOptionComp, SWT.NONE);
        dspGroup.setLayout(new GridLayout(6, false));

        // Distance display options
        Label label = new Label(dspGroup, SWT.NONE);
        label.setText("  Dist \n  Units ");
        label.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));

        distCombo = new Combo(dspGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        distCombo.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));

        int size = DISTANCEUNIT_OPTIONS.length;
        for (int i = 0; i < size; i++) {
            distCombo.add(DISTANCEUNIT_OPTIONS[i]);
        }
        distCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                distUnitIndex = distCombo.getSelectionIndex();
                formatResults();
                formatPointDist();
            }
        });
        distCombo.select(distUnitIndex);

        // Direction display options
        Label dirLabel = new Label(dspGroup, SWT.NONE);
        dirLabel.setText("         Dir \n        Units ");
        dirLabel.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));

        dirCombo = new Combo(dspGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        dirCombo.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));

        for (int i = 1; i < DIRECTIONUNIT_OPTIONS.length; i++) {
            dirCombo.add(DIRECTIONUNIT_OPTIONS[i]);
        }

        dirCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                dirUnitIndex = dirCombo.getSelectionIndex();
                formatResults();
                formatPointDist();
            }
        });
        dirCombo.select(dirUnitIndex);

        // Limit of display numbers
        Label limitLabel = new Label(dspGroup, SWT.NONE);
        limitLabel.setText("       Limit: ");
        limitLabel.setLayoutData(new GridData());

        final Spinner spinner = new Spinner(dspGroup, SWT.BORDER);
        spinner.setLayoutData(new GridData());
        spinner.setMinimum(1);
        spinner.setMaximum(25);
        spinner.setSelection(limitNo);

        spinner.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                limitNo = spinner.getSelection();
                formatResults();
            }
        });

    }

    private void createClickPointControls() {

        Composite clickComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = 5;
        clickComp.setLayout(gl);

        button1 = new Button(clickComp, SWT.PUSH);
        button1.setText("1");
        button1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                currentPointID = 0;
                setResults();
                setClickPtText();
            }
        });

        text1 = new Text(clickComp, SWT.BORDER);
        text1.setLayoutData(new GridData(130, SWT.DEFAULT));

        text1.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                // no operation
            }

            @Override
            public void keyReleased(KeyEvent e) {
                doKeyPressed(0, combo1, text1, e);
            }
        });

        combo1 = new Combo(clickComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo1.add("LATLON");

        clickPoint1 = new Button(clickComp, SWT.PUSH);
        clickPoint1.setText("Click Point");
        clickPoint1.setLayoutData(new GridData(100, SWT.DEFAULT));

        button2 = new Button(clickComp, SWT.PUSH);
        button2.setText("2");
        button2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                currentPointID = 1;
                setResults();
                setClickPtText();
            }
        });

        text2 = new Text(clickComp, SWT.BORDER);
        text2.setLayoutData(new GridData(130, SWT.DEFAULT));

        text2.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                // no operation
            }

            @Override
            public void keyReleased(KeyEvent e) {
                doKeyPressed(1, combo2, text2, e);
            }
        });

        combo2 = new Combo(clickComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo2.add("LATLON");

        try {
            combo1.setItems(SeekInfo.getStnTypes());
            combo2.setItems(SeekInfo.getStnTypes());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Button clickPoint2 = new Button(clickComp, SWT.PUSH);
        clickPoint2.setText("Click Point");
        clickPoint2.setLayoutData(new GridData(100, SWT.DEFAULT));

        clickPoint1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                currentPointID = 0;
                clickPtData[0].setActivated(true);
                clickPtData[1].setActivated(false);
                setResults();
            }
        });

        clickPoint2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                currentPointID = 1;
                clickPtData[1].setActivated(true);
                clickPtData[0].setActivated(false);
                setResults();
            }
        });

        combo1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

                currentPointID = 0;
                combo1Index = combo1.getSelectionIndex();
                clickPtData[0].setLocatorName(combo1.getText());
                setResults();
                setClickPtText();

                SeekInfo.preQueryDB(combo1.getItem(combo1Index),
                        currentPointID);
            }
        });

        combo2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

                currentPointID = 1;
                combo2Index = combo2.getSelectionIndex();
                clickPtData[1].setLocatorName(combo2.getText());
                setResults();
                setClickPtText();

                SeekInfo.preQueryDB(combo2.getItem(combo2Index),
                        currentPointID);
            }
        });

        combo1.select(combo1Index);
        combo2.select(combo2Index);
        clickPtData[0].setLocatorName(combo1.getText());
        clickPtData[1].setLocatorName(combo2.getText());

        text1.setText(txt1txt);
        text2.setText(txt2txt);
    }

    private void createPointsDistanceControls() {

        distComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = 10;
        distComp.setLayout(gl);

        point1 = new Label(distComp, SWT.NONE);
        point1.setText("Point 1 is ");

        if (point1Txt.isEmpty()) {
            point1Txt = point1.getText();
        } else {
            point1.setText(point1Txt);
        }

        distance = new Text(distComp, SWT.READ_ONLY | SWT.BORDER);
        distance.setLayoutData(new GridData(130, SWT.DEFAULT));

        if (distanceTxt.isEmpty()) {
            distanceTxt = distance.getText();
        } else {
            distance.setText(distanceTxt);
        }

        point2 = new Label(distComp, SWT.NONE);
        point2.setText("  of point 2      ");

        if (point2Txt.isEmpty()) {
            point2Txt = point2.getText();
        } else {
            point2.setText(point2Txt);
        }

        Button button12 = new Button(distComp, SWT.PUSH);

        Font arrowFont = new Font(shell.getDisplay(), "Monospace", 5, SWT.BOLD);
        button12.setFont(arrowFont);
        button12.setText("--->\n<---");
        button12.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                pointOrder = (pointOrder + 1) % 2;
                formatPointDist();
            }
        });

    }

    private void addSeparator() {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        Label sepLbl = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);
    }

    /**
     * Create the default and close button.
     */
    private void createButtons() {

        centeredComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(3, true);
        centeredComp.setLayout(gl);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        centeredComp.setLayoutData(gd);

        Button takeCtrlBtn = new Button(centeredComp, SWT.NONE);
        takeCtrlBtn.setText("Take Control");
        takeCtrlBtn.setLayoutData(gd);
        takeCtrlBtn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (associatedSeekAction != null) {

                    AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                            .getCurrentPerspectiveManager();
                    if (mgr != null) {
                        mgr.getToolManager()
                                .selectModalTool(associatedSeekAction);
                        associatedSeekAction.setEnabled(false);
                        try {
                            mgr.getToolManager().activateToolSet(
                                    associatedSeekAction.getCommandId());
                        } catch (VizException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                    associatedSeekAction.takeControl();
                }

            }
        });

        Button saveCPFBtn = new Button(centeredComp, SWT.NONE);
        saveCPFBtn.setText("Save CPF");
        saveCPFBtn.setLayoutData(gd);

        saveCPFBtn.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                saveCPF();
            }
        });

        Button closeBtn = new Button(centeredComp, SWT.NONE);
        closeBtn.setText("Close");
        closeBtn.setLayoutData(gd);
        closeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });

    }

    public void setPosition(Coordinate ll) {

        this.coordinate = ll;
        isClicked = true;

        // Set relative distance
        if (clickPtData[currentPointID].isActivated) {
            clickPtData[currentPointID].setCoord(ll);
            distanceBtwPoints();
        }

        // set Seek result for given point
        setResults();

        // set click point text
        if (clickPtData[currentPointID].isActivated) {
            setClickPtText();
        }

        isClicked = false;
    }

    public void setResults() {

        Coordinate coord = null;
        if (isClicked) {
            coord = coordinate;
        } else {
            coord = clickPtData[currentPointID].getCoord();
        }

        // Set active button color indicating current point
        if (currentPointID == 0) {
            button1.setBackground(
                    new Color(getParent().getDisplay(), new RGB(140, 255, 0)));
            button2.setBackground(Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        } else {
            button2.setBackground(
                    new Color(getParent().getDisplay(), new RGB(140, 255, 0)));
            button1.setBackground(Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }

        if (coord == null) {
            if (closePoints1 == null && closePoints2 == null) {
                return;
            }
        }

        String locName = clickPtData[currentPointID].getLocatorName();

        // LATLON
        if ("LATLON".equals(locName)) {
            resultList.removeAll();
            disposeList2(resultsComp);
            title.setEnabled(false);
            title2.setEnabled(false);
            return;
        } else {
            // read shapefile and get the closest points
            title.setEnabled(true);
            title2.setEnabled(true);
            if (locName != null) {
                title.setText(locName);
            }

            try {
                closePoints = SeekInfo.getClosestPoints(coord, locName);
                if (currentPointID == 0) {
                    closePoints1 = closePoints;
                } else {
                    closePoints2 = closePoints;
                }
            } catch (Exception e) {
                System.out.println(
                        "_________SeekResultsDialog: setResults(): Error:"
                                + e.getMessage());
            }

            formatResults();

        }

    }

    private void setClickPtText() {

        if (clickPtData[currentPointID].getCoord() == null) {
            return;
        }

        String locName = clickPtData[currentPointID].getLocatorName();
        String displayText = null;

        if ("LATLON".equals(locName)) {
            displayText = String.format("%6.2f %7.2f",
                    clickPtData[currentPointID].getCoord().y,
                    clickPtData[currentPointID].getCoord().x);
        } else {
            if (closePoints != null) {
                displayText = SeekInfo.getNameOrId(closePoints[0].getName(),
                        locName, false);
            }
        }

        if (displayText != null) {
            clickPtData[currentPointID].setText(displayText);
            if (currentPointID == 0) {
                text1.setText(displayText);
                txt1txt = displayText;
            } else {
                text2.setText(displayText);
                txt2txt = displayText;
            }
        }

        if (clickPtData[currentPointID].isActivated()) {
            clickPtData[currentPointID].setActivated(false);
        }

    }

    /*
     * Format and display the Seek results
     */
    private void formatResults() {
        resultList.removeAll();

        closePoints = currentPointID == 0 ? closePoints1 : closePoints2;

        if (closePoints == null) {
            return;
        }

        SeekPointData dt = null;
        int size = closePoints.length < limitNo ? closePoints.length : limitNo;

        for (int i = 0; i < size; i++) {

            dt = closePoints[i];
            String distStr = LocatorUtil.distanceDisplay(
                    dt.getDistanceInMeter(), 1, distCombo.getText());

            String dirStr = "-";
            if (dt.getDistanceInMeter() > 0) {

                if (dirUnitIndex == 0) {
                    dirStr = LocatorUtil.ConvertTO16PointDir(dt.getDir());
                } else {
                    dirStr = String.valueOf((int) dt.getDir()) + "deg";
                }
            }

            String name = SeekInfo.getNameOrId(dt.getName(),
                    clickPtData[currentPointID].getLocatorName(), true);

            if (name.length() > 25) {
                name = name.substring(0, 25);
            }

            String s = String.format("%-25s %6.2f %8.2f  %8s %6s", name,
                    dt.getLat(), dt.getLon(), distStr, dirStr);

            resultList.add(s);
        }
    }

    /**
     * Distance between two click points
     *
     * @param endPoint
     *            -- the last click point ID (0 or 1)
     */
    private void distanceBtwPoints() {
        if (clickPtData[0].getCoord() == null
                || clickPtData[1].getCoord() == null) {
            return;
        }

        // Calculate distance
        GeodeticCalculator gc = new GeodeticCalculator(DefaultEllipsoid.WGS84);
        gc.setStartingGeographicPoint(clickPtData[0].getCoord().x,
                clickPtData[0].getCoord().y);
        gc.setDestinationGeographicPoint(clickPtData[1].getCoord().x,
                clickPtData[1].getCoord().y);
        pointDistance = gc.getOrthodromicDistance();

        // Calculate direction
        double direction = gc.getAzimuth();
        if (direction < 0.0) {
            direction = 360 + direction;
        }
        clickPtData[1].setDirection(direction);

        gc.setStartingGeographicPoint(clickPtData[1].getCoord().x,
                clickPtData[1].getCoord().y);
        gc.setDestinationGeographicPoint(clickPtData[0].getCoord().x,
                clickPtData[0].getCoord().y);
        direction = gc.getAzimuth();
        if (direction < 0.0) {
            direction = 360 + direction;
        }
        clickPtData[0].setDirection(direction);

        formatPointDist();

    }

    private void formatPointDist() {

        String s = getFormatDistance(pointDistance,
                clickPtData[pointOrder].getDirection());

        // display point distance and direction
        distance.setText("    " + s);

        int first = pointOrder + 1;
        int last = pointOrder + 2;
        if (last > 2) {
            last = last - 2;
        }

        point1.setText("Point " + first + " is ");
        point2.setText("  of point " + last + "    ");

        point1Txt = point1.getText();
        point2Txt = point2.getText();
        distanceTxt = distance.getText();

    }

    public String getFormatDistance(double dist, double dir) {
        // format distance
        String distStr = LocatorUtil.distanceDisplay(dist, 1,
                distCombo.getText());

        // format direction
        String dirStr = "-";
        if (dist > 0) {
            if (dirUnitIndex == 0) {
                dirStr = LocatorUtil.ConvertTO16PointDir(dir);
            } else {
                dirStr = String.valueOf((int) dir) + "deg";
            }
        }
        return distStr + " " + dirStr;
    }

    // Get lat/lon of two end points
    public Coordinate[] getEndPoints() {
        Coordinate[] c = new Coordinate[2];
        c[0] = null;
        c[1] = null;
        if (clickPtData[0].getCoord() != null) {
            c[0] = clickPtData[0].getCoord();
        }
        if (clickPtData[1].getCoord() != null) {
            c[1] = clickPtData[1].getCoord();
        }

        return c;
    }

    /*
     * Convert string to lat/lon, and store
     *
     * @param s
     */
    private void stringToLatlon(String s) {
        String[] latlonStr = s.trim().split("\\s+");

        if (latlonStr.length >= 2) {
            try {
                double lat = Double.valueOf(latlonStr[0]);
                double lon = Double.valueOf(latlonStr[1]);
                if (lat >= -90.0 && lat <= 90 && lon >= -180.0 & lon < 180.0) {
                    coordinate = new Coordinate(lon, lat);
                    clickPtData[currentPointID].setCoord(coordinate);
                }
            } catch (Exception e) {
                System.out.println(
                        "____Error: SeekResultsDialog: stringToLatLon(): "
                                + e.getMessage());
            }
        }
    }

    /*
     * Draw line between two end points
     */
    private void drawLine(Coordinate c1, Coordinate c2) {
        distanceBtwPoints();
        setClickPtText();

        SeekDrawingLayer seekDrawingLayer = null;
        AbstractEditor theEditor = (AbstractEditor) EditorUtil
                .getActiveEditor();

        if (SeekResultsAction.isSeekToolAllowed(theEditor)) {

            ResourceList rscs = NcEditorUtil.getDescriptor(theEditor)
                    .getResourceList();
            for (ResourcePair r : rscs) {
                if (r.getResource() instanceof SeekDrawingLayer) {
                    seekDrawingLayer = (SeekDrawingLayer) r.getResource();
                    seekDrawingLayer.getResourceData().setFirstPt(c1);
                    seekDrawingLayer.getResourceData().setLastPt(c2);
                    theEditor.refresh();
                    break;
                }
            }
        }

    }

    /**
     * super class' isOpen() seems NOT work here! TODO
     */
    public boolean isDlgOpen() {
        return (clickPoint1 != null) && (!clickPoint1.isDisposed());
    }

    public void close() {
        if (this.shell != null && !this.shell.isDisposed()) {
            this.shell.dispose();
        }
    }

    /**
     * Save the CPF file.
     */
    private void saveCPF() {

        String dirLocal = new String(System.getProperty("user.home"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String time = sdf.format(new Date());

        SimpleDateFormat sdf2 = new SimpleDateFormat("MMM d yyyy");
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf2.format(new Date());
        // legacy application name: nmap2.cpf
        String fileName = "cave_" + time + ".cpf";

        try {

            File f = new File(dirLocal + File.separator + fileName);

            try (Writer output = new BufferedWriter(new FileWriter(f))) {
                output.write(getCPFTxt(fileName, date));
                output.flush();

            } catch (Exception ee) {
                System.out.println(ee.getMessage());
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Return the to be saved CPF Text String.
     *
     * @param fileName:
     *            the file name;
     * @param date:
     *            date and time append to the file name.
     * @return: the CPF Text String.
     */
    private String getCPFTxt(String fileName, String date) {

        StringBuilder sb = new StringBuilder();

        sb.append("!").append(fileName)
                .append("\n!\n!Cursor Point File (CPF)\t");
        sb.append(date).append("\n!\n!This file is created by CAVE, ")
                .append("DO NOT EDIT\n!\n  ");

        String yf = "00.00", xf = "000.00";

        if (clickPtData[0] != null && clickPtData[0].getCoord() != null) {
            double dy = clickPtData[0].getCoord().y;
            double dx = clickPtData[0].getCoord().x;
            sb.append(new DecimalFormat(yf).format(dy)).append("; ")
                    .append(new DecimalFormat(xf).format(dx));
            sb.append("\n  ");
        }

        if (clickPtData[1] != null && clickPtData[1].getCoord() != null) {
            double dy = clickPtData[1].getCoord().y;
            double dx = clickPtData[1].getCoord().x;
            sb.append(new DecimalFormat(yf).format(dy)).append("; ")
                    .append(new DecimalFormat(xf).format(dx));
        }

        return sb.toString();
    }

    /**
     * Draw the line between the two points.
     */
    private void setDrawing() {
        if (distanceTxt != null && !distanceTxt.isEmpty()) {
            drawLine(clickPtData[0].getCoord(), clickPtData[1].getCoord());
        }
    }

    /**
     * Get the close points for user manual input; based on pointSearch().
     *
     * @param text:
     *            the Text Widget user put input;
     * @param stnType:
     *            the station type;
     * @param id:
     *            the Text field id.
     */
    private void pointSearch2(Text text, String stnType, int id) {

        String prefix = text.getText().trim(), p1 = text.getText(), s = " ";

        // handle cases like Los_Angeles
        if (p1.contains(s) && (p1.lastIndexOf(s) != (p1.length() - 1))) {
            prefix = prefix.replace(s, "_");
        }

        // Gray out the titles
        title.setEnabled(false);
        title2.setEnabled(false);

        // Set active button color indicating current point
        if (currentPointID == 0) {
            button1.setBackground(
                    new Color(getParent().getDisplay(), new RGB(140, 255, 0)));
            button2.setBackground(Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        } else {
            button2.setBackground(
                    new Color(getParent().getDisplay(), new RGB(140, 255, 0)));
            button1.setBackground(Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }

        // get the points
        try {

            closePoints = SeekInfo.getMatchedPoints(prefix, stnType, id);

        } catch (Exception e) {
            System.out.println(
                    "_____________ SeekResultsDialog: pointSearch2(): Error: "
                            + e.getMessage());
        }

        if (closePoints != null && closePoints.length > 0) {

            // more than one stations returned: open the expandable List
            if (closePoints.length > 1) {

                if (resultsComp == null || resultsComp.isDisposed()) {
                    NotFirstOpen = true;
                    disposeList2(distComp);
                    disposeList2(centeredComp);
                    createResultList2();
                    createPointsDistanceControls();
                    createButtons();
                    shell.pack();
                    shell.layout();

                    NotFirstOpen = false;
                }

                if (resultList2 != null && !resultList2.isDisposed()) {
                    resultList2.removeAll();
                }

                for (SeekPointData dt : closePoints) {
                    resultList2.add(dt.getName() + ",    " + dt.getStateID());
                }

                // only one station, directly put into the Text field.
            } else {
                selectStn(0, text);
            }
            // NO station returned, close the expandable List if it is open.
        } else if (closePoints != null && closePoints.length == 0) {

            disposeList2(resultsComp);
        }

    }

    /**
     * create the expandable list if info for the two Text fields are manually
     * typed in .
     */
    private void createResultList2() {

        if (!NotFirstOpen) {
            return;
        }

        resultsComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        resultsComp.setLayout(gl);

        GridData gd = new GridData(365, 80);

        resultList2 = new List(resultsComp,
                SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        resultList2.setLayoutData(gd);
        resultList2.setFont(font);
        resultList2.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // no operation
            }

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (currentPointID == 0) {
                    txtNotSet1 = true;
                } else {
                    txtNotSet2 = true;
                }

                selectStn(resultList2.getSelectionIndex(), null);
            }

        });

    }

    /**
     * Close the bottom expandable List Widget.
     *
     * @param resultsComp:
     *            the composite for the List Widget.
     */
    private void disposeList2(Composite resultsComp) {

        if (resultsComp != null && !resultsComp.isDisposed()) {

            Control[] wids = resultsComp.getChildren();

            for (int kk = 0; wids != null && kk < wids.length; kk++) {
                wids[kk].dispose();
            }

            resultsComp.dispose();
            resultsComp = null;

        }

        shell.pack();
        shell.layout();

    }

    /**
     * pick a station from the List
     *
     * @param index:
     *            the index of selected station;
     * @param text:
     *            the Text to be set.
     */
    private void selectStn(int index, Text text) {
        if (closePoints != null) {

            Coordinate c = new Coordinate(closePoints[index].getLon(),
                    closePoints[index].getLat());
            clickPtData[currentPointID].setCoord(c);
            clickPtData[currentPointID].setActivated(true);
            clickPtData[currentPointID == 1 ? 0 : 1].setActivated(false);
            setResults();
            drawLine2(clickPtData[0].getCoord(), clickPtData[1].getCoord());

            disposeList2(resultsComp);
        }

    }

    /**
     * handle key pressed case, especially with CR. the text got from KeyEvent
     * is one Character fewer than actual input.
     *
     * @param id:
     *            the Text field id;
     * @param combo:
     *            the Combo Widget related to the Text field;
     * @param text:
     *            the Text field user put input;
     * @param e:
     *            the KeyEvent.
     */
    private void doKeyPressed(int id, Combo combo, Text text, KeyEvent e) {

        currentPointID = id;

        // allow text auto set
        if (currentPointID == 0) {
            txtNotSet1 = true;
        } else {
            txtNotSet2 = true;
        }

        // some keys are treated differently
        // press 'Enter' to search
        if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {

            if ("LATLON".equals(combo.getText())) {

                stringToLatlon(text.getText());
                drawLine(clickPtData[0].getCoord(), clickPtData[1].getCoord());
            } else {

                if (text.getText().trim().length() > 0) {
                    pointSearch2(text, combo.getText().trim(), id);
                }
            }
            // NO auto set for backspace/home; user manual input.
        } else if (e.keyCode == SWT.BS || e.keyCode == SWT.HOME) {

            if (currentPointID == 0) {
                txtNotSet1 = false;
            } else {
                txtNotSet2 = false;
            }

            if (!("LATLON".equals(combo.getText()))
                    && text.getText().trim().length() > 2) {
                pointSearch2(text, combo.getText().trim(), id);
            } else {
                disposeList2(resultsComp);
            }

        } else {
            if (!("LATLON".equals(combo.getText()))
                    && text.getText().trim().length() > 2) {

                pointSearch2(text, combo.getText().trim(), id);
            } else if (text.getText().trim().length() < 3) {

                disposeList2(resultsComp);
            }
        }

    }

    /**
     * Based on drawLine() but calls setClickPtText2()
     *
     * @param c1:
     *            Coordinate of a points;
     * @param c2:
     *            Coordinate of a points.
     */
    private void drawLine2(Coordinate c1, Coordinate c2) {

        distanceBtwPoints();
        setClickPtText2();

        SeekDrawingLayer seekDrawingLayer = null;
        AbstractEditor theEditor = (AbstractEditor) EditorUtil
                .getActiveEditor();
        ResourceList rscs = NcEditorUtil.getDescriptor(theEditor)
                .getResourceList();

        for (ResourcePair r : rscs) {
            if (r.getResource() instanceof SeekDrawingLayer) {

                seekDrawingLayer = (SeekDrawingLayer) r.getResource();
                seekDrawingLayer.getResourceData().setFirstPt(c1);
                seekDrawingLayer.getResourceData().setLastPt(c2);
                theEditor.refresh();
                break;
            }
        }

    }

    /**
     * Based on setClickPtText(), but handles txtNotSet1/2 flags
     */
    private void setClickPtText2() {

        if (clickPtData[currentPointID].getCoord() == null) {
            return;
        }

        String locName = clickPtData[currentPointID].getLocatorName();
        String displayText = null;

        if ("LATLON".equals(locName)) {
            displayText = String.format("%6.2f %7.2f",
                    clickPtData[currentPointID].getCoord().y,
                    clickPtData[currentPointID].getCoord().x);
        } else {
            if (closePoints != null) {
                displayText = SeekInfo.getNameOrId(closePoints[0].getName(),
                        locName, false);
            }
        }

        if (displayText != null) {
            clickPtData[currentPointID].setText(displayText);
            if (currentPointID == 0) {

                if (txtNotSet1) {
                    text1.setText("");
                    text1.append(displayText);
                    txt1txt = displayText;
                    txtNotSet1 = false;

                }
            } else {

                if (txtNotSet2) {
                    text2.setText("");
                    text2.append(displayText);
                    txt2txt = displayText;
                    txtNotSet2 = false;

                }
            }
        }

        if (clickPtData[currentPointID].isActivated()) {
            clickPtData[currentPointID].setActivated(false);
        }

    }

}
