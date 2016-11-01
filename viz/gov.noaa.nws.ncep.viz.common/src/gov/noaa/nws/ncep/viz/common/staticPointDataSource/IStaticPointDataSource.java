package gov.noaa.nws.ncep.viz.common.staticPointDataSource;

import java.util.List;
import java.util.Map;

import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/12/2016    R20573    jbeck   Code cleanup
 * 
 * </pre>
 * 
 */

public interface IStaticPointDataSource {
	
	public static enum StaticPointDataSourceType {
		NCEP_DB_TABLE,
		MAPS_DB_TABLE,
		LPI_FILE,
		SPI_FILE,
		STATIONS_DB_TABLE, // metadata's common_obs_spatial db table
		NCEP_STATIONS_TBL_FILES
	}
	
	public abstract StaticPointDataSourceType getSourceType();
	
	public abstract void loadData( ) throws VizException;
	
	public abstract void insertPoint( LabeledPoint lp );

	public abstract List<LabeledPoint> getPointData();

	public abstract Map<String,LabeledPoint> getPointDataByLabel();
	
	public abstract PointDirDist calculateNearestPoint2(Coordinate loc )  throws VizException; 
}
