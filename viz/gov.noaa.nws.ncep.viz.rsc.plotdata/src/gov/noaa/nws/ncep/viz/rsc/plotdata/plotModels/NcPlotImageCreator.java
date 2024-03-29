package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.measure.Unit;
import javax.measure.format.ParserException;
import javax.measure.quantity.Angle;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.units.UnitAdapter;
import com.raytheon.uf.viz.core.DrawableBasics;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.jobs.JobPool;

import gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter;
import gov.noaa.nws.ncep.edex.common.metparameters.Amount;
import gov.noaa.nws.ncep.edex.common.metparameters.PressureChange3HrAndTendency;
import gov.noaa.nws.ncep.ui.pgen.display.ArrowHead;
import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.display.IVector.VectorType;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefn;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefns;
import gov.noaa.nws.ncep.viz.rsc.plotdata.parameters.PlotParameterDefnsMngr;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModelElement;
import gov.noaa.nws.ncep.viz.rsc.plotdata.queue.QueueEntry;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Station;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Tracer;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import si.uom.NonSI;
import systems.uom.common.USCustomary;
import tec.uom.se.format.SimpleUnitFormat;

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
 * 08/08/2014    3477        bclement     changed plot info locations to floats
 * 09/10/2014    R4230       S. Russell   Fix wind barb/brbk menu option in plot model dialog box
 * 11/03/2014    R4830       S. Russell   Added elements to presWxSymbolNames
 * 11/03/2014    R5156       B. Hebbard   Allow use of system fonts in addition to file-based 3
 * 11/06/2014    R5156       B. Hebbard   Rename Helvetica & Times lookalike fonts/files to make clear they aren't Java/AWT logical SansSerif & Serif
 * 08/14/2015    R7757       B. Hebbard   Add support for directional arrow (no magnitude) parameters; also refactor so imageCreator belongs directly to resource
 *                                        (instead of NcPlotDataRequestor) for better frame status tracking; other cleanups.
 * 11/17/2015    R9579       B. Hebbard   Add support for MARK (marker) symbol parameter; various cleanups
 * 12/17/2015    R9579       B. Hebbard   Fix PTND regression preventing symbol draw; prevent NPE on null lookupTable return
 * 11/05/2015    5070        randerso     Adjust font sizes for dpi scaling
 * 04/18/2016    R17315      J. Beck      Fix Exception on startup coming from removeSign(). Fix logic in removeSign().Added Bruce's comments to removeSign().
 *                                        Added statusHandler wrapper -- problemHandler(), to make distinct error messages programmatically.
 * 11/07/2016    R23252      S. Russell   Updated createOneVector() to support
 *                                        Vector wind arrows with OPEN
 *                                        arrowheads and support plotMode
 *                                        "arrows"
 * 11/09/2016    R26156      S. Russell   update createOneVector() to handle
 *                                        the special case of parameter DARR
 *
 * 11/25/2016    R21762      P. Moyer     Implemented dropping of drawable items if their colors have an alpha of 0 (for conditional parameter coloring)
 * Mar 8, 2019   7581        tgurney      Fix NPE in createOneVector + Cleanup
 * </pre>
 */

