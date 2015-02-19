/*
 * gov.noaa.nws.ncep.common.staticDataProvider.SPCCountyProvider
 * 
 * 12 March 2012
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.staticdataprovider;

import gov.noaa.nws.ncep.common.staticdata.SPCCounty;
import gov.noaa.nws.ncep.viz.common.dbQuery.NcDirectDbQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.operation.valid.TopologyValidationError;

/**
 * Class to load SPC counties and marine zones, which are required when issuing watches.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 02/12		?			B. Yin   	Moved from PGEN 
 * 06/13	T1000/TTR580	J. Wu   	Use ID for marine zones (no FIPS). 
 *
 * </pre>
 * 
 * @author	B. Yin
 */

public class SPCCountyProvider {

	//counties and marine zones
	private static List<SPCCounty> allCounties;
	
	private static volatile boolean countyLoaded = false; 

    /*
     *  The 15 marine zones "State" names and its 2-digits IDs in NMAP2.
     */
    private static HashMap<String, String> marineZoneStates;
    
    
	/**
	 * Get all counties and marine zones from the database
	 * @return
	 */
	public static List<SPCCounty> getSPCCounties(){
		if ( !countyLoaded ){
			loadCountyTable();
		}

		return allCounties;
	}

	/**
	 * Load the county table and the marine zone table and merge them together.
	 * Also fix invalid polygon problem. 
	 * @return
	 */
	public static synchronized List<SPCCounty> loadCountyTable(){

		if ( !countyLoaded ) {

			allCounties = new ArrayList<SPCCounty>();

			List<Object[]> bnds;
			List<Object[]> stns;
			List<Object[]> zones;

			String queryStnTbl = "Select station_number, station_id, state, country FROM stns.mzcntys";
			String queryCntyBnds = "Select AsBinary(the_geom_0_064), countyname, state, fe_area, cwa, fips, lat, lon FROM mapdata.county";

			boolean noFips4MarineZones = true;
			String queryZoneBnds = "Select AsBinary(the_geom_0_064), id, name, wfo, lat, lon FROM mapdata.marinezones";
/*			boolean  noFips4MarineZones = !marineZoneHasFips();
			if ( noFips4MarineZones ) {
				queryZoneBnds = "Select AsBinary(the_geom_0_064), id, name, wfo, lat, lon FROM mapdata.marinezones";				
			}
			else {
				queryZoneBnds = "Select AsBinary(the_geom_0_064), id, name, wfo, fips, lat, lon FROM mapdata.marinezones";				
			}
*/
			try {
				bnds = NcDirectDbQuery.executeQuery( queryCntyBnds, "maps", QueryLanguage.SQL);
				zones = NcDirectDbQuery.executeQuery( queryZoneBnds, "maps", QueryLanguage.SQL);
				stns = NcDirectDbQuery.executeQuery( queryStnTbl, "ncep", QueryLanguage.SQL);

				WKBReader wkbReader = new WKBReader();

				int tt = 0;
				for ( Object[] bnd : bnds ){

					if ( bnd[0] != null && bnd[5] != null ) { //bounds and fips cannot be null

						//read shape
						byte[] wkb = (byte[]) bnd[0];
						Geometry cntyGeo = null;
						try {
							cntyGeo =  wkbReader.read(wkb);
						} catch (ParseException e) {
							e.printStackTrace();
						}

						//get county ugc
						if ( cntyGeo != null ){
							String cntyName = (String)bnd[1];
							String cntySt = (String)bnd[2];
							String cntyWfo = (String)bnd[4];

							if ( cntyName == null ) cntyName="";
							if ( cntySt == null ) cntySt = "";
							if ( cntyWfo == null ) cntyWfo = "";

							String cntyFips = (String)bnd[5];

							Coordinate loc = new Coordinate(0,0); 
							try {
								loc.x = ((Number)bnd[7]).doubleValue(); 
								loc.y =	((Number)bnd[6]).doubleValue();
							}
							catch ( Exception e ){
								// center location missing in database 
							}

							cntyGeo = removeSmallShells(cntyGeo, 0.001);

							String cntyUgc = "";
							String cntyCountry = "";
							boolean mZone = false;
							String znName = "";

							//test for invalid county shapes
							if ( !cntyGeo.isValid() ){
								IsValidOp vld = new IsValidOp(cntyGeo);
								TopologyValidationError err = vld.getValidationError();
								//					ii++;
								//					System.out.println("invalid county geo: " + err.getErrorType() + err.getCoordinate() + err.getMessage() + 
								//							" " + cntyName + " " + cntyFips + " " + ii );

								if ( err.getErrorType() == 7 ) cntyGeo = fixNestedShells(cntyGeo);
								//if ( err.getErrorType() == 5 ) cntyGeo = removeSmallShells(cntyGeo, 1e-3);
								if ( err.getErrorType() == 3 ) cntyGeo = fixNestedHoles(cntyGeo);
								if ( err.getErrorType() == 2 ) cntyGeo = fixHoleOutOfShell(cntyGeo);

							}

							if ( cntyFips.charAt(0) == '0' ){
								cntyFips = cntyFips.substring(1);
							}

							/*
							 * set county UGC and country
							 */
							cntyCountry = "US";
							if ( cntyFips.length() == 4 ) {
								cntyUgc = new String( cntySt + "C" + cntyFips.substring(1, 4) );
							}
							else if ( cntyFips.length() > 4 ) {
								cntyUgc = new String( cntySt + "C" + cntyFips.substring(2, 5) );
							}

							//for ( Object[] stn :stns ){
							//	if ( stn[0] != null && ((String)stn[0]).equalsIgnoreCase(cntyFips)){
							//		cntyUgc = (String)stn[1];
							//		cntyCountry = (String)stn[3];
							//	}
							//}

							SPCCounty existingCnty = findCounty( cntyFips );
							//				if ( existingCnty != null ){
							//					System.out.println( "Existing: " + existingCnty.getName() + " " + existingCnty.getState() + " " + existingCnty.getFips());
							//					System.out.println( "New:      " + cntyName + " " + cntySt + " " + cntyFips);
							//				}
							//both fips and name are same 
							if ( existingCnty != null && existingCnty.getName().equalsIgnoreCase(cntyName)  ){
								existingCnty.setShape(existingCnty.getShape().union( cntyGeo ));
							}
							else {
								SPCCounty cnty = new SPCCounty(cntyFips, cntyName, cntyWfo, cntyUgc, cntySt, cntyCountry,
										znName, loc, cntyGeo, mZone);
								allCounties.add(cnty);
								tt++;
							}
						}	
					}
				}

				//System.out.println("total counties: "+tt);
				int xIndex = 6;
				int yIndex = 5;
				if ( noFips4MarineZones ) {
					xIndex--;
					yIndex--;
				}
				
				for ( Object[] zn : zones ){

					if ( zn[0] != null  && zn[1] != null ){ //neither bound nor ugc can be null

						//read shape
						byte[] wkb = (byte[]) zn[0];
						Geometry zoneGeo = null;
						try {
							zoneGeo =  wkbReader.read(wkb);
						} catch (ParseException e) {
							e.printStackTrace();
						}

						//get zone f 
						if ( zoneGeo != null ){
							String ugc = (String)zn[1];
							String znName = (String)zn[2];
							String wfo = (String)zn[3];
							String cntyName = "";
							String fips = "";
							String znSt = "";
							String country = "";
							boolean mZone = true;

							Coordinate loc = new Coordinate(0,0); 
							try {
								loc.x = ((Number)zn[xIndex]).doubleValue(); 
								loc.y = ((Number)zn[yIndex]).doubleValue();
							}
							catch ( Exception e ){
								e.printStackTrace();
								// center location missing in database 
							}

							if ( znName == null ) znName = "";
							if ( wfo == null ) wfo ="";
							
							// Use zone id as FIPS.
							if ( !noFips4MarineZones && zn[4] != null ) {
								fips = ((Integer)zn[4]).toString();
							}
							else {
								fips = makeMarineZoneFips( (String)zn[1] );
							}

							zoneGeo = removeSmallShells(zoneGeo, 0.001);

							//test for invalid county shapes
							//int ii = 0;
							if ( !zoneGeo.isValid() ){
								IsValidOp vld = new IsValidOp(zoneGeo);
								TopologyValidationError err = vld.getValidationError();
								//				ii++;
								//				System.out.println("invalid zone geo: " + err.getErrorType() + err.getMessage() + 
								//						" " + znName + " " + fips + " " + ii );

								if ( err.getErrorType() == 7 ) zoneGeo = fixNestedShells(zoneGeo);
								if ( err.getErrorType() == 3 ) zoneGeo = fixNestedHoles(zoneGeo);
								if ( err.getErrorType() == 2 ) zoneGeo = fixHoleOutOfShell(zoneGeo);

							}

							/*
							 * set state and country
							 */
							country = "US";
							if ( ugc != null && ugc.length() > 2 ) {
								znSt = ugc.substring(0, 2 );
							}

							//for ( Object[] stn :stns ){
							//	if ( stn[0] != null && ((String)stn[1]).equalsIgnoreCase(ugc)){
							//		znSt = (String)stn[2];
							//		country = (String)stn[3];
							//	}
							//}

							//Set centriod.
							Point cent = zoneGeo.getCentroid();
							if ( cent != null ) {
								loc.x = cent.getX();
					            loc.y = cent.getY();
							}
							
							Geometry zgeo = zoneGeo; 
							SPCCounty existCnty = findCounty( fips );
							if ( existCnty != null && existCnty.getName().equalsIgnoreCase( cntyName )  ) {
								zgeo = existCnty.getShape().union( zoneGeo );
								existCnty.setShape( zgeo );
								
								Point cent1 = zoneGeo.getCentroid();
								if ( cent1 != null ) {
									loc.x = cent1.getX();
						            loc.y = cent1.getY();
									existCnty.setCentriod( loc );
								}
							}
							else {							
							SPCCounty cnty = new SPCCounty(fips, cntyName, wfo, ugc, znSt, country,
									znName, loc, zoneGeo, mZone);
							allCounties.add(cnty);
							}

						}
					}
				}
			}
			catch (Exception e ){
				System.out.println("db exception reading county tables!");	
				e.printStackTrace();
			}
						
			countyLoaded = true;

		}
		return allCounties;
	}

