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

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.gridcoverage.LambertConformalGridCoverage;
import com.raytheon.uf.common.gridcoverage.LatLonGridCoverage;
import com.raytheon.uf.common.gridcoverage.MercatorGridCoverage;
import com.raytheon.uf.common.gridcoverage.PolarStereoGridCoverage;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.gempak.common.data.retriever.IGempakSubgridCoverageRetriever;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.request.GempakSubgridCoverageRequest;
import com.raytheon.uf.viz.ncep.grid.customCoverage.CustomLambertConformalCoverage;
import com.raytheon.uf.viz.ncep.grid.customCoverage.CustomLatLonCoverage;
import com.raytheon.uf.viz.ncep.grid.customCoverage.CustomMercatorCoverage;
import com.raytheon.uf.viz.ncep.grid.customCoverage.CustomPolarStereoCoverage;

/**
 * Implementation of {@link IGempakSubgridCoverageRetriever} for retrieving
 * GEMPAK subgrid coverage from within CAVE.
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
public class GempakCaveSubgridCoverageRetriever
        implements IGempakSubgridCoverageRetriever {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GempakCaveSubgridCoverageRetriever.class);

    @Override
    public ISpatialObject getSubgridCoverage(
            GempakSubgridCoverageRequest request) throws GempakException {
        String[] gareaStr = request.getSubgGempakFormat().split(";");

        ISpatialObject subgSpatialObj = null;
        if (gareaStr.length >= 7) {
            if ("CED".equalsIgnoreCase(gareaStr[0])) {
                subgSpatialObj = createLatlonISPatialObj(request,
                        Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]),
                        Double.parseDouble(gareaStr[5]),
                        Double.parseDouble(gareaStr[6]));
            } else if ("LCC".equalsIgnoreCase(gareaStr[0])) {
                subgSpatialObj = createLambertConformalISPatialObj(request,
                        Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]));
            } else if ("MER".equalsIgnoreCase(gareaStr[0])) {
                subgSpatialObj = createMercatorISPatialObj(request,
                        Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]),
                        Double.parseDouble(gareaStr[5]),
                        Double.parseDouble(gareaStr[6]));
            } else if ("STR".equalsIgnoreCase(gareaStr[0])) {
                subgSpatialObj = createPolarStereoISPatialObj(request,
                        Integer.parseInt(gareaStr[1]),
                        Integer.parseInt(gareaStr[2]),
                        Double.parseDouble(gareaStr[3]),
                        Double.parseDouble(gareaStr[4]));
            }
        }
        return subgSpatialObj;
    }

    /**
     * Create Latitude/longitude coverage
     *
     * @param request
     * @param nx
     *            - Number of points along a parallel
     * @param ny
     *            - Number of points along a meridian
     * @param la1
     *            - Latitude of first grid point
     * @param lo1
     *            - Longitude of the first grid point
     * @param la2
     *            - Latitude of the last grid point
     * @param lo2
     *            - Longitude of the last grid point
     */
    private ISpatialObject createLatlonISPatialObj(
            GempakSubgridCoverageRequest request, int nx, int ny, double la1,
            double lo1, double la2, double lo2) {
        statusHandler.debug("nx:" + nx + " ny:" + ny + " la1:" + la1 + " lo1:"
                + lo1 + " la2:" + la2 + " lo2:" + lo2);
        CustomLatLonCoverage cv = new CustomLatLonCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);
        cv.setLa2(la2);
        cv.setLo2(lo2);
        GridCoverage gc = (GridCoverage) request.getSpatialObj();

        LatLonGridCoverage llgc = (LatLonGridCoverage) gc;
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            return cv;
        }
        return null;
    }

    /**
     * Create Lambert-Conformal coverage
     *
     * @param request
     * @param nx
     *            - Number of points along the x-axis
     * @param ny
     *            - Number of points along the y-axis
     * @param la1
     *            - Latitude of the first grid point
     * @param lo1
     *            - Longitude of the first grid point
     */
    private ISpatialObject createLambertConformalISPatialObj(
            GempakSubgridCoverageRequest request, int nx, int ny, double la1,
            double lo1) {
        CustomLambertConformalCoverage cv = new CustomLambertConformalCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);

        GridCoverage gc = (GridCoverage) request.getSpatialObj();

        LambertConformalGridCoverage llgc = (LambertConformalGridCoverage) gc;
        cv.setMajorAxis(llgc.getMajorAxis());
        cv.setMinorAxis(llgc.getMinorAxis());
        cv.setLatin1(llgc.getLatin1());
        cv.setLatin2(llgc.getLatin2());
        cv.setLov(llgc.getLov());
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            return cv;
        }
        return null;
    }

    /**
     * Create Mercator coverage
     *
     * @param request
     * @param nx
     *            - Number of points along a parallel
     * @param ny
     *            - Number of points along a meridian
     * @param la1
     *            - Latitude of first grid point
     * @param lo1
     *            - Longitude of the first grid point
     * @param la2
     *            - Latitude of the last grid point
     * @param lo2
     *            - Longitude of the last grid point
     */
    private ISpatialObject createMercatorISPatialObj(
            GempakSubgridCoverageRequest request, int nx, int ny, double la1,
            double lo1, double la2, double lo2) {
        CustomMercatorCoverage cv = new CustomMercatorCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);
        cv.setLa2(la2);
        cv.setLo2(lo2);
        GridCoverage gc = (GridCoverage) request.getSpatialObj();

        MercatorGridCoverage llgc = (MercatorGridCoverage) gc;
        cv.setMajorAxis(llgc.getMajorAxis());
        cv.setMinorAxis(llgc.getMinorAxis());
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            return cv;
        }
        return null;
    }

    /**
     * Create Polar-Stereo coverage
     *
     * @param request
     * @param nx
     *            - Number of points along the x-axis
     * @param ny
     *            - Number of points along the y-axis
     * @param la1
     *            - Latitude of the first grid point
     * @param lo1
     *            - Longitude of the first grid point
     */
    private ISpatialObject createPolarStereoISPatialObj(
            GempakSubgridCoverageRequest request, int nx, int ny, double la1,
            double lo1) {
        CustomPolarStereoCoverage cv = new CustomPolarStereoCoverage();
        cv.setNx(nx);
        cv.setNy(ny);
        cv.setLa1(la1);
        cv.setLo1(lo1);

        GridCoverage gc = (GridCoverage) request.getSpatialObj();

        PolarStereoGridCoverage llgc = (PolarStereoGridCoverage) gc;
        cv.setMajorAxis(llgc.getMajorAxis());
        cv.setMinorAxis(llgc.getMinorAxis());
        cv.setLov(llgc.getLov());
        cv.setLov(llgc.getLov());
        cv.setDx(llgc.getDx());
        cv.setDy(llgc.getDy());
        cv.setSpacingUnit(llgc.getSpacingUnit());
        if (cv.build()) {
            return cv;
        }
        return null;
    }
}
