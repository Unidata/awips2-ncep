package gov.noaa.nws.ncep.viz.rsc.satellite.area;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasConstants;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;
import gov.noaa.nws.ncep.viz.common.area.AreaName;
import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.IGridGeometryProvider;
import gov.noaa.nws.ncep.viz.common.area.INcAreaProviderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * An Area Provider Factory for Mcidas data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * 12/14          R5413     B. Yin      Remove Script Creator and use Thrift Client
 * Nov 04, 2015   10436     njensen     Corrected request constraint keys, formatted file
 * 
 * </pre>
 */

public class McIdasAreaProviderFactory implements INcAreaProviderFactory {

    public static class McIdasAreaProvider implements IGridGeometryProvider {

        private GeneralGridGeometry gridGeom;

        private final String areaName;

        public McIdasAreaProvider(String a) {
            gridGeom = null;
            areaName = a;
        }

        @Override
        public GeneralGridGeometry getGridGeometry() {
            if (gridGeom != null) {
                return gridGeom;
            }

            try {
                String[] satAndArea = areaName.split(File.separator);
                if (satAndArea.length != 2 || satAndArea[0].isEmpty()
                        || satAndArea[1].isEmpty()) {
                    throw new VizException(
                            "Invalid mcidas area name. Expecting <satName>/<areaName>.");
                }

                Map<String, RequestConstraint> reqConstraints = new HashMap<>();
                reqConstraints.put(PluginDataObject.PLUGIN_NAME_ID,
                        new RequestConstraint(McidasConstants.PLUGIN_NAME));
                reqConstraints.put(McidasConstants.SATELLITE_ID,
                        new RequestConstraint(satAndArea[0]));
                reqConstraints.put(McidasConstants.AREA_ID,
                        new RequestConstraint(satAndArea[1]));

                DbQueryRequest request = new DbQueryRequest();
                request.setConstraints(reqConstraints);

                DbQueryResponse response = (DbQueryResponse) ThriftClient
                        .sendRequest(request);

                Object[] satRecList = response.getResults().get(0).values()
                        .toArray();

                /*
                 * TODO: It would be nice to query the mcidas_area_names and/or
                 * mcidas_spatial directly so that we don't have to depend on
                 * data being in the db, but this is fine for now.
                 */
                if (satRecList == null || satRecList.length == 0
                        || !(satRecList[0] instanceof McidasRecord)) {
                    throw new VizException("No data for areaName " + areaName);
                }

                McidasRecord satRec = (McidasRecord) satRecList[0];

                if (satRec.getProjection().equalsIgnoreCase("STR")
                        || satRec.getProjection().equalsIgnoreCase("MER")
                        || satRec.getProjection().equalsIgnoreCase("LCC")) {

                    // for remapped projections such as MER, LCC, STR
                    gridGeom = MapUtil.getGridGeometry(satRec
                            .getSpatialObject());
                } else {
                    McidasMapCoverage coverage = satRec.getCoverage();

                    GeneralEnvelope env = new GeneralEnvelope(2);
                    env.setCoordinateReferenceSystem(satRec.getCoverage()
                            .getCrs());

                    int minX = coverage.getUpperLeftElement();
                    int maxX = coverage.getUpperLeftElement()
                            + (coverage.getNx() * coverage.getElementRes());
                    int minY = coverage.getUpperLeftLine()
                            + (coverage.getNy() * coverage.getLineRes());
                    minY = -minY;
                    int maxY = -1 * coverage.getUpperLeftLine();
                    env.setRange(0, minX, maxX);
                    env.setRange(1, minY, maxY);

                    gridGeom = new GridGeometry2D(new GeneralGridEnvelope(
                            new int[] { 0, 0 }, new int[] { coverage.getNx(),
                                    coverage.getNy() }, false), env);
                }
            } catch (VizException e) {
                // TODO log better?
//				throw new VizException("Could not query a McIdasRecord to get the image geometry:"+e.getMessage());
                System.out
                        .println("Could not query a McIdasRecord to get the image geometry:"
                                + e.getMessage());
            }

            return gridGeom;
        }

        @Override
        public AreaSource getSource() {
            return sourceName;
        }

        @Override
        public String getProviderName() {
            return areaName;
        }

        @Override
        public double[] getMapCenter() {
            return null;
        }

        @Override
        public String getZoomLevel() {
            return Double.toString(1.0);
        }

        @Override
        public void setZoomLevel(String zl) {
            // no-op
        }

    }

    private static AreaSource sourceName;

    /*
     * TODO set from ext point location parameter
     */

    /*
     * TODO use the location string to set the names of the db columns to query
     * for the area. hardcoded for now since the method for querying the area
     * may change if we can link the mcidas_area_names table to the
     * mcidas_spatial table.
     */
    private final Map<String, McIdasAreaProvider> availAreasMap = new HashMap<String, McIdasAreaProvider>();

    @Override
    public AreaSource getAreaSource() {
        return sourceName;
    }

    @Override
    public List<VizException> getInitializationExceptions() {
        return null;
    }

    @Override
    public List<AreaName> getAvailableAreaNames() {
        List<AreaName> areaNames = new ArrayList<AreaName>(availAreasMap.size());
        for (String aname : availAreasMap.keySet()) {
            areaNames.add(new AreaName(getAreaSource(), aname));
        }
        return areaNames;
    }

    @Override
    public void initialize(String srcName, String dataLoc, String configData)
            throws VizException {
        sourceName = AreaSource.createImagaeBasedAreaSource(srcName);

        Map<String, RequestConstraint> reqConstraints = new HashMap<>();
        reqConstraints.put(PluginDataObject.PLUGIN_NAME_ID,
                new RequestConstraint(McidasConstants.PLUGIN_NAME));

        try {
            DbQueryRequest request = new DbQueryRequest();
            request.setConstraints(reqConstraints);
            request.addRequestField(McidasConstants.SATELLITE_ID);
            request.addRequestField(McidasConstants.AREA_ID);
            request.setDistinct(true);
            DbQueryResponse response = (DbQueryResponse) ThriftClient
                    .sendRequest(request);

            for (Map<String, Object> result : response.getResults()) {
                String satAreaName = result.get(McidasConstants.SATELLITE_ID)
                        + File.separator + result.get(McidasConstants.AREA_ID);
                if (!availAreasMap.containsKey(satAreaName)) {
                    // get geom later when requested
                    availAreasMap.put(satAreaName, new McIdasAreaProvider(
                            satAreaName));
                }
            }
        } catch (Exception e) {
            throw new VizException(e);
        }
    }

    @Override
    public IGridGeometryProvider createGeomProvider(String areaName)
            throws VizException {
        if (!availAreasMap.containsKey(areaName)) {
            availAreasMap.put(areaName, new McIdasAreaProvider(areaName));
        }

        return availAreasMap.get(areaName);
    }
}