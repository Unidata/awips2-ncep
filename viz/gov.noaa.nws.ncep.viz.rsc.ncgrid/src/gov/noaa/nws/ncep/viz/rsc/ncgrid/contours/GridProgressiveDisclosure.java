/**
 * This software was developed and / or modified by Raytheon Company,
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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

import org.geotools.coverage.grid.GridEnvelope2D;

import com.raytheon.uf.viz.core.IExtent;
import org.locationtech.jts.geom.Coordinate;

/**
 * An algorithm for choosing which grid points to display. This algorithm is
 * designed to work well even when the reprojection of the grid is introducing
 * significant distortion. This calculates the distance between grid points in
 * the reprojected display and allows the caller to choose which points are
 * displayed by providing a display area and a minimum distance between points.
 * Display points are chosen that are within the area so that all displayed
 * points are at least the minimum distance form any other point chosen.
 * 
 * The algorithm is implemented by first choosing an arbitrary grid point as the
 * starting point and then finding the next grid point that is the farthest from
 * that point. This is repeated, each time choosing the grid point that is
 * farthest from all other grid points that have already been chosen. This can
 * continue until all points are included or until the farthest point is closer
 * than the desired minimum, at which point the algorithm has found the desired
 * set and will return. The progressive disclosure is intended to be flexible
 * for a variety of areas and "zoom" levels. In general as a user zooms in on an
 * area the displayed area shrinks but the minimum distance between points
 * decreases to allow more points to be displayed.
 * 
 * To balance different performance characteristics the same algorithm is
 * applied in 2 different ways. The first application of the algorithm is
 * applied to the entire grid and this is called the absolute disclosure. This
 * application is consistent for any area that is being displayed so the
 * disclosure can be calculated once and the results can be reused over any
 * area. This is achieved by saving the distance of each point when it is added
 * to the disclosure. By filtering the points based of the area and the distance
 * value the disclosure can be run again in a single iteration over the points
 * without the need for any more distance calculations. The problem is that when
 * only a small area is displayed the algorithm is calculating distances for the
 * entire grid and it can take a long time to find points that are within a
 * specific area.
 * 
 * To handle smaller areas a second application of the algorithm is provided
 * that only considers the points that are within the current area, this is
 * called the relative disclosure. For small areas only a small number of grid
 * points must be considered so this algorithm can run more quickly. The problem
 * is that these results are only valid for a specific area so even a small
 * change to the area requires running the entire algorithm again. This
 * algorithm also gets much slower as the requested area increases because more
 * grid points must be considered. Using this algorithm alone can also produce a
 * sense of grid movement if the displayed area is continuously changing. The
 * changed area can result in a completely new set of points being selected
 * which creates a visual effect that makes it seem like the grid is moving
 * along with the display area.
 * 
 * To balance the performance characteristics a combination of both the absolute
 * and relative disclosure should be used. The absolute disclosure should always
 * be run for long enough to handle a fully zoomed out display. This is the
 * worst case scenario for the relative disclosure so by using the absolute
 * disclosure we can perform the work of calculating distances once and reuse
 * the results whenever the area changes, avoiding any worst case performance.
 * 
 * Once the display is zoomed in far enough that the absolute disclosure is not
 * showing enough points then the relative disclosure can be run, first starting
 * with the points from the absolute disclosure and then adding in more points
 * to reach the desired minimum distance. When the display is zoomed out then
 * most of the points will be contributed by the absolute disclosure, there will
 * be a lot of points for the relative disclosure to consider but it still runs
 * quickly since it only needs to find a few points. As the display becomes more
 * zoomed in, the absolute algorithm can contribute fewer points but the total
 * number of grid points that need to be considered decreases so the relative
 * algorithm can find more points quicker.
 * 
 * The combination algorithm also helps to make the relative algorithm more
 * consistent. Since the absolute algorithm is contributing the starting points,
 * these points serve as an anchor to the relative algorithm and the effect of
 * points moving and jumping is greatly reduced.
 * 
 * For a grid that is being displayed for a long period of time across many
 * different areas, it is usually worthwhile to spend more time generating the
 * absolute disclosure so that each new area can be displayed faster. To help
 * support this the absolute algorithm was designed to be run incrementally and
 * can continue to run more distance calculation even while the relative
 * algorithm is being used. To quickly generate an initial display, it is
 * recommended to do a minimal amount of work for the absolute disclosure and
 * rely more on the relative disclosure and then continuing to refine the
 * absolute disclosure in the background so that eventually the absolute
 * disclosure can always be used which gives best performance.
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Aug 22, 2016  R15955   bsteffen    Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class GridProgressiveDisclosure {

    private final int nx;

    private final int ny;

    private final float[] plotLocations;

    private final float[] distances;

    private final BitSet included;

    /*
     * The index of the point with the farthest distance from the currently
     * included point. This is the entry point for refineAbsoluteDisclosure, and
     * it is updated each time through the loop.
     */
    private int nextFarthest = -1;

    public GridProgressiveDisclosure(int nx, int ny, float[] plotLocations) {
        this.nx = nx;
        this.ny = ny;
        this.plotLocations = plotLocations;
        this.distances = new float[nx * ny];
        Arrays.fill(this.distances, Float.POSITIVE_INFINITY);
        this.included = new BitSet(nx * ny);

        /*
         * Start disclosure from the center, unless center is invalid, then
         * start from the first valid point found.
         */
        int center = (nx / 2) * ny + ny / 2;
        float centerx = plotLocations[center * 2];
        float centery = plotLocations[center * 2 + 1];
        if (Float.isNaN(centerx) || Float.isNaN(centery)) {
            for (int i = 0; i < plotLocations.length; i += 2) {
                float x = plotLocations[i];
                float y = plotLocations[i + 1];
                if (!Float.isNaN(x) && !Float.isNaN(y)) {
                    nextFarthest = i;
                    break;
                }
            }
        } else {
            nextFarthest = center;
        }
    }

    /**
     * Run the absolute disclosure algorithm to optimize future calls to
     * {@link #runDisclosure(IExtent, double)}.
     * 
     * @param numPoints
     *            The number of points to disclose
     * @return true if the disclosure is complete and this method does not need
     *         to run ever again, false if there is still more work that could
     *         be done to optimize this disclosure.
     */
    public boolean refineAbsoluteDisclosure(int numPoints) {
        synchronized (distances) {
            while (numPoints > 0 && nextFarthest != -1) {

                Coordinate base = new Coordinate(
                        plotLocations[nextFarthest * 2],
                        plotLocations[nextFarthest * 2 + 1]);
                /*
                 * Bounding box containing all the points that could possibly
                 * need the distance value updated. It saves time to check the
                 * bounds before calculating distance.
                 */
                double minX = base.x - distances[nextFarthest];
                double maxX = base.x + distances[nextFarthest];
                double minY = base.y - distances[nextFarthest];
                double maxY = base.y + distances[nextFarthest];

                int farthest = -1;
                float maxDist = Float.MIN_VALUE;
                /*
                 * Calculate a new dist value for any points that need it and
                 * also find the point with the next biggest dist.
                 */
                for (int i = 0; i < nx * ny; i += 1) {
                    if (included.get(i) || i == nextFarthest) {
                        continue;
                    }
                    float px = plotLocations[i * 2];
                    float py = plotLocations[i * 2 + 1];
                    float d = distances[i];
                    if (px > minX && px < maxX && py > minY && py < maxY) {
                        double dist = base.distance(new Coordinate(px, py));
                        if (dist < d) {
                            d = (float) dist;
                            distances[i] = d;
                        }
                    }
                    if (d > maxDist) {
                        farthest = i;
                        maxDist = d;
                    }
                }
                included.set(nextFarthest);
                nextFarthest = farthest;

                numPoints -= 1;
            }
        }

        return nextFarthest == -1;
    }

    private Collection<Coordinate> runAbsoluteDisclosure(IExtent extent,
            double minDistance) {
        Collection<Coordinate> result = new ArrayList<>();
        for (int y = 0; y < ny; y += 1) {
            for (int x = 0; x < nx; x += 1) {
                int i = (x * ny + y);
                if (distances[i] > minDistance) {
                    float px = plotLocations[i * 2];
                    float py = plotLocations[i * 2 + 1];
                    if (extent.contains(new double[] { px, py })) {
                        result.add(new Coordinate(x, y));
                    }
                }
            }
        }
        return result;
    }

    private Collection<Coordinate> runRelativeDisclosure(double minDistance,
            float[] distances, GridEnvelope2D gridRange, int nextFarthest) {
        float biggestDist = distances[nextFarthest];
        Collection<Coordinate> result = new ArrayList<>();

        while (biggestDist > minDistance && nextFarthest != -1) {
            distances[nextFarthest] = 0;
            result.add(new Coordinate(nextFarthest / ny, nextFarthest % ny));
            Coordinate base = new Coordinate(plotLocations[nextFarthest * 2],
                    plotLocations[nextFarthest * 2 + 1]);

            double minX = base.x - biggestDist;
            double maxX = base.x + biggestDist;
            double minY = base.y - biggestDist;
            double maxY = base.y + biggestDist;

            nextFarthest = -1;
            biggestDist = Float.NEGATIVE_INFINITY;
            for (int y = gridRange.y; y <= gridRange.getMaxY(); y += 1) {
                for (int x = gridRange.x; x <= gridRange.getMaxX(); x += 1) {
                    int i = (x * ny + y);
                    if (distances[i] < minDistance) {
                        continue;
                    }
                    float px = plotLocations[i * 2];
                    float py = plotLocations[i * 2 + 1];
                    float d = distances[i];
                    if (px > minX && px < maxX && py > minY && py < maxY) {
                        double dist = base.distance(new Coordinate(px, py));
                        if (dist < d) {
                            d = (float) dist;
                            distances[i] = d;
                        }
                    }
                    if (d > biggestDist) {
                        biggestDist = d;
                        nextFarthest = i;
                    }
                }
            }
        }
        return result;
    }

    public Collection<Coordinate> runDisclosure(IExtent extent,
            double minDistance) {
        if (nextFarthest == -1 || minDistance > distances[nextFarthest]) {
            return runAbsoluteDisclosure(extent, minDistance);
        }
        Collection<Coordinate> result = new ArrayList<>();

        float[] distances = null;
        GridEnvelope2D gridRange = null;
        int farthest = -1;
        float biggestDist = Float.NEGATIVE_INFINITY;
        /*
         * This will actually run the absolute disclosure while also calculating
         * all the initialization information for the relative disclosure.
         */
        synchronized (this.distances) {
            /*
             * Need a copy of the distance array because new distance values are
             * added but they are only valid within the extent.
             */
            distances = Arrays.copyOf(this.distances, this.distances.length);
            /*
             * This loop does several things.
             * 
             * 1) Finds the farthest point within the extent
             * 
             * 2) Determines what range of grid points fall within the extent.
             * 
             * 3) Sets the distance value to 0 for points that are already
             * included or for points that are outside the extent. Setting to 0
             * allows the points to be ignored without the need to clone
             * included.
             */
            for (int y = 0; y < ny; y += 1) {
                for (int x = 0; x < nx; x += 1) {
                    int i = (x * ny + y);
                    float px = plotLocations[i * 2];
                    float py = plotLocations[i * 2 + 1];
                    if (!extent.contains(new double[] { px, py })) {
                        distances[i] = 0;
                    } else if (included.get(i)) {
                        result.add(new Coordinate(x, y));
                        distances[i] = 0;
                    } else {
                        if (distances[i] > biggestDist) {
                            farthest = i;
                            biggestDist = distances[i];
                        }
                        if (gridRange == null) {
                            gridRange = new GridEnvelope2D(x, y, 0, 0);
                        } else {
                            gridRange.add(x, y);
                        }
                    }
                }
            }
        }
        if (farthest != -1) {
            result.addAll(runRelativeDisclosure(minDistance, distances,
                    gridRange, farthest));
        }

        return result;

    }

}
