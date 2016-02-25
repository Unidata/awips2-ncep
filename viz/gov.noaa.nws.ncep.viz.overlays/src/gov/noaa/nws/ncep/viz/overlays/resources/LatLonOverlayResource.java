package gov.noaa.nws.ncep.viz.overlays.resources;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.Style;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Implements a drawing layer to draw lat/lon lines
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 *   06/15/09    #127        M. Gao	     Initial Creation
 *   06/17/09    #115        Greg Hull   Integrate with AbstractNatlCntrsResource
 *   08/07/09                Greg Hull   remove unused variables and methods
 *   11/18/09                Greg Hull   Incorporate to11d6 changes
 *   11/04/13    #880        Xiaochuan   set one wireframeShape for one lat or Lon lines.
 *                                       Set spatialChopFlag to be false.
 *   05/23/2014  #970        P.Swamy     Lat/lon label sizes need to be larger
 *   07/28/2014  #3397       vbclement   switched to non deprecated version of createWireframeShape()
 *                                          removed unneeded clearCoodrinatePointArrayList() method
 *   11/05/2015  #5070       randerso    Adjust font sizes for dpi scaling
 *   Jul 28, 2014 3397       bclement    switched to non deprecated version of createWireframeShape()
 *                                          removed unneeded clearCoordinatePointArrayList() method
 *   Nov 05, 2015 10436      njensen     Major cleanup and optimization
 * 
 * </pre>
 * 
 * @author mgao
 * 
 */