	/**
	 * Get the county with the input fips.
	 * @param fips
	 * @return
	 */
	public static SPCCounty findCounty( String fips ){

		if ( allCounties == null ){
			getSPCCounties();
		}

		for ( SPCCounty cnty : allCounties ){
			if ( cnty.getFips() != null && fips.equalsIgnoreCase(cnty.getFips()) ){
				return cnty;
			}
		}

		return null;
	}

	/**
	 * Get counties in the input geometry
	 * @param geo
	 * @return
	 */
	static public List<SPCCounty> getCountiesInGeometry(Geometry geo ){

		List<SPCCounty> rtv = new ArrayList<SPCCounty>();

		for ( int ii = 0; ii < geo.getNumGeometries(); ii++ ){
			Polygon poly = (Polygon)geo.getGeometryN(ii);

			for ( SPCCounty county : getSPCCounties() ){
				Geometry countyGeo = county.getShape();

				try {
					if (  countyGeo != null && (poly.intersects(countyGeo) ||
							poly.covers(countyGeo))) {
						rtv.add(county);
					}
				}
				catch (TopologyException te){
					//Calcasieu   					rtv.add(county);
					te.printStackTrace();
					continue;
				}

			}
		}
		return rtv;
	}

	//error type 7
	/**
	 * Removes small shells in the main shell(largest shell)
	 */
	private static Geometry fixNestedShells( Geometry geo ){

		if ( geo instanceof MultiPolygon ){

			ArrayList<Polygon> polyList = new ArrayList<Polygon>();
			for ( int ii = 0; ii < geo.getNumGeometries(); ii++ ){
				boolean nested = false;
				for ( int jj = 0; jj < geo.getNumGeometries(); jj++ ){
					if ( geo.getGeometryN(jj).contains(geo.getGeometryN(ii))){
						nested = true;
						break;
					}
				}
				if ( !nested ) polyList.add((Polygon)geo.getGeometryN(ii) );
			}

			MultiPolygon mpoly = geo.getFactory().createMultiPolygon( polyList.toArray( new Polygon[polyList.size()]) );

			return mpoly;
		}
		else {
			return geo;
		}

	}

