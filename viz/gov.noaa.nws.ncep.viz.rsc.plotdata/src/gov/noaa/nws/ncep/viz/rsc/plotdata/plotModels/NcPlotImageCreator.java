package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter;
import gov.noaa.nws.ncep.edex.common.metparameters.Amount;
import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.display.IVector.VectorType;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefn;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefns;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefnsMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModelElement;
import gov.noaa.nws.ncep.viz.rsc.plotdata.queue.QueueEntry;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.NcPlotResource2.Station;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Tracer;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.measure.quantity.Angle;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.units.UnitAdapter;
import com.raytheon.uf.viz.core.DrawableBasics;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.Style;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#     Engineer       Description
 * ------------ ---------- ----------- --------------------------
 * 05/20/2013     988        Archana.S    Initial creation.
 * 02/26/2014    1061        B. Hebbard   Relax tolerance for extent/zoom compare to avoid infinite loop.
 * 04/23/2014    922-923     S. Russell   Modified getDrawableStringForStation()
 * 06/10/2014    932         D. Sushon    Fix Symbol background mask to not obscure underlying imagery.
 * 06/17/2014    923         S. Russell   added method adjustForPTSYSymbol()
 * 06/17/2014    923         S. Russell   added method getLastCharInPosition()
 * 06/17/2014    923         S. Russell   altered method setUpSymbolMappingTables()
 * 06/17/2014    923         S. Russell   altered method createRenderableData()
 * 07/08/2014    TTR1027     B. Hebbard   Force createRenderableData to recreate wind vectors each time.
 * Aug 08, 2014  3477        bclement     changed plot info locations to floats
 * 09/10/2014    R4230       S. Russell   Fix wind barb/brbk menu option in plot model dialog box
 * 11/03/2014    R4830       S. Russell   Added elements to presWxSymbolNames
 * 11/03/2014    R5156       B. Hebbard   Allow use of system fonts in addition to file-based 3
 * 11/06/2014    R5156       B. Hebbard   Rename Helvetica & Times lookalike fonts/files to make clear they aren't Java/AWT logical SansSerif & Serif
 * 08/14/2015    R7757       B. Hebbard   Add support for directional arrow (no magnitude) parameters; also refactor so imageCreator belongs directly to resource (instead of NcPlotDataRequestor) for better frame status tracking; other cleanups.
 * 11/05/2015    5070         randerso    Adjust font sizes for dpi scaling
 */

public class NcPlotImageCreator {

    private JobPool imageCreationJobPool = null;

    private ConcurrentLinkedQueue<QueueEntry> queueOfStations = null;

    private IPointInfoRenderingListener iPointInfoRenderingListener = null;

    private PlotModel plotModel = null;

    private EnumMap<Position, PlotModelElement> plotModelPositionToPmeMap = null;

    public boolean isThereAConditionalFilter = false;

    private Map<String, Symbol> symbolNameToSymbolMap = null;

    private Map<DataTime, Map<String, Station>> mapOfStnsToDataTime = null;

    private Map<DataTime, Map<Position, List<SymbolLocationSet>>> mapOfSymbolsPerPlotPosPerFrame = null;

    private static String[] skycSymbolNames = new String[] { "SKY_COVER_00",
            "SKY_COVER_01", "SKY_COVER_02", "SKY_COVER_03", "SKY_COVER_04",
            "SKY_COVER_05", "SKY_COVER_06", "SKY_COVER_07", "SKY_COVER_08",
            "SKY_COVER_09", "SKY_COVER_10" };

    private static String[] presWxSymbolNames = new String[] {
            "PRESENT_WX_004", "PRESENT_WX_005", "PRESENT_WX_006",
            "PRESENT_WX_007", "PRESENT_WX_008", "PRESENT_WX_009",
            "PRESENT_WX_010", "PRESENT_WX_011", "PRESENT_WX_012",
            "PRESENT_WX_014", "PRESENT_WX_016", "PRESENT_WX_017",
            "PRESENT_WX_018", "PRESENT_WX_019", "PRESENT_WX_031",
            "PRESENT_WX_034", "PRESENT_WX_036", "PRESENT_WX_037",
            "PRESENT_WX_038", "PRESENT_WX_039", "PRESENT_WX_040",
            "PRESENT_WX_041", "PRESENT_WX_044", "PRESENT_WX_045",
            "PRESENT_WX_048", "PRESENT_WX_049", "PRESENT_WX_051",
            "PRESENT_WX_053", "PRESENT_WX_055", "PRESENT_WX_056",
            "PRESENT_WX_057", "PRESENT_WX_058", "PRESENT_WX_059",
            "PRESENT_WX_061", "PRESENT_WX_063", "PRESENT_WX_065",
            "PRESENT_WX_066", "PRESENT_WX_067", "PRESENT_WX_068",
            "PRESENT_WX_069", "PRESENT_WX_071", "PRESENT_WX_073",
            "PRESENT_WX_075", "PRESENT_WX_077", "PRESENT_WX_078",
            "PRESENT_WX_079", "PRESENT_WX_079", "PRESENT_WX_080",
            "PRESENT_WX_081", "PRESENT_WX_082", "PRESENT_WX_083",
            "PRESENT_WX_084", "PRESENT_WX_085", "PRESENT_WX_086",
            "PRESENT_WX_087", "PRESENT_WX_088", "PRESENT_WX_089",
            "PRESENT_WX_090", "PRESENT_WX_095", "PRESENT_WX_096",
            "PRESENT_WX_097", "PRESENT_WX_098", "PRESENT_WX_099",
            "PRESENT_WX_105", "PRESENT_WX_106", "PRESENT_WX_201",
            "PRESENT_WX_202", "PRESENT_WX_203" };

    private static String[] icingSymbolNames = new String[] { "ICING_00",
            "ICING_01", "ICING_02", "ICING_03", "ICING_04", "ICING_05",
            "ICING_06", "ICING_08" };

    private static String[] turbSymbolNames = new String[] { "TURBULENCE_0",
            "TURBULENCE_1", "TURBULENCE_2", "TURBULENCE_3", "TURBULENCE_4",
            "TURBULENCE_5", "TURBULENCE_6", "TURBULENCE_7" };

    private static String[] presTendencySymbolNames = new String[] {
            "PRESSURE_TENDENCY_00", "PRESSURE_TENDENCY_01",
            "PRESSURE_TENDENCY_02", "PRESSURE_TENDENCY_03",
            "PRESSURE_TENDENCY_04", "PRESSURE_TENDENCY_05",
            "PRESSURE_TENDENCY_06", "PRESSURE_TENDENCY_07",
            "PRESSURE_TENDENCY_08" };

    private Map<Position, PlotSymbolType> posToSymbolTypeMap = null;

    private Map<Position, RGB> plotPosToColorMap = null;

    private Map<PlotSymbolType, Boolean> symbolExistsMap = null;

    private PlotParameterDefns plotParameterDefinitions = null;

    private IView lastView = null;

    private double lastZoomLevel = Double.MIN_VALUE;

    private static final float INITIAL_FONT_SIZE = 12;

