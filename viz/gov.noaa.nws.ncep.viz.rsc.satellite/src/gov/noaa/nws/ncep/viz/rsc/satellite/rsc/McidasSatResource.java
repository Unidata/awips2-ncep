package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasConstants;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.manager.SatelliteAreaManager;
import gov.noaa.nws.ncep.viz.resources.manager.SatelliteImageTypeManager;
import gov.noaa.nws.ncep.viz.resources.manager.SatelliteNameManager;
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
 *  10/15/2015    #R7190     R. Reynolds  Added support for Mcidas
 *  12/03/2015    R12953     R. Reynolds Modified to enhance Legend title
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

    public McidasSatResource(SatelliteResourceData data, LoadProperties props) {
        super(data, props);
        satRscData = data;
        ResourceName rscName = satRscData.getResourceName();

        try {
            legendStr = "";
            String subtypeParamAlias = "";
            String subtypeParamValue = "";
            String satId = "";

            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();
            ResourceDefinition rscDefn = rscDefnsMngr
                    .getResourceDefinition(rscName.getRscType());
            SatelliteAreaManager satAreaMgr = SatelliteAreaManager
                    .getInstance();
            SatelliteImageTypeManager satImageMngr = SatelliteImageTypeManager
                    .getInstance();

            String[] subtypeParam = rscDefn.getSubTypeGenerator().split(",");

            for (int k = 0; k < subtypeParam.length; k++) {

                subtypeParamValue = subtypeParam[k].toString();

                // subtypeParamAlias could be resolution, sateliteId, areaId or
                // projection
                subtypeParamAlias = satRscData.getMetadataMap()
                        .get(subtypeParamValue).getConstraintValue().toString();
                // projection is already a String, but...
                if (subtypeParamValue
                        .equalsIgnoreCase(McidasConstants.RESOLUTION)) {
                    subtypeParamAlias += "km";

                } else if (subtypeParamValue.toString().equalsIgnoreCase(
                        McidasConstants.AREA_ID)) {
                    // get custom name for areaId as defined in
                    // satelliteAreas.xml
                    subtypeParamAlias = satAreaMgr
                            .getDisplayedName(SatelliteAreaManager.ResourceDefnName
                                    + SatelliteAreaManager.delimiter
                                    + subtypeParamAlias);

                } else if (subtypeParamValue.toString().equalsIgnoreCase(
                        McidasConstants.SATELLITE_ID)) {
                    // get custom name for satelliteId as defined in
                    // satelliteNames.xml
                    satId = subtypeParamAlias;
                    subtypeParamAlias = SatelliteNameManager.getInstance()
                            .getDisplayedNameByID(subtypeParamAlias);

                }

                legendStr += subtypeParamAlias + " ";

            }

            legendStr += satImageMngr.getSelectedAttrName(satId + ":"
                    + satRscData.getRscAttrSet().getRscAttrSetName());

            legendStr.trim();

        } catch (Exception ex) {

            statusHandler.handle(Priority.ERROR,
                    "Error building legend string ", ex.getStackTrace()
                            .toString());

        }

    }

    public boolean isCloudHeightCompatible() {
        RequestConstraint imgTypeConstraint = satRscData.getMetadataMap().get(
                "imageTypeId");
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

    }

    String getImageTypeFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getImageTypeId();
    }

    String getDataUnitsFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getCalType();
    }

    String getCreatingEntityFromRecord(PluginDataObject pdo) {
        return ((McidasRecord) pdo).getSatelliteId();
    }

    List<String> getParameterList(PluginDataObject pdo) {

        String paramStr = ((McidasRecord) pdo).getSatelliteId() + "_"
                + ((McidasRecord) pdo).getImageTypeId();
        List<String> paramList = new ArrayList<String>(0);
        paramList.add(paramStr);
        return paramList;
    }

    String getLocFilePathForImageryStyleRule() {
        return NcPathConstants.MCIDAS_IMG_STYLE_RULES;
    }

    int getImageTypeNumber(PluginDataObject pdo) {
        return Integer.parseInt(((McidasRecord) pdo).getImageTypeId());
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