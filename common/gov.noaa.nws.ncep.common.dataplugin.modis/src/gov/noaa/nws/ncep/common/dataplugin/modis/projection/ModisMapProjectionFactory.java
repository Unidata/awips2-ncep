package gov.noaa.nws.ncep.common.dataplugin.modis.projection;

import gov.noaa.nws.ncep.common.dataplugin.modis.ModisSpatialCoverage;

import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;

import com.raytheon.uf.common.geospatial.MapUtil;

/**
 * Factory for constructing CRSs for the MODIS projection.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/01/2014   R5116      kbugenhagen  Initial creation.
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisMapProjectionFactory {

    /** Using single factory is faster due to internal caching */
    private static DefaultMathTransformFactory dmtFactory = new DefaultMathTransformFactory();

    public static ProjectedCRS construct(ModisSpatialCoverage record,
            Object latitudes, Object longitudes) throws FactoryException {
        try {
            ParameterValueGroup group = dmtFactory
                    .getDefaultParameters(ModisMapProjection.MODIS_MAP_PROJECTION_GROUP);
            group.parameter(ModisMapProjection.CENTER_LATITUDES).setValue(
                    (float[]) latitudes);
            group.parameter(ModisMapProjection.CENTER_LONGITUDES).setValue(
                    (float[]) longitudes);
            group.parameter(ModisMapProjection.WIDTH).setValue(record.getNx());
            group.parameter(ModisMapProjection.HEIGHT).setValue(record.getNy());
            group.parameter(ModisMapProjection.CENTER_LENGTH).setValue(
                    record.getNy());
            group.parameter(ModisMapProjection.RESOLUTION).setValue(
                    record.getDy().doubleValue());
            group.parameter(ModisMapProjection.ENVELOPE).setValue(
                    record.getEnvelope());
            group.parameter(ModisMapProjection.SEMI_MAJOR).setValue(1.0);
            group.parameter(ModisMapProjection.SEMI_MINOR).setValue(1.0);
            return MapUtil.constructProjection(
                    ModisMapProjection.PROJECTION_NAME, group);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FactoryException(
                    "Error constructing CRS for spatial record: "
                            + e.getLocalizedMessage(), e);
        }
    }

}
