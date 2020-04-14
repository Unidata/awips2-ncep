/*****************************************************************************************
 * COPYRIGHT (c), 2007, RAYTHEON COMPANY
 * ALL RIGHTS RESERVED, An Unpublished Work
 *
 * RAYTHEON PROPRIETARY
 * If the end user is not the U.S. Government or any agency thereof, use
 * or disclosure of data contained in this source code file is subject to
 * the proprietary restrictions set forth in the Master Rights File.
 *
 * U.S. GOVERNMENT PURPOSE RIGHTS NOTICE
 * If the end user is the U.S. Government or any agency thereof, this source
 * code is provided to the U.S. Government with Government Purpose Rights.
 * Use or disclosure of data contained in this source code file is subject to
 * the "Government Purpose Rights" restriction in the Master Rights File.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * Use or disclosure of data contained in this source code file is subject to
 * the export restrictions set forth in the Master Rights File.
 ******************************************************************************************/
package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.geospatial.CRSCache;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.util.GridGeometryWrapChecker;
import com.raytheon.uf.common.geospatial.util.SubGridGeometryCalculator;
import com.raytheon.uf.common.geospatial.util.WorldWrapChecker;
import com.raytheon.uf.common.geospatial.util.WorldWrapCorrector;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.GridUtil;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.Style;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.grid.rsc.data.GeneralGridData;
import com.raytheon.uf.viz.core.grid.rsc.data.ScalarGridData;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.DataSourceTileImageCreator;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.uf.viz.ncep.grid.FloatGridData;
import com.raytheon.uf.viz.ncep.grid.NcgribLogger;
import com.raytheon.viz.core.contours.util.ContourContainer;
import com.raytheon.viz.core.contours.util.FortConBuf;
import com.raytheon.viz.core.contours.util.FortConConfig;
import com.raytheon.viz.core.contours.util.StreamLineContainer;
import com.raytheon.viz.core.contours.util.StreamLineContainer.StreamLinePoint;
import com.raytheon.viz.core.contours.util.StrmPak;
import com.raytheon.viz.core.contours.util.StrmPakConfig;
import com.raytheon.viz.core.rsc.jts.JTSCompiler;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;
import gov.noaa.nws.ncep.gempak.parameters.colorbar.CLRBAR;
import gov.noaa.nws.ncep.gempak.parameters.colorbar.ColorBarAttributesBuilder;
import gov.noaa.nws.ncep.gempak.parameters.core.contourinterval.CINT;
import gov.noaa.nws.ncep.gempak.parameters.infill.FINT;
import gov.noaa.nws.ncep.gempak.parameters.infill.FLine;
import gov.noaa.nws.ncep.gempak.parameters.intext.TextStringParser;
import gov.noaa.nws.ncep.gempak.parameters.line.LineDataStringParser;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResource;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResource.NcGridDataProxy;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import si.uom.SI;
import tec.uom.se.AbstractUnit;

/**
 * ContourSupport
 *
 * Provides contouring wrapper
 *
 * <pre>
 *
 *    SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer        Description
 * ------------- -------- --------------- --------------------------------------
 * Oct 22, 2007           chammack        Initial Creation.
 * May 26, 2009  2172     chammack        Use zoomLevel to calculate label
 *                                        spacing
 * Mar 10, 2010  164      M. Li           Control increments on zoom
 * May 18, 2011           M. Li           Add contour label frequency capability
 * May 26, 2011           M. Li           Add a new method createContourLabel
 * Aug 18, 2011           M. li           fixed reproject problems for
 *                                        streamline
 * Nov 08, 2011           X. Guo          Checked centeral_meridian and added
 *                                        vertices twice after subtract 360
 * Feb 15, 2012           X. Guo          Used cached contour information to
 *                                        re-create wired frame
 * Mar 01, 2012           X. Guo          Handle five zoom levels
 * Mar 13, 2012           X. Guo          Handle multi-threads
 * Mar 15, 2012           X. Guo          Refactor
 * Mar 27, 2012           X. Guo          Used contour lock instead of
 *                                        "synchronized"
 * May 23, 2012           X. Guo          Loaded ncgrib logger
 * Apr 26, 2013           B. Yin          Fixed the world wrap problem for
 *                                        centeral line 0/180.
 * Jun 06, 2013           B. Yin          fixed the half-degree grid porblem.
 * Jul 19, 2013           B. Hebbard      Merge in RTS change of
 *                                        Util-->ArraysUtil
 * Aug 19, 2013  743      S. Gurung       Added clrbar and corresponding
 *                                        getter/setter method (from Archana's
 *                                        branch) and fix for editing clrbar
 *                                        related attribute changess not being
 *                                        applied from right click legend.
 * Sep 17, 2013  1036     S. Gurung       Added TEXT attribute related changes
 *                                        to create labels with various
 *                                        parameters
 * Oct 30, 2013  1045     S. Gurung       Fix for FINT/FLINE parsing issues
 * Aug 27, 2013  2262     bsteffen        Convert to use new StrmPak.
 * Apr 23, 2014  856      pswamy          Missing color fill in grid
 *                                        diagnostics.
 * Apr 30, 2014  862      pswamy          Grid Precipitable Water Contour Labels
 *                                        needs two decimal points
 * Jun 26, 2014           sgilbert        Change world wrap processing.
 * May 08, 2015  7296     J. Wu           use JTSComplier for clipping against
 *                                        view area.
 * Jul 17, 2015  6916     B. Yin/rkean    Changes for Contour fill images
 * Nov 05, 2015  13016    bsteffen/rkean  - handle non-linear FINTs
 * Mar 08, 2016  16221    jhuber          make Fill color "0" transparent
 * Apr 21, 2016  17741    sgilbert        Changes to reduce memory usage
 * May 06, 2016  17323    kbugenhagen     In createColorFills, only subgrid if
 *                                        ncgrid proxy is not null.
 * Jul 21, 2016  20574    njensen         Switch contour algorithm from
 *                                        CNFNative to FortConBuf
 * Oct 31, 2016  16586    RCReynolds      Fix grid color fill when number of
 *                                        colors does not match number of
 *                                        increments In general fix/verify that
 *                                        map/scalebar are correct when number
 *                                        of colors are less than, equal too or
 *                                        greater than number of increments
 * Jan 17, 2017  19643    Edwin Brown     Moved the check of MAX_CONTOUR_LEVELS
 *                                        from CINT.java to here because it
 *                                        should be limiting the number of
 *                                        rendered contours, not the number or
 *                                        intervals between the min and the max
 * Mar 27, 2017  19634    bsteffen        Support subgrids.
 * Oct 25, 2018  54483    mapeters        Handle {@link NcgribLogger} refactor
 * Nov 13, 2018  54500    mrichardson     Added SubGridGeometryCalculator to
 *                                        createStreamLines to fix projection
 *                                        exception and prevent rendering
 *                                        unnecessary projection data
 * Nov 13, 2018  54502    mrichardson     Add additional error handling to
 *                                        contouring code in NCP
 * Jan 31, 2019  7726     mrichardson     Downgrade out of bounds coordinate
 *                                        message from warn to info; minor code
 *                                        clean-up
 * Feb 01, 2019  7720     mrichardson     Incorporated changes for subgrids.
 * Aug 29, 2019  67955    tjensen         Update for GeneralGridData refactor
 *
 * </pre>
 *
 */