public class LatLonOverlayResource extends
        AbstractVizResource<LatLonOverlayResourceData, IMapDescriptor>
        implements INatlCntrsResource {

    private static final GeometryFactory GEOM_FACTORY = new GeometryFactory();

    /** The wireframe object for drawing Latitude lines */
    private IWireframeShape wireframeShapeForLatLineArray;

    /** The wireframe object for drawing Longitude lines */
    private IWireframeShape wireframeShapeForLonLineArray;

    private Map<Double, Geometry> latitudeLineGeometries;

    private Map<Double, Geometry> longitudeLineGeometries;

    private double mapMinX;

    private double mapMaxY;

    private double mapMinY;

    private double mapMaxX;

    private double viewMinX;

    private double viewMaxY;

    private double viewMinY;

    private double viewMaxX;

    /*
     * The effective view borders (map borders inside the viewable user window)
     * used to paint labels
     */
    private Geometry effectiveLeftBorder;

    private Geometry effectiveBottomBorder;

    private Geometry mapBoundary;

    private Geometry mapMedianLine;

    private final double latLonDrawingPointInterval = 1.0;

    private final double drawingGap = 120;

    private double labelGap;

    private boolean needsUpdate = true;

    private IFont font;

    protected LatLonOverlayResource(LatLonOverlayResourceData llRscData,
            LoadProperties props) {
        super(llRscData, props);
    }

    @Override
    public void initInternal(IGraphicsTarget target) throws VizException {
        initializeMapMinAndMaxXAndY();
        font = target.initializeFont("Courier", 14, new Style[] { Style.BOLD });
    }

    @Override
    public void paintInternal(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        float zoomFactor = paintProps.getZoomLevel();
        labelGap = drawingGap * zoomFactor;

        initializeViewMinAndMaxXAndY(paintProps);
        double latitudeInterval = resourceData.getLatitudeInterval();
        double longitudeInterval = resourceData.getLongitudeInterval();

        /*
         * Only need to recreate the wireframeShapes if the intervals or line
         * types changed.
         */
        if (needsUpdate) {
            needsUpdate = false;

            // clear out and setup new geometries and wireframeShapes
            clearWireFrameShapeArray(wireframeShapeForLatLineArray);
            clearWireFrameShapeArray(wireframeShapeForLonLineArray);
            latitudeLineGeometries = new HashMap<>(
                    (int) (180 / latitudeInterval + 1), 1.0f);
            longitudeLineGeometries = new HashMap<>(
                    (int) (360 / longitudeInterval + 1), 1.0f);
            wireframeShapeForLatLineArray = target.createWireframeShape(false,
                    descriptor);
            wireframeShapeForLonLineArray = target.createWireframeShape(false,
                    descriptor);

            // make the latitude wireframeShape
            for (double latitudeValue = -90; latitudeValue <= 90; latitudeValue += latitudeInterval) {
                double[][] latLonCoordinateArray = createCoordinateArrayForLatitudeLine(
                        latitudeValue, latLonDrawingPointInterval);

                if (!(latitudeValue == -90 || latitudeValue == 90)
                        && latLonCoordinateArray.length > 0) {
                    wireframeShapeForLatLineArray
                            .addLineSegment(latLonCoordinateArray);
                }
            }
            wireframeShapeForLatLineArray.compile();

            // make the longitude wireframeShape
            for (double longitudeValue = -180; longitudeValue <= 180; longitudeValue += longitudeInterval) {
                double[][] latLonCoordinateArray = createCoordinateArrayLongitudeLine(
                        longitudeValue, latLonDrawingPointInterval);

                if (latLonCoordinateArray.length > 0) {
                    wireframeShapeForLonLineArray
                            .addLineSegment(latLonCoordinateArray);
                }
            }
            wireframeShapeForLonLineArray.compile();
        }

        /*
         * clear off the existing labels and recalculate them on every paint
         * because they may need to be placed differently if the user is
         * zooming/panning
         */
        wireframeShapeForLatLineArray.clearLabels();
        wireframeShapeForLonLineArray.clearLabels();

        // calculate and add latitude labels
        for (double latitudeValue = -90; latitudeValue <= 90; latitudeValue += latitudeInterval) {
            Coordinate intersection = null;
            Geometry latLine = latitudeLineGeometries.get(latitudeValue);
            if (latLine != null) {
                if (effectiveLeftBorder.intersects(latLine)) {
                    intersection = effectiveLeftBorder.intersection(latLine)
                            .getCoordinate();
                } else if (latLine.within(mapBoundary)
                        && mapMedianLine.intersects(latLine)) {
                    intersection = mapMedianLine.intersection(latLine)
                            .getCoordinate();
                    /*
                     * Removes default latitude label if it is too close to the
                     * bottom of the viewable edge to prevent the latitude label
                     * from being displayed over the longitude labels.
                     */
                    if (intersection.y > effectiveBottomBorder.getCoordinate().y
                            - 2.5 * labelGap) {
                        intersection = null;
                    }
                }
                if (intersection != null) {
                    wireframeShapeForLatLineArray
                            .addLabel(String.valueOf((int) latitudeValue),
                                    new double[] { intersection.x + labelGap,
                                            intersection.y });
                }
            }
        }

        // calculate and add longitude labels
        for (double longitudeValue = -180; longitudeValue <= 180; longitudeValue += longitudeInterval) {
            Geometry lonLine = longitudeLineGeometries.get(longitudeValue);
            if (lonLine != null) {
                if (lonLine.intersects(effectiveBottomBorder)) {
                    Coordinate intersection = effectiveBottomBorder
                            .intersection(lonLine).getCoordinate();
                    int lonInt = (int) longitudeValue;
                    String label = Math.abs(lonInt) != 180 ? String
                            .valueOf(lonInt) : "ID";
                    wireframeShapeForLonLineArray.addLabel(label, new double[] {
                            intersection.x, intersection.y - labelGap });
                }
            }
        }

        target.drawWireframeShape(wireframeShapeForLatLineArray,
                resourceData.getColor(), resourceData.getLineWidth(),
                resourceData.getLineStyle(), font);
        target.drawWireframeShape(wireframeShapeForLonLineArray,
                resourceData.getColor(), resourceData.getLineWidth(),
                resourceData.getLineStyle(), font);
    }

    private void initializeMapMinAndMaxXAndY() {
        mapMinX = descriptor.getGridGeometry().getGridRange().getLow(0);
        mapMaxX = descriptor.getGridGeometry().getGridRange().getHigh(0);
        mapMinY = descriptor.getGridGeometry().getGridRange().getLow(1);
        mapMaxY = descriptor.getGridGeometry().getGridRange().getHigh(1);

        double mapMidX = (mapMaxX - mapMinX) / 2;
        double mapMidY = (mapMaxY - mapMinY) / 2;
        mapMedianLine = GEOM_FACTORY.createLineString(new Coordinate[] {
                new Coordinate(mapMidX, mapMinY),
                new Coordinate(mapMidX, mapMidY) });
        /*
         * Map boundary must be created in a continuous coordinate order to form
         * a rectangle!
         */
        mapBoundary = GEOM_FACTORY.createPolygon(
                GEOM_FACTORY.createLinearRing(new Coordinate[] {
                        new Coordinate(mapMinX, mapMinY),
                        new Coordinate(mapMinX, mapMaxY),
                        new Coordinate(mapMaxX, mapMaxY),
                        new Coordinate(mapMaxX, mapMinY),
                        new Coordinate(mapMinX, mapMinY) }), null);
    }

    private void initializeViewMinAndMaxXAndY(PaintProperties paintProps) {
        viewMinX = paintProps.getView().getExtent().getMinX();
        viewMaxX = paintProps.getView().getExtent().getMaxX();
        viewMinY = paintProps.getView().getExtent().getMinY();
        viewMaxY = paintProps.getView().getExtent().getMaxY();

        updateEffectiveView();
    }

    private void updateEffectiveView() {
        double effectiveMinX = viewMinX;
        double effectiveMaxX = viewMaxX;
        double effectiveMinY = viewMinY;
        double effectiveMaxY = viewMaxY;

        if (mapMinX > viewMinX && mapMinX < viewMaxX) {
            effectiveMinX = mapMinX;
        }
        if (mapMaxX > viewMinX && mapMaxX < viewMaxX) {
            effectiveMaxX = mapMaxX;
        }
        if (mapMinY > viewMinY && mapMinY < viewMaxY) {
            effectiveMinY = mapMinY;
        }
        if (mapMaxY > viewMinY && mapMaxY < viewMaxY) {
            effectiveMaxY = mapMaxY;
        }
        double cornerGap = 2.5 * labelGap;
        Coordinate[] leftBorderCoordinates = new Coordinate[] {
                new Coordinate(effectiveMinX, effectiveMinY + cornerGap),
                new Coordinate(effectiveMinX, effectiveMaxY - cornerGap) };
        Coordinate[] bottomBorderCoordinates = new Coordinate[] {
                new Coordinate(effectiveMinX + cornerGap, effectiveMaxY),
                new Coordinate(effectiveMaxX - cornerGap, effectiveMaxY) };
        effectiveLeftBorder = GEOM_FACTORY
                .createLineString(leftBorderCoordinates);
        effectiveBottomBorder = GEOM_FACTORY
                .createLineString(bottomBorderCoordinates);
    }

    private double[][] createCoordinateArrayForLatitudeLine(
            double latitudeValue, double latLonPointInterval) {
        int coordinateArrayLength = (int) (360 / latLonPointInterval) + 1;
        List<Coordinate> latLineCoordinates = new ArrayList<Coordinate>(
                coordinateArrayLength);
        List<double[]> latLinePixels = new ArrayList<double[]>(
                coordinateArrayLength);
        double longitude = -180;
        for (int i = 0; i < coordinateArrayLength && longitude <= 180; i++) {
            double[] latLon = new double[] { longitude, latitudeValue };
            double[] screenPixel = descriptor.worldToPixel(latLon);
            if (screenPixel != null) {
                latLinePixels.add(screenPixel);
                latLineCoordinates.add(new Coordinate(screenPixel[0],
                        screenPixel[1]));
            }
            longitude += latLonPointInterval;
        }
        if (!latLineCoordinates.isEmpty()) {
            LineString lineString = GEOM_FACTORY
                    .createLineString(latLineCoordinates
                            .toArray(new Coordinate[] {}));
            /*
             * Depending on the projection, the latitude lines may wrap around
             * the world and off the projection, resulting in NaNs for the
             * coordinates off the projection. The filter will remove those
             * coordinates so we don't attempt to draw them.
             */
            IllegalCoordinateFilter filter = new IllegalCoordinateFilter();
            lineString.apply(filter);
            latitudeLineGeometries.put(latitudeValue, filter.getFilterResult());
        }
        return latLinePixels.toArray(new double[][] {});
    }

    private double[][] createCoordinateArrayLongitudeLine(
            double longitudeValue, double latLonPointInterval) {
        int coordinateArrayLength = (int) ((180 - 10) / latLonPointInterval);
        List<Coordinate> lonLineCoordinates = new ArrayList<Coordinate>(
                coordinateArrayLength);
        List<double[]> lonLinePixels = new ArrayList<double[]>(
                coordinateArrayLength);
        double latitude = -90 + latLonPointInterval;
        for (int i = 0; i < coordinateArrayLength && latitude <= 90; i++) {
            double[] latLon = new double[] { longitudeValue, latitude };
            double[] screenPixel = descriptor.worldToPixel(latLon);
            if (screenPixel != null) {
                lonLinePixels.add(screenPixel);
                lonLineCoordinates.add(new Coordinate(screenPixel[0],
                        screenPixel[1]));
            }
            latitude += latLonPointInterval;
        }
        if (!lonLineCoordinates.isEmpty()) {
            LineString lineString = GEOM_FACTORY
                    .createLineString(lonLineCoordinates
                            .toArray(new Coordinate[] {}));
            /*
             * Depending on the projection, the longitude lines may wrap around
             * the world and off the projection, resulting in NaNs for the
             * coordinates off the projection. The filter will remove those
             * coordinates so we don't attempt to draw them.
             */
            IllegalCoordinateFilter filter = new IllegalCoordinateFilter();
            lineString.apply(filter);
            longitudeLineGeometries.put(longitudeValue,
                    filter.getFilterResult());
        }
        return lonLinePixels.toArray(new double[][] {});
    }

    @Override
    public void disposeInternal() {
        clearWireFrameShapeArray(wireframeShapeForLatLineArray);
        clearWireFrameShapeArray(wireframeShapeForLonLineArray);
        font.dispose();
    }

    private void clearWireFrameShapeArray(IWireframeShape wireframeShapeArray) {
        if (wireframeShapeArray != null) {
            wireframeShapeArray.dispose();
            wireframeShapeArray = null;
        }
    }

    /*
     * the getters for Map's Min and Max X and Y, View's Min and Max X and Y
     */

    public double getViewMinX() {
        return viewMinX;
    }

    public double getViewMaxY() {
        return viewMaxY;
    }

    public double getViewMinY() {
        return viewMinY;
    }

    public double getViewMaxX() {
        return viewMaxX;
    }

    public double getMapMinX() {
        return mapMinX;
    }

    public double getMapMaxY() {
        return mapMaxY;
    }

    public double getMapMinY() {
        return mapMinY;
    }

    public double getMapMaxX() {
        return mapMaxX;
    }

    @Override
    public void resourceAttrsModified() {
        needsUpdate = true;
    }

    public boolean isProjectable(CoordinateReferenceSystem mapData) {
        return true;
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        needsUpdate = true;
        initializeMapMinAndMaxXAndY();
    }
}