	//error type 2
	/**
	 * Removes holes that are out of the polygon.
	 */
	private static Geometry fixHoleOutOfShell( Geometry geo ){

		if ( geo instanceof MultiPolygon ){
			GeometryFactory gf = geo.getFactory();
			ArrayList<Polygon> polyList = new ArrayList<Polygon>();

			//loop through polygons
			for ( int ii = 0; ii < geo.getNumGeometries(); ii++ ){

				if ( geo.getGeometryN(ii) instanceof Polygon  ){
					Polygon poly = (Polygon)geo.getGeometryN(ii);

					polyList.add( gf.createPolygon(gf.createLinearRing(poly.getExteriorRing().getCoordinates()), new LinearRing[]{}));

				}
			}

			MultiPolygon mpoly = gf.createMultiPolygon( polyList.toArray( new Polygon[polyList.size()]) );

			if (!mpoly.isValid() )     		{
				IsValidOp vld = new IsValidOp(mpoly);
				TopologyValidationError err = vld.getValidationError();

				if ( err.getErrorType() == 7 ) fixNestedShells(mpoly);

			}
			return mpoly;
		}
		else {
			return geo;
		}

	}  

	//error type 3
	/**
	 * Removes holes in holes.
	 */
	private static Geometry fixNestedHoles( Geometry geo ){

		if ( geo instanceof MultiPolygon ){
			GeometryFactory gf = geo.getFactory();
			ArrayList<Polygon> polyList = new ArrayList<Polygon>();


			//loop through polygons
			for ( int ii = 0; ii < geo.getNumGeometries(); ii++ ){

				if ( geo.getGeometryN(ii) instanceof Polygon  ){
					Polygon poly = (Polygon)geo.getGeometryN(ii);
					ArrayList<LinearRing> holeList = new ArrayList<LinearRing>();

					for ( int jj = 0; jj < poly.getNumInteriorRing(); jj++ ){
						boolean nested = false;

						Polygon hole1 = gf.createPolygon(gf.createLinearRing(poly.getInteriorRingN(jj).getCoordinates()), new LinearRing[]{});
						for ( int kk = 0; kk < poly.getNumInteriorRing(); kk++ ){
							Polygon hole2 = gf.createPolygon(gf.createLinearRing(poly.getInteriorRingN(kk).getCoordinates()), new LinearRing[]{});

							if ( hole2.contains(hole1)){
								nested = true;
								break;
							}

						}
						if ( !nested ) holeList.add( gf.createLinearRing(poly.getInteriorRingN(jj).getCoordinates()) );

					}

					polyList.add( gf.createPolygon(gf.createLinearRing(poly.getExteriorRing().getCoordinates()), holeList.toArray( new LinearRing[holeList.size()])));
				}
			}

			MultiPolygon mpoly = gf.createMultiPolygon( polyList.toArray( new Polygon[polyList.size()]) );

			if (!mpoly.isValid() ){     		

				IsValidOp vld = new IsValidOp(mpoly);
				TopologyValidationError err = vld.getValidationError();

				if ( err.getErrorType() == 7 ) fixNestedShells(mpoly);

			}
			return mpoly;
		}
		else {
			return geo;
		}

	}

