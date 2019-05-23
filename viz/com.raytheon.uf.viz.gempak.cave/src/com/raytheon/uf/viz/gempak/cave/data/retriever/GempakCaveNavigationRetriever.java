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
 */
package com.raytheon.uf.viz.gempak.cave.data.retriever;

import org.geotools.coverage.grid.GridGeometry2D;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.util.GridGeometryWrapChecker;
import com.raytheon.uf.common.gridcoverage.Corner;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.gridcoverage.LambertConformalGridCoverage;
import com.raytheon.uf.common.gridcoverage.LatLonGridCoverage;
import com.raytheon.uf.common.gridcoverage.MercatorGridCoverage;
import com.raytheon.uf.common.gridcoverage.PolarStereoGridCoverage;
import com.raytheon.uf.common.gridcoverage.exception.GridCoverageException;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.viz.gempak.common.data.GempakNavigationResponse;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakNavigationRetriever;
import com.raytheon.uf.viz.gempak.common.request.GempakNavigationRequest;

/**
 * Implementation of {@link IGempakNavigationRetriever} for retrieving a GEMPAK
 * grid navigation string from within CAVE.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 25, 2018 54483      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakCaveNavigationRetriever
        implements IGempakNavigationRetriever {

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(
                    GempakCaveNavigationRetriever.class.getSimpleName() + ":");

    @Override
    public GempakNavigationResponse getNavigation(
            GempakNavigationRequest request) {
        long t0 = System.currentTimeMillis();

        ISpatialObject obj = request.getSpatialObject();
        GridCoverage gc;
        try {
            gc = setCoverageForWorldWrap((GridCoverage) obj);
        } catch (GridCoverageException e) {
            gc = (GridCoverage) obj;
        }

        StringBuilder resultsBuilder = new StringBuilder();
        boolean enableFlip = false;

        if (gc instanceof LatLonGridCoverage) {
            /*
             * LatLonGridCoverage
             */
            LatLonGridCoverage llgc = (LatLonGridCoverage) gc;
            resultsBuilder.append("CED"); // 1
            resultsBuilder.append(";");
            resultsBuilder.append(llgc.getNx()); // 2
            resultsBuilder.append(";");
            resultsBuilder.append(llgc.getNy()); // 3
            resultsBuilder.append(";");
            Double dummy;
            double ddx = llgc.getDx() * (llgc.getNx() - 1);
            double ddy = llgc.getDy() * (llgc.getNy() - 1);

            if (llgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                // upper left
                dummy = llgc.getLa1() * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
                dummy = llgc.getLo1() * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
                // lower right
                dummy = (llgc.getLa1() - ddy) * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
                dummy = (llgc.getLo1() + ddx) * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
            } else { // assume there are only two options: UpperLeft and
                     // LowerLeft
                dummy = (llgc.getLa1() + ddy) * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
                dummy = llgc.getLo1() * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");

                dummy = llgc.getLa1() * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
                dummy = (llgc.getLo1() + ddx) * 10_000;
                resultsBuilder.append(dummy.intValue());
                resultsBuilder.append(";");
            }

            dummy = -9999.0;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = llgc.getDx() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = llgc.getDy() * 10_000;
            resultsBuilder.append(dummy.intValue());
        } else if (gc instanceof LambertConformalGridCoverage) {
            resultsBuilder.append("LCC");
            resultsBuilder.append(";");
            LambertConformalGridCoverage lcgc = (LambertConformalGridCoverage) gc;
            if (lcgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                enableFlip = true;
            }
            resultsBuilder.append(lcgc.getNx());
            resultsBuilder.append(";");
            resultsBuilder.append(lcgc.getNy());
            resultsBuilder.append(";");
            Double dummy = lcgc.getLa1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = lcgc.getLo1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = lcgc.getLatin1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = lcgc.getLatin2() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = lcgc.getLov() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = lcgc.getDx() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = lcgc.getDy() * 10_000;
            resultsBuilder.append(dummy.intValue());
        } else if (gc instanceof MercatorGridCoverage) {
            MercatorGridCoverage mgc = (MercatorGridCoverage) gc;
            if (mgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                enableFlip = true;
            }
            resultsBuilder.append("MER");
            resultsBuilder.append(";");
            resultsBuilder.append(mgc.getNx());
            resultsBuilder.append(";");
            resultsBuilder.append(mgc.getNy());
            resultsBuilder.append(";");
            Double dummy = mgc.getLa1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = mgc.getLo1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = mgc.getLatin() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = mgc.getLa2() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = mgc.getLo2() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = mgc.getDx() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = mgc.getDy() * 10_000;
            resultsBuilder.append(dummy.intValue());
        } else if (gc instanceof PolarStereoGridCoverage) {
            /*
             * PolarStereoGridCoverage
             */
            PolarStereoGridCoverage psgc = (PolarStereoGridCoverage) gc;
            if (psgc.getFirstGridPointCorner() == Corner.UpperLeft) {
                enableFlip = true;
            }
            resultsBuilder.append("STR");
            resultsBuilder.append(";");
            resultsBuilder.append(psgc.getNx());
            resultsBuilder.append(";");
            resultsBuilder.append(psgc.getNy());
            resultsBuilder.append(";");
            Double dummy = psgc.getLa1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = psgc.getLo1() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = -9999.0;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = -9999.0;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = psgc.getLov() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = psgc.getDx() * 10_000;
            resultsBuilder.append(dummy.intValue());
            resultsBuilder.append(";");
            dummy = psgc.getDy() * 10_000;
            resultsBuilder.append(dummy.intValue());
        }

        String content = resultsBuilder.toString();
        GempakNavigationResponse response = new GempakNavigationResponse(
                content, enableFlip);

        long t1 = System.currentTimeMillis();
        perfLog.logDuration("Retrieving GEMPAK grid navigation", t1 - t0);

        return response;
    }

    private GridCoverage setCoverageForWorldWrap(GridCoverage orig)
            throws GridCoverageException {
        GridCoverage dataLoc = orig;
        GridGeometry2D dataGeom = dataLoc.getGridGeometry();
        int wrapX = GridGeometryWrapChecker.checkForWrapping(dataGeom);
        if (wrapX != -1) {
            // add one column
            int newX = wrapX + 1;
            if (newX == dataLoc.getNx()) {
                return orig;
            } else if (newX < dataLoc.getNx()) {
                return orig;
            } else {
                GridCoverage requestCoverage;
                if (dataLoc instanceof LatLonGridCoverage) {
                    LatLonGridCoverage newLoc = new LatLonGridCoverage(
                            (LatLonGridCoverage) dataLoc);
                    newLoc.setLa2(0);
                    newLoc.setLo2(0);
                    requestCoverage = newLoc;
                } else if (dataLoc instanceof MercatorGridCoverage) {
                    MercatorGridCoverage newLoc = new MercatorGridCoverage(
                            (MercatorGridCoverage) dataLoc);
                    newLoc.setLa2(null);
                    newLoc.setLo2(null);
                    requestCoverage = newLoc;
                } else if (dataLoc instanceof LambertConformalGridCoverage) {
                    requestCoverage = new LambertConformalGridCoverage(
                            (LambertConformalGridCoverage) dataLoc);
                } else if (dataLoc instanceof PolarStereoGridCoverage) {
                    requestCoverage = new PolarStereoGridCoverage(
                            (PolarStereoGridCoverage) dataLoc);
                } else {
                    throw new GridCoverageException(
                            "Cannot wrap data for projection of type "
                                    + dataLoc.getClass().getName());
                }
                requestCoverage.setNx(newX);
                requestCoverage.setGridGeometry(null);
                requestCoverage.initialize();
                return requestCoverage;
            }
        } else {
            return orig;
        }
    }
}