public class NcPlotImageCreator {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NcPlotImageCreator.class);

    private JobPool imageCreationJobPool = null;

    private ConcurrentLinkedQueue<QueueEntry> queueOfStations = null;

    private IPointInfoRenderingListener iPointInfoRenderingListener = null;

    private PlotModel plotModel = null;

    private EnumMap<Position, PlotModelElement> plotModelPositionToPmeMap = null;

    public boolean isThereAConditionalFilter = false;

    private Map<DataTime, Map<String, Station>> mapOfStnsToDataTime = null;

    private Map<DataTime, Map<Position, List<SymbolLocationSet>>> mapOfSymbolsPerPlotPosPerFrame = null;

    private Map<Position, PlotSymbolType> positionToSymbolTypeMap = null;

    private PlotParameterDefns plotParameterDefinitions = null;

    private IView lastView = null;

    private double lastZoomLevel = Double.MIN_VALUE;

    private static double TOLERANCE = 1E-04;

    private static double ZOOM_TOLERANCE = 1E-04;

    private static Amount WIND_SPD_3KNOTS = new Amount(3, USCustomary.KNOT);

    private Map<PlotSymbolType, StringLookup> symbolTypeToLookupTableMap = null;

    private Map<DataTime, Map<Position, Map<String, DrawableString>>> dataTimeToText = null;

    private RGB defaultColor = null;

    private PlotFontMngr plotFontMngr = null;

    public static enum Position {
        TC, UL, UC, UR, ML, MC, MR, LL, LC, LR, BC,
        // ----
        // special sky coverage position -- plots at MC
        SC,
        // special wind barb position -- plots at MC
        WD,
        // ----
        INVALID
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
        queueOfStations = new ConcurrentLinkedQueue<>();
        iPointInfoRenderingListener = listener;
        mapOfStnsToDataTime = new HashMap<>();
        dataTimeToText = new HashMap<>();
        positionToSymbolTypeMap = new EnumMap<>(Position.class);
        symbolTypeToLookupTableMap = new EnumMap<>(PlotSymbolType.class);
        plotModelPositionToPmeMap = new EnumMap<>(Position.class);
        mapOfSymbolsPerPlotPosPerFrame = new HashMap<>();
        plotFontMngr = new PlotFontMngr();
        setUpPlotPositionToPlotModelElementMapping(plotModel);
        setUpSymbolLookupTables();
        Tracer.print("< Exit");
    }

    public void queueStationsToCreateImages(DataTime dt,
            Collection<Station> stations) {
        Tracer.print("> Entry " + Tracer.shortTimeString(dt));
        QueueEntry qe = new QueueEntry(dt, stations);
        Tracer.print(
                "About to queue " + stations.size() + " stations from frame "
                        + Tracer.shortTimeString(dt) + " for image creation\n");
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

    public Position getPositionFromPlotModelElement(PlotModelElement pme) {
        Position position = Position.INVALID;
        try {
            position = Position.valueOf(pme.getPosition());
        } catch (IllegalArgumentException e) {
            position = Position.INVALID;
            statusHandler
                    .warn("Failed to get position from plot model element (got: "
                            + Objects.toString(pme.getPosition()) + ")", e);
        }
        return position;
    }

    public void setUpPlotPositionToPlotModelElementMapping(PlotModel pm) {
        Tracer.print("> Entry");
        plotModelPositionToPmeMap.clear();
        List<PlotModelElement> plotModelElementsList = pm
                .getAllPlotModelElements();
        if (plotModelElementsList != null && !plotModelElementsList.isEmpty()) {
            synchronized (plotModelElementsList) {
                for (PlotModelElement pme : plotModelElementsList) {
                    // NcPlotImageCreator will not process BRBK unless it has
                    // the abstract ( not on the grid of plot model buttons )
                    // position "WD"
                    if ("BRBK".equalsIgnoreCase(pme.getParamName())) {
                        pme.setPosition("WD");
                    }
                    Position position = getPositionFromPlotModelElement(pme);
                    plotModelPositionToPmeMap.put(position, pme);
                }
            }
        }
        Tracer.print("< Exit");
    }

    public void setPlotModel(PlotModel pm) {
        Tracer.print("> Entry");
        this.plotModel = pm;
        // Change in plot model triggers update to map and lookup tables
        setUpPlotPositionToPlotModelElementMapping(plotModel);
        setUpSymbolLookupTables();
        Tracer.print("< Exit");
    }

    public void removeObsoletePMEEntries(PlotModel pm) {
        Tracer.print("> Entry");
        List<PlotModelElement> plotModelElementsList = pm
                .getAllPlotModelElements();
        Set<Position> posToRemove = EnumSet.noneOf(Position.class);
        if (plotModelPositionToPmeMap != null
                && !plotModelPositionToPmeMap.isEmpty()) {
            Set<Position> posSet = EnumSet.noneOf(Position.class);
            posSet.addAll(plotModelPositionToPmeMap.keySet());
            synchronized (posSet) {
                for (Position pos : posSet) {
                    boolean matchFound = false;
                    synchronized (plotModelElementsList) {
                        for (PlotModelElement pme : plotModelElementsList) {
                            if (pos.toString()
                                    .equalsIgnoreCase(pme.getPosition())) {
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
                    positionToSymbolTypeMap.remove(p);
                    if (p != Position.WD) {
                        if (dataTimeToText != null
                                && !dataTimeToText.isEmpty()) {
                            Set<DataTime> frameTimeSet = dataTimeToText
                                    .keySet();
                            if (frameTimeSet != null
                                    && !frameTimeSet.isEmpty()) {
                                synchronized (frameTimeSet) {
                                    for (DataTime dt : frameTimeSet) {
                                        Map<Position, Map<String, DrawableString>> mapOfStrPosition = dataTimeToText
                                                .get(dt);
                                        if (mapOfStrPosition != null
                                                && !mapOfStrPosition
                                                        .isEmpty()) {
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
                            if (frameTimeSet != null
                                    && !frameTimeSet.isEmpty()) {
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

    /**
     * For each position in the plot model that requires a symbol lookup table,
     * read the lookup file into a lookup table, map the lookup table from the
     * symbol type, and map the symbol type from the plot position
     */
    public void setUpSymbolLookupTables() {
        Tracer.print("> Entry");
        symbolTypeToLookupTableMap.clear();
        Set<Position> positionSet = EnumSet.noneOf(Position.class);
        positionSet.addAll(plotModelPositionToPmeMap.keySet());
        synchronized (positionSet) {
            for (Position position : positionSet) {
                PlotModelElement pme = plotModelPositionToPmeMap.get(position);
                String plotParamName = pme.getParamName();
                // Temporarily change the name of the combination element, the
                // PTND button, to get the PTSY symbol part of it to process
                if ("PTND".equalsIgnoreCase(plotParamName)) {
                    plotParamName = "PTSY";
                }
                PlotParameterDefn thisPlotParamDefn = plotParameterDefinitions
                        .getPlotParamDefn(plotParamName);
                if (thisPlotParamDefn == null) {
                    Tracer.print("Unable to find " + plotParamName
                            + " in the list of plot parameter definitions for "
                            + plotModel.getPlugin() + ":"
                            + plotModel.getName());
                    continue;
                }
                if (thisPlotParamDefn.getPlotMode()
                        .equalsIgnoreCase(PlotParameterDefn.PLOT_MODE_MARKER)
                        || thisPlotParamDefn.getPlotMode().equalsIgnoreCase(
                                PlotParameterDefn.PLOT_MODE_TABLE)) {
                    PlotSymbolType symbolType = getPlotSymbolType(
                            plotParamName);
                    positionToSymbolTypeMap.put(position, symbolType);
                    if (symbolTypeToLookupTableMap.get(symbolType) == null) {
                        if (symbolType != PlotSymbolType.INVALID) {
                            String lookupTableName = thisPlotParamDefn
                                    .getPlotLookupTable();
                            if (lookupTableName != null) {
                                // Some symbol types (MARK) don't use lookup
                                // table
                                StringLookup lookupTable = StringLookup
                                        .readS2SFile(lookupTableName);
                                symbolTypeToLookupTableMap.put(symbolType,
                                        lookupTable);
                            }
                        }
                    }
                }
            }
        }
        Tracer.print("< Exit");
    }

    public static enum PlotSymbolType {
        WSYM, SKYC, ICSY, TBSY, PTSY, MARK, INVALID
    }

    private PlotSymbolType getPlotSymbolType(String symbolGEMPAKName) {
        PlotSymbolType symbolType = PlotSymbolType.INVALID;
        if ("PTND".equalsIgnoreCase(symbolGEMPAKName)) {
            symbolType = PlotSymbolType.PTSY;
        } else {
            try {
                symbolType = PlotSymbolType.valueOf(symbolGEMPAKName);
            } catch (IllegalArgumentException e) {
                symbolType = PlotSymbolType.INVALID;
                statusHandler
                        .warn("Failed to get plot symbol type from string (value: "
                                + Objects.toString(symbolGEMPAKName) + ")", e);
            }
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
            this.listOfStations = new ArrayList<>(listOfStationsToDrawImages);
            activePane = NcDisplayMngr.getActiveNatlCntrsEditor()
                    .getActiveDisplayPane();
            aTarget = activePane.getTarget();
            mapDescriptor = activePane.getDescriptor();
            renderableDisplay = mapDescriptor.getRenderableDisplay();
            view = renderableDisplay.getView();

            if (lastZoomLevel == Double.MIN_VALUE
                    || (Math.abs(renderableDisplay.getZoom()
                            - lastZoomLevel) > ZOOM_TOLERANCE)) {
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
            return Long.toString(Math.round(lon * 1000.0)) + ","
                    + Long.toString(Math.round(lat * 1000.0));

        }

        private String createKeyFromTextCoordinates(DrawableBasics db) {
            if (db == null) {
                return null;
            }
            String key = Long.toString(Math.round(db.x * 10_000)) + ","
                    + Long.toString(Math.round(db.y * 10_000));
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
                Collection<Station> stnColl, String plotUnit, double symbolSize,
                RGB rgb, String metPrm1, String metPrm2, PlotModelElement pme,
                boolean directionOnly, boolean directionReverse,
                String plotmode, String paramname) {

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
                    Integer alpha = null;
                    try {
                        IVector vector = null;
                        synchronized (currentStation.parametersToPlot) {
                            for (AbstractMetParameter metPrm : currentStation.parametersToPlot) {
                                if (metPrm.getMetParamName()
                                        .equalsIgnoreCase(metPrm1)) {
                                    vectorParam1 = metPrm;
                                }
                                if (metPrm.getMetParamName()
                                        .equalsIgnoreCase(metPrm2)) {
                                    vectorParam2 = metPrm;
                                }
                            }
                            if ((vectorParam1 != null && ((vectorParam2 != null)
                                    || directionOnly))) {
                                if (pme.hasAdvancedSettings()) {
                                    rgb = getConditionalColor(currentStation,
                                            pme);
                                    alpha = getConditionalAlpha(currentStation,
                                            pme);
                                }
                                if (rgb == null) {
                                    rgb = new RGB(255, 255, 255);
                                }
                                if ((alpha != null)
                                        && (alpha.intValue() == 0)) {
                                    // alpha = new Integer(255);
                                    continue;
                                } else {
                                    alpha = new Integer(255);
                                }

                                /*
                                 * dropping of vector via "continue" when
                                 * receiving a conditional color with alpha 0.
                                 */

                                vector = createOneVector(vectorParam1,
                                        vectorParam2, plotUnit, symbolSize, rgb,
                                        alpha, stationWorldLoc,
                                        directionReverse, plotmode, paramname);
                            }
                            if (vector != null) {
                                Tracer.printX(Tracer.shortTimeString(
                                        this.dataTime) + " Adding a vector for "
                                        + currentStation.info.stationId);
                                List<IVector> vectorsAtThisLocation = vectorCoordinatesToVectorsMap
                                        .get(vector.getLocation());
                                if (vectorsAtThisLocation == null) {
                                    vectorsAtThisLocation = new ArrayList<>();
                                    vectorCoordinatesToVectorsMap.put(
                                            vector.getLocation(),
                                            vectorsAtThisLocation);
                                }
                                vectorsAtThisLocation.add(vector);
                                Tracer.printX(Tracer.shortTimeString(
                                        this.dataTime) + " Created vector for "
                                        + currentStation.info.stationId);
                            }
                        }
                    } catch (Exception e) {
                        statusHandler.warn(e.getLocalizedMessage(), e);
                    }
                }
            }
            Tracer.print("< Exit  " + Tracer.shortTimeString(this.dataTime));
        }

        private IVector createOneVector(AbstractMetParameter metParam1,
                AbstractMetParameter metParam2, String plotUnit,
                double symbolSize, RGB rgb, Integer alpha, double[] stationLoc,
                boolean directionReverse, String plotmode, String paramname) {
            Tracer.printX("> Entry");
            AbstractMetParameter speed = null;
            AbstractMetParameter direction = null;
            Vector vector = null;
            ArrowHead.ArrowHeadType arrowHeadType = ArrowHead.ArrowHeadType.FILLED;

            if (metParam1 instanceof Angle) {
                direction = metParam1;
                speed = metParam2;
            } else if (metParam2 instanceof Angle) {
                direction = metParam2;
                speed = metParam1;
            }
            Color[] vectorColorArray = new Color[] {
                    new Color(rgb.red, rgb.green, rgb.blue, alpha.intValue()) };
            try {

                // The units in the element are for the speed and not the
                // direction.
                Number nDirection = direction.getValueAs(NonSI.DEGREE_ANGLE);
                if (nDirection == null) {
                    return null;
                }

                double dDirection = nDirection.doubleValue();

                if (plotmode.equalsIgnoreCase(
                        PlotParameterDefn.PLOT_MODE_DIRECTIONAL)) {

                    // TODO: A more elegant way of accommodating the special
                    // case of DARR should be found. Directional arrows
                    // usually have FILLED arrowheads, but NMAP is inconsistent
                    // in regards to DARR, which is a directional arrow. NMAP
                    // renders DARR with OPEN arrowheads, so this hackish method
                    // is done here to make CAVE match NMAP while making a
                    // deadline.
                    if ("DARR".equalsIgnoreCase(paramname)) {
                        arrowHeadType = ArrowHead.ArrowHeadType.OPEN;
                    }

                    // Directional arrow.
                    // Always the same size arrows
                    // Only indicates direction

                    Number nSpeed = (speed == null) ? null
                            : speed.getValueAs(plotUnit);
                    // If speed not reported (as for wave direction), assume
                    // positive for following test.
                    // If speed reported as zero, preserve as zero so arrow will
                    // be suppressed.
                    double dSpeed = (nSpeed == null) ? 1.0
                            : nSpeed.doubleValue();

                    /*
                     * Suppress arrow if calm (direction and speed both zero).
                     * Note that if direction is nonzero (non-north), then we
                     * assume it's significant (and produce the directional
                     * arrow) even if speed is zero, in case speed of zero is
                     * used to indicate missing value (say, "0" instead of "/")
                     */
                    if (dDirection > 0.0 || dSpeed > 0.0) {
                        if (directionReverse) {
                            dDirection = dDirection > 180.0 ? dDirection - 180.0
                                    : dDirection + 180.0;
                        }
                        /*
                         * Numeric constants in the following are "tuned" to
                         * match NMAP2 display characteristics.
                         */
                        vector = new Vector(null, vectorColorArray, 1.20f,
                                symbolSize * 2.60, false,
                                new Coordinate(stationLoc[0], stationLoc[1]),
                                VectorType.ARROW, dSpeed, dDirection, 0.9, true,
                                "Vector", "Arrow", arrowHeadType);
                    }
                } else if (plotmode
                        .equalsIgnoreCase(PlotParameterDefn.PLOT_MODE_BARB)) {
                    // Wind barb
                    Number speedConverted = (speed == null) ? null
                            : speed.getValueAs(plotUnit);
                    double uSpeed = (speedConverted == null) ? Double.MIN_VALUE
                            : speedConverted.doubleValue();
                    double dSpeed = Double.MIN_VALUE;
                    Unit<?> unit;
                    unit = (Unit<?>) SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII)
                            .parse(plotUnit);
                    double cWindSpeedThresh = WIND_SPD_3KNOTS.getValueAs(unit)
                            .doubleValue();
                    if (uSpeed >= cWindSpeedThresh) {
                        dSpeed = roundTo5(uSpeed);
                    }
                    vector = new Vector(null, vectorColorArray, 1.0f,
                            symbolSize, false,
                            new Coordinate(stationLoc[0], stationLoc[1]),
                            VectorType.WIND_BARB, dSpeed, dDirection, 1.0, true,
                            "Vector", "Barb");
                } else if (plotmode
                        .equalsIgnoreCase(PlotParameterDefn.PLOT_MODE_ARROW)) {

                    // Arrow == Vector arrow, the arrow is built on direction
                    // AND speed. Faster vectors are longer arrows, slower
                    // vectors are shorter arrows.

                    arrowHeadType = ArrowHead.ArrowHeadType.OPEN;

                    Number nSpeed = null;
                    double dSpeed = 1.0d;

                    nSpeed = (speed == null) ? null
                            : speed.getValueAs(plotUnit);

                    dSpeed = (nSpeed == null) ? 1.0 : nSpeed.doubleValue();

                    if (dDirection > 0.0 || dSpeed > 0.0) {
                        if (directionReverse) {
                            dDirection = dDirection > 180.0 ? dDirection - 180.0
                                    : dDirection + 180.0;
                        }

                        // Numeric constants in the following are "tuned" to
                        // match NMAP2 display characteristics.
                        vector = new Vector(null, vectorColorArray, 2.50f,
                                symbolSize * 1.28, false,
                                new Coordinate(stationLoc[0], stationLoc[1]),
                                VectorType.ARROW, dSpeed, dDirection, 0.9,
                                false, "VECTOR", "Arrow", arrowHeadType);
                    }

                }

            } catch (ParserException e) {
                statusHandler
                        .warn("Failed to parse plot unit from string (value: "
                                + Objects.toString(plotUnit) + ")", e);
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

            // Initialize the 3 'drawables' collections we're building for
            // return back to the main resource class
            List<DrawableString> stringsToDraw = new ArrayList<>();
            List<IVector> vectorsToDraw = new ArrayList<>(0);
            List<SymbolLocationSet> symbolLocationSetsToDraw = new ArrayList<>(
                    0);

            // Local structures only
            Map<Position, Map<String, DrawableString>> localDMap = null;
            Map<Position, List<SymbolLocationSet>> positionToSymbolLocationSetsMap = new EnumMap<>(
                    Position.class);

            if (dataTimeToText.get(dataTime) != null) {
                localDMap = new EnumMap<>(dataTimeToText.get(dataTime));
            } else {
                localDMap = new EnumMap<>(Position.class);
            }

            Set<Position> positionSet = EnumSet.noneOf(Position.class);
            positionSet.addAll(plotModelPositionToPmeMap.keySet());
            Map<String, Station> stationMap = new HashMap<>(
                    listOfStations.size());
            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " listOfStations has " + listOfStations.size()
                    + " stations right after stationMap creation");

            Tracer.print(
                    "From createRenderableData()  - list of parameters plotted per station for frame: "
                            + Tracer.shortTimeString(dataTime));

            /* Create a map of stations that have been currently disclosed */
            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " stationMap has " + stationMap.size()
                    + " stations before loop (should be 0)");
            for (Station station : listOfStations) {
                String key = stationMapKey(station.info.longitude,
                        station.info.latitude) + "-"
                        + station.info.dataTime.toString();
                Tracer.printX(Tracer.shortTimeString(this.dataTime)
                        + " stnplotMap for " + station.info.stationId + " : "
                        + station.positionToLocationMap.toString());
                Tracer.printX("For frame " + Tracer.shortTimeString(dataTime)
                        + " " + station.info.stationId + " at time "
                        + station.info.dataTime.toString()
                        + " - List of parameters:  "
                        + station.parametersToPlot.toString());
                stationMap.put(key, station);
            }
            Tracer.printX(
                    Tracer.shortTimeString(this.dataTime) + " stationMap has "
                            + stationMap.size() + " stations after loop");

            if (lastView == null) {
                lastView = view.clone();
            }

            double width = aTarget
                    .getStringsBounds(new DrawableString("M", defaultColor))
                    .getWidth();
            double height = aTarget
                    .getStringsBounds(new DrawableString("'y", defaultColor))
                    .getHeight();
            Rectangle textBounds = new Rectangle(0, 0, (int) width,
                    (int) height);

            Tracer.print("textBounds initialized to " + textBounds);

            Tracer.print(Tracer.shortTimeString(this.dataTime)
                    + " positionSet has " + positionSet.size() + " elements");

            Map<Coordinate, List<IVector>> vectorCoordinatesToVectorsMap = new HashMap<>(
                    0);

            for (Position position : positionSet) {

                // Get the formatting information from the PlotModelElement for
                // the current plot position
                PlotModelElement pme = plotModelPositionToPmeMap.get(position);

                RGB pmeColor = pme.getColorRGB();

                // Set the font information
                IFont font = plotFontMngr.getFont(pme.getTextFont(),
                        pme.getTextStyle(),
                        Integer.parseInt(pme.getTextSize()));
                if (font == null) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to obtain font:  " + pme.getTextFont() + "-"
                                    + pme.getTextStyle() + "-"
                                    + Integer.parseInt(pme.getTextSize()));
                }

                // Get the data retrieval information from the PlotParameterDefn
                // corresponding to the current PlotModelElement
                PlotParameterDefn plotParamDefn = plotParameterDefinitions
                        .getPlotParamDefn(pme.getParamName());

                String plotmode = plotParamDefn.getPlotMode();
                boolean drawVector = (position == Position.WD
                        || plotmode
                                .equals(PlotParameterDefn.PLOT_MODE_DIRECTIONAL)
                        || plotmode.equals(PlotParameterDefn.PLOT_MODE_ARROW));

                boolean drawSymbol = (positionToSymbolTypeMap
                        .get(position) != null);
                // TODO Suggest refining following boolean expression (to test
                // what the plot mode *is*, rather than what things it is not).
                // @formatter:off
                boolean drawText = (!plotParamDefn.getPlotMode()
                        .equals(PlotParameterDefn.PLOT_MODE_BARB)
                        && !plotParamDefn.getPlotMode()
                                .equals(PlotParameterDefn.PLOT_MODE_DIRECTIONAL))
                        && !plotParamDefn.getPlotMode()
                                .equals(PlotParameterDefn.PLOT_MODE_MARKER)
                        && !plotParamDefn.getPlotMode()
                                .equals(PlotParameterDefn.PLOT_MODE_TABLE);
                // @formatter:on

                if (drawText) {
                    Map<String, DrawableString> mapOfStrPositions = localDMap
                            .get(position);
                    /*
                     * TODO: Note setting this to true is not safe -- distorts
                     * station plots after zoom!
                     */
                    if (mapOfStrPositions == null) {

                        mapOfStrPositions = new HashMap<>();

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
                                    station.positionToLocationMap.put(position,
                                            drawableString.basics);
                                    // Add the pixel coordinates and the string
                                    // to render to the Map<DrawableBasics,
                                    // DrawableString>
                                    String dbKey = createKeyFromTextCoordinates(
                                            drawableString.basics);
                                    mapOfStrPositions.put(dbKey,
                                            drawableString);
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
                            List<String> pixPosToRemoveList = new ArrayList<>();
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
                                    statusHandler.warn(e.getLocalizedMessage(),
                                            e);
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
                                DrawableBasics dbStn = station.positionToLocationMap
                                        .get(position);

                                if (dbStn != null) {

                                    // Fetch the corresponding string from the
                                    // Map<DrawableBasics, DrawableString>
                                    String strPosKey = createKeyFromTextCoordinates(
                                            dbStn);
                                    DrawableString strToReposition = mapOfStrPositions
                                            .get(strPosKey);

                                    if (strToReposition != null) {
                                        mapOfStrPositions.remove(strPosKey);
                                        station.positionToLocationMap
                                                .remove(position);

                                        // If the string exists, update its
                                        // coordinates
                                        Rectangle2D rr = aTarget
                                                .getStringsBounds(
                                                        new DrawableString(
                                                                "'" + strToReposition
                                                                        .getText()[0]
                                                                        + "y",
                                                                strToReposition
                                                                        .getColors()[0]));
                                        textBounds = new Rectangle(0, 0,
                                                (int) rr.getWidth(),
                                                (int) rr.getHeight());

                                        Tracer.print(
                                                "textBounds updated in TEXT draw for station "
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

                                        if (!pme.hasAdvancedSettings()) {
                                            strToReposition.setText(
                                                    strToReposition.getText(),
                                                    pmeColor);
                                        } else if (pme.hasAdvancedSettings()) {
                                            RGB rgb = getConditionalColor(
                                                    station, pme);
                                            strToReposition.setText(
                                                    strToReposition.getText(),
                                                    rgb);

                                            Integer alpha = getConditionalAlpha(
                                                    station, pme);
                                            if ((alpha == null) || (alpha
                                                    .intValue() == 0)) {
                                                continue;
                                            }
                                        }

                                        if ((!font.getFontName()
                                                .equals(strToReposition.font
                                                        .getFontName()))
                                                || (Math.abs(font.getFontSize()
                                                        - strToReposition.font
                                                                .getFontSize()) > TOLERANCE)
                                                || (font.getStyle() != null
                                                        && strToReposition.font
                                                                .getStyle() != null
                                                        && font.getStyle().length > 0
                                                        && strToReposition.font
                                                                .getStyle().length > 0
                                                        && font.getStyle()[0] != strToReposition.font
                                                                .getStyle()[0])
                                                || ((font.getStyle() == null
                                                        || font.getStyle().length == 0)
                                                        && strToReposition.font
                                                                // The style is
                                                                // set to null
                                                                // for
                                                                // plain style
                                                                // fonts
                                                                .getStyle() != null)
                                                || (font.getStyle() != null
                                                        && (strToReposition.font
                                                                .getStyle() == null
                                                                || strToReposition.font
                                                                        .getStyle().length == 0))) {
                                            strToReposition.font = font;
                                        }

                                        synchronized (strToReposition.basics) {
                                            station.positionToLocationMap.put(
                                                    position,
                                                    strToReposition.basics);
                                            String localDbKey = createKeyFromTextCoordinates(
                                                    strToReposition.basics);
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
                                            station.positionToLocationMap.put(
                                                    position,
                                                    drawableString.basics);
                                            String keydb = createKeyFromTextCoordinates(
                                                    drawableString.basics);
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
                                                        || oldColor.green != pmeColor.green
                                                        || oldColor.blue != pmeColor.blue)) {
                                            drawableString.setText(
                                                    drawableString.getText(),
                                                    pmeColor);
                                        } else if (pme.hasAdvancedSettings()) {
                                            RGB rgb = getConditionalColor(
                                                    station, pme);
                                            drawableString.setText(
                                                    drawableString.getText(),
                                                    rgb);

                                            Integer alpha = getConditionalAlpha(
                                                    station, pme);
                                            if ((alpha == null) || (alpha
                                                    .intValue() == 0)) {
                                                continue;
                                            }
                                        }
                                        if (!font.getFontName()
                                                .equals(drawableString.font
                                                        .getFontName())
                                                || (Math.abs(font.getFontSize()
                                                        - drawableString.font
                                                                .getFontSize()) > TOLERANCE)
                                                || (font.getStyle() != null
                                                        && drawableString.font
                                                                .getStyle() != null
                                                        && font.getStyle().length > 0
                                                        && drawableString.font
                                                                .getStyle().length > 0
                                                        && font.getStyle()[0] != drawableString.font
                                                                .getStyle()[0])
                                                || ((font.getStyle() == null
                                                        || font.getStyle().length == 0)
                                                        && drawableString.font
                                                                // The style is
                                                                // set to null
                                                                // for
                                                                // plain style
                                                                // fonts
                                                                .getStyle() != null)
                                                || (font.getStyle() != null
                                                        && (drawableString.font
                                                                .getStyle() == null
                                                                || drawableString.font
                                                                        .getStyle().length == 0))) {
                                            drawableString.font = font;
                                        }

                                        synchronized (drawableString.basics) {
                                            station.positionToLocationMap.put(
                                                    position,
                                                    drawableString.basics);
                                            String dbkey = createKeyFromTextCoordinates(
                                                    drawableString.basics);
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
                    String paramname = pme.getParamName();
                    double symbolSize = (d == null ? 1.0 : d.doubleValue());
                    boolean directionOnly = plotParamDefn.getPlotMode()
                            .equals(PlotParameterDefn.PLOT_MODE_DIRECTIONAL);
                    boolean directionReverse = plotParamDefn
                            .getTransform() != null
                            && plotParamDefn.getTransform().equalsIgnoreCase(
                                    PlotParameterDefn.PLOT_TRANSFORM_REVERSE);
                    Collection<Station> stnColl = stationMap.values();
                    createVectors(vectorCoordinatesToVectorsMap, stnColl,
                            plotParamDefn.getPlotUnit(), symbolSize, pmeColor,
                            metPrm1, metPrm2, pme, directionOnly,
                            directionReverse, plotmode, paramname);
                }

                if (drawSymbol) {
                    // Since the Symbol associated with each SymbolLocationSet
                    // is passed in as an argument to the constructor there is
                    // no explicit method to set the symbol should its
                    // attributes change. Hence the symbols get recreated afresh
                    // each time.
                    boolean wasPTND = false;
                    PlotSymbolType symbolType = positionToSymbolTypeMap
                            .get(position);

                    // Temporarily change the name of the combination element,
                    // the PTND button, to get the PTSY symbol part of it to
                    // process.
                    // To this end, also set a flag to let us know this
                    // temporary name change, and that PTND processing is
                    // happening
                    String metParamName = plotParamDefn.getMetParamName();
                    if (metParamName != null && metParamName
                            .equalsIgnoreCase(PressureChange3HrAndTendency.class
                                    .getSimpleName())) {
                        plotParamDefn = plotParameterDefinitions
                                .getPlotParamDefn("PTSY");
                        wasPTND = true;
                    }

                    // Holder for the P03C value in the combination PTND element
                    // which holds values for P03C and PTSY
                    AbstractMetParameter ptnd = null;

                    if (symbolType != null) {

                        StringLookup lookupTable = symbolTypeToLookupTableMap
                                .get(symbolType);

                        positionToSymbolLocationSetsMap = new EnumMap<>(
                                Position.class);

                        List<Coordinate> listOfCoords = null;
                        Map<SymbolKey, List<Coordinate>> symbolKeyToSetOfCoordsMap = new HashMap<>();
                        Collection<Station> stnColl = stationMap.values();

                        synchronized (stnColl) {

                            for (Station station : stnColl) {
                                AbstractMetParameter tableParamToPlot = null;
                                String symbolPatternName;

                                synchronized (station.parametersToPlot) {
                                    try {
                                        // Temporarily change the name of the
                                        // combination element, the PTND button,
                                        // to get the PTSY symbol part of it
                                        // to process.
                                        if (wasPTND) {
                                            // Temporarily change back
                                            // to PTND to match condition in
                                            // next for loop
                                            metParamName = "PressureChange3HrAndTendency";
                                        }
                                        if (metParamName != null) {
                                            for (AbstractMetParameter metPrm : station.parametersToPlot) {
                                                if (metParamName.equals(metPrm
                                                        .getMetParamName())) {
                                                    tableParamToPlot = metPrm;
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        statusHandler.warn(
                                                e.getLocalizedMessage(), e);
                                    }
                                }
                                // Temporarily change the name of the
                                // combination element, the PTND button,
                                // to get the PTSY symbol part of it to
                                // process.
                                if (wasPTND && tableParamToPlot != null
                                        && "PressureChange3HrAndTendency"
                                                .equalsIgnoreCase(
                                                        tableParamToPlot
                                                                .getMetParamName())) {
                                    if (tableParamToPlot
                                            .getAssociatedMetParam() != null
                                            && tableParamToPlot
                                                    .hasValidValue()) {
                                        /*
                                         * Save a copy of the P03C values from
                                         * the PTND metparameter
                                         */
                                        ptnd = tableParamToPlot;
                                        // Get the stored PTSY metparameter
                                        tableParamToPlot = tableParamToPlot
                                                .getAssociatedMetParam();
                                    } else {
                                        tableParamToPlot = null;
                                    }
                                }
                                String formattedString = null;
                                if (symbolType == PlotSymbolType.MARK) {
                                    symbolPatternName = pme.getMarkerType()
                                            .toString();
                                } else {
                                    formattedString = getFormattedValueToPlot(
                                            plotParamDefn, tableParamToPlot);

                                    if (formattedString == null
                                            || formattedString.isEmpty()) {
                                        continue;
                                    }
                                    if (lookupTable == null) {
                                        continue;
                                    }

                                    symbolPatternName = lookupTable
                                            .recursiveTranslation(
                                                    formattedString);
                                    if (symbolPatternName == null
                                            || symbolPatternName.isEmpty()) {
                                        continue;
                                    }
                                    symbolPatternName = symbolPatternName
                                            .trim();
                                }

                                // Determine color to paint the symbol:
                                // Use conditional color if applicable;
                                // otherwise, use default color
                                // associated with this PME
                                RGB rgb = null;
                                Integer alpha = null;
                                if (pme.hasAdvancedSettings()) {
                                    rgb = getConditionalColor(station, pme);
                                    alpha = getConditionalAlpha(station, pme);
                                } else {
                                    rgb = pme.getColorRGB();
                                    alpha = new Integer(255);
                                }
                                if (rgb == null) {
                                    continue;
                                }
                                if ((alpha == null)
                                        || (alpha.intValue() == 0)) {
                                    continue;
                                }

                                // Create a key object based on symbol
                                // pattern, type, color, size, width.
                                // This will be used (1) to create the
                                // actual Symbol object, and (2)
                                // as key to a map to locations sharing the
                                // same symbol characteristics.
                                SymbolKey symbolKey = new SymbolKey(
                                        symbolPatternName,
                                        (symbolType == PlotSymbolType.MARK),
                                        rgb, alpha,
                                        (float) pme.getSymbolSize()
                                                .doubleValue(),
                                        (float) pme.getSymbolWidth()
                                                .doubleValue());

                                // Get coordinates for the symbol. Start
                                // with actual station coordinates, and
                                // then offset as needed
                                double worldLoc[] = new double[] {
                                        station.info.longitude,
                                        station.info.latitude };
                                double[] tempPixLoc = mapDescriptor
                                        .worldToPixel(worldLoc);
                                textBounds = new Rectangle(0, 0, (int) width,
                                        (int) height);
                                // Add a horizontal offset to the
                                // PTSY symbol so it doesn't overwrite
                                // the P03C number
                                tempPixLoc[0] = adjustForPTSYSymbol(ptnd,
                                        symbolKey.symbolPatternName, position,
                                        textBounds, tempPixLoc[0], view,
                                        canvasBounds);
                                // Adjust the coordinates to offset from
                                // center depending on the plot position
                                tempPixLoc = getAdjustedCoordinates(textBounds,
                                        tempPixLoc[0], tempPixLoc[1], view,
                                        canvasBounds, position);
                                worldLoc = mapDescriptor
                                        .pixelToWorld(tempPixLoc);

                                // Pull up the list of coordinates we're
                                // currently building for these
                                // particular symbol settings. If no
                                // such list exists yet, create one.
                                listOfCoords = symbolKeyToSetOfCoordsMap
                                        .get(symbolKey);
                                if (listOfCoords == null) {
                                    listOfCoords = new ArrayList<>();
                                    symbolKeyToSetOfCoordsMap.put(symbolKey,
                                            listOfCoords);
                                }

                                // Add to that list the coordinates at
                                // which to draw the symbol for this
                                // station
                                listOfCoords.add(new Coordinate(worldLoc[0],
                                        worldLoc[1]));

                            }
                        }
                        List<SymbolLocationSet> symbolLocationSets = new ArrayList<>();
                        for (SymbolKey symbolKey : symbolKeyToSetOfCoordsMap
                                .keySet()) {
                            List<Coordinate> coordSet = symbolKeyToSetOfCoordsMap
                                    .get(symbolKey);
                            if (coordSet == null) {
                                continue;
                            }

                            RGB rgb = symbolKey.rgb;
                            Integer alpha = symbolKey.alpha;
                            Color symbolColor = new Color(rgb.red, rgb.green,
                                    rgb.blue, alpha.intValue());
                            Color[] colorArray = new Color[] { symbolColor };

                            symbolLocationSets.add(new SymbolLocationSet(null,
                                    colorArray, symbolKey.width,
                                    symbolKey.size * 0.75, // sizeScale
                                    false, // clear
                                    coordSet.toArray(new Coordinate[0]),
                                    symbolKey.symbolIsAMarker ? "Marker"
                                            : "Symbol", // category
                                    symbolKey.symbolPatternName));
                        }
                        positionToSymbolLocationSetsMap.put(position,
                                symbolLocationSets);
                        mapOfSymbolsPerPlotPosPerFrame.put(dataTime,
                                positionToSymbolLocationSetsMap);
                    }
                }
            }

            /*
             * Consolidate all the DrawableString at all positions into a single
             * list
             */
            try {
                for (Position pos : positionSet) {
                    Map<String, DrawableString> mapOfStrPositions = localDMap
                            .get(pos);
                    if (mapOfStrPositions != null
                            && !mapOfStrPositions.isEmpty()) {
                        stringsToDraw.addAll(mapOfStrPositions.values());
                    }
                }
            } catch (Exception e) {
                statusHandler.warn(e.getLocalizedMessage(), e);
            }

            /* Consolidate all vectors at all positions into a single list */
            vectorsToDraw = new ArrayList<>(0);
            for (List<IVector> vColl : vectorCoordinatesToVectorsMap.values()) {
                try {
                    vectorsToDraw.addAll(vColl);
                } catch (Exception e) {
                    statusHandler.warn(e.getLocalizedMessage(), e);
                }
            }

            /* Consolidate all symbols at all positions into a single list */
            for (Position pos : positionSet) {
                List<SymbolLocationSet> symbolLocSetList = positionToSymbolLocationSetsMap
                        .get(pos);
                if (symbolLocSetList != null) {
                    symbolLocationSetsToDraw.addAll(symbolLocSetList);
                }
            }

            /* Update the map of text entries for the current frame */
            if (dataTimeToText != null) {
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

        private DrawableString getDrawableStringForStation(Station station,
                PlotParameterDefn plotParamDefn, IFont font, RGB textColor,
                Position position, IGraphicsTarget aTarget,
                PlotModelElement pme) {

            Tracer.printX("> Entry");
            DrawableString drawableString = null;
            String metParamName = plotParamDefn.getMetParamName();
            boolean skipAlpha = false;
            Integer alpha = null;

            synchronized (station.parametersToPlot) {
                try {
                    if (pme.hasAdvancedSettings()) {
                        textColor = getConditionalColor(station, pme);

                        alpha = getConditionalAlpha(station, pme);
                        if ((alpha == null) || (alpha.intValue() == 0)) {
                            skipAlpha = true;
                        }
                    }

                    if (!skipAlpha) {
                        // loop over all values of station.parametersToPlot
                        for (AbstractMetParameter metParamToPlot : station.parametersToPlot) {
                            if (metParamToPlot.getMetParamName()
                                    .equals(metParamName)) {
                                String formattedString = getFormattedValueToPlot(
                                        plotParamDefn, metParamToPlot);

                                if (formattedString == null) {
                                    return null;
                                }

                                formattedString = removeSign(metParamToPlot,
                                        formattedString);

                                drawableString = new DrawableString(
                                        formattedString, textColor);
                                drawableString.horizontalAlignment = HorizontalAlignment.CENTER;
                                drawableString.verticallAlignment = VerticalAlignment.MIDDLE;
                                drawableString.font = font;
                                drawableString.magnification = 1.0;

                                Rectangle2D bounds = aTarget
                                        .getStringsBounds(new DrawableString(
                                                "'" + drawableString
                                                        .getText()[0] + "y",
                                                drawableString.getColors()[0]));
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
                            }
                        }
                    }

                } catch (Exception e) {
                    statusHandler.warn(e.getLocalizedMessage(), e);
                    Tracer.print("< Bad Exit");
                    return null;
                }
            }
            Tracer.print("< Exit");

            return drawableString;
        }
    }

    /**
     * Remove the arithmetic sign when the pressure change over the last 3 hours
     * is zero. Remove the arithmetic sign when the Pressure Tendency is steady.
     *
     * In order to make it easier to understand, the algorithm we use here is
     * not optimized.
     *
     * TODO: (Bruce H.) Consider improving the OO design so that this central
     * class (NcPlotImageCreator) doesn't need to know anything about specific
     * metParameters. For example, in this case, the removeSign operation might
     * be delegated to the specific child class of AbstractMetParameter needing
     * it (PressChange3Hr or Pressurechange3HrAndTendency) as an implementation
     * of a more generic method (e.g., something like cleanFormattedString()
     * with a default no-op (empty) implementation). This might be included with
     * a broader re-factor of metParameters (to handle known special cases such
     * as this) and of the Point Data Display resource (.rsc.plotdata) in
     * general.
     *
     * @param metParamToPlot
     * @param formattedString
     * @return the value formatted as a string
     */
    private String removeSign(AbstractMetParameter metParamToPlot,
            String formattedString) {

        // for temporary calculations
        String s = formattedString;

        int pressureIsSteady = 4;

        // Remove the sign
        if (s.startsWith("+") || s.startsWith("-")) {
            s = s.substring(1);
        }

        String metParamName = metParamToPlot.getMetParamName();

        if ("PressChange3Hr".equalsIgnoreCase(metParamName)) {

            // Return with sign removed
            if (metParamToPlot.getValue().doubleValue() == 0.0) {
                return s;
            }
        }

        if ("PressureChange3HrAndTendency".equalsIgnoreCase(metParamName)) {

            try {

                String pressureTendencySymbol = metParamToPlot
                        .getAssociatedMetParam().getStringValue();

                // Return with sign removed
                if (Integer
                        .parseInt(pressureTendencySymbol) == pressureIsSteady) {
                    return s;
                }

            } catch (Exception e) {
                statusHandler.warn(e.getLocalizedMessage(), e);
                return null;
            }
        }

        // Return the value passed in, including the sign
        return formattedString;
    }

    private String getFormattedValueToPlot(PlotParameterDefn plotParamDefn,
            AbstractMetParameter metPrm) {

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
                    formattedStringToPlot = metPrm.getStringValue();
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

                String formattedStr = metPrm
                        .getFormattedString(plotParamDefn.getPlotFormat())
                        .trim();
                String plotTrim = plotParamDefn.getPlotTrim();

                if (plotTrim != null && !plotTrim.isEmpty()) {
                    int plotTrimVal = Integer.parseInt(plotTrim);
                    formattedStringToPlot = formattedStr.substring(plotTrimVal);
                } else {
                    // just use the formattedString without trimming it
                    formattedStringToPlot = formattedStr;
                }

            }

        } catch (Exception e) {
            statusHandler.warn(e.getLocalizedMessage(), e);
            return plotUnit;
        }

        Tracer.printX("< Exit");

        return formattedStringToPlot;
    }

    /*
     * Calculate a horizontal offset for the PTSY symbol so that it does not
     * overwrite the P03C value
     */
    private double adjustForPTSYSymbol(AbstractMetParameter ptnd,
            String symbolName, Position position, Rectangle textBounds,
            double xPos, IView view, Rectangle canvasSize) {

        // NOT PTSY, return
        if (!symbolName.startsWith("PRESSURE_TENDENCY_")) {
            return xPos;
        }

        Tracer.print("PTSY adjust with textBounds " + textBounds);

        // Extract the number "00 - 08" from the end
        String PTSYvaluestr = symbolName.substring(18, symbolName.length());
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
            if ("C".equalsIgnoreCase(positionLastLetter)) {
                offsetXpos = xPos + (3 * adjustedWidth);
            } else if ("R".equalsIgnoreCase(positionLastLetter)) {
                if (numPreceedingChars == 1) {
                    m = 3;
                } else if (numPreceedingChars == 2) {
                    m = 4;
                } else if (numPreceedingChars == 3) {
                    m = 5;
                }
                offsetXpos = xPos + (m * adjustedWidth);
            } else if ("L".equalsIgnoreCase(positionLastLetter)) {
                offsetXpos = xPos + adjustedWidth;
            }
        } else {
            // PTSY symbol independent of a PTND button combination
            if ("C".equalsIgnoreCase(positionLastLetter)) {
                offsetXpos = xPos + (1 * adjustedWidth);
            } else if ("R".equalsIgnoreCase(positionLastLetter)) {
                offsetXpos = xPos + (2 * adjustedWidth);
            } else if ("L".equalsIgnoreCase(positionLastLetter)) {
                offsetXpos = xPos - (1 * adjustedWidth);
            }
        }

        return offsetXpos;
    }

    /*
     * Get the last letter of the position in the plot model. "LC" becomes "C"
     * (center)
     */
    private String getLastCharInPosition(Position position) {
        String lastLetter = null;
        String sPosition = position.name();
        int length = sPosition.length();
        length = length - 1;
        lastLetter = sPosition.substring(length);
        return lastLetter;
    }

    private double[] getAdjustedCoordinates(Rectangle textBounds, double xPos,
            double yPos, IView view, Rectangle canvasSize, Position position) {

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

    private RGB getConditionalColor(Station currentStation,
            PlotModelElement pme) {
        PlotParameterDefn condPlotParamDefn = PlotParameterDefnsMngr
                .getInstance().getPlotParamDefns(plotModel.getPlugin())
                .getPlotParamDefn(pme.getConditionalParameter());
        Tracer.printX("> Entry");
        RGB rgb = null;
        if (!currentStation.conditionalColorParameters.isEmpty()) {
            synchronized (currentStation.conditionalColorParameters) {
                for (AbstractMetParameter condColorMetPrm : currentStation.conditionalColorParameters) {
                    if (condColorMetPrm.getMetParamName()
                            .equals(condPlotParamDefn.getMetParamName())) {
                        String condParamValue = getConditionalParameterValue(
                                condPlotParamDefn, condColorMetPrm);
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

    private Integer getConditionalAlpha(Station currentStation,
            PlotModelElement pme) {
        PlotParameterDefn condPlotParamDefn = PlotParameterDefnsMngr
                .getInstance().getPlotParamDefns(plotModel.getPlugin())
                .getPlotParamDefn(pme.getConditionalParameter());
        Tracer.printX("> Entry");
        Integer alpha = null;
        if (!currentStation.conditionalColorParameters.isEmpty()) {
            synchronized (currentStation.conditionalColorParameters) {
                for (AbstractMetParameter condColorMetPrm : currentStation.conditionalColorParameters) {
                    if (condColorMetPrm.getMetParamName()
                            .equals(condPlotParamDefn.getMetParamName())) {
                        String condParamValue = getConditionalParameterValue(
                                condPlotParamDefn, condColorMetPrm);
                        if (condParamValue != null
                                && !condColorMetPrm.hasStringValue()) {
                            float value = Float.parseFloat(condParamValue);
                            if (pme.getConditionalColorBar() != null) {
                                alpha = pme.getConditionalColorBar()
                                        .getAlphaForInterval(value);
                                break;
                            }
                        }
                    }
                }
            }
        }
        Tracer.printX("< Exit " + alpha);
        return alpha;
    }

    private String getConditionalParameterValue(PlotParameterDefn plotParamDefn,
            AbstractMetParameter metPrm) {
        return getFormattedValueToPlot(plotParamDefn, metPrm);
    }

    public synchronized Map<Position, PlotModelElement> getPlotModelPositionMap() {
        return plotModelPositionToPmeMap;
    }

    public void removeObsoletePlotEntriesAtThisPositionForAllFrames(
            Position p) {
        Tracer.print("> Entry" + "  Position " + p);
        if (dataTimeToText != null && !dataTimeToText.isEmpty()) {
            Set<DataTime> dtSet = dataTimeToText.keySet();
            synchronized (dtSet) {
                for (DataTime dt : dtSet) {
                    Map<Position, Map<String, DrawableString>> mapOfStrPos = dataTimeToText
                            .get(dt);
                    if ((mapOfStrPos != null && !mapOfStrPos.isEmpty()
                            && mapOfStrPos.containsKey(p)) && (p != Position.SC)
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

        if (dataTimeToText != null) {
            dataTimeToText.clear();
            dataTimeToText = null;
        }

        if (mapOfStnsToDataTime != null) {
            mapOfStnsToDataTime.clear();
            mapOfStnsToDataTime = null;
        }

        if (mapOfSymbolsPerPlotPosPerFrame != null) {
            mapOfSymbolsPerPlotPosPerFrame.clear();
            mapOfSymbolsPerPlotPosPerFrame = null;
        }

        if (symbolTypeToLookupTableMap != null) {
            symbolTypeToLookupTableMap.clear();
            symbolTypeToLookupTableMap = null;
        }

        if (positionToSymbolTypeMap != null) {
            positionToSymbolTypeMap.clear();
            positionToSymbolTypeMap = null;
        }

        if (plotModelPositionToPmeMap != null) {
            plotModelPositionToPmeMap.clear();
            plotModelPositionToPmeMap = null;
        }

        if (plotFontMngr != null) {
            plotFontMngr.dispose();
            plotFontMngr = null;
        }
        Tracer.print("< Exit");
    }
}
