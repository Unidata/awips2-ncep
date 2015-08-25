package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;

import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

/**
 * Provides satellite raster rendering support
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#     Engineer    Description
 *  ------------ ----------  ----------- --------------------------
 *  05/24/2010    #281        ghull       Initial creation 
 *  06/07/2012    #717       archana    Added the methods getImageTypeNumber(),
 *                                      getParameterList(), getLocFilePathForImageryStyleRule()
 *                                      Updated getDataUnitsFromRecord() to get the units from the database
 *  11/29/2012    #630       ghull      IGridGeometryProvider
 *  02/13/2015    #R6345     mkean      add areaName, resolution and getRscAttrSetName 
 *                                      to legendStr
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class McidasSatResource extends AbstractSatelliteResource implements
        ICloudHeightCapable, INatlCntrsResource {

    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(McidasSatResource.class);

    protected NcIRPixelToTempConverter pixelToTemperatureConverter = null;

    // McidasMapCoverage coverage = null;

    public McidasSatResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);
        satRscData = data;

        // set the legend from:
        // satellite name, areaName, resolution and resource
        // NOTE: this assumes that the request type of EQUALS
        // (ie only one kind of imageType and satellite name)

        if (satRscData.getMetadataMap().containsKey("satelliteName")
                && satRscData.getMetadataMap().containsKey("areaName")) {

            legendStr = satRscData.getMetadataMap().get("satelliteName")
                    .getConstraintValue()
                    + " "
                    + satRscData.getMetadataMap().get("areaName")
                            .getConstraintValue();

            try {
                // Note: if the value of "resolution" is <= zero,
                // do not include it in the legend string.
                // if the value of "resolution" > 0,
                // add the string 'km' to the resolution to indicate kilometers.

                String tmpRes = satRscData.getMetadataMap().get("resolution")
                        .getConstraintValue();

                if (Double.parseDouble(tmpRes) > 0.0) {
                    legendStr += "_" + tmpRes + "km";
                }
            } catch (NumberFormatException e) {
                statusHandler.handle(Priority.INFO,
                        "NumberFormatException parsing resolution - omitted");
            }

            legendStr += " " + satRscData.getRscAttrSet().getRscAttrSetName();
        }
    }

    public boolean isCloudHeightCompatible() {
        RequestConstraint imgTypeConstraint = satRscData.getMetadataMap().get(
                "imageType");
        if (imgTypeConstraint != null) {
            // TODO : Not sure if we could handle a constraint type like 'IN' or
            // not?
            // The Image may or may not be cloudHeightCompatible if some image
            // types are
            // IR and some aren't.
            if (imgTypeConstraint.getConstraintType() == ConstraintType.EQUALS
                    && imgTypeConstraint.getConstraintValue().equals("IR")) {
                return true;
            }
        }
        return false;
        // can't do this since imageTypes is not set til a record is processed
        // and
        // we need the dataUnits before that.
        // for( String physElmt : getImageTypes() ) {
        // if( !physElmt.equals("IR") ) {
        // return false;
        // }
        // }
    }

    String getImageTypeFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getImageType();
    }

    String getDataUnitsFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getCalType();
    }

    String getCreatingEntityFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getSatelliteName();
    }

    List<String> getParameterList(PluginDataObject pdo) {

        String paramStr = ((McidasRecord) pdo).getSatelliteName() + "_"
                + ((McidasRecord) pdo).getImageType();
        List<String> paramList = new ArrayList<String>(0);
        paramList.add(paramStr);
        return paramList;
    }

    String getLocFilePathForImageryStyleRule() {
        return NcPathConstants.MCIDAS_IMG_STYLE_RULES;
    }

    int getImageTypeNumber(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getImageTypeNumber().intValue();
    }

    @Override
    String getProjectionFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getProjection();
    }

    /*
     * Create GridGeometry base on Satellite image's spatial info (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.viz.rsc.satellite.rsc.AbstractSatelliteResource#
     * createNativeGeometry(com.raytheon.uf.common.dataplugin.PluginDataObject)
     */
    @Override
    public GridGeometry2D createNativeGeometry(PluginDataObject pdo) {

        if (!(pdo instanceof McidasRecord))
            return null;

        McidasRecord satRec = (McidasRecord) pdo;
        McidasMapCoverage coverage = satRec.getCoverage();

        GeneralEnvelope env = new GeneralEnvelope(2);
        env.setCoordinateReferenceSystem(satRec.getCoverage().getCrs());

        int minX = coverage.getUpperLeftElement();
        int maxX = coverage.getUpperLeftElement()
                + (coverage.getNx() * coverage.getElementRes());
        int minY = coverage.getUpperLeftLine()
                + (coverage.getNy() * coverage.getLineRes());
        minY = -minY;
        int maxY = -1 * coverage.getUpperLeftLine();
        env.setRange(0, minX, maxX);
        env.setRange(1, minY, maxY);

        GridGeometry2D mapGeom = new GridGeometry2D(new GeneralGridEnvelope(
                new int[] { 0, 0 }, new int[] { coverage.getNx(),
                        coverage.getNy() }, false), env);

        return mapGeom;
    }
}