	//error type 5: all of the self-intersection errors are caused by small shells
	/**
	 * Removes small shells in a polygon
	 * geo  - the working polygon
	 * area - threshold. Polygons with area less than this value will be removed. 
	 */
	private static 	Geometry removeSmallShells( Geometry geo, double area ){

		if ( geo instanceof MultiPolygon ){
			ArrayList<Polygon> polyList = new ArrayList<Polygon>();

			MultiPolygon	mp = (MultiPolygon)geo;
			for ( int ii = 0; ii < mp.getNumGeometries(); ii++ ){
				Polygon poly = (Polygon)mp.getGeometryN(ii);

				if ( poly.getArea() > area ){
					polyList.add(poly);
				}

			}
			MultiPolygon mpoly = geo.getFactory().createMultiPolygon( polyList.toArray( new Polygon[polyList.size()]) );

			return mpoly;
		}
		else {
			return geo;
		}
	}
	
	/**
	 *  Check if there is "fips" column for marine zone table.
	 */
	private static boolean marineZoneHasFips() {

		boolean  hasFips4MarineZones = true;

		String queryZoneFips = "Select fips FROM mapdata.marinezones";
		
		try {
			NcDirectDbQuery.executeQuery( queryZoneFips, "maps", QueryLanguage.SQL);	
		}
		catch (Exception e ) {
			hasFips4MarineZones = false;
		}
		
		return hasFips4MarineZones;
	}
	