    private static final File COURIER_NORMAL_FONT_FILE = NcPathManager
            .getInstance().getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR + "cour.pfa");

    private static final File TIMES_LIKE_NORMAL_FONT_FILE = NcPathManager
            .getInstance()
            .getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR + "VeraSe.ttf");

    private static final File TIMES_LIKE_BOLD_FONT_FILE = NcPathManager
            .getInstance().getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR
                            + "l049016t.pfa");

    private static final File TIMES_LIKE_ITALIC_FONT_FILE = NcPathManager
            .getInstance().getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR
                            + "l049033t.pfa");

    private static final File TIMES_LIKE_BOLD_ITALIC_FONT_FILE = NcPathManager
            .getInstance().getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR
                            + "l049036t.pfa");

    private static final File HELVETICA_LIKE_NORMAL_FONT_FILE = NcPathManager
            .getInstance()
            .getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR + "luxisr.ttf");

    private static final File HELVETICA_LIKE_ITALIC_FONT_FILE = NcPathManager
            .getInstance().getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR
                            + "luxisri.ttf");

    private static final File HELVETICA_LIKE_BOLD_FONT_FILE = NcPathManager
            .getInstance()
            .getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR + "luxisb.ttf");

    private static final File HELVETICA_LIKE_BOLD_ITALIC_FONT_FILE = NcPathManager
            .getInstance().getStaticFile(
                    NcPathManager.NcPathConstants.FONT_FILES_DIR
                            + "luxisbi.ttf");

    private static IFont COURIER_BOLD_FONT = null;

    private static IFont COURIER_NORMAL_FONT = null;

    private static IFont COURIER_ITALIC_FONT = null;

    private static IFont COURIER_BOLD_ITALIC_FONT = null;

    private static IFont TIMES_LIKE_BOLD_ITALIC_FONT = null;

    private static IFont HELVETICA_LIKE_BOLD_ITALIC_FONT = null;

    private static IFont TIMES_LIKE_BOLD_FONT = null;

    private static IFont HELVETICA_LIKE_BOLD_FONT = null;

    private static IFont TIMES_LIKE_ITALIC_FONT = null;

    private static IFont HELVETICA_LIKE_ITALIC_FONT = null;

    private static IFont TIMES_LIKE_NORMAL_FONT = null;

    private static IFont HELVETICA_LIKE_NORMAL_FONT = null;

    private static double TOLERANCE = 1E-04; // was 1E-24;

    private static double ZOOM_TOLERANCE = 1E-04; // was 1E-22;

    private static Amount WIND_SPD_3KNOTS = new Amount(3, NonSI.KNOT);

    private Map<PlotSymbolType, StringLookup> symbolLookupTable = null;

    private Map<DataTime, Map<Position, Map<String, DrawableString>>> dataTimeToText = null;

    private HashMap<Position, Double> plotPosToSymbolSizeMap = null;

    private HashMap<Position, IFont> plotPosToFontMap = null;

    private RGB defaultColor = null;

    String prevFontStyle = null;

    IFont prevFont = null;

    // @formatter:off
    public static enum Position {
            TC,
        UL, UC, UR,
        ML, MC, MR,
        LL, LC, LR,
            BC,
        // ----
            SC,  // special sky coverage position -- plots at MC
            WD,  // special wind barb position -- plots at MC
            INVALID
    }
    // @formatter:on

    public static enum DisplayMode { // TODO: Not used?? Should be?
        TEXT, BARB, TABLE, NULL // ARROW, DIRECTIONAL, AVAIL, RANGE, SAMPLE
    }

    public NcPlotImageCreator(IPointInfoRenderingListener listener,
            PlotModel plotModel) {
        Tracer.print("> Entry");
        defaultColor = new RGB(255, 255, 255);
        this.plotModel = plotModel;
        plotParameterDefinitions = PlotParameterDefnsMngr.getInstance()
                .getPlotParamDefns(plotModel.getPlugin());
        imageCreationJobPool = new JobPool("Creating station plots...", 8,
                false);
        queueOfStations = new ConcurrentLinkedQueue<QueueEntry>();
        iPointInfoRenderingListener = listener;
        mapOfStnsToDataTime = new HashMap<DataTime, Map<String, Station>>();
        dataTimeToText = new HashMap<DataTime, Map<Position, Map<String, DrawableString>>>();
        posToSymbolTypeMap = new HashMap<Position, PlotSymbolType>();
        symbolLookupTable = new HashMap<PlotSymbolType, StringLookup>();
        symbolNameToSymbolMap = new HashMap<String, Symbol>(0);
        plotModelPositionToPmeMap = new EnumMap<Position, PlotModelElement>(
                Position.class);
        plotPosToColorMap = new HashMap<Position, RGB>(11);
        plotPosToSymbolSizeMap = new HashMap<Position, Double>(11);
        plotPosToFontMap = new HashMap<Position, IFont>(11);
        mapOfSymbolsPerPlotPosPerFrame = new HashMap<DataTime, Map<Position, List<SymbolLocationSet>>>();
        symbolExistsMap = new HashMap<PlotSymbolType, Boolean>();

        setUpPlotPositionToPlotModelElementMapping(plotModel);
        setUpSymbolMappingTables();
        initializeFonts();
        Tracer.print("< Exit");
    }

    // TODO check need for synchronized on following...
    // Note locks up on attribute change (Plot All) if present!
    public/* synchronized */void queueStationsToCreateImages(DataTime dt,
            Collection<Station> stations) {
        Tracer.print("> Entry " + Tracer.shortTimeString(dt));
        QueueEntry qe = new QueueEntry(dt, stations);
        Tracer.print("About to queue " + stations.size()
                + " stations from frame " + Tracer.shortTimeString(dt)
                + " for image creation\n");
        queueOfStations.add(qe);
        runCreateImageTask();
        Tracer.print("< Exit  " + Tracer.shortTimeString(dt));
    }

    private void runCreateImageTask() {
        Tracer.print("> Entry");
        if (queueOfStations.peek() == null) {
            return;
        }
        while (queueOfStations.peek() != null) {
            QueueEntry qe = queueOfStations.poll();
            Tracer.print("About to schedule image drawing task for "
                    + qe.getStations().size() + " stations from frame "
                    + qe.getDataTime().toString() + "\n");
            CreateImageTask task = new CreateImageTask(qe.getDataTime(),
                    qe.getStations());
            imageCreationJobPool.schedule(task);
        }
        Tracer.print("< Exit");
    }

    private void initializeFonts() {
        Tracer.print("> Entry");
        IDisplayPane displayPane = NcDisplayMngr.getActiveNatlCntrsEditor()
                .getActiveDisplayPane();
        IGraphicsTarget target = displayPane.getTarget();
        COURIER_BOLD_FONT = target.initializeFont(COURIER_NORMAL_FONT_FILE,
                IFont.FontType.TYPE1, INITIAL_FONT_SIZE,
                new IFont.Style[] { IFont.Style.BOLD });
        COURIER_BOLD_ITALIC_FONT = target.initializeFont(
                COURIER_NORMAL_FONT_FILE, IFont.FontType.TYPE1,
                INITIAL_FONT_SIZE, new IFont.Style[] { IFont.Style.BOLD,
                        IFont.Style.ITALIC });
        COURIER_ITALIC_FONT = target.initializeFont(COURIER_NORMAL_FONT_FILE,
                IFont.FontType.TYPE1, INITIAL_FONT_SIZE,
                new IFont.Style[] { IFont.Style.ITALIC });
        COURIER_NORMAL_FONT = target.initializeFont(COURIER_NORMAL_FONT_FILE,
                IFont.FontType.TYPE1, INITIAL_FONT_SIZE, null);

        TIMES_LIKE_BOLD_FONT = target.initializeFont(TIMES_LIKE_BOLD_FONT_FILE,
                IFont.FontType.TYPE1, INITIAL_FONT_SIZE,
                new IFont.Style[] { IFont.Style.BOLD });
        TIMES_LIKE_BOLD_ITALIC_FONT = target.initializeFont(
                TIMES_LIKE_BOLD_ITALIC_FONT_FILE, IFont.FontType.TYPE1,
                INITIAL_FONT_SIZE, new IFont.Style[] { IFont.Style.BOLD,
                        IFont.Style.ITALIC });
        TIMES_LIKE_ITALIC_FONT = target.initializeFont(
                TIMES_LIKE_ITALIC_FONT_FILE, IFont.FontType.TYPE1,
                INITIAL_FONT_SIZE, new IFont.Style[] { IFont.Style.ITALIC });
        TIMES_LIKE_NORMAL_FONT = target.initializeFont(
                TIMES_LIKE_NORMAL_FONT_FILE, IFont.FontType.TRUETYPE,
                INITIAL_FONT_SIZE, null);

        HELVETICA_LIKE_BOLD_FONT = target.initializeFont(
                HELVETICA_LIKE_BOLD_FONT_FILE, IFont.FontType.TRUETYPE,
                INITIAL_FONT_SIZE, new IFont.Style[] { IFont.Style.BOLD });
        HELVETICA_LIKE_BOLD_ITALIC_FONT = target.initializeFont(
                HELVETICA_LIKE_BOLD_ITALIC_FONT_FILE, IFont.FontType.TRUETYPE,
                INITIAL_FONT_SIZE, new IFont.Style[] { IFont.Style.BOLD,
                        IFont.Style.ITALIC });
        HELVETICA_LIKE_ITALIC_FONT = target.initializeFont(
                HELVETICA_LIKE_ITALIC_FONT_FILE, IFont.FontType.TRUETYPE,
                INITIAL_FONT_SIZE, new IFont.Style[] { IFont.Style.ITALIC });
        HELVETICA_LIKE_NORMAL_FONT = target.initializeFont(
                HELVETICA_LIKE_NORMAL_FONT_FILE, IFont.FontType.TRUETYPE,
                INITIAL_FONT_SIZE, null);
        Tracer.print("< Exit");
    }

    public Position getPositionFromPlotModelElementPosition(
            String plotModelElementPosition) {
        Position position = Position.INVALID;
        try {
            position = Position.valueOf(plotModelElementPosition);
        } catch (IllegalArgumentException e) {
            position = Position.INVALID;
        }
        return position;
    }

    private void setUpPlotModelElementToPlotColorMapping(Position p,
            PlotModelElement pme) {
        Tracer.print("> Entry");
        if (pme != null) {
            gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.Color pmeColor = pme
                    .getColor();
            RGB oldColor = plotPosToColorMap.get(p);
            if (oldColor == null
                    || ((oldColor.red != pmeColor.getRed())
                            || (oldColor.green != pmeColor.getGreen()) || (oldColor.blue != pmeColor
                            .getBlue()))) {
                RGB newColor = new RGB(pmeColor.getRed(), pmeColor.getGreen(),
                        pmeColor.getBlue());
                plotPosToColorMap.put(p, newColor);
            }
        }
        Tracer.print("< Exit");
    }

    private void setUpPlotModelToSymbolSizeMapping(Position p,
            PlotModelElement pme) {
        Tracer.print("> Entry");
        if (pme != null) {
            Double newSymbolSize = pme.getSymbolSize();
            Double oldSymbolSize = plotPosToSymbolSizeMap.get(p);
            if (oldSymbolSize == null
                    || (Math.abs(oldSymbolSize.doubleValue()
                            - newSymbolSize.doubleValue()) > 0.001)) {
                plotPosToSymbolSizeMap.put(p, newSymbolSize);
            }
        }
        Tracer.print("< Exit");
    }

    private void setUpPlotModelToFontMapping(Position p, PlotModelElement pme) {
        Tracer.print("> Entry");
        if (pme != null) {

            /* Set the font information */
            int fontSize = Integer.parseInt(pme.getTextSize());
            String fontName = pme.getTextFont();
            String fontStyle = pme.getTextStyle();
            IFont font = null;
            if (prevFontStyle == null || prevFontStyle.isEmpty()) {
                prevFontStyle = new String(fontStyle);
            }

            if (prevFont == null) {
                font = getFont(fontName, fontSize, fontStyle);
                prevFont = font;
            } else {
                /*
                 * Change the font only if the style/size/font name do not match
                 * the previous font
                 */
                String fontNameToCompare = prevFont.getFontName();
                int fontSizeToCompare = (int) prevFont.getFontSize();
                if ((fontSizeToCompare != fontSize)
                        || !fontNameToCompare.equalsIgnoreCase(fontName)
                        || !prevFontStyle.equalsIgnoreCase(fontStyle)) {
                    font = getFont(fontName, fontSize, fontStyle);
                    prevFont = font;
                }
                if (!prevFontStyle.equalsIgnoreCase(fontStyle)) {
                    prevFontStyle = fontStyle;
                }
            }
            if (plotPosToFontMap.get(p) == null
                    || !plotPosToFontMap.get(p).equals(prevFont)) {
                plotPosToFontMap.put(p, prevFont);
            }
        }
        Tracer.print("< Exit");
    }

    public void setUpPlotPositionToPlotModelElementMapping(PlotModel pm) {
        Tracer.print("> Entry");
        List<PlotModelElement> plotModelElementsList = pm
                .getAllPlotModelElements();
        if (plotModelElementsList != null && !plotModelElementsList.isEmpty()) {
            synchronized (plotModelElementsList) {
                for (PlotModelElement pme : plotModelElementsList) {

                    // Redmine 4230
                    // NcPlotImageCreator will not process BRBK unless it has
                    // the abstract ( not on the grid of plot model buttons )
                    // position "WD"
                    if (pme.getParamName().equalsIgnoreCase("BRBK")) {
                        pme.setPosition("WD");
                    }

                    Position position = getPositionFromPlotModelElementPosition(pme
                            .getPosition());
                    plotModelPositionToPmeMap.put(position, pme);
                    setUpPlotModelElementToPlotColorMapping(position, pme);
                    setUpPlotModelToFontMapping(position, pme);
                    setUpPlotModelToSymbolSizeMapping(position, pme);
                }
            }
        }
        Tracer.print("< Exit");
    }

    public void setPlotModel(PlotModel pm) {
        Tracer.print("> Entry");
        this.plotModel = pm;
        Tracer.print("< Exit");
    }

    public void removeObsoletePMEEntries(PlotModel pm) {
        Tracer.print("> Entry");
        List<PlotModelElement> plotModelElementsList = pm
                .getAllPlotModelElements();
        Set<Position> posToRemove = new HashSet<Position>(0);
        if (plotModelPositionToPmeMap != null
                && !plotModelPositionToPmeMap.isEmpty()) {
            Set<Position> posSet = plotModelPositionToPmeMap.keySet();
            synchronized (posSet) {
                for (Position pos : posSet) {
                    boolean matchFound = false;
                    synchronized (plotModelElementsList) {
                        for (PlotModelElement pme : plotModelElementsList) {
                            if (pos.toString().equalsIgnoreCase(
                                    pme.getPosition())) {
                                matchFound = true;
                                break;
                            }
                        }
                    }
                    if (!matchFound) {
                        posToRemove.add(pos);
                    }
                }
            }
            synchronized (posToRemove) {
                for (Position p : posToRemove) {
                    plotModelPositionToPmeMap.remove(p);
                    plotPosToColorMap.remove(p);
                    plotPosToSymbolSizeMap.remove(p);
                    plotPosToFontMap.remove(p);
                    posToSymbolTypeMap.remove(p);
                    if (p != Position.WD) {
                        if (dataTimeToText != null && !dataTimeToText.isEmpty()) {
                            Set<DataTime> frameTimeSet = dataTimeToText
                                    .keySet();
                            if (frameTimeSet != null && !frameTimeSet.isEmpty()) {
                                synchronized (frameTimeSet) {
                                    for (DataTime dt : frameTimeSet) {
                                        Map<Position, Map<String, DrawableString>> mapOfStrPosition = dataTimeToText
                                                .get(dt);
                                        if (mapOfStrPosition != null
                                                && !mapOfStrPosition.isEmpty()) {
                                            mapOfStrPosition.remove(p);
                                            dataTimeToText.put(dt,
                                                    mapOfStrPosition);
                                        }
                                    }
                                }
                            }
                        }
                        if (mapOfSymbolsPerPlotPosPerFrame != null
                                && !mapOfSymbolsPerPlotPosPerFrame.isEmpty()) {
                            Set<DataTime> frameTimeSet = mapOfSymbolsPerPlotPosPerFrame
                                    .keySet();
                            if (frameTimeSet != null && !frameTimeSet.isEmpty()) {
                                synchronized (frameTimeSet) {
                                    for (DataTime dt : frameTimeSet) {
                                        Map<Position, List<SymbolLocationSet>> mapOfSymbolsToEachPosition = mapOfSymbolsPerPlotPosPerFrame
                                                .get(dt);
                                        if (mapOfSymbolsToEachPosition != null
                                                && !mapOfSymbolsToEachPosition
                                                        .isEmpty()) {
                                            mapOfSymbolsToEachPosition
                                                    .remove(p);
                                            mapOfSymbolsPerPlotPosPerFrame.put(
                                                    dt,
                                                    mapOfSymbolsToEachPosition);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Tracer.print("< Exit");
    }

    private IFont getFont(String fontName, int fontSize, String fontStyle) {
        Tracer.print("> Entry");
        IFont font = null;

        if (COURIER_BOLD_FONT == null || COURIER_ITALIC_FONT == null
                || COURIER_NORMAL_FONT == null
                || COURIER_BOLD_ITALIC_FONT == null
                || TIMES_LIKE_BOLD_FONT == null
                || TIMES_LIKE_ITALIC_FONT == null
                || TIMES_LIKE_BOLD_ITALIC_FONT == null
                || TIMES_LIKE_NORMAL_FONT == null
                || HELVETICA_LIKE_ITALIC_FONT == null
                || HELVETICA_LIKE_BOLD_ITALIC_FONT == null
                || HELVETICA_LIKE_NORMAL_FONT == null
                || HELVETICA_LIKE_BOLD_FONT == null) {
            initializeFonts();
        }

        if (fontName.equals("Courier")) {
            if (fontStyle.equals("Bold")) {
                font = COURIER_BOLD_FONT;
            } else if (fontStyle.equals("Italic")) {
                font = COURIER_ITALIC_FONT;
            } else if (fontStyle.equals("Bold-Italic")) {
                font = COURIER_BOLD_ITALIC_FONT;
            } else {
                font = COURIER_NORMAL_FONT;
            }
        }

        else if (fontName.equals("Times")) {
            if (fontStyle.equals("Bold")) {
                font = TIMES_LIKE_BOLD_FONT;
            } else if (fontStyle.equals("Italic")) {
                font = TIMES_LIKE_ITALIC_FONT;
            } else if (fontStyle.equals("Bold-Italic")) {
                font = TIMES_LIKE_BOLD_ITALIC_FONT;
            } else {
                font = TIMES_LIKE_NORMAL_FONT;
            }
        }

        else if (fontName.equals("Helvetica")) {
            if (fontStyle.equals("Bold")) {
                font = HELVETICA_LIKE_BOLD_FONT;
            } else if (fontStyle.equals("Italic")) {
                font = HELVETICA_LIKE_ITALIC_FONT;
            } else if (fontStyle.equals("Bold-Italic")) {
                font = HELVETICA_LIKE_BOLD_ITALIC_FONT;
            } else {
                font = HELVETICA_LIKE_NORMAL_FONT;
            }
        }

        else { // use a system (name-based) font, instead of a file-based one
            Style[] styles = null;
            if (fontStyle.equals("Bold")) {
                styles = new Style[] { Style.BOLD };
            } else if (fontStyle.equals("Italic")) {
                styles = new Style[] { Style.ITALIC };
            } else if (fontStyle.equals("Bold-Italic")) {
                styles = new Style[] { Style.BOLD, Style.ITALIC };
            }
            IDisplayPane displayPane = NcDisplayMngr.getActiveNatlCntrsEditor()
                    .getActiveDisplayPane();
            IGraphicsTarget target = displayPane.getTarget();
            font = target.initializeFont(fontName, fontSize, styles);
            // TODO in this case, need to worry about disposal of the font
            // later?
        }

        if (font != null && fontSize != INITIAL_FONT_SIZE) {
            font = font.deriveWithSize(fontSize);
        }

        if (font != null) {
            font.setMagnification(1);
            // disable anti-aliasing, to make text less fuzzy
            font.setScaleFont(false);
            font.setSmoothing(false);
        }
        Tracer.print("< Exit");

        return font;
    }

    /**
     * Creates symbols based on attributes provided in the PlotModelElement and
     * maps them to the symbol pattern name as specified in PGEN.
     */
    public void setUpSymbolMappingTables() {
        Tracer.print("> Entry");
        Set<Position> positionSet = plotModelPositionToPmeMap.keySet();
        synchronized (positionSet) {
            for (Position position : positionSet) {
                PlotModelElement pme = plotModelPositionToPmeMap.get(position);
                String plotParamName = pme.getParamName();
                // TTR 923 Temporarily change the name of the combination
                // element, the PTND button, to get the PTSY symbol part of it
                // to process
                if (plotParamName.equalsIgnoreCase("PTND")) {
                    plotParamName = "PTSY";
                }

                PlotParameterDefn thisPlotParamDefn = plotParameterDefinitions
                        .getPlotParamDefn(plotParamName);
                if (thisPlotParamDefn == null) {
                    Tracer.print("Unable to find " + plotParamName
                            + " in the list of plot parameter definitions for "
                            + plotModel.getPlugin() + ":" + plotModel.getName());
                    continue;
                }

                if (thisPlotParamDefn.getPlotMode().equalsIgnoreCase("table")) {
                    // TTR 923 - use plotParamName to get the PTND button
                    // processed as a PTSY symbol
                    PlotSymbolType symbolType = getPlotSymbolType(plotParamName);

                    if (symbolType != PlotSymbolType.INVALID) {
                        posToSymbolTypeMap.put(position, symbolType);
                        if (symbolExistsMap.get(symbolType) == null) {
                            symbolExistsMap.put(symbolType, Boolean.FALSE);
                        }
                        StringLookup lookupTable = StringLookup
                                .readS2SFile(thisPlotParamDefn
                                        .getPlotLookupTable());
                        symbolLookupTable.put(symbolType, lookupTable);
                        String[] arrayToSearch = new String[0];
                        switch (symbolType) {
                        case WSYM:
                            arrayToSearch = Arrays.copyOf(presWxSymbolNames,
                                    presWxSymbolNames.length);
                            break;
                        case SKYC:
                            arrayToSearch = Arrays.copyOf(skycSymbolNames,
                                    skycSymbolNames.length);
                            break;
                        case ICSY:
                            arrayToSearch = Arrays.copyOf(icingSymbolNames,
                                    icingSymbolNames.length);
                            break;
                        case TBSY:
                            arrayToSearch = Arrays.copyOf(turbSymbolNames,
                                    turbSymbolNames.length);
                            break;
                        case PTSY:
                            arrayToSearch = Arrays.copyOf(
                                    presTendencySymbolNames,
                                    presTendencySymbolNames.length);
                            break;
                        default:
                            break;
                        }

                        if (arrayToSearch.length > 0) {
                            RGB rgb = plotPosToColorMap.get(position);
                            Color symbolColor = new Color(rgb.red, rgb.green,
                                    rgb.blue);
                            Color[] colorArray = new Color[] { symbolColor };
                            Coordinate dummyCoordinate = new Coordinate(0.0,
                                    0.0);
                            float symbolSize = plotPosToSymbolSizeMap.get(
                                    position).floatValue();
                            // Create each symbol once to render it at different
                            // locations as needed
                            synchronized (arrayToSearch) {
                                for (String symbolName : arrayToSearch) {
                                    Symbol symbol = symbolNameToSymbolMap
                                            .get(symbolName);
                                    if (symbol == null) {
                                        symbol = new Symbol(null, colorArray,
                                                symbolSize, symbolSize, false,
                                                dummyCoordinate, "Symbol",
                                                symbolName);
                                    } else {
                                        if (!symbolColor.equals(symbol
                                                .getColors()[0])) {
                                            symbol.setColors(colorArray);
                                        }
                                        if (Math.abs(symbolSize
                                                - symbol.getSizeScale()) > 0.01) {
                                            symbol.setSizeScale(symbolSize);
                                            plotPosToSymbolSizeMap.put(
                                                    position, new Double(
                                                            symbolSize));
                                        }
                                    }
                                    symbolNameToSymbolMap.put(symbolName,
                                            symbol);
                                }
                            }
                        }
                    }
                }
            }
        }
        Tracer.print("< Exit");
    }

    public static enum PlotSymbolType {
        // TODO: There were also commented-out values ITSY, TFSY, TTSY (between
        // ICSY and TBSY). Investigate whether these could still be needed.
        WSYM, SKYC, ICSY, TBSY, PTSY, INVALID
    }

    private PlotSymbolType getPlotSymbolType(String symbolGEMPAKName) {
        PlotSymbolType symbolType = PlotSymbolType.INVALID;
        try {
            symbolType = PlotSymbolType.valueOf(symbolGEMPAKName);
        } catch (IllegalArgumentException e) {
            symbolType = PlotSymbolType.INVALID;
        }
        return symbolType;
    }

    private final class CreateImageTask implements Runnable {

        private DataTime dataTime = null;

        private List<Station> listOfStations = null;

        private IDescriptor mapDescriptor = null;

        private IGraphicsTarget aTarget = null;

        private IDisplayPane activePane = null;

        private IView view = null;

        private Rectangle canvasBounds = null;

        private IRenderableDisplay renderableDisplay = null;

        private boolean drawTextFirstTime = false;

        @Override
        public void run() {
            Tracer.print("> Entry  START TASK "
                    + Tracer.shortTimeString(this.dataTime) + " "
                    + this.listOfStations.size() + " stations");
            createRenderableData();
            Tracer.print("< Exit   END TASK   "
                    + Tracer.shortTimeString(this.dataTime) + " "
                    + this.listOfStations.size() + " stations");
        }

        public CreateImageTask(DataTime time,
                Collection<Station> listOfStationsToDrawImages) {
            Tracer.print("> Entry " + Tracer.shortTimeString(time));
            Tracer.print("Creating a CreateImageTask for the frame time: "
                    + Tracer.shortTimeString(time) + " with "
                    + listOfStationsToDrawImages.size() + " stations");
            this.dataTime = new DataTime(time.getRefTime());
            this.listOfStations = new ArrayList<Station>(
                    listOfStationsToDrawImages);
            activePane = NcDisplayMngr.getActiveNatlCntrsEditor()
                    .getActiveDisplayPane();
            aTarget = activePane.getTarget();
            mapDescriptor = activePane.getDescriptor();
            renderableDisplay = mapDescriptor.getRenderableDisplay();
            view = renderableDisplay.getView();

            if (lastZoomLevel == Double.MIN_VALUE
                    || (Math.abs(renderableDisplay.getZoom() - lastZoomLevel) > ZOOM_TOLERANCE)) {
                lastZoomLevel = renderableDisplay.getZoom();
            }
            VizApp.runSync(new Runnable() {
                @Override
                public void run() {
                    Tracer.printX("> Entry  [runSync]");
                    if (activePane != null) {
                        canvasBounds = activePane.getBounds();
                    }
                    Tracer.printX("< Exit   [runSync]");
                }
            });
            Tracer.print("< Exit  " + Tracer.shortTimeString(time));

        }

        /**
         * Creates a key for the Station based on its lat/lon
         * 
         * @param lat
         * @param lon
         * @return
         */
        private String stationMapKey(Float lon, Float lat) {
            return new String("" + Math.round(lon * 1000.0) + ","
                    + Math.round(lat * 1000.0));

        }

        private String createKeyFromTextCoordinates(DrawableBasics db) {
            if (db == null) {
                return null;
            }
            String key = new String("" + Math.round(db.x * 10000) + ","
                    + Math.round(db.y * 10000));
            return key;
        }

        /**
         * Rounds wind speed to the nearest 5 (knots)
         * 
         * @param windSpeed
         * @return The nearest 5 knot speed
         */
        private int roundTo5(double dWindSpeed) {
            int windSpeed = (int) dWindSpeed;
            int major = windSpeed / 5;
            int minor = windSpeed % 5;
            if (minor >= 3) {
                major++;
            }
            return major * 5;
        }

        private void createVectors(
                Map<Coordinate, List<IVector>> vectorCoordinatesToVectorsMap,
                Collection<Station> stnColl, String plotUnit,
                double symbolSize, RGB rgb, String metPrm1, String metPrm2,
                PlotModelElement pme, boolean directionOnly,
                boolean directionReverse) {
            Tracer.print("> Entry " + Tracer.shortTimeString(this.dataTime)
                    + " with " + stnColl.size() + " stations" + " metPrm1 "
                    + metPrm1 + " metPrm2 " + metPrm2);
            mapDescriptor = NcDisplayMngr.getActiveNatlCntrsEditor()
                    .getActiveDisplayPane().getDescriptor();
            synchronized (stnColl) {
                Tracer.printX(Tracer.shortTimeString(this.dataTime)
                        + " Still have " + stnColl.size()
                        + " stations inside synchronized (stnColl)");
                for (Station currentStation : stnColl) {
                    double[] stationWorldLoc = { currentStation.info.longitude,
                            currentStation.info.latitude };
                    AbstractMetParameter vectorParam1 = null;
                    AbstractMetParameter vectorParam2 = null;
                    try {
                        IVector vector = null;
                        synchronized (currentStation.listOfParamsToPlot) {
                            for (AbstractMetParameter metPrm : currentStation.listOfParamsToPlot) {
                                if (metPrm.getMetParamName().equalsIgnoreCase(
                                        metPrm1)) {
                                    vectorParam1 = metPrm;
                                }
                                if (metPrm.getMetParamName().equalsIgnoreCase(
                                        metPrm2)) {
                                    vectorParam2 = metPrm;
                                }
                            }
                            if ((vectorParam1 != null && ((vectorParam2 != null) || directionOnly))) {
                                if (pme.hasAdvancedSettings()) {
                                    rgb = getConditionalColor(currentStation,
                                            pme);
                                }
                                if (rgb == null) {
                                    rgb = new RGB(255, 255, 255);
                                }
                                vector = createOneVector(vectorParam1,
                                        vectorParam2, plotUnit, symbolSize,
                                        rgb, stationWorldLoc, directionOnly,
                                        directionReverse);
                            }
                            if (vector != null) {
                                Tracer.printX(Tracer
                                        .shortTimeString(this.dataTime)
                                        + " Adding a vector for "
                                        + currentStation.info.stationId);
                                List<IVector> vectorsAtThisLocation = vectorCoordinatesToVectorsMap
                                        .get(vector.getLocation());
                                if (vectorsAtThisLocation == null) {
                                    vectorsAtThisLocation = new ArrayList<IVector>();
                                    vectorCoordinatesToVectorsMap.put(
                                            vector.getLocation(),
                                            vectorsAtThisLocation);
                                }
                                vectorsAtThisLocation.add(vector);
                                Tracer.printX(Tracer
                                        .shortTimeString(this.dataTime)
                                        + " Created vector for "
                                        + currentStation.info.stationId);
                            }
                        }
                    } catch (ArithmeticException ae) {
                        Tracer.print("ArithmeticException instead of azimuth for station : "
                                + currentStation.info.stationId);
                    } catch (IllegalStateException ise) {
                        Tracer.print("IllegalStateException instead of azimuth for station : "
                                + currentStation.info.stationId);
                    } catch (NullPointerException npe) {
                        Tracer.print("NullPointerException for "
                                + currentStation.info.stationId);
                    } catch (Exception e) {
                    }
                }
            }
            Tracer.print("< Exit  " + Tracer.shortTimeString(this.dataTime));
        }

        private IVector createOneVector(AbstractMetParameter metParam1,
                AbstractMetParameter metParam2, String plotUnit,
                double symbolSize, RGB rgb, double[] stationLoc,
                boolean directionOnly, boolean directionReverse) {
            Tracer.printX("> Entry");
            AbstractMetParameter speed = null, direction = null;
            Vector vector = null;
            if (metParam1 instanceof Angle) {
                direction = metParam1;
                speed = metParam2;
            } else if (metParam2 instanceof Angle) {
                direction = metParam2;
                speed = metParam1;
            }
            Color[] vectorColorArray = new Color[] { new Color(rgb.red,
                    rgb.green, rgb.blue) };
            try {
                // The units in the element are for the speed and not the
                // direction.
                Number nDirection = direction.getValueAs(NonSI.DEGREE_ANGLE);
                if (nDirection == null) {
                    return null;
                }
                double dDirection = nDirection.doubleValue();
                if (directionOnly) {
                    // Directional arrow
                    Number nSpeed = speed == null ? null : speed
                            .getValueAs(plotUnit);
                    // If speed not reported (as for wave direction), assume
                    // positive for following test.
                    // If speed reported as zero, preserve as zero so arrow will
                    // be suppressed.
                    double dSpeed = nSpeed == null ? 1.0 : nSpeed.doubleValue();
                    // Suppress arrow if calm (direction and speed both zero).
                    // Note that if direction is nonzero (non-north), then we
                    // assume it's significant (and produce the directional
                    // arrow) even if speed is zero, in case speed of zero is
                    // used to indicate missing value (say, "0" instead of "/")
                    if (dDirection > 0.0 || dSpeed > 0.0) {
                        if (directionReverse) {
                            dDirection = dDirection > 180.0 ? dDirection - 180.0
                                    : dDirection + 180.0;
                        }
                        // Numeric constants in the following are "tuned" to
                        // match NMAP2 display characteristics.
                        vector = new Vector(null, vectorColorArray, 2.50f,
                                symbolSize * 1.28, false, new Coordinate(
                                        stationLoc[0], stationLoc[1]),
                                VectorType.ARROW, dSpeed, dDirection, 0.9,
                                true, "Vector", "Arrow");
                    }
                } else {
                    // Wind barb
                    double uSpeed = speed.getValueAs(plotUnit).doubleValue();
                    double dSpeed = Double.MIN_VALUE;
                    Unit<?> unit;
                    unit = (Unit<?>) UnitFormat.getUCUMInstance().parseObject(
                            plotUnit);
                    double cWindSpeedThresh = WIND_SPD_3KNOTS.getValueAs(unit)
                            .doubleValue();
                    if (uSpeed >= cWindSpeedThresh) {
                        dSpeed = roundTo5(uSpeed);
                    }
                    vector = new Vector(null, vectorColorArray, 1.0f,
                            symbolSize, false, new Coordinate(stationLoc[0],
                                    stationLoc[1]), VectorType.WIND_BARB,
                            dSpeed, dDirection, 1.0, true, "Vector", "Barb");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Tracer.printX("< Exit" + "  returning "
                    + ((vector == null) ? "NULL" : "a vector"));
            return vector;
        }

        private final synchronized void createRenderableData() {
            Tracer.print("> Entry " + Tracer.shortTimeString(this.dataTime));
            if (listOfStations == null || listOfStations.isEmpty()) {
                iPointInfoRenderingListener.renderingAborted(dataTime);
                return;
            }
            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " listOfStations has " + listOfStations.size()
                    + " stations after entry");
            Tracer.print("About to create renderable data for frame: "
                    + Tracer.shortTimeString(dataTime));
            Map<Position, List<SymbolLocationSet>> mapOfAllSymbolsAtEachPlotPosition = null;
            Map<Position, Map<String, DrawableString>> localDMap = null;
            List<DrawableString> stringsToDraw = new ArrayList<DrawableString>();
            List<IVector> vectorsToDraw = new ArrayList<IVector>(0);
            List<SymbolLocationSet> symbolLocationSetsToDraw = new ArrayList<SymbolLocationSet>(
                    0);

            if (dataTimeToText.get(dataTime) != null) {
                localDMap = new HashMap<Position, Map<String, DrawableString>>(
                        dataTimeToText.get(dataTime));
            } else {
                localDMap = new HashMap<Position, Map<String, DrawableString>>();
            }

            Set<Position> positionSet = plotModelPositionToPmeMap.keySet();
            Map<String, Station> stationMap = new HashMap<String, Station>(
                    listOfStations.size());
            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " listOfStations has " + listOfStations.size()
                    + " stations right after stationMap creation");

            Tracer.print("From createRenderableData()  - list of parameters plotted per station for frame: "
                    + Tracer.shortTimeString(dataTime));

            /* Create a map of stations that have been currently disclosed */
            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " stationMap has " + stationMap.size()
                    + " stations before loop (should be 0)");
            for (Station station : listOfStations) {
                String key = stationMapKey(station.info.longitude,
                        station.info.latitude)
                        + "-"
                        + station.info.dataTime.toString();
                Tracer.print(Tracer.shortTimeString(this.dataTime)
                        + " stnplotMap for " + station.info.stationId + " : "
                        + station.stnPlotMap.toString());
                Tracer.print("For frame " + Tracer.shortTimeString(dataTime)
                        + " " + station.info.stationId + " at time "
                        + station.info.dataTime.toString()
                        + " - List of parameters:  "
                        + station.listOfParamsToPlot.toString());
                stationMap.put(key, station);
            }
            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " stationMap has " + stationMap.size()
                    + " stations after loop");

            if (lastView == null) {
                lastView = view.clone();
            }

            double width = aTarget.getStringsBounds(
                    new DrawableString("M", defaultColor)).getWidth();
            double height = aTarget.getStringsBounds(
                    new DrawableString("'y", defaultColor)).getHeight();
            Rectangle textBounds = new Rectangle(0, 0, (int) width,
                    (int) height);

            Tracer.print("textBounds initialized to " + textBounds);

            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " positionSet has " + positionSet.size() + " elements");

            Map<Coordinate, List<IVector>> vectorCoordinatesToVectorsMap = new HashMap<Coordinate, List<IVector>>(
                    0);

            for (Position position : positionSet) {

                // Get the formatting information from the PlotModelElement for
                // the current plot position
                PlotModelElement pme = plotModelPositionToPmeMap.get(position);

                RGB pmeColor = plotPosToColorMap.get(position);

                // Set the font information
                IFont font = plotPosToFontMap.get(position);
                // TODO: Handle font==null error

                // Get the data retrieval information from the PlotParameterDefn
                // corresponding to the current PlotModelElement
                PlotParameterDefn plotParamDefn = plotParameterDefinitions
                        .getPlotParamDefn(pme.getParamName());

                // TODO how about using enum DisplayMode here?
                boolean drawVector = (position == Position.WD || plotParamDefn
                        .getPlotMode().equals("directional"));
                boolean drawSymbol = (posToSymbolTypeMap.get(position) != null);
                // TODO do better than the following...?
                // @formatter:off
                boolean drawText = (
                        !plotParamDefn.getPlotMode().equals(
                                PlotParameterDefn.PLOT_MODE_TABLE) && 
                        !plotParamDefn.getPlotMode().equals(
                                PlotParameterDefn.PLOT_MODE_BARB) && 
                        !plotParamDefn.getPlotMode().equals(
                                PlotParameterDefn.PLOT_MODE_DIRECTIONAL));
                // @formatter:on

                if (drawText) {
                    Map<String, DrawableString> mapOfStrPositions = localDMap
                            .get(position);
                    drawTextFirstTime = (mapOfStrPositions == null);

                    if (drawTextFirstTime) { // TODO: Note setting this to true
                                             // is not safe -- distorts station
                                             // plots after zoom!

                        mapOfStrPositions = new HashMap<String, DrawableString>();

                        // Loop thru all the stations to create the a list of
                        // DrawableString for each position

                        Set<String> stnKeySet = stationMap.keySet();
                        synchronized (stnKeySet) {
                            for (String key : stnKeySet) {
                                Station station = stationMap.get(key);

                                // Sanity check - shouldn't happen since this is
                                // already taken care of after querying the data
                                // from Postgres
                                if (station.info.longitude > 180
                                        || station.info.longitude < -180
                                        || station.info.latitude > 90
                                        || station.info.latitude < -90) {
                                    // System.out.println("Skipping the station: "
                                    // + station.info.stationId +
                                    // " since it has invalid or missing coordinates");
                                    continue;
                                }

                                double worldLoc[] = new double[] {
                                        station.info.longitude,
                                        station.info.latitude };
                                double[] tempPixLoc = mapDescriptor
                                        .worldToPixel(worldLoc);
                                station.pixelLocation = new Coordinate(
                                        tempPixLoc[0], tempPixLoc[1]);

                                // Create the string to be rendered
                                DrawableString drawableString = getDrawableStringForStation(
                                        station, plotParamDefn, font, pmeColor,
                                        position, aTarget, pme);

                                if (drawableString != null) {
                                    // For each station - Store the pixel
                                    // coordinates of the string to be rendered,
                                    // mapped to the position at which it needs
                                    // to be rendered
                                    station.stnPlotMap.put(position,
                                            drawableString.basics);
                                    // Add the pixel coordinates and the string
                                    // to render to the Map<DrawableBasics,
                                    // DrawableString>
                                    String dbKey = createKeyFromTextCoordinates(drawableString.basics);
                                    mapOfStrPositions
                                            .put(dbKey, drawableString);
                                }

                                // Update the stationMap with the current
                                // station
                                stationMap.put(key, station);

                            }
                        }
                    }

                    else {
                        // Loop through the remaining strings at the current
                        // plot position and if they don't lie within the
                        // current extents, discard them - this takes care of
                        // removing strings for stations that are currently
                        // disclosed - whose previously rendered strings need to
                        // be repositioned per the current pixel position of the
                        // station
                        synchronized (mapOfStrPositions) {
                            List<String> pixPosToRemoveList = new ArrayList<String>();
                            Set<String> dbSet = mapOfStrPositions.keySet();
                            synchronized (dbSet) {
                                try {
                                    for (String db : dbSet) {
                                        String[] coords = db.split(",");
                                        double dbx = Double
                                                .parseDouble(coords[0]) / 1000;
                                        double dby = Double
                                                .parseDouble(coords[1]) / 1000;
                                        if (!view.getExtent().contains(
                                                new double[] { dbx, dby })) {
                                            pixPosToRemoveList.add(db);
                                        }
                                    }
                                    synchronized (pixPosToRemoveList) {
                                        for (String db : pixPosToRemoveList) {
                                            mapOfStrPositions.remove(db);
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }

                        lastView = view.clone();

                        Set<String> stnKeySet = stationMap.keySet();
                        synchronized (stnKeySet) {

                            for (String key : stnKeySet) {

                                Station station = stationMap.get(key);

                                // Sanity check - shouldn't happen since this is
                                // already taken care of after querying the data
                                // from Postgres
                                if (station.info.longitude > 180
                                        || station.info.longitude < -180
                                        || station.info.latitude > 90
                                        || station.info.latitude < -90) {
                                    // System.out.println("Skipping the station: "
                                    // + station.info.stationId +
                                    // " since it has invalid or missing coordinates");
                                    continue;
                                }

                                // For each station, update its pixel
                                // coordinates per the current map descriptor

                                double worldLoc[] = new double[] {
                                        station.info.longitude,
                                        station.info.latitude };
                                double[] tempPixLoc = mapDescriptor
                                        .worldToPixel(worldLoc);
                                station.pixelLocation.x = tempPixLoc[0];
                                station.pixelLocation.y = tempPixLoc[1];

                                // Working off of the pixel coordinates at each
                                // position per station
                                DrawableBasics dbStn = station.stnPlotMap
                                        .get(position);

                                if (dbStn != null) {

                                    // Fetch the corresponding string from the
                                    // Map<DrawableBasics, DrawableString>
                                    String strPosKey = createKeyFromTextCoordinates(dbStn);
                                    DrawableString strToReposition = mapOfStrPositions
                                            .get(strPosKey);

                                    if (strToReposition != null) {
                                        mapOfStrPositions.remove(strPosKey);
                                        station.stnPlotMap.remove(position);

                                        // If the string exists, update its
                                        // coordinates
                                        Rectangle2D rr = aTarget
                                                .getStringsBounds(new DrawableString(
                                                        "'"
                                                                + strToReposition
                                                                        .getText()[0]
                                                                + "y",
                                                        strToReposition
                                                                .getColors()[0]));
                                        textBounds = new Rectangle(0, 0,
                                                (int) rr.getWidth(),
                                                (int) rr.getHeight());

                                        Tracer.print("textBounds updated in TEXT draw for station "
                                                + station.info.stationId
                                                + " to " + textBounds);

                                        double[] pixLoc = getAdjustedCoordinates(
                                                textBounds,
                                                station.pixelLocation.x,
                                                station.pixelLocation.y, view,
                                                canvasBounds, position);

                                        strToReposition.setCoordinates(
                                                pixLoc[0], pixLoc[1]);

                                        // Update the map of strings to be
                                        // rendered as well as the stationMap

                                        RGB oldColor = strToReposition
                                                .getColors()[0];
                                        if ((!pme.hasAdvancedSettings())
                                                && (oldColor != null)
                                                && (oldColor.red != pmeColor.red
                                                        || oldColor.green != pmeColor.green || oldColor.blue != pmeColor.blue)) {
                                            strToReposition.setText(
                                                    strToReposition.getText(),
                                                    pmeColor);
                                        } else if (pme.hasAdvancedSettings()) {
                                            RGB rgb = getConditionalColor(
                                                    station, pme);
                                            strToReposition.setText(
                                                    strToReposition.getText(),
                                                    rgb);
                                        }

                                        if ((!font.getFontName().equals(
                                                strToReposition.font
                                                        .getFontName()))
                                                || (Math.abs(font.getFontSize()
                                                        - strToReposition.font
                                                                .getFontSize()) > TOLERANCE)
                                                || (font.getStyle() != null
                                                        && strToReposition.font
                                                                .getStyle() != null
                                                        && font.getStyle().length > 0
                                                        && strToReposition.font
                                                                .getStyle().length > 0 && font
                                                        .getStyle()[0] != strToReposition.font
                                                        .getStyle()[0])
                                                || ((font.getStyle() == null || font
                                                        .getStyle().length == 0) && strToReposition.font
                                                // The style is set to null for
                                                // plain style fonts
                                                        .getStyle() != null)
                                                || (font.getStyle() != null && (strToReposition.font
                                                        .getStyle() == null || strToReposition.font
                                                        .getStyle().length == 0))) {
                                            strToReposition.font = font;
                                        }

                                        synchronized (strToReposition.basics) {
                                            station.stnPlotMap.put(position,
                                                    strToReposition.basics);
                                            String localDbKey = createKeyFromTextCoordinates(strToReposition.basics);
                                            mapOfStrPositions.put(localDbKey,
                                                    strToReposition);
                                        }
                                    } else {

                                        // If none exists - create it - maybe
                                        // the station reports this parameter
                                        // only at a designated time
                                        DrawableString drawableString = getDrawableStringForStation(
                                                station, plotParamDefn, font,
                                                pmeColor, position, aTarget,
                                                pme);
                                        if (drawableString != null) {

                                            // Update the map of strings to be
                                            // rendered as well as the
                                            // stationMap
                                            station.stnPlotMap.put(position,
                                                    drawableString.basics);
                                            String keydb = createKeyFromTextCoordinates(drawableString.basics);
                                            mapOfStrPositions.put(keydb,
                                                    drawableString);
                                        }
                                    }

                                    stationMap.put(key, station);

                                } else {

                                    // This could either be a new station or the
                                    // previous pixel position could be
                                    // obsolete. So create the string to be
                                    // rendered.

                                    // System.out.println( dataTime.toString() +
                                    // " Trying to create the met parameter value for a new station ?");
                                    DrawableString drawableString = getDrawableStringForStation(
                                            station, plotParamDefn, font,
                                            pmeColor, position, aTarget, pme);

                                    // And if it is not null - update the map of
                                    // strings to be rendered as well as the
                                    // stationMap
                                    if (drawableString != null) {
                                        RGB oldColor = drawableString
                                                .getColors()[0];
                                        if ((!pme.hasAdvancedSettings())
                                                && (oldColor.red != pmeColor.red
                                                        || oldColor.green != pmeColor.green || oldColor.blue != pmeColor.blue)) {
                                            drawableString.setText(
                                                    drawableString.getText(),
                                                    pmeColor);
                                        } else if (pme.hasAdvancedSettings()) {
                                            RGB rgb = getConditionalColor(
                                                    station, pme);
                                            drawableString.setText(
                                                    drawableString.getText(),
                                                    rgb);
                                        }
                                        if (!font.getFontName().equals(
                                                drawableString.font
                                                        .getFontName())
                                                || (Math.abs(font.getFontSize()
                                                        - drawableString.font
                                                                .getFontSize()) > TOLERANCE)
                                                || (font.getStyle() != null
                                                        && drawableString.font
                                                                .getStyle() != null
                                                        && font.getStyle().length > 0
                                                        && drawableString.font
                                                                .getStyle().length > 0 && font
                                                        .getStyle()[0] != drawableString.font
                                                        .getStyle()[0])
                                                || ((font.getStyle() == null || font
                                                        .getStyle().length == 0) && drawableString.font
                                                // The style is set to null for
                                                // plain style fonts
                                                        .getStyle() != null)
                                                || (font.getStyle() != null && (drawableString.font
                                                        .getStyle() == null || drawableString.font
                                                        .getStyle().length == 0))) {
                                            drawableString.font = font;
                                        }

                                        synchronized (drawableString.basics) {
                                            station.stnPlotMap.put(position,
                                                    drawableString.basics);
                                            String dbkey = createKeyFromTextCoordinates(drawableString.basics);
                                            mapOfStrPositions.put(dbkey,
                                                    drawableString);
                                        }
                                    }
                                    stationMap.put(key, station);
                                }
                            }
                        }
                    }

                    // After looping thru all stations, for each position,
                    // update the Map<Position, Map<DrawableBasics,
                    // DrawableString>>
                    localDMap.put(position, mapOfStrPositions);
                }

                if (drawVector) {
                    String[] vectorPrmNames = plotParamDefn
                            .getMetParamNamesForVectorPlot();
                    String metPrm1 = vectorPrmNames[0];
                    String metPrm2 = null;
                    if (vectorPrmNames.length > 1) {
                        metPrm2 = vectorPrmNames[1];
                    }
                    Double d = pme.getSymbolSize();
                    double symbolSize = (d == null ? 1.0 : d.doubleValue());
                    boolean directionOnly = plotParamDefn.getPlotMode().equals(
                            "directional");
                    boolean directionReverse = plotParamDefn.getTransform() != null
                            && plotParamDefn.getTransform().equalsIgnoreCase(
                                    "reverse");
                    Collection<Station> stnColl = stationMap.values();
                    Tracer.print(Tracer.shortTimeString(this.dataTime)
                            + " (1) stnColl has " + stnColl.size()
                            + " stations");
                    createVectors(vectorCoordinatesToVectorsMap, stnColl,
                            plotParamDefn.getPlotUnit(), symbolSize, pmeColor,
                            metPrm1, metPrm2, pme, directionOnly,
                            directionReverse);
                }

                if (drawSymbol) {
                    // Since the Symbol associated with each SymbolLocationSet
                    // is passed in as an argument to the constructor there is
                    // no explicit method to set the symbol should its
                    // attributes change. Hence the symbols get recreated afresh
                    // each time.
                    boolean wasPTND = false;
                    mapOfAllSymbolsAtEachPlotPosition = mapOfSymbolsPerPlotPosPerFrame
                            .get(dataTime);
                    PlotSymbolType symbolType = posToSymbolTypeMap
                            .get(position);

                    // TTR 923 Temporarily change the name of the combination
                    // element, the PTND button, to get the PTSY symbol part of
                    // it to process. To this end also set a flag to let us know
                    // this temporarily name change, PTND processing is
                    // happening
                    if (plotParamDefn.getMetParamName().equalsIgnoreCase(
                            "PressureChange3HrAndTendency")) {
                        plotParamDefn = plotParameterDefinitions
                                .getPlotParamDefn("PTSY");
                        wasPTND = true;
                    }

                    String metParamName = plotParamDefn.getMetParamName();
                    // TTR 923: Holder for the P03C value in the combination
                    // PTND element which holds values for P03C and PTSY
                    AbstractMetParameter ptnd = null;

                    if (symbolType != null) {

                        StringLookup lookupTable = symbolLookupTable
                                .get(symbolType);
                        symbolExistsMap.put(symbolType, Boolean.TRUE);

                        if (mapOfAllSymbolsAtEachPlotPosition == null) {
                            mapOfAllSymbolsAtEachPlotPosition = new HashMap<Position, List<SymbolLocationSet>>();
                        }

                        List<Coordinate> listOfCoords = null;
                        Map<Symbol, List<Coordinate>> symbolToSetOfCoordsMap = new HashMap<Symbol, List<Coordinate>>();
                        Collection<Station> stnColl = stationMap.values();

                        synchronized (stnColl) {

                            for (Station station : stnColl) {
                                AbstractMetParameter tableParamToPlot = null;
                                String symbolPatternName;

                                synchronized (station.listOfParamsToPlot) {
                                    try {
                                        // TTR 923 Temporarily change the name
                                        // of the combination element, the PTND
                                        // button, to get the PTSY symbol part
                                        // of it to process.
                                        if (wasPTND) {
                                            // TTR 923 temporarily change back
                                            // to PTND to match condition in
                                            // next for loop
                                            metParamName = "PressureChange3HrAndTendency";
                                        }
                                        for (AbstractMetParameter metPrm : station.listOfParamsToPlot) {
                                            if (metParamName.equals(metPrm
                                                    .getMetParamName())) {
                                                tableParamToPlot = metPrm;
                                                break;
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (tableParamToPlot != null) {
                                    // TTR 923 Temporarily change the name of
                                    // the combination element, the PTND button,
                                    // to get the PTSY symbol part of it to
                                    // process.
                                    if (wasPTND
                                            && tableParamToPlot
                                                    .getMetParamName()
                                                    .equalsIgnoreCase(
                                                            "PressureChange3HrAndTendency")) {
                                        if (tableParamToPlot
                                                .getAssociatedMetParam() != null
                                                && tableParamToPlot
                                                        .hasValidValue()) {
                                            // Save a copy of the P03C values
                                            // from the PTND metparameter
                                            ptnd = tableParamToPlot;
                                            // Get the stored PTSY metparameter
                                            tableParamToPlot = tableParamToPlot
                                                    .getAssociatedMetParam();
                                        } else {
                                            tableParamToPlot = null;
                                        }
                                    }
                                    String formattedString = null;
                                    try {
                                        formattedString = getFormattedValueToPlot(
                                                plotParamDefn, tableParamToPlot);

                                        if (formattedString == null
                                                || formattedString.isEmpty()) {
                                            continue;
                                        }
                                        if (lookupTable == null
                                                || lookupTable
                                                        .recursiveTranslation(formattedString) == null) {
                                            continue;
                                        }

                                        symbolPatternName = new String(
                                                lookupTable
                                                        .recursiveTranslation(formattedString));
                                        if (symbolPatternName == null
                                                || symbolPatternName.isEmpty()) {
                                            continue;
                                        } else {
                                            // Redmine 4230
                                            symbolPatternName = symbolPatternName
                                                    .trim();
                                        }

                                        Symbol symbol = symbolNameToSymbolMap
                                                .get(symbolPatternName);

                                        RGB rgb = null;
                                        // Get the conditional color for the
                                        // symbol if applicable
                                        if (pme.hasAdvancedSettings()) {
                                            rgb = getConditionalColor(station,
                                                    pme);
                                        }

                                        if (symbol == null) {
                                            continue;
                                        }

                                        double worldLoc[] = new double[] {
                                                station.info.longitude,
                                                station.info.latitude };
                                        double[] tempPixLoc = mapDescriptor
                                                .worldToPixel(worldLoc);

                                        // Recompute original textBounds, since
                                        // it may have been clobbered by
                                        // individual text element computations
                                        // above
                                        double xwidth = aTarget
                                                .getStringsBounds(
                                                        new DrawableString("M",
                                                                defaultColor))
                                                .getWidth();

                                        // Re-initialize textBounds as first
                                        // done in method; may have been changed
                                        // since.
                                        double xheight = aTarget
                                                .getStringsBounds(
                                                        new DrawableString(
                                                                "'y",
                                                                defaultColor))
                                                .getHeight(); // may
                                        // need
                                        // original
                                        // above?
                                        textBounds = new Rectangle(0, 0,
                                                (int) width, (int) height); // TODO
                                                                            // xwidth/xwheight
                                                                            // ??

                                        // TTR 922-923 Add a horizontal offset
                                        // to the PTSY symbol so it doesn't
                                        // overwrite the P03C number
                                        tempPixLoc[0] = adjustForPTSYSymbol(
                                                ptnd, symbol, position,
                                                textBounds, tempPixLoc[0],
                                                view, canvasBounds);

                                        tempPixLoc = getAdjustedCoordinates(
                                                textBounds, tempPixLoc[0],
                                                tempPixLoc[1], view,
                                                canvasBounds, position);
                                        worldLoc = mapDescriptor
                                                .pixelToWorld(tempPixLoc);
                                        listOfCoords = symbolToSetOfCoordsMap
                                                .get(symbol);

                                        if (listOfCoords == null) {
                                            listOfCoords = new ArrayList<Coordinate>();
                                        }

                                        listOfCoords.add(new Coordinate(
                                                worldLoc[0], worldLoc[1]));

                                        if (rgb != null) {
                                            symbol.setColors(new Color[] { new Color(
                                                    rgb.red, rgb.blue,
                                                    rgb.green) });
                                        } else {
                                            Color currSymbolColor = symbol
                                                    .getColors()[0];
                                            rgb = new RGB(
                                                    currSymbolColor.getRed(),
                                                    currSymbolColor.getGreen(),
                                                    currSymbolColor.getBlue());
                                            if (!rgb.equals(pmeColor)) {
                                                symbol.setColors(new Color[] { new Color(
                                                        pmeColor.red,
                                                        pmeColor.blue,
                                                        pmeColor.green) });
                                            }
                                        }
                                        double prevSizeOfSymbol = symbol
                                                .getSizeScale();
                                        double expectedSymbolSize = plotPosToSymbolSizeMap
                                                .get(position).doubleValue();
                                        if (Math.abs(expectedSymbolSize
                                                - prevSizeOfSymbol) > TOLERANCE) {
                                            symbol.setSizeScale(expectedSymbolSize);
                                        }
                                        symbolToSetOfCoordsMap.put(symbol,
                                                listOfCoords);
                                    } catch (VizException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        Set<Symbol> symSet = symbolToSetOfCoordsMap.keySet();
                        List<SymbolLocationSet> listOfSymLocSet = new ArrayList<SymbolLocationSet>();
                        for (Symbol symbol : symSet) {
                            List<Coordinate> coordSet = symbolToSetOfCoordsMap
                                    .get(symbol);
                            if (symbol == null) {
                                continue;
                            }
                            SymbolLocationSet sset = new SymbolLocationSet(
                                    symbol, coordSet.toArray(new Coordinate[0]));
                            listOfSymLocSet.add(sset);
                        }
                        mapOfAllSymbolsAtEachPlotPosition.put(position,
                                listOfSymLocSet);
                        mapOfSymbolsPerPlotPosPerFrame.put(dataTime,
                                mapOfAllSymbolsAtEachPlotPosition);
                    }
                }
            }

            /* Consolidate all the DrawableString into a single list */
            try {
                synchronized (positionSet) {
                    for (Position pos : positionSet) {
                        Map<String, DrawableString> mapOfStrPositions = localDMap
                                .get(pos);
                        if (mapOfStrPositions != null
                                && !mapOfStrPositions.isEmpty()) {
                            synchronized (mapOfStrPositions) {
                                synchronized (stringsToDraw) {
                                    stringsToDraw.addAll(mapOfStrPositions
                                            .values());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }

            /* Consolidate all vectors into a single list */
            vectorsToDraw = new ArrayList<IVector>(0);
            if (vectorCoordinatesToVectorsMap != null
                    && !vectorCoordinatesToVectorsMap.isEmpty()) {
                for (List<IVector> vColl : vectorCoordinatesToVectorsMap
                        .values()) {
                    try {
                        vectorsToDraw.addAll(vColl);
                    } catch (Exception e) {
                    }
                }
            }

            /* Consolidate all symbols into a single list */
            synchronized (symbolLocationSetsToDraw) {
                for (Position pos : positionSet) {
                    if (mapOfAllSymbolsAtEachPlotPosition == null
                            || mapOfAllSymbolsAtEachPlotPosition.get(pos) == null) {
                        continue;
                    }
                    synchronized (mapOfAllSymbolsAtEachPlotPosition) {
                        List<SymbolLocationSet> symbolLocSetList = new ArrayList<SymbolLocationSet>(
                                mapOfAllSymbolsAtEachPlotPosition.get(pos));
                        if (!symbolLocSetList.isEmpty()) {
                            symbolLocationSetsToDraw.addAll(symbolLocSetList);
                        }
                    }
                }
            }

            /* Update the map of text entries for the current frame */
            if (dataTimeToText != null && localDMap != null) {
                dataTimeToText.put(dataTime, localDMap);
            }

            /*
             * Update the map of vectors with the latest list of vectors for
             * this frame
             */

            // Inform the resource that we're all ready for rendering,
            // and deliver the goods
            iPointInfoRenderingListener.renderingComplete(dataTime,
                    stationMap.values(), stringsToDraw, vectorsToDraw,
                    symbolLocationSetsToDraw);

            Tracer.print("< Exit");
        }

        /**
         * Creates and formats the string to be plotted at the input plot
         * position for the input station
         * 
         * @param station
         * @param plotParamDefn
         * @param font
         * @param textColor
         * @param position
         * @param textBounds
         * @return
         */

        private DrawableString getDrawableStringForStation(Station station,
                PlotParameterDefn plotParamDefn, IFont font, RGB textColor,
                Position position, IGraphicsTarget aTarget, PlotModelElement pme) {

            Tracer.print("> Entry");
            DrawableString drawableString = null;
            String metParamName = plotParamDefn.getMetParamName();

            synchronized (station.listOfParamsToPlot) {
                try {
                    if (pme.hasAdvancedSettings()) {
                        textColor = getConditionalColor(station, pme);
                    }
                    for (AbstractMetParameter metParamToPlot : station.listOfParamsToPlot) {
                        if (metParamToPlot.getMetParamName().equals(
                                metParamName)) {

                            try {
                                String formattedString = getFormattedValueToPlot(
                                        plotParamDefn, metParamToPlot);

                                if (formattedString == null) {
                                    return null;
                                }
                                // TTR 923 remove sign for specific values
                                formattedString = removeSign(metParamToPlot,
                                        formattedString);

                                drawableString = new DrawableString(
                                        formattedString, textColor);
                                drawableString.horizontalAlignment = HorizontalAlignment.CENTER;
                                drawableString.verticallAlignment = VerticalAlignment.MIDDLE;
                                drawableString.font = font;
                                drawableString.magnification = 1.0;
                                drawableString.textStyle = TextStyle.NORMAL;

                                Rectangle2D bounds = aTarget
                                        .getStringsBounds(new DrawableString(
                                                "'"
                                                        + drawableString
                                                                .getText()[0]
                                                        + "y", drawableString
                                                        .getColors()[0]));
                                Rectangle textBounds = new Rectangle(0, 0,
                                        (int) bounds.getWidth(),
                                        (int) bounds.getHeight());
                                double[] pixLoc = getAdjustedCoordinates(
                                        textBounds, station.pixelLocation.x,
                                        station.pixelLocation.y, lastView,
                                        canvasBounds, position);
                                if (pixLoc != null) {
                                    drawableString.setCoordinates(pixLoc[0],
                                            pixLoc[1]);
                                } else {
                                    drawableString.setCoordinates(
                                            station.pixelLocation.x,
                                            station.pixelLocation.y);
                                }

                                return drawableString;

                            } catch (VizException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            Tracer.print("< Exit");

            return drawableString;
        }
    }

    /*
     * TTR 923 remove the sign on pressure tendency values of zero or where the
     * value of PTSY was 4
     */
    private String removeSign(AbstractMetParameter metParamToPlot,
            String formattedString) {

        if (!metParamToPlot.getMetParamName()
                .equalsIgnoreCase("PressChange3Hr")
                && !metParamToPlot.getMetParamName().equalsIgnoreCase(
                        "PressureChange3HrAndTendency")) {
            return formattedString;
        }

        String str = formattedString;
        AbstractMetParameter amp = metParamToPlot.getAssociatedMetParam();
        String sPTSY = amp.getStringValue();
        int iPTSY = 0;
        double iP03C = 0.0d;
        iP03C = metParamToPlot.getValue().doubleValue();
        iPTSY = Integer.parseInt(sPTSY);

        // If zero pressure tendency value or ptsy symbol is 4 - neutral
        if (iP03C == 0.0 || iPTSY == 4) {
            // Remove the sign
            if (formattedString.contains("+") || formattedString.contains("-")) {
                str = formattedString.substring(1);
            }
        }

        return str;

    }

    private String getFormattedValueToPlot(PlotParameterDefn plotParamDefn,
            AbstractMetParameter metPrm) throws VizException {

        Tracer.printX("> Entry");
        String formattedStringToPlot = null;

        // No value, abort.
        if (metPrm == null) {
            return null;
        }

        // No value, abort.
        if (!metPrm.hasValidValue()) {
            return formattedStringToPlot;
        }

        String plotUnit = plotParamDefn.getPlotUnit();

        if (plotUnit == null) {
            if (metPrm.hasStringValue()) {
                if (metPrm.getStringValue() != null
                        && !metPrm.getStringValue().isEmpty()) {
                    formattedStringToPlot = new String(metPrm.getStringValue());
                }
            }
        }

        try {
            Unit<?> newUnit = new UnitAdapter().unmarshal(plotUnit);

            if (newUnit.isCompatible(metPrm.getUnit())
                    || newUnit.equals(metPrm.getUnit())) {

                metPrm.setValue(metPrm.getValueAs(newUnit), newUnit);

                if (!metPrm.hasValidValue()) {
                    return null;
                }

                String formattedStr = new String(metPrm.getFormattedString(
                        plotParamDefn.getPlotFormat()).trim());
                String plotTrim = plotParamDefn.getPlotTrim();

                if (plotTrim != null && !plotTrim.isEmpty()) {
                    int plotTrimVal = Integer.parseInt(plotTrim);
                    formattedStringToPlot = new String(
                            formattedStr.substring(plotTrimVal));
                } else { // just use the formattedString without trimming it
                    formattedStringToPlot = new String(formattedStr);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            return plotUnit;
        }

        Tracer.printX("< Exit");

        return formattedStringToPlot;
    }

    /*
     * TTR 923, calculate a horizontal offset for the PTSY symbol so tht it does
     * not overwrite the P03C value
     */
    private double adjustForPTSYSymbol(AbstractMetParameter ptnd,
            Symbol symbol, Position position, Rectangle textBounds,
            double xPos, IView view, Rectangle canvasSize) {
        String symbol_name = symbol.getName();

        // NOT PTSY, return
        if (!symbol_name.startsWith("PRESSURE_TENDENCY_")) {
            return xPos;
        }

        Tracer.print("PTSY adjust with textBounds " + textBounds);

        // Extract the number "00 - 08" from the end
        String PTSYvaluestr = symbol_name.substring(18, symbol_name.length());
        int PTSYvalue = new Integer(PTSYvaluestr).intValue();

        // PTSY, but not between 00 - 08, return
        if (PTSYvalue < 0 || PTSYvalue > 8) {
            return xPos;
        }

        // Is the symbol place Left (L),Center (C), or Right (R) ?
        String positionLastLetter = getLastCharInPosition(position);
        int numPreceedingChars = 1;
        double offsetXpos = 0.0D;
        double charWidth = textBounds.width;
        double xScale = (view.getExtent().getWidth() / canvasSize.width);
        double adjustedWidth = charWidth * 0.625 * xScale;
        int m = 0;

        // If the PTSY symbol is part of a PTND button
        if (ptnd != null) {

            int iP03C = ptnd.getValue().intValue();
            int length_P03C = 0;
            String sP03C = Integer.toString(iP03C);
            length_P03C = sP03C.length();
            numPreceedingChars = numPreceedingChars * length_P03C;

            // Always put the PTSY symbol to the right of the P03C value
            // by adding the adjustedWidth to the xPos
            if (positionLastLetter.equalsIgnoreCase("C")) {
                offsetXpos = xPos + (3 * adjustedWidth);
            } else if (positionLastLetter.equalsIgnoreCase("R")) {
                if (numPreceedingChars == 1) {
                    m = 3;
                } else if (numPreceedingChars == 2) {
                    m = 4;
                } else if (numPreceedingChars == 3) {
                    m = 5;
                }
                offsetXpos = xPos + (m * adjustedWidth);
            } else if (positionLastLetter.equalsIgnoreCase("L")) {
                offsetXpos = xPos + adjustedWidth;
            }
        }// end if PTND

        // PTSY symbol independent of a PTND button combination
        else if (ptnd == null) {
            if (positionLastLetter.equalsIgnoreCase("C")) {
                offsetXpos = xPos + (1 * adjustedWidth);
            } else if (positionLastLetter.equalsIgnoreCase("R")) {
                offsetXpos = xPos + (2 * adjustedWidth);
            } else if (positionLastLetter.equalsIgnoreCase("L")) {
                offsetXpos = xPos - (1 * adjustedWidth);
            }
        }

        return offsetXpos;
    }

    /*
     * TTR 923 Get the last letter of the position in the plot model. "LC"
     * becomes "C" (center)
     */
    private String getLastCharInPosition(Position position) {
        String lastLetter = null;
        String sPosition = position.name();
        int length = sPosition.length();
        length = length - 1;
        lastLetter = sPosition.substring(length);
        return lastLetter;
    }

    private synchronized double[] getAdjustedCoordinates(Rectangle textBounds,
            double xPos, double yPos, IView view, Rectangle canvasSize,
            Position position) {

        Tracer.printX("> Entry");
        double charWidth = textBounds.width;
        double charHeight = textBounds.height;

        double xScale = (view.getExtent().getWidth() / canvasSize.width);
        double yScale = (view.getExtent().getHeight() / canvasSize.height);
        double adjustedWidth = charWidth * 0.625 * xScale;
        double adjustedHeight = charHeight * 0.625 * yScale;

        double canvasX1 = 0.0;
        double canvasY1 = 0.0;

        switch (position) {

        case ML:
            canvasX1 = xPos - adjustedWidth;
            canvasY1 = yPos;
            break;

        case MC:
            canvasX1 = xPos;
            canvasY1 = yPos;
            break;

        case MR:
            canvasX1 = xPos + adjustedWidth;
            canvasY1 = yPos;
            break;

        case UL:
            canvasX1 = xPos - adjustedWidth;
            canvasY1 = yPos - adjustedHeight;
            break;

        case UC:
            canvasX1 = xPos;
            canvasY1 = yPos - adjustedHeight;
            break;

        case UR:
            canvasX1 = xPos + adjustedWidth;
            canvasY1 = yPos - adjustedHeight;
            break;

        case LL:
            canvasX1 = xPos - adjustedWidth;
            canvasY1 = yPos + adjustedHeight;
            break;

        case LC:
            canvasX1 = xPos;
            canvasY1 = yPos + adjustedHeight;
            break;

        case LR:
            canvasX1 = xPos + adjustedWidth;
            canvasY1 = yPos + adjustedHeight;
            break;

        case BC:
            canvasX1 = xPos;
            canvasY1 = yPos + 2 * adjustedHeight;
            break;

        case TC:
            canvasX1 = xPos;
            canvasY1 = yPos - 2 * adjustedHeight;
            break;

        case SC:
            canvasX1 = xPos;
            canvasY1 = yPos;
            break;

        case WD:
            canvasX1 = xPos;
            canvasY1 = yPos;
            break;

        default:
            break;
        }
        Tracer.printX("< Exit");

        return new double[] { canvasX1, canvasY1 };
    }

    private RGB getConditionalColor(Station currentStation, PlotModelElement pme) {
        PlotParameterDefn condPlotParamDefn = PlotParameterDefnsMngr
                .getInstance().getPlotParamDefns(plotModel.getPlugin())
                .getPlotParamDefn(pme.getConditionalParameter());
        Tracer.printX("> Entry");

        RGB rgb = null;
        if (!currentStation.setOfConditionalColorParams.isEmpty()) {
            synchronized (currentStation.setOfConditionalColorParams) {
                for (AbstractMetParameter condColorMetPrm : currentStation.setOfConditionalColorParams) {
                    if (condColorMetPrm.getMetParamName().equals(
                            condPlotParamDefn.getMetParamName())) {
                        String condParamValue = getConditionalParameterValue(
                                condPlotParamDefn, condColorMetPrm,
                                currentStation);
                        if (condParamValue != null
                                && !condColorMetPrm.hasStringValue()) {
                            float value = Float.parseFloat(condParamValue);
                            if (pme.getConditionalColorBar() != null) {
                                rgb = pme.getConditionalColorBar()
                                        .getRGBForInterval(value);
                                break;
                            }
                        }
                    }
                }
            }
        }

        Tracer.printX("< Exit " + rgb);
        return rgb;
    }

    private String getConditionalParameterValue(
            PlotParameterDefn plotParamDefn, AbstractMetParameter metPrm,
            Station station) {
        try {
            return getFormattedValueToPlot(plotParamDefn, metPrm);
        } catch (VizException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized Map<Position, PlotModelElement> getPlotModelPositionMap() {
        return plotModelPositionToPmeMap;
    }

    public void removeObsoletePlotEntriesAtThisPositionForAllFrames(Position p) {
        Tracer.print("> Entry" + "  Position " + p);
        if (dataTimeToText != null && !dataTimeToText.isEmpty()) {
            Set<DataTime> dtSet = dataTimeToText.keySet();
            synchronized (dtSet) {
                for (DataTime dt : dtSet) {
                    Map<Position, Map<String, DrawableString>> mapOfStrPos = dataTimeToText
                            .get(dt);
                    if ((mapOfStrPos != null && !mapOfStrPos.isEmpty() && mapOfStrPos
                            .containsKey(p))
                            && (p != Position.SC)
                            && (p != Position.WD)) {
                        mapOfStrPos.remove(p);
                        dataTimeToText.put(dt, mapOfStrPos);
                    }
                }
            }
        }

        if ((mapOfSymbolsPerPlotPosPerFrame != null)
                && (!mapOfSymbolsPerPlotPosPerFrame.isEmpty())) {
            Set<DataTime> dtSet = mapOfSymbolsPerPlotPosPerFrame.keySet();
            synchronized (dtSet) {
                for (DataTime dt : dtSet) {
                    Map<Position, List<SymbolLocationSet>> symbolPosMap = mapOfSymbolsPerPlotPosPerFrame
                            .get(dt);
                    symbolPosMap.remove(p);
                    mapOfSymbolsPerPlotPosPerFrame.put(dt, symbolPosMap);
                }
            }
        }
        Tracer.print("< Exit");

    }

    public void dispose() {
        Tracer.print("> Entry");

        if (imageCreationJobPool.isActive()) {
            imageCreationJobPool.cancel();
        }
        imageCreationJobPool = null;

        if (queueOfStations != null) {
            queueOfStations.clear();
            queueOfStations = null;
        }

        if (dataTimeToText != null && !dataTimeToText.isEmpty()) {
            dataTimeToText.clear();
            dataTimeToText = null;
        }

        if (mapOfStnsToDataTime != null && !mapOfStnsToDataTime.isEmpty()) {
            mapOfStnsToDataTime.clear();
            mapOfStnsToDataTime = null;
        }

        if (mapOfSymbolsPerPlotPosPerFrame != null
                && !mapOfSymbolsPerPlotPosPerFrame.isEmpty()) {
            mapOfSymbolsPerPlotPosPerFrame.clear();
            mapOfSymbolsPerPlotPosPerFrame = null;
        }

        if (symbolNameToSymbolMap != null && !symbolNameToSymbolMap.isEmpty()) {
            symbolNameToSymbolMap.clear();
            symbolNameToSymbolMap = null;
        }

        if (symbolLookupTable != null && !symbolLookupTable.isEmpty()) {
            symbolLookupTable.clear();
            symbolLookupTable = null;
        }

        if (plotPosToSymbolSizeMap != null && !plotPosToSymbolSizeMap.isEmpty()) {
            plotPosToSymbolSizeMap.clear();
            plotPosToSymbolSizeMap = null;
        }

        if (posToSymbolTypeMap != null && !posToSymbolTypeMap.isEmpty()) {
            posToSymbolTypeMap.clear();
            posToSymbolTypeMap = null;
        }

        if (plotPosToFontMap != null && !plotPosToFontMap.isEmpty()) {
            plotPosToFontMap.clear();
            plotPosToFontMap = null;
        }

        if (plotModelPositionToPmeMap != null
                && !plotModelPositionToPmeMap.isEmpty()) {
            plotModelPositionToPmeMap.clear();
            plotModelPositionToPmeMap = null;
        }

        if (COURIER_BOLD_FONT != null) {
            COURIER_BOLD_FONT.dispose();
            COURIER_BOLD_FONT = null;
        }

        if (COURIER_BOLD_ITALIC_FONT != null) {
            COURIER_BOLD_ITALIC_FONT.dispose();
            COURIER_BOLD_ITALIC_FONT = null;
        }

        if (COURIER_ITALIC_FONT != null) {
            COURIER_ITALIC_FONT.dispose();
            COURIER_ITALIC_FONT = null;
        }

        if (COURIER_NORMAL_FONT != null) {
            COURIER_NORMAL_FONT.dispose();
            COURIER_NORMAL_FONT = null;
        }

        if (TIMES_LIKE_BOLD_FONT != null) {
            TIMES_LIKE_BOLD_FONT.dispose();
            TIMES_LIKE_BOLD_FONT = null;
        }

        if (TIMES_LIKE_BOLD_ITALIC_FONT != null) {
            TIMES_LIKE_BOLD_ITALIC_FONT.dispose();
            TIMES_LIKE_BOLD_ITALIC_FONT = null;
        }

        if (TIMES_LIKE_ITALIC_FONT != null) {
            TIMES_LIKE_ITALIC_FONT.dispose();
            TIMES_LIKE_ITALIC_FONT = null;
        }

        if (TIMES_LIKE_NORMAL_FONT != null) {
            TIMES_LIKE_NORMAL_FONT.dispose();
            TIMES_LIKE_NORMAL_FONT = null;
        }

        if (HELVETICA_LIKE_BOLD_FONT != null) {
            HELVETICA_LIKE_BOLD_FONT.dispose();
            HELVETICA_LIKE_BOLD_FONT = null;
        }

        if (HELVETICA_LIKE_BOLD_ITALIC_FONT != null) {
            HELVETICA_LIKE_BOLD_ITALIC_FONT.dispose();
            HELVETICA_LIKE_BOLD_ITALIC_FONT = null;
        }

        if (HELVETICA_LIKE_ITALIC_FONT != null) {
            HELVETICA_LIKE_ITALIC_FONT.dispose();
            HELVETICA_LIKE_ITALIC_FONT = null;
        }

        if (HELVETICA_LIKE_NORMAL_FONT != null) {
            HELVETICA_LIKE_NORMAL_FONT.dispose();
            HELVETICA_LIKE_NORMAL_FONT = null;
        }
        Tracer.print("< Exit");
    }
}