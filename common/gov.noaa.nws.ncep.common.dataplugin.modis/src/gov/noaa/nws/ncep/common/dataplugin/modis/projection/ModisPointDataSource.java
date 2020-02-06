package gov.noaa.nws.ncep.common.dataplugin.modis.projection;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

/***
 * Class for loading MODIS lat/lon data into an STRtree.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 08/06/2015   R7270      kbugenhagen  Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisPointDataSource {

    private int width;

    private int height;

    private float[] latitudes;

    private float[] longitudes;

    public ModisPointDataSource(int width, int height, float[] latitudes,
            float[] longitudes) {
        this.width = width;
        this.height = height;
        this.latitudes = latitudes;
        this.longitudes = longitudes;
    }

    private STRtree pointDataSortTree = new STRtree();

    // Specifies distance used to create envelope around point being searched
    // for. Larger DIST value returns far more points during query, slowing down
    // image load. Values < 0.005 won't find enough points.
    private static final double DIST = 0.005;

    /**
     * Container class to hold both lat/lon and i/j values.
     */
    public class PointData {

        private int i;

        private int j;

        private double latitude;

        private double longitude;

        public PointData(int i, int j, double latitude, double longitude) {
            super();
            this.i = i;
            this.j = j;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public int getJ() {
            return j;
        }

        public void setJ(int j) {
            this.j = j;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }

    /**
     * Inserts a point along with its envelope bounding box into the STR tree.
     * 
     * @param pdata
     *            point data to be inserted
     */
    public void insertPoint(PointData pdata) {
        Coordinate coord = new Coordinate(pdata.getLongitude(),
                pdata.getLatitude());
        Envelope env = new Envelope(coord.x - DIST, coord.x + DIST, coord.y
                - DIST, coord.y + DIST);
        pointDataSortTree.insert(env, pdata);
        return;
    }

    /**
     * Loads all lat/lons into the STR tree.
     */
    public void loadData() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int index = i * width + j;
                double lat = latitudes[index];
                double lon = longitudes[index];
                PointData pdata = new PointData(i, j, lat, lon);
                insertPoint(pdata);
            }
        }
    }

    /**
     * Find the closest pixel (i,j) to lat/lon location.
     * 
     * @param loc
     *            The lat/lon location
     * @return the closest pixel (i,j)
     */
    public Point2D getNearestPoint(Point2D loc) {
        List<PointData> points = new ArrayList<PointData>();
        PointData[] nearest = new PointData[3];
        Envelope searchEnv;
        double searchRange = DIST;
        int nearestIndex = 0;

        // Query to get at least two points to interpolate. Double the search
        // range until at least two are found.
        do {
            searchRange *= 2;
            searchEnv = new Envelope(loc.getX() - searchRange, loc.getX()
                    + searchRange, loc.getY() - searchRange, loc.getY()
                    + searchRange);

            points = pointDataSortTree.query(searchEnv);

        } while (points.size() < 2);

        // Compute distance (in meters) of the nearest point
        Point2D point = new Point2D.Double();
        int smallestDist = Integer.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            GeodeticCalculator gc = new GeodeticCalculator(
                    DefaultEllipsoid.WGS84);
            PointData pointData = points.get(i);
            gc.setStartingGeographicPoint(pointData.getLongitude(),
                    pointData.getLatitude());
            gc.setDestinationGeographicPoint(loc.getX(), loc.getY());
            double dist = gc.getOrthodromicDistance();
            // Keep track of the shortest distance, rotating through the array
            // of nearest points if we exceed the nearest array size.
            if (dist < smallestDist) {
                smallestDist = (int) dist;
                point = new Point2D.Double();
                point.setLocation(pointData.getJ(), pointData.getI());
                if (nearestIndex > (nearest.length - 1)) {
                    nearestIndex = 0;
                }
                nearest[nearestIndex] = pointData;
                nearestIndex++;
            }
        }
        if (nearestIndex < (nearest.length - 1)) {
            for (int i = nearestIndex; i < (nearest.length); i++) {
                if (i == nearest.length - 1) {
                    nearest[i] = nearest[0];
                } else {
                    nearest[i] = nearest[i + 1];
                }
            }
        }

        // interpolate nearest points to get pixel point (i, j)
        point = interpolatePointFromLatLons(loc, nearest);

        return point;
    }

    /**
     * Interpolate the nearest points to the location to derive the i,j values
     * for the closest pixel point.
     * 
     * @param loc
     *            The lat/lon location
     * @param nearest
     *            Array of points nearest loc
     * @return The closest pixel (i,j) point
     */
    private Point2D interpolatePointFromLatLons(Point2D loc, PointData[] nearest) {
        Point2D point = new Point2D.Double();

        int index = 1;
        if (nearest[nearest.length - 1] == null) {
            index = 0;
        }
        PointData p1 = nearest[index];
        PointData p2 = nearest[index + 1];
        // if we only have one point, use that
        if (p1 == null) {
            point.setLocation(p2.j, p2.i);
            return point;
        }
        /*
         * 
         */
        double j = ((loc.getX() - p2.getLongitude()) / -(p1.getLongitude() - p2
                .getLongitude())) * (p1.getJ() - p2.getJ()) + p2.getJ();
        double i = ((loc.getY() - p2.getLatitude()) / -(p1.getLatitude() - p2
                .getLatitude())) * (p1.getI() - p2.getI()) + p2.getI();
        point.setLocation(j, i);

        return point;
    }

}
