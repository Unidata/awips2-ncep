//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.03.25 at 04:38:22 PM EDT 
//


package gov.noaa.nws.ncep.viz.ui.locator.resource;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import gov.noaa.nws.ncep.viz.common.LocatorUtil;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.DbTablePointDataSource;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.IStaticPointDataSource.StaticPointDataSourceType;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.StaticPointDataSourceMngr;
import gov.noaa.nws.ncep.viz.common.staticPointDataSource.PointDirDist;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.viz.core.exception.VizException;
import org.locationtech.jts.geom.Coordinate;


/**
 * This function reads locator settings from locator_tbl.xml table.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 12/2008  	22    		M. Li      Initial Creation
 * 11/25/2009    138        G. Hull    add writeLocatorTable (from lost to10 change)
 * 12/07/2011    #561       G. Hull    use SerializationUtil; 
 * 12/07/2011    #561       G. Hull    renamed class; split table into multiple files; use NcPathManager
 * 12/23/2011    #561       G. Hull    use BoundsDataSource,PointDataSource classes to lookup data
 * 12/14/2012    #903       G. Hull    add the Source name when there is no data found.
 * 07/05/2013    #1010      G. Hull    IStaticPointDataSource
 *                              
 * </pre>
 * 
 * @author M. Li
 * @version 1
 */
@XmlRootElement(name = "LocatorDataSource")
@XmlAccessorType(XmlAccessType.NONE)
public class LocatorDataSource implements ISerializableObject {

	
	public static enum LocatorType {
		POINT,
		BOUNDED_AREA,
		LATLON//,
		//SFC_STATIONS//, // code was special-cased for source == SFSTATION  so at least 
		//CITIES       // this is a little more explicit...
	}

	private LocalizationFile localizationFile;
	
	@XmlElement
    protected String sourceName;
    
	// not to be confused with the sourceType of the point data. 
	// currently this only works for the ncep db table point data sources. 
	// It would not be a big change to support other point source types such as the common_obs_spatial db.
	//
    @XmlElement
    protected LocatorType sourceType;

    @XmlElement
    protected String dbName;    // default to 'ncep'

    @XmlElement
    protected String dbTableName;
        
    @XmlElement
    protected String dbFieldName;

    @XmlElement
    protected Boolean loadWithoutPartitioning;
    
	public static final String NOT_AVAILABLE = "---NO DATA---";

	private BoundsDataSource boundsSource = null;
	
//	private CitiesDataSource citiesSource = null;
 	private IStaticPointDataSource pointSource = null;
	
	private Boolean dataLoaded = false;
	
//    @XmlElement
//    protected String stateID;
    
    public LocatorDataSource( ) {
    	
    }

    public LocatorDataSource( LocatorDataSource lds ) {
    	sourceName = lds.sourceName;
    	sourceType  = lds.sourceType;
    	dbName      = lds.dbName;
    	dbTableName = lds.dbTableName;
    	dbFieldName = lds.dbFieldName;
//    	attributeID = lds.attributeID;
//    	usedInSeek  = lds.usedInSeek;
//    	stateID     = lds.stateID;
    }
    
//    @XmlElement(name = "DisplayOptions")
//    protected DisplayOptions displayOptions;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String value) {
        this.sourceName = value;
    }

	public LocatorType getLocatorType() {
		return sourceType;
	}

	public void setSourceType(LocatorType sourceType) {
		this.sourceType = sourceType;
	}

    public String getDbTableName() {
        return dbTableName;
    }

    public void setDbTableName(String value) {
        this.dbTableName = value;
    }

    public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDbFieldName() {
		return dbFieldName;
	}

	public void setDbFieldName(String dbFieldName) {
		this.dbFieldName = dbFieldName;
	}

    public Boolean getLoadWithoutPartitioning() {
		return loadWithoutPartitioning;
	}

	public void setLoadWithoutPartitioning(Boolean loadWithoutPartitioning) {
		this.loadWithoutPartitioning = loadWithoutPartitioning;
	}

	public LocalizationFile getLocalizationFile() {
		return localizationFile;
	}

	public void setLocalizationFile(LocalizationFile localizationFile) {
		this.localizationFile = localizationFile;
	}
	
	// Note : this may take a few seconds so it should be called in a new thread.
	//
	public void loadSourceData( ) throws VizException  {
		if( dataLoaded ) {
			return;
		}
		
		if( getLocatorType() == LocatorType.POINT )  {
			// TODO : could improve on this to get a factory for
			// NcepDbTable point data sources. but for now just 
			// use a list since there are diff number of params for diff 
			// types of point data sources (ie. LPI files, common_obs_spatial....)
			List<String> initParams = new ArrayList<String>();
			//initParams.add( dbName );
//			initParams.add( dbTableName );
			initParams.add( dbFieldName );
			pointSource = StaticPointDataSourceMngr.createPointDataSource( 
					StaticPointDataSourceType.NCEP_DB_TABLE, dbTableName, initParams);	
			pointSource.loadData();
		}
		else if( getLocatorType() == LocatorType.BOUNDED_AREA )  {
			boundsSource = new BoundsDataSource( this );	
			boundsSource.loadData();
		}
//		else if( getSourceType() == SourceType.SFC_STATIONS ) {
//			//SurfaceStationPointData();
//		}
		else if( getLocatorType() == LocatorType.LATLON )  {
			// continue
		}		
		dataLoaded = true;
	}
	
	public boolean isDataLoaded() {
		return dataLoaded;
	}

	public String getLocatorString( Coordinate coor, LocatorDisplayAttributes dispAttrs ){
		if( coor == null ) {
			return NOT_AVAILABLE;
		}
		
		//TODO: SFSTATION is null too, maybe others!
		if( sourceType == LocatorType.LATLON ) {
			return formatLatLongCoordinate( coor, dispAttrs );
		}		
//		else if( sourceType == SourceType.SFC_STATIONS ) {
//			return SurfaceStationPointData.calculateNearestPoint( coor );   
//	        	//return("STN lon = "+ aLatLon.x+ " lat = "+ aLatLon.y );
//		}
		else if( sourceType == LocatorType.POINT ) { //.contains("stns"))
			return getNearestPointFormattedString( coor, dispAttrs);
		}
		else if( sourceType == LocatorType.BOUNDED_AREA ) { //s.contains("bounds"))
			try{ 
				String s = boundsSource.getBoundsValue(coor, dispAttrs);
				
				return getNormalizedString(s);
			
			} catch(Exception e){
				System.out.println("Error in getBoundsDataText() of LocatorResource: "+e.getMessage());
				return "--ERROR--";
			}		
		}
		else {
			System.out.println("Unrecognized Locator Source Type???");
			return "UNKNOWN LOCATOR TYPE";//NOT_AVAILABLE;
		}

	}
	
	// currently this is only used by NSharp for sfc stations.
