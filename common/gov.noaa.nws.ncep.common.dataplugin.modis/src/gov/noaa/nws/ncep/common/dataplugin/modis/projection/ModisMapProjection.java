/**
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/

package gov.noaa.nws.ncep.common.dataplugin.modis.projection;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.TreeMap;

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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Modis map projection
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Oct 01, 2014            kbugenhagen  Initial creation
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisMapProjection extends MapProjection {

    private static final long serialVersionUID = -7936272226439544019L;

    public static final String Modis_MAP_PROJECTION_GROUP = "Modis_CENTER_MAPPING";

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

    private final float[] latitudes;

    private final float[] longitudes;

    private final Geometry envelope;

    private final double resolution;

    private final int actualHeight;

    private final int width;

    private final int height;

    private Point2D lastPoint;

    private final TreeMap<String, Point2D> latLons;

    private void logDuration(long startTime, String method) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        if (duration > 0) {
            System.out.println(method + " took: " + duration + " ms");
        }
    }

    // TODO: Write a more efficient method. This takes a LONG time to run.

    private final TreeMap<String, Point2D> createLatLons() {
        TreeMap<String, Point2D> map = new TreeMap<String, Point2D>();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Point2D point = new Point2D.Double();
                point.setLocation(j, i);
                int index = i * width + j;
                String latLon = new String(latLonToString(latitudes[index],
                        longitudes[index]));
                map.put(latLon, point);
            }
        }
        return map;
    }

    private String latLonToString(float lat, float lon) {
        String latitude = String.valueOf(new BigDecimal(lat).setScale(4,
                RoundingMode.HALF_UP));
        String longitude = String.valueOf(new BigDecimal(lon).setScale(4,
                RoundingMode.HALF_UP));
        StringBuilder builder = new StringBuilder();
        builder.append(latitude);
        builder.append("/");
        builder.append(longitude);
        return builder.toString();
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
        this.latLons = createLatLons();
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

        Point2D point = ptDst != null ? ptDst : new Point2D.Double();

        String latLon = latLonToString((float) Math.toDegrees(aLatR),
                (float) Math.toDegrees(aLonR));

        Coordinate[] coords = envelope.getCoordinates();

        // if lat/lon not within envelope, return last good point found
        if (!contains(coords, aLonR, aLatR)) {
            return lastPoint == null ? point : lastPoint;
        }

        Point2D lookupPoint = latLons.get(latLon);
        if (lookupPoint != null) {
            lastPoint = point;
        } else {
            lookupPoint = findClosestPoint(latLon, aLonR);
        }

        double x = lookupPoint.getX();
        double y = lookupPoint.getY();

        point.setLocation(x, y);

        // System.out.println("transform: returning point: " + point.getX() +
        // ", "
        // + point.getY() + " from lat/lon " + latLon);

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

    private Point2D findClosestPoint(String latLon, double aLonR) {
        String lat = latLon.split("/")[0];
        double aLonD = Math.toDegrees(aLonR);
        double smallestDiff = 10000.0;
        String closestLatLon = null;
        Point2D point = null;

        // search subset of lat/lon map starting at lat passed in
        for (String ll : latLons.tailMap(lat).keySet()) {
            String[] llParts = ll.split("/");
            String latLookup = llParts[0];
            String lonLookup = llParts[1];
            if (lat.equals(latLookup)) {
                double lonCheck = Double.valueOf(lonLookup);
                double diff = Math.abs(lonCheck - aLonD);
                if (diff < smallestDiff) {
                    smallestDiff = diff;
                    closestLatLon = ll;
                }
            } else {
                // map is sorted so we've looked at all the latitudes of
                // interest
                if (closestLatLon != null) {
                    point = latLons.get(closestLatLon);
                    lastPoint = point;
                } else {
                    point = lastPoint;
                }
                break;
            }
        }
        if (point == null) {
            point = lastPoint;
        }

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
            Point2D ptDst) throws ProjectionException {

        Coordinate latLon = new Coordinate();
        double xi = x / resolution - 1;
        double yi = y / resolution - 1;
        Point2D point = ptDst != null ? ptDst : new Point2D.Double();

        if (yi < 0.0) {
            yi = 0.0;
        }
        if (xi < 0.0) {
            xi = 0.0;
        }
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

        // System.out.println("inverse transform: returning point = " + latLon
        // + "from pixel xi, yi = " + xi + ", " + yi);

        return point;
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

    public TreeMap<String, Point2D> getLatLons() {
        return latLons;
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
                Modis_MAP_PROJECTION_GROUP, new ParameterDescriptor[] {
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

}
