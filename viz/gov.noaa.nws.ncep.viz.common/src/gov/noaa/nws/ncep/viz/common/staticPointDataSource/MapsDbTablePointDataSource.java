package gov.noaa.nws.ncep.viz.common.staticPointDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * <pre>
 * 
 * SOFTWARE HISTORY
 *   
 * Date          Ticket#       Engineer         Description
 * ------------  ----------    -----------      --------------------------
 * 10/11/2016    R20573        jbeck            Initial creation
 *                                              Support county names overlay: get point data from maps database
 * </pre>
 * 
 * @author jbeck
 */

public class MapsDbTablePointDataSource extends AbstractPointDataSource {

    protected final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final static String DB_NAME = "maps";

    private final static String DB_SCHEMA_NAME = "mapdata";

    private final static String DB_GEOM_FIELD = "the_geom";

    private String dbTableName;

    private String dbFieldName;

    private List<LabeledPoint> countyNamePoints = new ArrayList<>();

    public MapsDbTablePointDataSource(String dbTableName, String dbFieldName) {

        this.dbTableName = DB_SCHEMA_NAME + "." + dbTableName;
        this.dbFieldName = dbFieldName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource
     * #getSourceType()
     */
    @Override
    public StaticPointDataSourceType getSourceType() {
        return StaticPointDataSourceType.MAPS_DB_TABLE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource
     * #loadData()
     */
    @Override
    public void loadData() throws VizException {

        String sql = "SELECT AsBinary(" + DB_GEOM_FIELD + ")," + dbFieldName
                + " FROM " + dbTableName;

        try {
            List<Object[]> countyNamesObjList = DirectDbQuery.executeQuery(sql,
                    DB_NAME, DirectDbQuery.QueryLanguage.SQL);

            for (Object[] countyNamesObj : countyNamesObjList) {

                if (countyNamesObj == null || countyNamesObj.length != 2) {

                    statusHandler.handle(Priority.INFO,
                            "Error querying " + dbTableName
                                    + ": wrong # of expected return values??");

                    continue;

                } else if (!(countyNamesObj[0] instanceof byte[])
                        || !(countyNamesObj[1] instanceof String)) {

                    statusHandler.handle(Priority.INFO,
                            "Error querying " + dbTableName
                                    + ": wrong # of expected return values??");
                    continue;
                }

                byte[] bytes = (byte[]) countyNamesObj[0];
                WKBReader reader = new WKBReader();
                Geometry geom = null;

                try {
                    geom = reader.read(bytes);
                    Coordinate coordinate = geom.getCoordinate();

                    LabeledPoint labeledPoint = new LabeledPoint(
                            (String) countyNamesObj[1], coordinate.y,
                            coordinate.x);
                    super.insertPoint(labeledPoint);
                    countyNamePoints.add(labeledPoint);

                } catch (ParseException e) {

                    statusHandler.handle(Priority.INFO, "Error querying "
                            + dbTableName + ": error parsing Geometry coord??");

                    continue;
                }
            }

        } catch (Exception e) {
            throw new VizException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource
     * #getPointData()
     */
    @Override
    public List<LabeledPoint> getPointData() {
        return countyNamePoints;
    }

    /*
     * (non-Javadoc) not implemented
     * 
     * @see
     * gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource
     * #getPointDataByLabel()
     */
    @Override
    public Map<String, LabeledPoint> getPointDataByLabel() {
        return null;
    }

}
