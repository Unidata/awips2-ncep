/*
 * gov.noaa.nws.ncep.edex.util.McidasCRSBuilder
 * 
 * March 2014
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.edex.util;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.geospatial.MapUtil;

/**
 * Class to construct CRS for mcidas.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 03/2014      TTR957       B. Yin     Moved from McidasSpatialFactory.
 * Nov 04, 2015 10436        njensen    Fixed nav_block_pattern to support multiline matches
 *                                       Moved to gov.noaa.nws.ncep.common.dataplugin.mcidas
 * 
 * </pre>
 * 
 * @author byin
 */

public class McidasCRSBuilder {

    protected static final Pattern AREA_PATTERN = Pattern
            .compile("PROJCS\\[\"MCIDAS\\sAREA\\s(.*)\"");

    protected static final Pattern NAV_BLOCK_PATTERN = Pattern.compile(
            "\\[\"NAV_BLOCK_BASE64\",\\s\"(.*)\"\\]", Pattern.MULTILINE
                    | Pattern.DOTALL);

    private static final Logger log = LoggerFactory
            .getLogger(McidasCRSBuilder.class);

    public static ProjectedCRS constructCRSfromWKT(String crsWKT) {
        Matcher m = AREA_PATTERN.matcher(crsWKT);
        m.find();
        ProjectedCRS crsObject = null;

        if (m.groupCount() == 1) {
            String type = m.group(1);
            m = NAV_BLOCK_PATTERN.matcher(crsWKT);
            boolean found = m.find();
            if (found) {
                String navBlock = m.group(1);
                crsObject = McidasCRSBuilder.constructCRS(type, navBlock);
            }
        }

        return crsObject;
    }

    public static ProjectedCRS constructCRS(String type, String encoded) {
        ParameterValueGroup pvg = null;

        DefaultMathTransformFactory dmtFactory = new DefaultMathTransformFactory();
        try {
            pvg = dmtFactory.getDefaultParameters("MCIDAS_AREA_NAV");
        } catch (NoSuchIdentifierException e1) {
            e1.printStackTrace();
        }

        /*
         * semi_major and semi_minor parameters are set to 1, so that no global
         * scaling is performed during coordinate transforms by
         * org.geotools.referencing.operation.projection.MapProjection based on
         * the radius of earth
         */
        pvg.parameter("semi_major").setValue(1.0);
        pvg.parameter("semi_minor").setValue(1.0);
        pvg.parameter("central_meridian").setValue(0.0);
        pvg.parameter("NAV_BLOCK_BASE64").setValue(encoded);

        String projectionName = "MCIDAS AREA " + type;
        ProjectedCRS mcidasCRS = null;
        try {
            mcidasCRS = MapUtil.constructProjection(projectionName, pvg);
        } catch (FactoryException e) {
            log.error("Error constructing Mcidas CRS", e);
        }

        return mcidasCRS;
    }
}
