package gov.noaa.nws.ncep.viz.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.opengis.geometry.Envelope;

import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.geospatial.util.EnvelopeIntersection;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * 
 * Class containing methods for interrogation and projection of satellite
 * resource records (e.g. Modis and Viirs).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 11/30/2015   R13133      kbugenhagen  Initial creation.
 * 06/01/2016   R18511      kbugenhagen  Changes for satellite viz resource 
 *                                       refactoring.
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 * @param <S>
 *            data record
 */
public abstract class AbstractSatelliteRecordData<S extends IPersistable> {

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractSatelliteRecordData.class);

    /** String id to look for satellite-provided data values */
    public static final String SATELLITE_DATA_INTERROGATE_ID = "satelliteDataValue";

    /** Intersection geometry for the target */
    protected List<PreparedGeometry> targetIntersection;

    /** Renderable for the data record */
    protected TileSetRenderable tileSet;

    /** Flag designated if a project call is required next paint */
    protected boolean project;

    protected double resolution;

    protected S record;

    public boolean contains(Geometry geom) {
        if (targetIntersection != null) {
            for (PreparedGeometry pg : targetIntersection) {
                if (pg.contains(geom)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected double interrogate(Coordinate latLon) throws VizException {
        return tileSet.interrogate(latLon);
    }

    public void project() {
        this.project = true;
    }

    protected void projectInternal(GeneralGridGeometry targetGeometry) {
        if (tileSet.getTargetGeometry() != targetGeometry) {
            tileSet.project(targetGeometry);

            try {
                Envelope tileSetEnvelope = tileSet.getTileSetGeometry()
                        .getEnvelope();

                double intersectionFactor = getIntersectionFactor();
                targetIntersection = null;
                Geometry intersection = EnvelopeIntersection
                        .createEnvelopeIntersection(
                                tileSetEnvelope,
                                targetGeometry.getEnvelope(),
                                resolution,
                                (int) (tileSetEnvelope.getSpan(0) / (resolution * intersectionFactor)),
                                (int) (tileSetEnvelope.getSpan(1) / (resolution * intersectionFactor)));
                if (intersection != null) {
                    int numGeoms = intersection.getNumGeometries();
                    targetIntersection = new ArrayList<>(numGeoms);
                    for (int n = 0; n < numGeoms; ++n) {
                        targetIntersection.add(PreparedGeometryFactory
                                .prepare(intersection.getGeometryN(n).buffer(
                                        resolution * intersectionFactor)));
                    }
                }
            } catch (Exception e) {
                statusHandler.handle(
                        Priority.PROBLEM,
                        "Error finding intersection: "
                                + e.getLocalizedMessage(), e);
            }
        }
    }

    public Collection<DrawableImage> getImagesToRender(IGraphicsTarget target,
            PaintProperties paintProps, GeneralGridGeometry targetGeometry)
            throws VizException {

        if (project) {
            projectInternal(targetGeometry);
            project = false;
        }
        if (targetIntersection != null) {
            return tileSet.getImagesToRender(target, paintProps);
        } else {
            return Collections.emptyList();
        }
    }

    public S getRecord() {
        return record;
    }

    public void dispose() {
        if (tileSet != null) {
            tileSet.dispose();
            tileSet = null;
        }
        targetIntersection = null;
    }

    protected abstract double getIntersectionFactor();

}
