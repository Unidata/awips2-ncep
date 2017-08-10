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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.coverage.grid.GridGeometry;

import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.grid.display.GridCellLocationCache;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * Caches and optimizes {@link GridProgressiveDisclosure}. This will use a
 * shared instance of the disclosure whenever possible, most frequently it will
 * share on all frames of a loop. This attempts to balance responsiveness with
 * optimization. A background job is used to continuously optimize the
 * disclosure, the more a disclosure is used the more time that will be spent
 * ensuring it performs well.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 22, 2016            bsteffen     Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class GridProgressiveDisclosureCache {

    private static final int INITIAL_LOAD = 1000;

    private static Map<MultiKey, Reference<GridProgressiveDisclosureJob>> cache = new HashMap<>();

    public static synchronized ProgressiveDisclosure getDisclosure(
            GridGeometry sourceGeom,
            GridGeometry targetGeom) {
        GridGeometry2D sourceGeom2D = GridGeometry2D.wrap(sourceGeom);
        GridGeometry2D targetGeom2D = GridGeometry2D.wrap(targetGeom);
        MultiKey key = new MultiKey(sourceGeom2D, targetGeom2D);
        GridProgressiveDisclosureJob disclosure = null;
        Reference<GridProgressiveDisclosureJob> disclosureRef = cache.get(key);
        if (disclosureRef != null) {
            disclosure = disclosureRef.get();
        }
        if (disclosure == null) {
            GridEnvelope2D gridRange = sourceGeom2D.getGridRange2D();
            float[] plotLocations = GridCellLocationCache.getInstance()
                    .getPlotLocations(sourceGeom2D, targetGeom2D);
            disclosure = new GridProgressiveDisclosureJob(gridRange.width,
                    gridRange.height, plotLocations);
            cache.put(key, new SoftReference<>(disclosure));
        }
        disclosure.schedule();
        return disclosure;
    }

    /**
     * An interface for a progressive disclosure, this allows the cache to
     * provide a stable API if the internal data structures are modified.
     */
    public static interface ProgressiveDisclosure {
        public Collection<Coordinate> runDisclosure(IExtent extent,
                double minDistance);
    }

    private static class GridProgressiveDisclosureJob extends Job implements
            ProgressiveDisclosure {

        private final GridProgressiveDisclosure disclosure;

        private final int numPoints;

        /**
         * Track whether optimization is complete so the job doesn't get
         * scheduled when it is not necessary.
         */
        private boolean complete = false;

        /**
         * How long to spend optimizing this disclosure in the background
         * thread. This is set to however long it takes to run the disclosure so
         * that when it is slow to run it will be optimized alot and faster next
         * time.
         */
        private AtomicInteger disclosureEffort = new AtomicInteger(INITIAL_LOAD);

        public GridProgressiveDisclosureJob(int nx, int ny,
                float[] plotLocations) {
            super("Grid Progressive Disclosure Optimization");
            disclosure = new GridProgressiveDisclosure(nx, ny,
                    plotLocations);
            numPoints = nx * ny;
            setSystem(true);
            disclosure.refineAbsoluteDisclosure(INITIAL_LOAD);
        }

        @Override
        public boolean shouldSchedule() {
            return !complete;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            int effort = disclosureEffort.getAndAdd(-200);
            /*
             * Run for at least 50 ms but not more than 200ms because shorter
             * runs don't accomplish much and longer runs will lock out other
             * threads for too long.
             */
            if (effort < 50) {
                runDisclosure(50);
            } else if (effort < 200) {
                runDisclosure(effort);
            } else {
                runDisclosure(200);
                schedule(10);
            }
            return Status.OK_STATUS;
        }

        /**
         * Convert a normalized effort value into the number of points to
         * disclose. This is necessary because
         * {@link GridProgressiveDisclosure#refineAbsoluteDisclosure(int)}
         * performs much differently for different sized grids and this class
         * would like to have predictable consistent performance.
         * 
         * This scales the effort value based on an average grid size(512x512)
         * to determine how many points can be disclosed with the same amount of
         * CPU time. This is a rough approximation so there will be some
         * variability but it will be much more consistent than using the same
         * number of points. Generally each point on an average sized grid takes
         * about 1ms to disclose so the effort value is roughly equivalent to
         * the number of ms to spend running the disclosure.
         * 
         * @param effortValue
         *            a base value for how much time/effort should spent
         *            disclosing points.
         * 
         * @return the number of points to disclose.
         */
        private int convertEffort(int effortValue) {
            double ratio = (512.0 * 512.0) / numPoints;
            return (int) Math.ceil(effortValue * ratio);
        }

        private void runDisclosure(int effortValue) {
            complete = disclosure
                    .refineAbsoluteDisclosure(convertEffort(effortValue));
        }

        @Override
        public Collection<Coordinate> runDisclosure(IExtent extent,
                double minDistance) {
            long startTime = System.currentTimeMillis();
            Collection<Coordinate> result = disclosure.runDisclosure(extent,
                    minDistance);
            long endTime = System.currentTimeMillis();
            disclosureEffort.lazySet((int) (endTime - startTime));
            schedule();
            return result;
        }

    }

}
