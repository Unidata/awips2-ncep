/**
 * example for cities station table mapping
 * 
 * This java class defines the getters and setters for the 
 *      
 * 
 * HISTORY
 *
 * Date     	Author		Description
 * ------------	----------	-----------	--------------------------
 * 01/2010		Uma Josyula	Initial creation	
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.viz.common.staticPointDataSource;

import java.util.List;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
<pre>
SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * ????          ???            ???     Initial creation.
 * 06/20/2017    R34594     J. Huber    Add status handler, format, and clear Eclipse warnings.
*/

public abstract class AbstractPointDataSource
        implements IStaticPointDataSource {

    private STRtree pointDataSortTree = new STRtree();

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractPointDataSource.class);

    public AbstractPointDataSource() {

    }

    private static final double DIST = 1.0;

    public void insertPoint(LabeledPoint lp) {
        Coordinate c = new Coordinate(lp.getLongitude(), lp.getLatitude());
        Envelope env = new Envelope(c.x - DIST, c.x + DIST, c.y - DIST,
                c.y + DIST);

        pointDataSortTree.insert(env, lp);
        return;
    }

    public PointDirDist calculateNearestPoint2(Coordinate loc)
            throws VizException {

        // Check for invalid Coordinate

        if (loc == null || loc.x > 180.0 || loc.x < -180.0 || loc.y > 90.0
                || loc.y < -90.0) {
            return null;
        }

        List<?> rawPointList = null;
        double searchRange = DIST;

        do {
            searchRange *= 2;

            if (searchRange > 130) {
                break;
            }

            Envelope searchEnv = new Envelope(loc.x - searchRange,
                    loc.x + searchRange, loc.y - searchRange,
                    loc.y + searchRange);
            rawPointList = pointDataSortTree.query(searchEnv);

        } while (rawPointList.isEmpty());

        /*
         * Compute distance (in meter) of the nearest point
         */

        PointDirDist nearestPoint = null;

        for (int i = 0; i < rawPointList.size(); i++) {

            GeodeticCalculator gc = new GeodeticCalculator(
                    DefaultEllipsoid.WGS84);
            LabeledPoint namedPoint = (LabeledPoint) rawPointList.get(i);
            gc.setStartingGeographicPoint(namedPoint.getLongitude(),
                    namedPoint.getLatitude());
            gc.setDestinationGeographicPoint(loc.x, loc.y);

            double dist = 0;
            dist = gc.getOrthodromicDistance();

            if (nearestPoint == null
                    || dist < nearestPoint.getDistanceInMeter()) {

                double direction = gc.getAzimuth();
                if (direction < 0) {
                    direction = 360 + direction;
                }

                nearestPoint = new PointDirDist(namedPoint, (int) (dist + .5),
                        direction);
            }
        }

        return nearestPoint;
    }
}