public class ContourSupport {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ContourSupport.class);

    private final Logger logger = LoggerFactory.getLogger("PerformanceLogger");

    private FloatGridData records;

    private int level;

    private IExtent extent;

    private double currentDensity;

    private IMapDescriptor descriptor;

    private ContourAttributes attr;

    private String cint;

    private String fint;

    private String type;

    private String fline;

    private String name;

    private float zoom;

    private String text;

    // calculated values
    private ContourGroup contourGroup = null;

    private MathTransform rastPosToWorldGrid = null;

    private MathTransform rastPosToLatLon = null;

    private MathTransform rastPosLatLonToWorldGrid = null;

    private MathTransform worldGridToLatlon = null;

    private int zoomLevelIndex;

    private ContourGridData cntrData = null;

    private boolean globalData = false;

    // return value from raytheon's worldWrapChecker
    private boolean worldWrapChecker;

    private WorldWrapCorrector corrector;

    // flag that indicates world wrap is needed
    private boolean worldWrap;

    // central meridian
    private double centralMeridian = 0;

    // screen width of the map
    private double mapScreenWidth;

    // screen x of the zero longitude
    private double zeroLonOnScreen;

    // maximum number of grid along x direction
    private int maxGridX;

    private boolean isCntrsCreated;

    private GeneralGridGeometry imageGridGeometry;

    private final NcgridResource resource;

    private IGraphicsTarget target;

    private List<Integer> fillColorsIndex = null;

    private static NcgribLogger ncgribLogger = NcgribLogger.getInstance();

    /** Maximum number of contour levels */
    private static final int MAX_CONTOUR_LEVELS = 100;

    private static final int IMAGE_TILE_SIZE = 2048;

    /**
     * Constructor
     *
     * @param records
     * @param level
     * @param extent
     * @param currentDensity
     * @param worldGridToCRSTransform
     * @param imageGridGeometry
     * @param mapGridGeometry
     * @param target
     * @param descriptor
     * @param attr
     * @param name
     * @param zoom
     * @param contourGp
     */
    public ContourSupport(NcgridResource res, FloatGridData records, int level,
            IExtent extent, double currentDensity,
            MathTransform worldGridToCRSTransform,
            GeneralGridGeometry imageGridGeometry,
            GeneralGridGeometry mapGridGeometry, IGraphicsTarget target,
            IMapDescriptor descriptor, ContourAttributes attr, String name,
            float zoom, ContourGroup contourGp) {

        this.resource = res;
        initContourSupport(records, level, extent, currentDensity,
                worldGridToCRSTransform, imageGridGeometry, mapGridGeometry,
                target, descriptor, attr, name, zoom, contourGp);
    }

    /**
     * Data structure for contour, fill, and streamline renderables
     */
    public static class ContourGroup {
        public int zoomLevel;

        public IWireframeShape streamlines;

        public IWireframeShape contours;

        public PixelExtent lastUsedPixelExtent;

        public double lastDensity;

        public List<Double> cvalues;

        public List<Double> fvalues;

        public Map<String, Geometry> latlonContours;

        public CLRBAR clrbar;

        public ColorBar colorBarForGriddedFill;

        public List<DrawableString> labels;

        public ContourLabelParameters labelParms;

        public boolean colorImage = false;

        public TileSetRenderable colorFillImage = null;

        public void dispose() {
            if (this.streamlines != null) {
                this.streamlines.dispose();
            }
            if (this.contours != null) {
                this.contours.dispose();
            }
            if (this.cvalues != null) {
                this.cvalues.clear();
            }
            if (this.fvalues != null) {
                this.fvalues.clear();
            }
            if (this.latlonContours != null) {
                this.latlonContours.clear();
            }
            if (this.colorBarForGriddedFill != null) {
                this.colorBarForGriddedFill.dispose();
            }
            if (this.labels != null) {
                this.labels.clear();
            }
            if (this.labelParms != null && this.labelParms.font != null) {
                this.labelParms.font.dispose();
            }
            if (this.colorFillImage != null) {
                this.colorFillImage.dispose();
            }
        }
    }

    private class ContourGridData implements DataSource {
        private float minValue;

        private float maxValue;

        private final FloatGridData dataRecord;

        private final int szX;

        private final int szY;

        public ContourGridData(FloatGridData record) {
            maxValue = Float.MIN_VALUE;
            minValue = Float.MAX_VALUE;
            long[] sz = record.getSizes();

            dataRecord = record;
            FloatBuffer data1D = dataRecord.getXdata();

            szX = (int) sz[0];
            szY = (int) sz[1];

            for (int j = 0; j < szY; j++) {
                for (int i = 0; i < szX; i++) {
                    if (data1D.get(
                            (szX * j) + i) != IDecoderConstantsN.GRID_MISSING) {
                        maxValue = Math.max(maxValue,
                                data1D.get((szX * j) + i));
                        minValue = Math.min(minValue,
                                data1D.get((szX * j) + i));
                    }
                }
            }
        }

        public float getMinValue() {
            return minValue;
        }

        public float getMaxValue() {
            return maxValue;
        }

        public float[] getData() {
            return dataRecord.getXdataAsArray();
        }

        public int getX() {
            return szX;
        }

        public int getY() {
            return szY;
        }

        @Override
        public double getDataValue(int x, int y) {
            FloatBuffer data1D = dataRecord.getXdata();
            if (x < szX && y < szY) {
                if (data1D.get(y * szX + x) == -999999.f) {
                    return Double.NaN;
                }
                return data1D.get(y * szX + x);
            } else {
                return Double.NaN;
            }
        }
    }

    public class ContourLabelParameters {
        public IFont font;

        public RGB color;

        public HorizontalAlignment justification;

        public double rotation;

        public TextStyle textStyle;

        public RGB boxColor;

        public ContourLabelParameters(IGraphicsTarget target) {

            font = target.getDefaultFont();

            font = target.initializeFont(font.getFontName(),
                    (float) (font.getFontSize() / 1.4), null);

            LineDataStringParser lineAttr = new LineDataStringParser(
                    attr.getLine());
            color = GempakColor.convertToRGB(lineAttr.getInstanceOfLineBuilder()
                    .getLineColorsList().get(0));

            justification = HorizontalAlignment.CENTER;
            rotation = 0.0;
            boxColor = null;

            if (text != null && !text.isEmpty()) {
                TextStringParser textAttr = new TextStringParser(text);

                /* Set text style */
                int fstyle = textAttr.getTextStyle();
                Style[] styles = null;
                switch (fstyle) {
                case 1:
                    styles = new Style[] { Style.ITALIC };
                    break;
                case 2:
                    styles = new Style[] { Style.BOLD };
                    break;
                case 3:
                    styles = new Style[] { Style.BOLD, Style.ITALIC };
                    break;
                }

                font = target.initializeFont(
                        getFontName(textAttr.getTextFont()),
                        textAttr.getTextSize(), styles);

                /* Set text rotation */
                if (textAttr.getTextRotation() == 'N') {
                    // North relative, to be processed later when
                    // location coordinate is available
                    rotation = -1;
                } else {
                    // Screen relative
                    rotation = 0.0;
                }

                /* Set text horizontal alignment */
                if (textAttr.getTextJustification() == 'L') {
                    justification = HorizontalAlignment.LEFT;
                } else if (textAttr.getTextJustification() == 'R') {
                    justification = HorizontalAlignment.RIGHT;
                } else {
                    justification = HorizontalAlignment.CENTER;
                }

                String border = textAttr.getTextBorder() + "";

                /* set border and type */
                if (border.startsWith("12")) {
                    textStyle = TextStyle.BLANKED;
                    boxColor = new RGB(0, 0, 0);
                }

                if (border.startsWith("2") && border.endsWith("1")) {

                    textStyle = TextStyle.BOXED;
                    // set blank fill
                    if (border.length() == 3) {
                        if (border.charAt(1) == '1') {
                            boxColor = null;
                        }
                        if (border.charAt(1) == '2') {
                            boxColor = null;
                        }
                        if (border.charAt(1) == '3') {
                            boxColor = color;
                            color = new RGB(0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    private void initContourSupport(FloatGridData records, int level,
            IExtent extent, double currentDensity,
            MathTransform worldGridToCRSTransform,
            GeneralGridGeometry imageGridGeometry,
            GeneralGridGeometry mapGridGeometry, IGraphicsTarget target,
            IMapDescriptor descriptor, ContourAttributes attr, String name,
            float zoom, ContourGroup contourGp) {
        isCntrsCreated = true;
        if (records == null || attr == null) {
            isCntrsCreated = false;
            return;
        }
        if (!initMathTransform(imageGridGeometry, mapGridGeometry)) {
            isCntrsCreated = false;
            return;
        }
        this.records = records;
        this.level = level;
        this.extent = extent;
        this.currentDensity = currentDensity;
        this.descriptor = descriptor;
        this.attr = attr;
        this.cint = attr.getCint();
        this.type = attr.getType();
        this.fint = attr.getFint();
        this.fline = attr.getFline();
        this.text = attr.getText();
        this.name = name;
        this.zoom = zoom;
        this.imageGridGeometry = imageGridGeometry;
        this.target = target;
        this.cntrData = new ContourGridData(records);
        this.centralMeridian = getCentralMeridian(descriptor);
        if (centralMeridian == -180) {
            centralMeridian = 180;
        }
        this.worldWrapChecker = new WorldWrapChecker(
                descriptor.getGridGeometry().getEnvelope()).needsChecking();
        this.corrector = new WorldWrapCorrector(descriptor.getGridGeometry());
        this.worldWrap = needWrap(imageGridGeometry, rastPosToLatLon);
        mapScreenWidth = this.getMapWidth();
        maxGridX = this.getMaxGridX(imageGridGeometry);
        initContourGroup(target, contourGp);
    }

    /**
     * Create contours and fill renderables from provided parameters
     */
    public void createContours() {

        long t0 = System.currentTimeMillis();
        /*
         * Contours and/or color fills
         */
        if (!records.isVector()) {

            long t1 = System.currentTimeMillis();
            logger.debug("Preparing " + name + " grid data took: " + (t1 - t0));

            /*
             * ZoomLevel.
             */
            initZoomIndex();

            long t1a = System.currentTimeMillis();
            logger.debug("new ContourGenerator took: " + (t1a - t1));

            /*
             * Get contour values from CINT
             */
            List<Double> cvalues = calcCintValue();

            /*
             * Generate contours and create contour wireframes
             */
            if (cvalues != null && !cvalues.isEmpty()) {
                /*
                 * Regenerate contours if new contour intervals requested
                 */
                if (!contourGroup.cvalues.equals(cvalues)) {
                    contourGroup.latlonContours.clear();
                    /*
                     * This feeds all of the geometries/contours (with
                     * associated key cvalue) into hash map latlonContours
                     */
                    genContour(cvalues);

                    if (!isCntrsCreated) {
                        return;
                    }
                    contourGroup.cvalues.clear();
                    contourGroup.cvalues.addAll(cvalues);
                }

                createContourLines();
            }

            /*
             * Create color fills if requested
             */
            List<Double> fvalues = calcFintValue();
            if (fvalues != null && !fvalues.isEmpty()) {
                contourGroup.fvalues.clear();
                contourGroup.fvalues.addAll(fvalues);
                createColorFills();
            }

            long t10 = System.currentTimeMillis();
            logger.debug("===Total time for (" + name + ") " + " took: "
                    + (t10 - t0) + "\n");
        } else {
            /*
             * create Streamlines
             */
            createStreamLines();
        }
    }

    private void createContourLabel(IExtent extent, ContourGroup contourGroup,
            float contourValue, Coordinate[] llcoords,
            IMapDescriptor descriptor) {

        double minx = extent.getMinX();
        double miny = extent.getMinY();
        double maxx = extent.getMaxX();
        double maxy = extent.getMaxY();

        double[][] visiblePts = new double[llcoords.length][2];
        int actualLength = 0;

        double[] in = new double[2];
        double[] out = new double[2];
        for (Coordinate coord : llcoords) {
            in[0] = coord.x;
            in[1] = coord.y;
            try {
                rastPosLatLonToWorldGrid.transform(in, 0, out, 0, 1);
            } catch (TransformException e) {
                statusHandler.info("The coordinate [" + in[0] + ", " + in[1]
                        + "] could not be transformed. Continuing with remaining coordinates.");
                continue;
            }
            if (out[0] > minx && out[0] < maxx && out[1] > miny
                    && out[1] < maxy) {
                visiblePts[actualLength][0] = out[0];
                visiblePts[actualLength][1] = out[1];
                actualLength++;
            }
        }

        DecimalFormat df = new DecimalFormat("0.##");
        double[] loc = { 0.0, 0.0 };

        if (actualLength > 0) {
            loc[0] = visiblePts[actualLength / 2][0];
            loc[1] = visiblePts[actualLength / 2][1];

            DrawableString string = new DrawableString(df.format(contourValue),
                    contourGroup.labelParms.color);
            string.setCoordinates(loc[0], loc[1]);
            string.font = contourGroup.labelParms.font;
            string.horizontalAlignment = contourGroup.labelParms.justification;
            string.verticallAlignment = VerticalAlignment.MIDDLE;

            if (contourGroup.labelParms.textStyle != null) {
                string.addTextStyle(contourGroup.labelParms.textStyle,
                        contourGroup.labelParms.boxColor);
            }

            if (contourGroup.labelParms.rotation == -1) {
                // North relative rotation
                string.rotation = northOffsetAngle(
                        new Coordinate(loc[0], loc[1]), descriptor);
            }

            contourGroup.labels.add(string);
        }

    }

    private double[][] toScreenRightOfZero(Coordinate[] coords,
            MathTransform xform, int minX, int minY) {

        double[][] out = new double[coords.length][3];

        for (int i = 0; i < coords.length; i++) {
            double[] tmp = new double[2];
            tmp[0] = coords[i].x + minX;
            tmp[1] = coords[i].y + minY;

            try {
                xform.transform(tmp, 0, out[i], 0, 1);
            } catch (TransformException e) {
                statusHandler.error("Error transforming coordinates", e);
                return null;
            }

            if (out[i][0] < zeroLonOnScreen
                    || (tmp[0] == maxGridX && out[i][0] == zeroLonOnScreen)) {
                out[i][0] += mapScreenWidth;

            }
        }

        if (out.length > 0) {
            return out;
        } else {
            return null;
        }
    }

    private double[][] toScreenLeftOfZero(Coordinate[] coords,
            MathTransform xform, int minX, int minY) {

        double[][] out = new double[coords.length][3];

        for (int i = 0; i < coords.length; i++) {
            double[] tmp = new double[2];
            tmp[0] = coords[i].x + minX;
            tmp[1] = coords[i].y + minY;

            try {
                xform.transform(tmp, 0, out[i], 0, 1);
            } catch (TransformException e) {
                statusHandler.error("Error transforming coordinates", e);
                return null;
            }

            if (out[i][0] > zeroLonOnScreen
                    || (tmp[0] == 0 && out[i][0] == zeroLonOnScreen)) {

                out[i][0] -= mapScreenWidth;
            }
        }

        if (out.length > 0) {
            return out;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static Geometry polyToLine(Polygon poly) {
        GeometryFactory gf = new GeometryFactory();

        if (poly.getNumInteriorRing() == 0) {
            return poly;
        }

        poly.normalize();
        LineString outerPoly = poly.getExteriorRing();

        /*
         * sort interior rings
         */
        TreeMap<Coordinate, LineString> orderedHoles = new TreeMap<>();
        for (int i = 0; i < poly.getNumInteriorRing(); i++) {
            LineString hole = poly.getInteriorRingN(i);

            Coordinate min = CoordinateArrays
                    .minCoordinate(hole.getCoordinates());
            orderedHoles.put(min, hole);
        }

        for (Coordinate leftmost : orderedHoles.keySet()) {
            CoordinateList clist = new CoordinateList();
            LineString hole = orderedHoles.get(leftmost);

            Coordinate testCoord = new Coordinate(0, leftmost.y);
            LineSegment testSegment = new LineSegment(leftmost, testCoord);

            Coordinate max = findSegments(outerPoly, leftmost.y, testSegment);
            Coordinate[] connector = new Coordinate[] { max, leftmost };

            LocationIndexedLine outerLil = new LocationIndexedLine(outerPoly);
            LinearLocation outerLoc = outerLil.indexOf(connector[0]);
            LocationIndexedLine innerLil = new LocationIndexedLine(hole);
            LinearLocation innerLoc = innerLil.indexOf(connector[1]);

            clist.add(outerLil.extractLine(outerLil.getStartIndex(), outerLoc)
                    .getCoordinates(), true);

            clist.add(innerLil.extractLine(innerLoc, innerLil.getEndIndex())
                    .getCoordinates(), true);
            clist.add(innerLil.extractLine(innerLil.getStartIndex(), innerLoc)
                    .getCoordinates(), true);

            clist.add(outerLil.extractLine(outerLoc, outerLil.getEndIndex())
                    .getCoordinates(), true);

            outerPoly = gf.createLineString(clist.toCoordinateArray());

        }

        return outerPoly;
    }

    private static Coordinate findSegments(LineString outerPoly, double y,
            LineSegment seg) {

        Coordinate max = new Coordinate(0, 0);

        Coordinate[] coords = outerPoly.getCoordinates();
        for (int i = 0; i < coords.length - 1; i++) {
            Coordinate intx = null;
            if (((y <= coords[i].y) && (y >= coords[i + 1].y))
                    || ((y >= coords[i].y) && (y <= coords[i + 1].y))) {
                LineSegment temp = new LineSegment(coords[i], coords[i + 1]);
                intx = seg.intersection(temp);
            }

            if (intx != null) {
                if (max.compareTo(intx) == -1) {
                    max = intx;
                }
            }
        }

        return max;
    }

    public static double getCentralMeridian(IMapDescriptor descriptor) {
        MapProjection worldProjection = CRS
                .getMapProjection(descriptor.getCRS());
        if (worldProjection != null) {
            ParameterValueGroup group = worldProjection.getParameterValues();
            double centralMeridian = group.parameter(
                    AbstractProvider.CENTRAL_MERIDIAN.getName().getCode())
                    .doubleValue();
            if (centralMeridian > 180) {
                centralMeridian -= 360;
            }
            return centralMeridian;
        }
        return -999;
    }

    private void initContourGroup(IGraphicsTarget target,
            ContourGroup contourGp) {
        contourGroup = new ContourGroup();
        contourGroup.lastDensity = currentDensity;

        contourGroup.zoomLevel = level;

        contourGroup.cvalues = new ArrayList<>();

        contourGroup.fvalues = new ArrayList<>();

        contourGroup.latlonContours = new HashMap<>();

        if (contourGp != null) {
            if (contourGp.cvalues != null && !contourGp.cvalues.isEmpty()) {
                contourGroup.cvalues.addAll(contourGp.cvalues);
            }
            if (contourGp.latlonContours != null
                    && contourGp.latlonContours.size() > 0) {
                contourGroup.latlonContours.putAll(contourGp.latlonContours);
            }
        }

        contourGroup.lastUsedPixelExtent = (PixelExtent) extent.clone();
        contourGroup.lastUsedPixelExtent.getEnvelope().expandBy(
                contourGroup.lastUsedPixelExtent.getWidth() * .25,
                contourGroup.lastUsedPixelExtent.getHeight() * .25);

        contourGroup.labels = new ArrayList<>();
        contourGroup.labelParms = new ContourLabelParameters(target);
    }

    private boolean initMathTransform(GeneralGridGeometry imageGridGeometry,
            GeneralGridGeometry mapGridGeometry) {
        try {
            DefaultMathTransformFactory factory = new DefaultMathTransformFactory();

            CoordinateReferenceSystem rastCrs = imageGridGeometry
                    .getCoordinateReferenceSystem();
            CoordinateReferenceSystem mapCrs = mapGridGeometry
                    .getCoordinateReferenceSystem();

            MathTransform rastGridToCrs = imageGridGeometry
                    .getGridToCRS(PixelInCell.CELL_CENTER);
            MathTransform mapCrsToGrid = mapGridGeometry
                    .getGridToCRS(PixelInCell.CELL_CORNER).inverse();

            MathTransform rastCrsToLatLon = MapUtil
                    .getTransformToLatLon(rastCrs);

            MathTransform rastCrsToWorldGrid = MapUtil
                    .getTransformFromLatLon(mapCrs);
            MathTransform crs2crs = CRSCache.getInstance()
                    .findMathTransform(rastCrs, mapCrs);

            rastPosToWorldGrid = factory.createConcatenatedTransform(
                    factory.createConcatenatedTransform(rastGridToCrs, crs2crs),
                    mapCrsToGrid);

            rastPosToLatLon = factory.createConcatenatedTransform(rastGridToCrs,
                    rastCrsToLatLon);
            rastPosLatLonToWorldGrid = factory.createConcatenatedTransform(
                    rastCrsToWorldGrid, mapCrsToGrid);
            worldGridToLatlon = factory.createConcatenatedTransform(
                    rastPosToWorldGrid.inverse(), rastPosToLatLon);
        } catch (Exception e) {
            statusHandler.error("Error building Transforms:", e);
            return false;
        }
        return true;
    }

    private void initZoomIndex() {
        zoomLevelIndex = level + 1;
        if (zoomLevelIndex < 1) {
            zoomLevelIndex = 1;
        }
        int maxZoomLevel = 5;
        String cint = attr.getCint();
        if (cint != null) {
            maxZoomLevel = cint.trim().split(">").length;
        }
        if (zoomLevelIndex > maxZoomLevel) {
            zoomLevelIndex = maxZoomLevel;
        }
    }

    private List<Double> calcCintValue() {
        List<Double> cvalues = null;
        if (type.trim().toUpperCase().contains("C")) {
            cvalues = CINT.parseCINT(cint, zoomLevelIndex,
                    cntrData.getMinValue(), cntrData.getMaxValue());
        }
        return cvalues;
    }

    private List<Double> calcFintValue() {

        List<Double> fvalues = null;
        if (type.trim().toUpperCase().contains("F")
                || type.trim().toUpperCase().contains("I")) {
            fvalues = FINT.parseFINT(fint, zoomLevelIndex,
                    cntrData.getMinValue(), cntrData.getMaxValue());

        }
        return fvalues;
    }

    private void createContourLines() {

        contourGroup.contours = target.createWireframeShape(false, descriptor);

        long total_labeling_time = 0;
        long t2 = System.currentTimeMillis();
        if (type.trim().toUpperCase().contains("C")
                && !contourGroup.cvalues.isEmpty()) {
            int labelFreq = 1;
            String[] tempLineStrs = attr.getLine().split("/");
            List<Integer> labelValues = null;
            if (tempLineStrs.length >= 4) {
                if (tempLineStrs[3].trim().contains(";")) {
                    LineDataStringParser lineAttr = new LineDataStringParser(
                            attr.getLine());
                    labelValues = lineAttr.getInstanceOfLineBuilder()
                            .getLineLabelPresentList();
                } else {
                    labelFreq = Math
                            .abs(Integer.parseInt(tempLineStrs[3].trim()));
                }
            }

            JTSCompiler jtsCompiler = new JTSCompiler(null,
                    contourGroup.contours, descriptor);

            int n = 0;

            long elapsedTime = -System.nanoTime();

            int contourCount = 0;

            for (Double cval : contourGroup.cvalues) {
                float fval = (float) (cval * 1.0f);
                boolean toLabel = false;

                // Label frequency
                if (labelValues != null) {
                    for (Integer value : labelValues) {
                        if (value == Math.rint(fval)) {
                            toLabel = true;
                            break;
                        }
                    }
                } else {
                    if (labelFreq == 0) {
                        toLabel = false;
                    } else {
                        toLabel = (n % labelFreq == 0) ? true : false;
                    }
                }

                Geometry g = contourGroup.latlonContours.get(cval.toString());
                if (g == null) {
                    continue;
                }

                contourCount++;
                // If we've reached over the max allowed rendered contours for
                // this pass then break out of for loop
                if (contourCount > MAX_CONTOUR_LEVELS) {
                    break;
                }

                // force packed w/ no SoftReference
                Geometry gcopy = (Geometry) g.clone();
                Geometry correctedGeom = corrector.correct(gcopy);

                for (int i = 0; i < correctedGeom.getNumGeometries(); i++) {
                    Geometry gn = correctedGeom.getGeometryN(i);

                    /*
                     * clip against view area to remove pole points and extra
                     * lines outside of view area. The clipping is moved to
                     * JTSCompiler.
                     */
                    try {
                        if (gn instanceof Polygon) {
                            jtsCompiler
                                    .handle(((Polygon) gn).getExteriorRing());
                            for (int ii = 0; ii < ((Polygon) gn)
                                    .getNumInteriorRing(); ii++) {
                                jtsCompiler.handle(
                                        ((Polygon) gn).getInteriorRingN(ii));
                            }
                        } else {
                            jtsCompiler.handle(gn);
                        }
                    } catch (VizException e) {
                        statusHandler.error("JTS Compiler error while creating "
                                + "contour lines in ContourSupport", e);
                    }

                    if (toLabel) {
                        long tl0 = System.currentTimeMillis();
                        createContourLabel(extent, contourGroup, fval,
                                gn.getCoordinates(), descriptor);

                        long tl1 = System.currentTimeMillis();
                        total_labeling_time += (tl1 - tl0);
                    }
                }
                n++;
            }
            elapsedTime += System.nanoTime();
            logger.debug("Total contour time: "
                    + TimeUnit.NANOSECONDS.toMillis(elapsedTime));
        }
        contourGroup.contours.compile();
        long t3 = System.currentTimeMillis();
        logger.debug("===Creating label wireframes for (" + name + ") took: "
                + total_labeling_time);
        if (ncgribLogger.isEnableContourLogs()) {
            logger.debug("===Creating contour line wireframes for (" + name
                    + ")took: " + (t3 - t2));
        }
    }

    // get MapProjection name for the given CoordinateReferenceSystem
    String getProjectionName(
            CoordinateReferenceSystem coordinateReferenceSystem) {
        String projectionName = "";
        MapProjection mapProjection = CRS
                .getMapProjection(coordinateReferenceSystem);

        if (mapProjection != null) {
            projectionName = mapProjection.getName();
        }
        return projectionName;
    }

    private void createColorFills() {
        long t3 = System.currentTimeMillis();
        NcGridDataProxy ncGridDataProxy = null;

        if (type.toUpperCase().contains("F")
                || type.toUpperCase().contains("I")) {

            int worldWidth = GridGeometryWrapChecker
                    .checkForWrapping(imageGridGeometry);
            int geomWidth = imageGridGeometry.getGridRange().getSpan(0);
            /*
             * Grids larger than the world need to be reduced to render
             * correctly.
             */
            if (worldWidth > 0 && worldWidth < geomWidth) {
                GridEnvelope gridRange = imageGridGeometry.getGridRange();
                int[] low = gridRange.getLow().getCoordinateValues();
                int[] high = gridRange.getHigh().getCoordinateValues();

                high[0] = low[0] + worldWidth;

                gridRange = new GeneralGridEnvelope(low, high);

                imageGridGeometry = new GridGeometry2D(gridRange,
                        imageGridGeometry.getGridToCRS(),
                        imageGridGeometry.getCoordinateReferenceSystem());

                // Equidistant_Cylindrical image requires special handling
                // (subgridding)
                if ("Equidistant_Cylindrical"
                        .equals(getProjectionName(imageGridGeometry
                                .getCoordinateReferenceSystem()))
                        && !"MCIDAS_AREA_NAV".equals(
                                getProjectionName(descriptor.getCRS()))) {

                    /*
                     * Geotools does not "roll the longitude" for map
                     * projections with a central meridian of 0. See
                     * MapProjection.transform() for an explanation.
                     *
                     * Grids larger than the world always have a central
                     * meridian of 0 because rolling the edges would cause the
                     * grid to collapse down to the width of a single grid cell.
                     *
                     * For the display we need to ensure that the longitude is
                     * rolled so that the image is centered on the display
                     * instead of rendering off the edge of the display.
                     */
                    try {
                        ReferencedEnvelope envelope = new ReferencedEnvelope(
                                imageGridGeometry.getEnvelope());
                        /*
                         * The envelope will span the entire valid width of the
                         * new CRS. Due to floating point inaccuracies the
                         * conversion to a new CRS can cause both corners to get
                         * rolled onto the same side. Reducing the envelope
                         * slightly accounts for the floating point inaccuracies
                         * with no noticeable impact on the display.
                         */
                        envelope.expandBy(-0.0001, 0);
                        envelope = envelope
                                .transform(DefaultGeographicCRS.WGS84, true);
                        double center = envelope.getMedian(0);

                        /*
                         * Copy all the parameters from the existing projection
                         * except the central meridian
                         */
                        MapProjection worldProjection = CRS
                                .getMapProjection(imageGridGeometry
                                        .getCoordinateReferenceSystem());
                        ParameterValueGroup group = worldProjection
                                .getParameterValues();
                        group.parameter(AbstractProvider.CENTRAL_MERIDIAN
                                .getName().getCode()).setValue(center);
                        String name = "ContourSupportGenerated: CM=" + center;
                        DefaultProjectedCRS projCrs = MapUtil
                                .constructProjection(name, group);

                        envelope = envelope.transform(projCrs, true);
                        imageGridGeometry = new GridGeometry2D(gridRange,
                                envelope);
                    } catch (FactoryException | TransformException e) {
                        statusHandler.error("Failed to project grid properly",
                                e);
                    }

                    /*
                     * The imageGridGeometry passed into this method
                     * (newSpatialObject, which is 360 degrees) causes problems
                     * in display when it gets subgridded. Ignore the
                     * subgridding if the proxy is null. If the proxy is not
                     * null, the imageGridGeometry associated with it
                     * (spatialObject, which is 359 degrees) is correct and so
                     * you can do the subgridding.
                     */
                    try {

                        // create subGrid (same as D2D)
                        SubGridGeometryCalculator subGridGeometry = new SubGridGeometryCalculator(
                                descriptor.getGridGeometry().getEnvelope(),
                                imageGridGeometry);
                        if (!subGridGeometry.isEmpty()) {
                            imageGridGeometry = subGridGeometry
                                    .getSubGridGeometry2D();
                        }
                    } catch (Exception ex) {
                        statusHandler.error("Error Creating subGrid: ", ex);
                    }
                }
            }

            contourGroup.colorImage = true;

            GeneralGridData ggd = ScalarGridData
                    .createScalarData(imageGridGeometry, cntrData, SI.METRE);

            contourGroup.colorFillImage = createRenderableImage(target, ggd);
        }

        long t4 = System.currentTimeMillis();
        if (ncgribLogger.isEnableContourLogs()) {
            logger.debug("===Creating color fills for (" + name + ") took : "
                    + (t4 - t3));
        }
    }

    private TileSetRenderable createRenderableImage(IGraphicsTarget target,
            GeneralGridData data) {

        ColorMapCapability colorMapCap = resource
                .getCapability(ColorMapCapability.class);
        ImagingCapability imagingCap = resource
                .getCapability(ImagingCapability.class);
        imagingCap.setInterpolationState(true);

        ColorMapParameters params = null;

        params = createColorMapParameters();

        if (params.getColorMap() == null) {
            if (params.getColorMapName() == null) {
                params.setColorMapName("Grid/gridded data yin");
            }

            if (fline == null || fline.trim().length() < 1) {
                for (int i = 0; i < contourGroup.fvalues.size() + 2; i++) {
                    if (i <= 30) {
                        fillColorsIndex.add(i + 1);
                    } else {
                        fillColorsIndex.add(30);
                    }
                }
            } else {
                FLine flineInfo = new FLine(fline.trim());
                fillColorsIndex = flineInfo.getFillColorList();
                /*
                 * Repeat colors if not enough input color(s) provided.
                 */
                if (contourGroup.fvalues != null && fillColorsIndex
                        .size() < (contourGroup.fvalues.size() + 1)) {
                    fillColorsIndex = handleNotEnoughFillColors(
                            contourGroup.fvalues.size(), fillColorsIndex);
                }
                if (contourGroup.fvalues != null && fillColorsIndex
                        .size() > (contourGroup.fvalues.size() + 1)) {
                    fillColorsIndex = handleTooManyFillColors(
                            contourGroup.fvalues.size(), fillColorsIndex);
                }
            }
            ColorMap cm = new ColorMap(fillColorsIndex.size());

            for (int ii = 0; ii < fillColorsIndex.size(); ii++) {

                float alpha = .5f;
                Integer colorInt = fillColorsIndex.get(ii);

                RGB color = GempakColor.convertToRGB(colorInt);
                if (colorInt == 0) {
                    alpha = 0;
                }
                Color clr = new Color((float) (color.red / 255.),
                        (float) (color.green / 255.),
                        (float) (color.blue / 255.), alpha);
                cm.setColor(ii, clr);
            }
            params.setColorMap(cm);
        }
        colorMapCap.setColorMapParameters(params);

        if (params.getDataMapping() != null) {
            data.convert(params.getColorMapUnit());
        }

        TileImageCreator creator = new DataSourceTileImageCreator(
                data.getData(), data.getDataUnit(), ColorMapDataType.FLOAT,
                colorMapCap);

        TileSetRenderable renderable = new TileSetRenderable(imagingCap,
                (GridGeometry2D) imageGridGeometry, creator, 1,
                IMAGE_TILE_SIZE);
        renderable.project(descriptor.getGridGeometry());

        createColorBar();

        return renderable;
    }

    private void createColorBar() {
        if (attr.getClrbar() != null || !"0".equals(attr.getClrbar())) {
            ColorBar tempColorBar = generateColorBarInfo(contourGroup.fvalues,
                    fillColorsIndex);
            if (tempColorBar != null) {
                contourGroup.colorBarForGriddedFill = new ColorBar(
                        tempColorBar);
            }
        } else {
            contourGroup.colorBarForGriddedFill = null;
        }
    }

    /*-
     *  Create colormap for any resource. Modified to include
     *     non-linear FINTs (unequal fill intervals ie. precip)
     */
    private ColorMapParameters createColorMapParameters() {

        ColorMapParameters params = new ColorMapParameters();

        double[] pixels = new double[contourGroup.fvalues.size() + 2];
        double[] displays = new double[pixels.length];
        double first = cntrData.getMinValue();
        double last = cntrData.getMaxValue();
        double interval = last - first;

        // check fill intervals for bounds
        if (!contourGroup.fvalues.isEmpty()) {
            first = Math.min(first, contourGroup.fvalues.get(0));
            last = Math.max(last,
                    contourGroup.fvalues.get(contourGroup.fvalues.size() - 1));
            interval = (last - first) / contourGroup.fvalues.size();
        }

        // set arbitrary large values beyond bounds (x10) of colormap.
        displays[0] = first - (interval * 10.0);
        displays[displays.length - 1] = last + (interval * 10.0);
        pixels[pixels.length - 1] = pixels.length - 1;

        for (int i = 1; i < pixels.length - 1; i += 1) {
            pixels[i] = i;
            displays[i] = contourGroup.fvalues.get(i - 1);
        }

        params.setColorMapUnit(new ContourUnit<>((AbstractUnit<?>)SI.METRE, pixels, displays));
        params.setColorMapMin((float) displays[0]);
        params.setColorMapMax((float) displays[displays.length - 1]);

        return params;
    }

    private void createStreamLines() {
        int[] rangeHigh = {};
        int[] rangeLow = {};
        int x, szX, szY, maxX, maxY, minX, minY = 0;

        // Step 1: Get the actual data
        contourGroup.streamlines = target.createWireframeShape(false,
                descriptor);

        FloatBuffer uW = null;
        FloatBuffer vW = null;

        // Step 2: Determine the subgrid, if any
        if (imageGridGeometry != null) {
            try {
                SubGridGeometryCalculator subGridGeometry = new SubGridGeometryCalculator(
                        descriptor.getGridGeometry().getEnvelope(),
                        imageGridGeometry);
                if (!subGridGeometry.isEmpty()) {
                    imageGridGeometry = subGridGeometry.getSubGridGeometry2D();
                    rangeHigh = subGridGeometry.getGridRangeHigh(false);
                    rangeLow = subGridGeometry.getGridRangeLow(true);
                }
            } catch (Exception ex) {
                statusHandler.error("Error Creating subGrid for streamlines: ",
                        ex);
            }
        }

        if (rangeHigh.length == 2 && rangeLow.length == 2) {
            x = maxX = rangeHigh[0];
            maxY = rangeHigh[1];
            minX = rangeLow[0];
            minY = rangeLow[1];
            szX = (maxX - minX) + 1;
            szY = (maxY - minY) + 1;
        } else {
            // default to the old way so something is at least displayed
            long[] sz = records.getSizes();
            minX = 0;
            minY = 0;
            maxX = (int) sz[0] - 1;
            maxY = (int) sz[1] - 1;
            szX = (maxX - minX) + 1;
            szY = (maxY - minY) + 1;
            x = (int) sz[0];
        }

        uW = records.getXdata();
        vW = records.getYdata();

        if (globalData) {
            // remove column 360
            x--;
            szX--;
            maxX--;
        }

        int totalSz = szX * szY;
        if (totalSz <= 0) {
            isCntrsCreated = false;
            return;
        }

        float[][] adjustedUw = new float[szX][szY];
        float[][] adjustedVw = new float[szX][szY];

        if (globalData) {
            for (int j = 0; j < szY; j++) {
                for (int i = 0; i < szX + 1; i++) {
                    if ((i + minX) == 360) {
                        continue;
                    }
                    adjustedUw[szX - i - 1][j] = uW
                            .get(((x + 1) * (j + minY)) + (i + minX));
                    adjustedVw[szX - i - 1][j] = vW
                            .get(((x + 1) * (j + minY)) + (i + minX));
                }
            }
        } else {
            for (int j = 0; j < szY; j++) {
                for (int i = 0; i < szX; i++) {
                    adjustedUw[szX - i - 1][j] = uW
                            .get((x * (j + minY)) + (i + minX));
                    adjustedVw[szX - i - 1][j] = vW
                            .get((x * (j + minY)) + (i + minX));
                }
            }
        }
        uW = null;
        vW = null;

        // Use ported legacy code to determine contour interval

        double spadiv = 1 * contourGroup.lastDensity * 500 / 25;

        double minSpacing = 1.0 / spadiv;
        double maxSpacing = 3.0 / spadiv;

        float minspc = 0;
        float maxspc = 0;

        if (minSpacing > 1) {
            minspc = (float) Math.sqrt(minSpacing);
        }
        if (minspc < 0.1) {
            minspc = 0.1f;
        }
        if (maxSpacing > 1) {
            maxspc = (float) Math.sqrt(maxSpacing);
        }
        if (maxspc < 0.25) {
            maxspc = 0.25f;
        }

        /*
         * Fix arrow size by M. Li
         */
        float arrowSize = (float) (0.4f / Math.sqrt(zoom));
        if (arrowSize > 0.4) {
            arrowSize = 0.4f;
        }

        StrmPakConfig config = new StrmPakConfig(arrowSize, minspc, maxspc,
                -1000000f, -999998f);
        StreamLineContainer streamLines = StrmPak.strmpak(adjustedUw,
                adjustedVw, szX, szX, szY, config);

        List<double[]> vals = new ArrayList<>();
        List<Coordinate> pts = new ArrayList<>();
        double[][] screen, screenx;

        GeometryFactory gf = new GeometryFactory();

        JTSCompiler jtsCompiler = new JTSCompiler(null,
                contourGroup.streamlines, descriptor);

        try {

            for (List<StreamLinePoint> line : streamLines.streamLines) {
                for (StreamLinePoint point : line) {
                    double[] out = new double[2];

                    try {
                        rastPosToWorldGrid
                                .transform(
                                        new double[] { point.getX() + minX,
                                                point.getY() + minY },
                                        0, out, 0, 1);
                    } catch (TransformException e) {
                        statusHandler.error("Error trying to transform point: "
                                + "[" + point.getX() + minX + ", "
                                + point.getY() + minY + "]. "
                                + "Displayed data may be incomplete or "
                                + "not entirely correct.", e);
                    }
                    pts.add(new Coordinate(point.getX(), point.getY()));
                    vals.add(out);
                }

                if (!pts.isEmpty()) {

                    if (worldWrap) {
                        screen = toScreenRightOfZero(
                                pts.toArray(new Coordinate[pts.size()]),
                                rastPosToWorldGrid, minX, minY);
                        if (screen != null) {
                            addStreamLineToJTS(screen, gf, worldGridToLatlon,
                                    jtsCompiler, corrector);
                        }

                        screenx = toScreenLeftOfZero(
                                pts.toArray(new Coordinate[pts.size()]),
                                rastPosToWorldGrid, minX, minY);
                        if (screenx != null) {
                            addStreamLineToJTS(screenx, gf, worldGridToLatlon,
                                    jtsCompiler, corrector);
                        }
                    } else {
                        double[][] valsArr = vals
                                .toArray(new double[vals.size()][2]);
                        addStreamLineToJTS(valsArr, gf, worldGridToLatlon,
                                jtsCompiler, corrector);
                    }

                    vals.clear();
                    pts.clear();
                }
            }

            if (!vals.isEmpty()) {

                double[][] valsArr = vals.toArray(new double[vals.size()][2]);
                addStreamLineToJTS(valsArr, gf, worldGridToLatlon, jtsCompiler,
                        corrector);

                if (worldWrap) {
                    screen = toScreenRightOfZero(
                            pts.toArray(new Coordinate[pts.size()]),
                            rastPosToWorldGrid, minX, minY);
                    if (screen != null) {
                        addStreamLineToJTS(screen, gf, worldGridToLatlon,
                                jtsCompiler, corrector);
                    }

                    screenx = toScreenLeftOfZero(
                            pts.toArray(new Coordinate[pts.size()]),
                            rastPosToWorldGrid, minX, minY);
                    if (screenx != null) {
                        addStreamLineToJTS(screenx, gf, worldGridToLatlon,
                                jtsCompiler, corrector);
                    }
                }
                vals.clear();
            }
        } catch (Throwable e) {
            statusHandler.error("Error postprocessing contours:", e);
            isCntrsCreated = false;
            return;
        }
        contourGroup.streamlines.compile();
    }

    private ColorBar generateColorBarInfo(List<Double> fIntvls,
            List<Integer> fColors) {

        if (attr.getClrbar() != null && !attr.getClrbar().isEmpty()) {
            contourGroup.clrbar = new CLRBAR(attr.getClrbar());
            ColorBarAttributesBuilder cBarAttrBuilder = contourGroup.clrbar
                    .getcBarAttributesBuilder();
            ColorBar colorBar = new ColorBar();
            if (cBarAttrBuilder.isDrawColorBar()) {
                colorBar.setAttributesFromColorBarAttributesBuilder(
                        cBarAttrBuilder);
                colorBar.setColorDevice(NcDisplayMngr.getActiveNatlCntrsEditor()
                        .getActiveDisplayPane().getDisplay());

                List<Double> fillIntvls = new ArrayList<>();
                if (fIntvls != null && !fIntvls.isEmpty()) {
                    fillIntvls.addAll(fIntvls);
                } else {
                    FINT theFillIntervals = new FINT(fint.trim());
                    fillIntvls = theFillIntervals
                            .getUniqueSortedFillValuesFromAllZoomLevels();
                }

                List<Integer> fillColors = new ArrayList<>();
                if (fColors != null && !fColors.isEmpty()) {
                    fillColors.addAll(fColors);
                } else {
                    FLine fillColorString = new FLine(fline.trim());
                    fillColors = fillColorString.getFillColorList();
                }

                fillIntvls.add(0, Double.NEGATIVE_INFINITY);
                int numFillIntervals = fillIntvls.size();
                fillIntvls.add(numFillIntervals, Double.POSITIVE_INFINITY);
                int numDecimals = 0;
                for (int index = 0; index <= numFillIntervals - 1; index++) {

                    if (index < fillColors.size()) {
                        colorBar.addColorBarInterval(
                                fillIntvls.get(index).floatValue(),
                                fillIntvls.get(index + 1).floatValue(),
                                GempakColor
                                        .convertToRGB(fillColors.get(index)));
                        String tmp[] = fillIntvls.get(index).toString()
                                .split("\\.");
                        if (tmp.length > 1 && tmp[1].length() > numDecimals
                                && !"0".equals(tmp[1])) {
                            numDecimals = tmp[1].length();
                        }
                    }
                }
                colorBar.setNumDecimals(numDecimals);

                return colorBar;
            }
        }
        return null;
    }

    /**
     * Uses the FortConBuf algorithm to generate contour lines in x/y space.
     *
     * @param contourVals
     *            The desired values of the lines
     * @return a Map of the contour line value to a Geometry of the contour(s)
     *         in x/y space
     */
    private Map<Float, Geometry> fortConBuf(List<Double> contourVals) {
        float[] contVals = new float[contourVals.size()];
        for (int i = 0; i < contourVals.size(); i++) {
            contVals[i] = contourVals.get(i).floatValue();
        }

        FortConConfig cfg = new FortConConfig();
        // mode: seed.length
        cfg.mode = contVals.length;
        // seed: desired contour lines/values
        cfg.seed = contVals;
        // badlo: always seems to be this number
        cfg.badlo = GridUtil.GRID_FILL_VALUE - 1;
        // badhi: always seem to be this number
        cfg.badhi = GridUtil.GRID_FILL_VALUE + 1;
        cfg.xOffset = 0;
        cfg.yOffset = 0;

        ContourContainer contours = FortConBuf.contour(cntrData,
                cntrData.getX(), cntrData.getY(), cfg);
        GeometryFactory geomFactory = new GeometryFactory();

        // map of contour value to xy lines
        Map<Float, List<LineString>> contourResult = new HashMap<>();
        int nLines = contours.contourVals.size();
        for (int i = 0; i < nLines; i++) {
            // value of the line
            Float contourVal = contours.contourVals.get(i).floatValue();
            // build a line
            float[] pointArray = contours.xyContourPoints.get(i);
            CoordinateSequence coords = new PackedCoordinateSequence.Double(
                    pointArray, 2, 0);
            if (coords.size() < 2) {
                continue;
            }
            LineString line = geomFactory.createLineString(coords);

            /*
             * a single contour value, e.g. 850, could have multiple lines on
             * different areas, so we need a List of lines for that value
             */
            List<LineString> linesForContourVal = contourResult.get(contourVal);
            if (linesForContourVal == null) {
                linesForContourVal = new ArrayList<>();
                contourResult.put(contourVal, linesForContourVal);
            }
            linesForContourVal.add(line);
        }

        // create MultiLineStrings so we get all lines for each value
        Map<Float, Geometry> result = new HashMap<>();
        for (Entry<Float, List<LineString>> cr : contourResult.entrySet()) {
            List<LineString> lines = cr.getValue();
            MultiLineString mls = geomFactory
                    .createMultiLineString(lines.toArray(new LineString[0]));
            result.put(cr.getKey(), mls);
        }

        return result;
    }

    private void genContour(List<Double> cvalues) {
        List<Double> allvalues = new ArrayList<>(cvalues);
        Collections.sort(allvalues);

        long t1a = System.currentTimeMillis();
        Map<Float, Geometry> contours = fortConBuf(cvalues);
        for (Entry<Float, Geometry> c : contours.entrySet()) {
            Geometry xygeom = contours.get(c.getKey());
            Geometry llgeom = transformGeometry(xygeom, rastPosToLatLon);
            contourGroup.latlonContours.put(c.getKey().toString(), llgeom);
        }
        long t2 = System.currentTimeMillis();
        logger.debug(
                "Total generating contour line values took: " + (t2 - t1a));

        if (ncgribLogger.isEnableContourLogs()) {
            printSize();
        }
    }

    private void printSize() {
        int num = 0;
        for (Geometry g : contourGroup.latlonContours.values()) {
            num += g.getNumPoints();
        }
        logger.debug("This CONTOURGROUP contains " + num + " coordinates");
    }

    private Geometry transformGeometry(Geometry geom, MathTransform xform) {
        GeometryFactory gf = geom.getFactory();
        List<Geometry> llgeoms = new ArrayList<>();

        for (int i = 0; i < geom.getNumGeometries(); i++) {
            Geometry gn = geom.getGeometryN(i);

            if (gn instanceof LineString) {
                Coordinate[] llcoords = transformCoordinates(
                        gn.getCoordinates(), xform);
                CoordinateSequence latlonseq = new PackedCoordinateSequence.Float(
                        llcoords, 2);
                LineString ls = gf.createLineString(latlonseq);
                llgeoms.add(ls);
            } else if (gn instanceof Polygon) {
                Polygon poly = transformPolygon((Polygon) gn, xform);
                llgeoms.add(poly);
            }
        }
        return gf.createGeometryCollection(llgeoms.toArray(new Geometry[] {}));
    }

    private Polygon transformPolygon(Polygon pgn, MathTransform xform) {
        GeometryFactory gf = pgn.getFactory();
        Polygon poly;
        int numInterior;

        // Transform exterior ring
        Coordinate[] llcoords = transformCoordinates(
                pgn.getExteriorRing().getCoordinates(), xform);
        CoordinateSequence latlonseq = new PackedCoordinateSequence.Float(
                llcoords, 2);
        LinearRing lr = gf.createLinearRing(latlonseq);

        numInterior = pgn.getNumInteriorRing();

        if (numInterior == 0) {
            poly = gf.createPolygon(lr, null);
            return poly;
        }

        // Transform all interior rings
        LinearRing[] holes = new LinearRing[numInterior];
        for (int n = 0; n < numInterior; n++) {
            llcoords = transformCoordinates(
                    pgn.getInteriorRingN(n).getCoordinates(), xform);
            latlonseq = new PackedCoordinateSequence.Float(llcoords, 2);
            holes[n] = gf.createLinearRing(latlonseq);
        }

        poly = gf.createPolygon(lr, holes);
        return poly;
    }

    private Coordinate[] transformCoordinates(Coordinate[] coordinates,
            MathTransform xform) {
        CoordinateList clist = new CoordinateList();
        double[] tmp = new double[2];
        double[] out = new double[2];

        for (Coordinate loc : coordinates) {

            tmp[0] = loc.x;
            tmp[1] = loc.y;

            try {
                xform.transform(tmp, 0, out, 0, 1);
                if (out[0] < -180 || out[0] > 180.) {
                    out[0] = ((out[0] + 180) % 360) - 180;
                }
                if (out[0] == 0.0) {
                    out[0] = 0.001;
                }
                clist.add(new Coordinate(out[0], out[1]), true);
            } catch (TransformException e) {
            }
        }
        return clist.toCoordinateArray();
    }

    public ContourGroup getContours() {
        if (!isCntrsCreated) {
            return null;
        }
        return contourGroup;
    }

    /**
     * If the worldWrapChecker is true and the grid is split by the map border.
     *
     * @param imageGridGeometry
     * @param rastPosToLatLon
     * @return
     */
    private boolean needWrap(GeneralGridGeometry imageGridGeometry,
            MathTransform rastPosToLatLon) {
        boolean ret = worldWrapChecker;

        if (ret) {
            // minimum, maximum X grid
            int minx = imageGridGeometry.getGridRange().getLow(0);
            int maxx = imageGridGeometry.getGridRange().getHigh(0);

            double[] out0 = new double[3];
            double[] out1 = new double[3];

            // minimum, maximum longitudes
            try {
                rastPosToLatLon.transform(new double[] { minx, 0 }, 0, out0, 0,
                        1);
                rastPosToLatLon.transform(new double[] { maxx, 0 }, 0, out1, 0,
                        1);
            } catch (TransformException e) {
                ret = false;
            }

            double minLon = (out0[0] >= 0) ? out0[0] : out0[0] + 360;
            double maxLon = (out1[0] >= 0) ? out1[0] : out1[0] + 360;

            if (minLon == 0 && maxLon == 360) {
                globalData = true;
            }

            if (maxLon >= 360) {
                maxLon = 359;
            }
            double right = centralMeridian + 180;

            if (maxLon > minLon) {
                ret = (right > minLon) && (right < maxLon);
            } else {
                ret = !(right > minLon) && (right < maxLon);
            }
        }

        MapProjection worldProjection = CRS
                .getMapProjection(descriptor.getCRS());
        try {
            if (worldProjection.getClass().getCanonicalName()
                    .contains("Lambert")) {
                ret = false;
            }
        } catch (Exception e) {
            logger.info(" Can't get Map projection");
        }
        return ret;
    }

    /**
     * Gets the maximum grid number in x direction
     *
     * @param imageGridGeometry
     * @return int - maximum grid number in x direction
     */
    private int getMaxGridX(GeneralGridGeometry imageGridGeometry) {
        return imageGridGeometry.getGridRange().getHigh(0);
    }

    /**
     * Gets the map width in screen coordinate.
     *
     * @return
     */
    private double getMapWidth() {
        if (worldWrapChecker) {
            double right[] = new double[] { -180, 0 };
            double left[] = new double[] { 0, 0 };

            double screenLeft[] = new double[2];
            double screenRight[] = new double[2];

            try {
                double center[] = new double[] { 0, 0 };
                double out[] = new double[2];
                rastPosLatLonToWorldGrid.transform(center, 0, out, 0, 1);
                zeroLonOnScreen = out[0];

                rastPosLatLonToWorldGrid.transform(left, 0, screenLeft, 0, 1);
                rastPosLatLonToWorldGrid.transform(right, 0, screenRight, 0, 1);

                return Math.abs(screenRight[0] - screenLeft[0]) * 2;
            } catch (TransformException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Get Font Name
     */
    private String getFontName(int fontNum) {
        String font = "Monospace";
        String[] name = { "Courier", "Helvetica", "TimesRoman" };

        if ((fontNum > 0) && (fontNum <= name.length)) {
            font = name[fontNum - 1];
        }
        return font;
    }

    /**
     * Calculates the angle difference of "north" relative to the screen's
     * y-axis at a given pixel location.
     *
     * @param loc
     *            - The point location in pixel coordinates
     * @return The angle difference of "north" versus world coordinate's y-axis
     */
    private static double northOffsetAngle(Coordinate loc,
            IMapDescriptor descriptor) {
        double delta = 0.05;

        /*
         * Calculate points in world coordinates just south and north of
         * original location.
         */
        double[] south = { loc.x, loc.y - delta, 0.0 };
        double[] pt1 = descriptor.pixelToWorld(south);

        double[] north = { loc.x, loc.y + delta, 0.0 };
        double[] pt2 = descriptor.pixelToWorld(north);

        if (pt1 != null && pt2 != null) {
            return -90.0 - Math.toDegrees(
                    Math.atan2((pt2[1] - pt1[1]), (pt2[0] - pt1[0])));
        } else {
            return 0.0;
        }
    }

    private List<Integer> handleNotEnoughFillColors(int fillIntvlsSize,
            List<Integer> fillColors) {

        List<Integer> newFillColors = new ArrayList<>();
        newFillColors.addAll(fillColors);

        int index = 0;
        for (int i = fillColors.size() + 1; i < fillIntvlsSize + 2
                && !fillColors.isEmpty(); i++) {

            if (index >= fillColors.size()) {
                index = 0;
            }

            newFillColors.add(fillColors.get(index));

            index++;
        }
        return newFillColors;
    }

    private List<Integer> handleTooManyFillColors(int fillIntvlsSize,
            List<Integer> fillColors) {

        List<Integer> newFillColors = new ArrayList<>();

        for (int i = 0; i < fillIntvlsSize + 1; i++) {
            newFillColors.add(fillColors.get(i));
        }

        return newFillColors;
    }

    /*
     * Adds a stream line to JTSCompiler for clipping against view area.
     *
     * Clipping removed from GLGeometryObject2D and the clipping is moved to
     * JTSCompiler. So we use this method to clip against view area to remove
     * pole points and extra lines outside of view area.
     *
     * @param points - The point location in world grid coordinates
     *
     * @param gf - Geometry factory.
     *
     * @param mf - math transform to convert points into map coordinates.
     *
     * @param jtsCompiler - A JTS compiler to accept/handle the LineString.
     *
     * @param wcr - A WorldWrapCorrector to handle world wrap.
     *
     * @return
     */
    private void addStreamLineToJTS(double[][] points, GeometryFactory gf,
            MathTransform mf, JTSCompiler jtsCompiler, WorldWrapCorrector wcr) {
        // Put points in world grid into a coordinate array.
        Coordinate[] crds = new Coordinate[points.length];
        for (int ii = 0; ii < points.length; ii++) {
            crds[ii] = new Coordinate(points[ii][0], points[ii][1]);
        }

        // Transform points into map coordinates.
        Coordinate[] mapCrds = transformCoordinates(crds, mf);

        // Create a LineString
        LineString lnst = gf.createLineString(mapCrds);

        // Do world wrap correction
        Geometry correctedLnst = wcr.correct(lnst);

        // Add into a JTSCompiler to handle.
        try {
            jtsCompiler.handle(correctedLnst);
        } catch (VizException e) {
            statusHandler
                    .error("Error occurred while trying to add a stream line"
                            + " to the JTSCompiler to handle:", e);
        }
    }
}
