package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasConstants;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.manager.AttributeSet;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefinition;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceName;
import gov.noaa.nws.ncep.viz.resources.util.VariableSubstitutorNCEP;
import gov.noaa.nws.ncep.viz.rsc.satellite.units.NcIRPixelToTempConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.raytheon.viz.satellite.SatelliteConstants;

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
 *  06/07/2012    #717       archana      Added the methods getImageTypeNumber(),
 *                                        getParameterList(), getLocFilePathForImageryStyleRule()
 *                                        Updated getDataUnitsFromRecord() to get the units from the database
 *  11/29/2012    #630       ghull        IGridGeometryProvider
 *  02/13/2015    #R6345     mkean        add areaName, resolution and getRscAttrSetName 
 *                                        to legendStr
 *  10/15/2015    #R7190     R. Reynolds  Added support for Mcidas
 *  12/03/2015    R12953     R. Reynolds  Modified to enhance Legend title
 *  02/04/2016    R14142     RCReynolds   Moved mcidas related string construction out to ResourceDefinition
 *  04/12/2016    R15945     RCReynolds   Added code to build customized string for input to getLegendString
 *  06/06/2016    R15945     RCReynolds   Used McidasConstants instead of SatelliteConstants
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

        /**
         * Get configuredLegendString (with aliases) from resourceDefinition. If
         * empty string is returned then get regular aliased legend.
         * 
         */

        satRscData = data;
        ResourceName rscName = satRscData.getResourceName();

        try {

            legendStr = "";
            StringBuffer sb = new StringBuffer("");
            char x;

            ResourceDefnsMngr rscDefnsMngr = ResourceDefnsMngr.getInstance();

            ResourceDefinition rscDefn = rscDefnsMngr
                    .getResourceDefinition(rscName.getRscType());

            AttributeSet attributeSet = rscDefnsMngr.getAttrSet(rscName);

            rscDefn.setAttributeSet(attributeSet);

            HashMap<String, String> attributes = attributeSet.getAttributes();

            Map<String, String> variables = new HashMap<String, String>();

            String legendStringAttribute = attributes.get("legendString");

            String[] subtypeParam = rscDefn.getSubTypeGenerator().split(",");

            for (int k = 0; k < subtypeParam.length; k++) {
                legendStr += satRscData.getMetadataMap()
                        .get(subtypeParam[k].toString()).getConstraintValue()
                        .toString()
                        + "_";
            }

            legendStr = rscDefn.getRscGroupDisplayName(legendStr) + " ";

            String[] keysValues = rscDefn.getMcidasAliasedValues().split(","); // what's
                                                                               // in
                                                                               // subType
                                                                               // generators
            /**
             * These are the keywords found in the legendString (built from
             * subType generator content)
             */
            String value = "";
            for (int i = 0; i < keysValues.length; i++) {
                value = keysValues[i].split(":")[1];
                if (value != null && !value.isEmpty()) {
                    if (keysValues[i].startsWith(McidasConstants.SATELLLITE
                            + ":")) {
                        variables.put(McidasConstants.SATELLLITE, value);
                    } else if (keysValues[i].startsWith(McidasConstants.AREA
                            + ":")) {
                        variables.put(McidasConstants.AREA, value);
                    } else if (keysValues[i]
                            .startsWith(McidasConstants.RESOLUTION + ":")) {
                        variables.put(McidasConstants.RESOLUTION, value);
                    }
                }
            }

            // add in these last two.

            value = rscDefn.getResourceDefnName();
            if (value != null && !value.isEmpty())
                variables.put(McidasConstants.RESOURCE_DEFINITION,
                        rscDefn.getResourceDefnName());

            value = rscDefnsMngr.getAttrSet(rscName).getName();
            if (value != null && !value.isEmpty())
                variables.put(McidasConstants.CHANNEL, rscDefnsMngr
                        .getAttrSet(rscName).getName());

            /*
             * "variables map" now contains keywords/values available for
             * building the custom legend string. Examine marked-up legend
             * string looking for {keyword} that matches in "variables". If it
             * doesn't then remove it from legendString.
             */
            Pattern p = Pattern.compile("\\{(.*?)\\}");
            Matcher m = p.matcher(legendStringAttribute.toString());

            while (m.find()) {
                value = variables.get(m.group(1));
                if (value == null || value.isEmpty()) {

                    legendStringAttribute = legendStringAttribute.replace("{"
                            + m.group(1) + "}", "");
                }
            }

            /*
             * change all occurrences of '{' to "${" because that's what
             * VariableSubstituterNCEP expects
             */
            for (int ipos = 0; ipos < legendStringAttribute.length(); ipos++) {
                x = legendStringAttribute.charAt(ipos);
                sb.append(x == '{' ? "${" : x);
            }

            String customizedlegendString = VariableSubstitutorNCEP
                    .processVariables(sb.toString(), variables);

            /*
             * If user coded legendString properly there shouldn't be any "${"
             * present, but if there are then change them back to "{"
             */
            sb.setLength(0);
            for (int ipos = 0; ipos < customizedlegendString.length(); ipos++) {
                x = customizedlegendString.charAt(ipos);
                sb.append(x == '$' ? "{" : x);
            }
            customizedlegendString = sb.toString();

            legendStr += rscDefn.getRscAttributeDisplayName("");

            legendStr = (customizedlegendString.isEmpty()) ? legendStr
                    : customizedlegendString;

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