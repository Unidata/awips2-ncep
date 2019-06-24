package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.CGAlgorithms;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageWriter;

import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.image.Colormapper;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.ArraysUtil;
import com.raytheon.uf.viz.datacube.DataCubeContainer;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisRecord;

/**
 * Provides methods to generate a geoTiff file for a MODIS satellite resource.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#    Engineer     Description
 *  ------------ --------   -----------  --------------------------
 *  05/17/2016   R18511     kbugenhagen  Initial creation.
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 */

public class ModisSatGeoTiffCreator {

    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(ModisSatGeoTiffCreator.class);

    private ModisRecord record;

    private ColorMapParameters colorMapParameters;

    private String satName;

    public ModisSatGeoTiffCreator(ModisRecord record,
            ColorMapParameters colorMapParameters, String satName) {
        this.record = record;
        this.colorMapParameters = colorMapParameters;
        this.satName = satName;
    }

    /**
     * Create geoTIFF from satellite spatial coverage.
     */
    public void create() {

        Envelope envelope = new Envelope();

        float[] lons = record.getCoverage().getLongitudes();
        float[] lats = record.getCoverage().getLatitudes();
        for (int i = 0; i < lons.length; i += 1) {
            envelope.expandToInclude(lons[i], lats[i]);
        }
        float[] data = null;
        try {
            IDataRecord[] dataRecords = DataCubeContainer.getDataRecord(record,
                    Request.ALL, ModisRecord.getDataSet(0));
            data = (float[]) dataRecords[0].getDataObject();
        } catch (DataCubeException e) {
            statusHandler.warn("Unable to create geotiff");
            e.printStackTrace();
            return;
        }
        int nx = record.getCoverage().getNx();
        int ny = record.getCoverage().getNy();

        /*
         * The decoder flips the lats and lons in order to display the image
         * correctly. Flip them back so the ordinate indices line up with the
         * data indices. Don't want to calculate 2 different indices for each
         * point so just flip now.
         */
        float[] longitudes = Arrays.copyOf(lons, lons.length);
        float[] latitudes = Arrays.copyOf(lats, lats.length);
        ArraysUtil.flipHoriz(longitudes, ny, nx);
        ArraysUtil.flipHoriz(latitudes, ny, nx);

        int[] dimensions = { 2048, 2048 };
        double dx = envelope.getWidth() / dimensions[0];
        double dy = envelope.getHeight() / dimensions[1];

        float[] grid = new float[dimensions[0] * dimensions[1]];
        Arrays.fill(grid, Float.NaN);

        /* Iterate each pixel of the source image */
        for (int j = 1; j < ny; j += 1) {
            for (int i = 1; i < nx; i += 1) {
                int i0 = (j - 1) * nx + (i - 1);
                int i1 = j * nx + (i - 1);
                int i2 = (j - 1) * nx + i;
                int i3 = j * nx + i;

                Coordinate c0 = new Coordinate(longitudes[i0], latitudes[i0]);
                Coordinate c1 = new Coordinate(longitudes[i1], latitudes[i1]);
                Coordinate c2 = new Coordinate(longitudes[i2], latitudes[i2]);
                Coordinate c3 = new Coordinate(longitudes[i3], latitudes[i3]);

                Envelope cellEnv = new Envelope(
                        c0);
                cellEnv.expandToInclude(c1);
                cellEnv.expandToInclude(c2);
                cellEnv.expandToInclude(c3);

                int minX = (int) ((cellEnv.getMinX() - envelope.getMinX()) / dx);
                int maxX = (int) ((cellEnv.getMaxX() - envelope.getMinX()) / dx);
                int minY = (int) ((cellEnv.getMinY() - envelope.getMinY()) / dy);
                int maxY = (int) ((cellEnv.getMaxY() - envelope.getMinY()) / dy);
                /*
                 * Iterate each target pixel that is in the envelope of the
                 * source pixel.
                 */
                for (int ic = minX; ic <= maxX; ic += 1) {
                    double x = envelope.getMinX() + ic * dx;
                    for (int jc = minY; jc <= maxY; jc += 1) {
                        double y = envelope.getMinY() + jc * dy;
                        Coordinate t = new Coordinate(x, y);
                        if (isInCell(t, c0, c1, c2, c3)) {
                            /*
                             * This is doing bilinear interpolation, it could
                             * easily be switched to nearest neighbor. This data
                             * seems particularly full of static so bilinear
                             * softens the data and nearest neighbor might be
                             * better. Even better yet might be if we do a
                             * better no data value filtering for bilinear or
                             * perhaps some type of maxVal selection .
                             */
                            double d0 = c0.distance(t);
                            double d1 = c1.distance(t);
                            double d2 = c2.distance(t);
                            double d3 = c3.distance(t);
                            double dt = d0 + d1 + d2 + d3;

                            double v0 = (d0 / dt) * data[i0];
                            double v1 = (d1 / dt) * data[i1];
                            double v2 = (d2 / dt) * data[i2];
                            double v3 = (d3 / dt) * data[i3];

                            /*
                             * NOTE: In LonLat space down is smaller values but
                             * in the image space up is smaller values so this
                             * index operation is performing a inversion along
                             * the y axis.
                             */
                            int dataIndex = (dimensions[1] - jc - 1)
                                    * dimensions[0] + ic;
                            if (dataIndex < 0
                                    || dataIndex >= (dimensions[0] * dimensions[1])) {
                                statusHandler.error("dataIndex: " + dataIndex
                                        + " is out of bounds for granule "
                                        + satName);
                            } else {
                                grid[dataIndex] = (float) (v0 + v1 + v2 + v3);
                            }
                        }
                    }
                }
            }
        }

        ColorMapData colorMapData = new ColorMapData(FloatBuffer.wrap(grid),
                dimensions);
        RenderedImage image = Colormapper.colorMap(colorMapData,
                colorMapParameters);
        GridCoverageFactory factory = new GridCoverageFactory();
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage coverage = factory.create("modis.tif", image,
                new ReferencedEnvelope(envelope, DefaultGeographicCRS.WGS84));
        try {
            File tiffFile = new File("/tmp/" + satName + ".tiff");
            GridCoverageWriter writer = format.getWriter(tiffFile);
            writer.write(coverage, null);
            writer.dispose();
        } catch (IllegalArgumentException | IOException e) {
            statusHandler.warn("Unable to create geotiff");
            e.printStackTrace();
            return;
        }

    }

