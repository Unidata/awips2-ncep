package gov.noaa.nws.ncep.common.dataplugin.modis.projection;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.operation.MathTransformProvider;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * MODIS map projection
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * 08/04/2015   R7270      kbugenhagen  Modified transformNormalized to 
 *                                      interpolate to get i,j points instead
 *                                      of finding closest.
 * 08/12/2015   R7270      kbugenhagen  Modified transformNormalized to use
 *                                      STRtree search in ModisPointDataSource class
 *                                      instead of map lookup.
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisMapProjection extends MapProjection {

    private static final long serialVersionUID = -7936272226439544019L;

    public static final String MODIS_MAP_PROJECTION_GROUP = "Modis_CENTER_MAPPING";

    public static final String PROJECTION_NAME = "NPP Modis";

    public static final String CENTER_LATITUDES = "center_lats";

    public static final String CENTER_LONGITUDES = "center_lons";

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String DIRECTIONS = "directions";

    public static final String RESOLUTION = "resolution";

    public static final String CENTER_LENGTH = "center_length";

    public static final String ENVELOPE = "envelope";

    public static final String SEMI_MAJOR = Provider.SEMI_MAJOR.getName()
            .getCode();

    public static final String SEMI_MINOR = Provider.SEMI_MINOR.getName()
            .getCode();

    public static final double TWOPI = Math.PI * 2;

    public static final double PI = Math.PI;

    public static final double PIO2 = Math.PI / 2;

    private float[] latitudes;

    private final float[] longitudes;

    private final Geometry envelope;

    private final double resolution;

    private final int actualHeight;

    private final int width;

    private final int height;

    private Point2D lastPoint;

    private ModisPointDataSource pointDataSource;

    private void logDuration(long startTime, String method) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        if (duration > 0) {
            System.out.println(method + " took: " + duration + " ms");
        }
    }

    private ModisPointDataSource createPointDataSource() {
        ModisPointDataSource dataSource = new ModisPointDataSource(width,
                height, latitudes, longitudes);
        dataSource.loadData();
        return dataSource;
    }

    /**
     * @param values
     * @throws ParameterNotFoundException
     */
    protected ModisMapProjection(ParameterValueGroup values)
            throws ParameterNotFoundException {
        super(values);
        this.latitudes = Provider.getValue(Provider.CENTER_LATS, values);
        this.longitudes = Provider.getValue(Provider.CENTER_LONS, values);
        this.width = Provider.getValue(Provider.WIDTH, values);
        this.height = Provider.getValue(Provider.HEIGHT, values);
        this.pointDataSource = createPointDataSource();
        this.envelope = Provider.getValue(Provider.ENVELOPE, values);
        this.resolution = Provider.getValue(Provider.RESOLUTION, values);
        this.actualHeight = Provider.getValue(Provider.CENTER_LENGTH, values);
        if (latitudes.length != longitudes.length) {
            throw new IllegalArgumentException(
                    "Center lat/lon and direction arrays must be same length");
        }
    }

    /*
     * lat/lon to pixel coordinates transform
     * 
     * (non-Javadoc)
     * 
     * @see org.geotools.referencing.operation.projection.MapProjection#
     * transformNormalized(double, double, java.awt.geom.Point2D)
     */
    @Override
    protected Point2D transformNormalized(double aLonR, double aLatR,
            Point2D ptDst) throws ProjectionException {

        long startTime = System.nanoTime();

        Point2D point = ptDst != null ? ptDst : new Point2D.Double();
        double aLatD = Math.toDegrees(aLatR);
        double aLonD = Math.toDegrees(aLonR);

        Coordinate[] coords = envelope.getCoordinates();

        // if lat/lon not within envelope, return last good point found
        if (!contains(coords, aLonR, aLatR)) {
            point.setLocation(Double.NaN, Double.NaN);
            return point;
        }

        Point2D lookupPoint = null;

        Point2D point1 = new Point2D.Double(aLonD, aLatD);

        // Find the closest pixel (i,j) for the lat/lon
        lookupPoint = pointDataSource.getNearestPoint(point1);

        if (lookupPoint != null) {
            double x = lookupPoint.getX();
            double y = lookupPoint.getY();
            point.setLocation(x * resolution, y * resolution);
        } else {
            point.setLocation(Double.NaN, Double.NaN);
        }

        logDuration(startTime, "transformNormalized");

        return point;
    }

    /*
     * pixel to lat/lon coordinates transform
     * 
     * (non-Javadoc)
     * 
     * @see org.geotools.referencing.operation.projection.MapProjection#
     * inverseTransformNormalized(double, double, java.awt.geom.Point2D)
     */
    @Override
    protected Point2D inverseTransformNormalized(double x, double y,
            Point2D inputPoint) throws ProjectionException {

        Coordinate latLon = new Coordinate();
        double xi = x / resolution;
        double yi = y / resolution;

        Point2D point = inputPoint != null ? inputPoint : new Point2D.Double();

        if (yi >= height) {
            yi = height - 1;
        }
        if (xi >= width) {
            xi = width - 1;
        }

        int idx = (int) yi * width + (int) xi;
        latLon.x = longitudes[idx];
        latLon.y = latitudes[idx];

        point.setLocation(Math.toRadians(latLon.x), Math.toRadians(latLon.y));

        return point;
    }

    private boolean contains(Coordinate[] coords, double aLonR, double aLatR) {
        double[] xCoords = new double[5];
        double[] yCoords = new double[5];

        int i = 0;
        for (Coordinate c : coords) {
            xCoords[i] = c.x;
            yCoords[i] = c.y;
            i++;
        }
        Arrays.sort(xCoords);
        Arrays.sort(yCoords);

        if (Math.toDegrees(aLonR) < xCoords[0]
                || Math.toDegrees(aLonR) > xCoords[xCoords.length - 1]
                || Math.toDegrees(aLatR) < yCoords[0]
                || Math.toDegrees(aLatR) > yCoords[xCoords.length - 1]) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * Returns a hash value for this map projection.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + actualHeight;
        result = prime * result + Arrays.hashCode(latitudes);
        result = prime * result + Arrays.hashCode(longitudes);
        long temp;
        temp = Double.doubleToLongBits(resolution);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ModisMapProjection other = (ModisMapProjection) obj;
        if (actualHeight != other.actualHeight)
            return false;
        if (!Arrays.equals(latitudes, other.latitudes))
            return false;
        if (!Arrays.equals(longitudes, other.longitudes))
            return false;
        if (Double.doubleToLongBits(resolution) != Double
                .doubleToLongBits(other.resolution))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.referencing.operation.projection.MapProjection#
     * getParameterDescriptors()
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Provider.PARAMETERS;
    }

    public static class Provider extends AbstractProvider {

        private static final long serialVersionUID = 1266944341235566642L;

        static final ParameterDescriptor<float[]> CENTER_LATS = DefaultParameterDescriptor
                .create(ModisMapProjection.CENTER_LATITUDES,
                        "Latitude locations of center points", float[].class,
                        null, true);

        static final ParameterDescriptor<float[]> CENTER_LONS = DefaultParameterDescriptor
                .create(ModisMapProjection.CENTER_LONGITUDES,
                        "Longitude locations of center points", float[].class,
                        null, true);

        static final ParameterDescriptor<Double> RESOLUTION = DefaultParameterDescriptor
                .create(ModisMapProjection.RESOLUTION,
                        "Spacing of cells in meters", Double.class, 1000.0,
                        true);

        static final ParameterDescriptor<Integer> WIDTH = DefaultParameterDescriptor
                .create(ModisMapProjection.WIDTH,
                        "Spatial coverage Nx dimension", Integer.class, 1354,
                        true);

        static final ParameterDescriptor<Integer> HEIGHT = DefaultParameterDescriptor
                .create(ModisMapProjection.HEIGHT,
                        "Spatial coverage Ny dimension", Integer.class, 2040,
                        true);

        static final ParameterDescriptor<Geometry> ENVELOPE = DefaultParameterDescriptor
                .create(ModisMapProjection.ENVELOPE,
                        "Spatial coverage envelope", Geometry.class, null, true);

        static final ParameterDescriptor<Integer> CENTER_LENGTH = DefaultParameterDescriptor
                .create(ModisMapProjection.CENTER_LENGTH,
                        "Full size of center data", Integer.class, 0, true);

        static final ParameterDescriptorGroup PARAMETERS = new DefaultParameterDescriptorGroup(
                MODIS_MAP_PROJECTION_GROUP, new ParameterDescriptor[] {
                        CENTER_LATS, CENTER_LONS, CENTER_LENGTH, RESOLUTION,
                        SEMI_MAJOR, SEMI_MINOR, CENTRAL_MERIDIAN, WIDTH,
                        HEIGHT, ENVELOPE });

        public Provider() {
            super(PARAMETERS);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.geotools.referencing.operation.MathTransformProvider#
         * createMathTransform (org.opengis.parameter.ParameterValueGroup)
         */
        @Override
        protected MathTransform createMathTransform(ParameterValueGroup values)
                throws InvalidParameterNameException,
                ParameterNotFoundException, InvalidParameterValueException,
                FactoryException {

            return new ModisMapProjection(values);
        }

        static <T> T getValue(ParameterDescriptor<T> descriptor,
                ParameterValueGroup group) {
            return MathTransformProvider.value(descriptor, group);
        }
    }

    public Point2D getLastPoint() {
        return lastPoint;
    }

}