//	public Coordinate getCoordinateForStation( String stn ) {
//		if( sourceType == SourceType.SFC_STATIONS ) {
//			
//		}
//		return new Coordinate();
//	}	
//	public PointData getNearestPointData( 
//			Coordinate coord ) throws VizException {
//		if( sourceType == SourceType.POINT ) {
//			return pointSource.calculateNearestPoint2( coord );
//		}
//		else {
//			throw new VizException("LocatorDataSource: can't call getNearestPointFormattedString for"+
//					"non POINT sourceType");
//		}
//	}
	
	// 		
	private String getNearestPointFormattedString( 
			Coordinate coord, LocatorDisplayAttributes dispAttrs ) {
							 
		String label = null;
		try{
			PointDirDist nearestPt = pointSource.calculateNearestPoint2( coord );//, dispAttrs );
			if( nearestPt == null ) {
				return "No "+dispAttrs.getLocatorSource()+" Data";//NOT_AVAILABLE;
			}

			StringBuilder sb = new StringBuilder();

			// if enabled 
			if( dispAttrs.getDistanceUnit() != null &&
			   !dispAttrs.getDistanceUnit().isEmpty() &&
			   !dispAttrs.getDistanceUnit().equalsIgnoreCase("omit") ) {
				
				String distanceOuput = LocatorUtil.distanceDisplay(
						nearestPt.getDistanceInMeter(), 
						dispAttrs.getRoundToNearest(), 
						dispAttrs.getDistanceUnit() );
				
				sb.append( distanceOuput ).append(" ");				
			}

			if( dispAttrs.getDirectionUnit() != null &&
			   !dispAttrs.getDirectionUnit().isEmpty() &&
			   !dispAttrs.getDirectionUnit().equalsIgnoreCase("omit") ) {
				
				String dirOutput = LocatorUtil.directionDisplay(
						nearestPt.getDirection(), dispAttrs.getDirectionUnit());
				
				sb.append(dirOutput).append(" ");	
			}
			
			sb.append( nearestPt.getName() );
			
			return sb.toString();
 
		} 
		catch(Exception e){
			System.out.println("Error: in getPointDataText() of LocatorResource: "+e.getMessage());		
			return "--ERROR--";
		}		
	}
	
	
    private static String formatLatLongCoordinate( Coordinate theLatLon, LocatorDisplayAttributes dispAttrs) {
		String label = null;
		String unit = dispAttrs.getDistanceUnit().toLowerCase();
		
		// default to decimal
        if( unit == null ||
        	unit.indexOf( "degrees" ) == -1 ) { //|| unit.equalsIgnoreCase( LATLONUNIT_OPTIONS[0])) {

            // TODO : remove unit name dependency here...
        	NumberFormat intFormat = NumberFormat.getInstance();
            intFormat.setMinimumIntegerDigits(2);
            intFormat.setMaximumIntegerDigits(2);
            
        	double x = Math.abs(theLatLon.x - (int)theLatLon.x);
        	double y = Math.abs(theLatLon.y - (int)theLatLon.y);
        	
        	x = x * 60;
        	y = y * 60;
        	
        	label = (int)theLatLon.y  + ":" + intFormat.format((int)y) + ", " +
        		    (int)theLatLon.x  + ":" + intFormat.format((int)x);
        }
        else {
        	NumberFormat nf = DecimalFormat.getInstance();
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            
        	label = nf.format(theLatLon.y) + ", " + nf.format(theLatLon.x);
        }

        return label;
	}

	private String getNormalizedString(String s){
		if( s == null || s.isEmpty())
			return NOT_AVAILABLE;
		
		s = s.replace("_", " ");
		return s.toUpperCase();
	}

}