    /**
     * Custom method for determining if a point lies within a quadrilateral.
     * This only works for convex quadrilaterals but I've never seen an image
     * with a concave pixel so it seems good enough . This method is 10x faster
     * than doing a Polygon.contains(), I'm sure we lose some robustness for the
     * extra speed but it seems to work well for now. The vertices are connected
     * as shown below, the important thing to note is that v1 is not connected
     * to v2, if you accidentally try to connect them then its no longer convex
     * and the whole method falls apart.
     * 
     * <pre>
     * v0---v1
     *  |   |
     *  |   |
     *  |   |
     * v2---v3
     * </pre>
     * 
     * @param p
     *            The point
     * @param v0
     *            Upper left cell boundary
     * @param v1
     *            Upper right cell boundary
     * @param v2
     *            Lower left cell boundary
     * @param v3
     *            Lower right cell boundary
     * @return
     */
    protected boolean isInCell(Coordinate p, Coordinate v0, Coordinate v1,
            Coordinate v2, Coordinate v3) {
        int orientation0 = CGAlgorithms.orientationIndex(v0, v1, p);
        if (orientation0 == 0) {
            return true;
        }
        int orientation1 = CGAlgorithms.orientationIndex(v1, v3, p);
        if (orientation0 != orientation1) {
            if (orientation1 == 0) {
                return true;
            } else {
                return false;
            }
        }
        int orientation2 = CGAlgorithms.orientationIndex(v3, v2, p);
        if (orientation0 != orientation2) {
            if (orientation2 == 0) {
                return true;
            } else {
                return false;
            }
        }
        int orientation3 = CGAlgorithms.orientationIndex(v2, v0, p);
        if (orientation0 != orientation3) {
            if (orientation3 == 0) {
                return true;
            } else {
                return false;
            }
        }
        return true;

    }
}