    /**
     *  The equivalent state names and numeric IDs for marine zones.
     *  These name/numeric IDs match those used in NMAP2.
     */
    private static HashMap<String, String> getMarineZoneStates() {
    	if ( marineZoneStates == null ) {
    		marineZoneStates = new HashMap<String, String>();
    		marineZoneStates.put( "LC",  "60" );
    		marineZoneStates.put( "PZ",  "61" );
    		marineZoneStates.put( "LO",  "62" );
    		marineZoneStates.put( "LE",  "63" );
    		marineZoneStates.put( "LM",  "64" );
    		marineZoneStates.put( "LS",  "65" );
    		marineZoneStates.put( "AM",  "66" );
    		marineZoneStates.put( "AN",  "67" );
    		marineZoneStates.put( "GM",  "68" );
    		marineZoneStates.put( "PK",  "69" );
    		marineZoneStates.put( "PH",  "70" );
    		marineZoneStates.put( "PM",  "71" );
    		marineZoneStates.put( "PS",  "72" );
    		marineZoneStates.put( "SL",  "73" );
    		marineZoneStates.put( "LH",  "74" );
	    }
    	
    	return marineZoneStates;
    }
    
    /**
     *  Make FIPS for marine zones to match those in Legacy NMAP2.
     *  
     *  Each FIPS is a 6-digit number string as following:
     *  
     *  The first two number is the numeric ID of a marine zones "State"
     *  see getMarineZoneStates() above.
     *  
     *  The next three digits is the marine zone's assigned id - the 
     *  last three digits in its ID (e. g., 123 in "PKZ123" ).
     *  
     *  The last digit is always "0".
     *  
     *  06/10/2013 - Decided to use the marine zone's ID directly as FIPS.
     *  
     */
    private static String makeMarineZoneFips( String mzID ) {
	    
		String fips = "000000";
		if ( mzID != null && mzID.trim().length() >= 0 ) {
//			if ( mzID != null && mzID.trim().length() >= 6 ) {
			fips = new String( mzID );
//		    String stKey = mzID.substring(0, 2).toUpperCase();
//		    fips = new String( getMarineZoneStates().get( stKey ) + mzID.substring(3, 6) + "0" );
		}
    	
    	return fips;
    }

}
