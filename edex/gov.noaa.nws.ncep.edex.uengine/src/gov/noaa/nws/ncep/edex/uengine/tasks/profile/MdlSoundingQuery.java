package gov.noaa.nws.ncep.edex.uengine.tasks.profile;

/**
 * 
 * gov.noaa.nws.ncep.edex.uengine.tasks.profile.MdlSoundingQuery
 * 
 * This java class performs the Grid model sounding data query functions.
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 04/04/2011	301			Chin Chen	Initial coding
 * 02/28/2012               Chin Chen   modify several sounding query algorithms for better performance
 * 03/28/2012               Chin Chen   Add new API to support query multiple Points at one shoot and using
 * 										dataStore.retrieveGroups()
 * Oct 15, 2012 2473        bsteffen    Remove ncgrib
 * 03/2014		1116		T. Lee		Added DpD
 * 01/2015      DR#16959    Chin Chen   Added DpT support to fix DR 16959 NSHARP freezes when loading a sounding from 
 *                                      HiRes-ARW/NMM models
 * 02/03/2015   DR#17084    Chin Chen   Model soundings being interpolated below the surface for elevated sites                                     
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingModel;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.raytheon.edex.uengine.tasks.query.TableQuery;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.grid.GridConstants;
import com.raytheon.uf.common.dataplugin.grid.GridInfoRecord;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.PointUtil;
import com.raytheon.uf.common.topo.TopoQuery;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.database.query.DatabaseQuery;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

//import org.opengis.geometry.Envelope;

public class MdlSoundingQuery {
    private static final String GRID_TBL_NAME = "grid";

    private static String GRID_PARMS = "GH, uW, vW,T, DWPK, SPFH,OMEG, RH, DpD, DpT";

    private enum GridParmNames {
        GH, uW, vW, T, DWPK, SPFH, OMEG, RH, DpD, DpT
    };

    public static UnitConverter kelvinToCelsius = SI.KELVIN
            .getConverterTo(SI.CELSIUS);

    private static final UnitConverter metersPerSecondToKnots = SI.METERS_PER_SECOND
            .getConverterTo(NonSI.KNOT);

    // Note; we are using NCInventory now. So, this api is actually not used.
    public static NcSoundingTimeLines getMdlSndTimeLine(String mdlType,
            String currentDBTblName) {
        NcSoundingTimeLines tl = new NcSoundingTimeLines();
        /*
         * if(currentDBTblName.equals(NCGRIB_TBL_NAME)){ Object[] refTimeAry =
         * null; String queryStr = new String("Select Distinct reftime FROM " +
         * currentDBTblName + " where modelname='" + mdlType +
         * "' ORDER BY reftime DESC");
         * 
         * CoreDao dao = new CoreDao(DaoConfig.forClass(NcgribRecord.class));
         * refTimeAry = (Object[]) dao.executeSQLQuery(queryStr);
         * tl.setTimeLines(refTimeAry); }else
         * if(currentDBTblName.equals(D2DGRIB_TBL_NAME)){ TableQuery query; try
         * { query = new TableQuery("metadata", GridRecord.class.getName());
         * query.setDistinctField("dataTime.refTime");
         * query.addParameter(GridConstants.DATASET_ID, mdlType);
         * query.setSortBy("dataTime.refTime", false);
         * 
         * @SuppressWarnings("unchecked") List<GridRecord> recList =
         * (List<GridRecord>) query.execute();
         * tl.setTimeLines(recList.toArray()); } catch (DataAccessLayerException
         * e) { // TODO Auto-generated catch block e.printStackTrace(); } catch
         * (Exception e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         * 
         * }
         */

        // Chin: modified for Unified Grid DB
        // Use the following SQL statement
        // Select Distinct reftime FROM grid FULL JOIN grid_info ON
        // grid.info_id=grid_info.id where grid_info.datasetid='gfs' ORDER BY
        // reftime DESC
        Object[] refTimeAry = null;
        String queryStr = new String(
                "Select Distinct reftime FROM grid FULL JOIN grid_info ON grid.info_id=grid_info.id where grid_info.datasetid='"
                        + mdlType + "' ORDER BY reftime DESC");

        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        refTimeAry = dao.executeSQLQuery(queryStr);
        tl.setTimeLines(refTimeAry);

        return tl;
    }

    public static NcSoundingTimeLines getMdlSndRangeTimeLine(String mdlType,
            String refTimeStr, String currentDBTblName) {
        NcSoundingTimeLines tl = new NcSoundingTimeLines();
        /*
         * if(currentDBTblName.equals(NCGRIB_TBL_NAME)){ Object[] refTimeAry =
         * null; String queryStr = new String("Select Distinct rangestart FROM "
         * + currentDBTblName + " where modelname='" + mdlType + "' AND " +
         * "reftime='" + refTimeStr + ":00:00'" + " ORDER BY rangestart");
         * System.out.println("queryStr  " + queryStr);
         * 
         * CoreDao dao = new CoreDao(DaoConfig.forClass(SoundingSite.class));
         * refTimeAry = (Object[]) dao.executeSQLQuery(queryStr);
         * tl.setTimeLines(refTimeAry); } else
         * if(currentDBTblName.equals(D2DGRIB_TBL_NAME)){ TableQuery query; try
         * { query = new TableQuery("metadata", GridRecord.class.getName());
         * query.setDistinctField("dataTime.validPeriod.start");
         * query.addParameter(GridConstants.DATASET_ID, mdlType);
         * query.addParameter("dataTime.refTime", refTimeStr + ":00:00");
         * query.setSortBy("dataTime.validPeriod.start", true);
         * 
         * @SuppressWarnings("unchecked") List<GridRecord> recList =
         * (List<GridRecord>) query.execute();
         * tl.setTimeLines(recList.toArray()); } catch (DataAccessLayerException
         * e) { // TODO Auto-generated catch block e.printStackTrace(); } catch
         * (Exception e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         */
        // Chin: modified for Unified Grid DB
        // make sure data in DB is not just nHour data, as those data are not
        // used by Nsharp. And when query to it, the returned will be
        // null. We do not want to show such sounding time line to user.
        // use this SQL query string for gfs as example.
        /*
         * Select Distinct rangestart FROM grid FULL JOIN grid_info ON
         * grid.info_id=grid_info.id where grid.reftime = '2012-01-26 00:00:00'
         * AND grid.rangestart = grid.rangeend AND
         * grid_info.datasetid='mesoEta212' AND
         * grid_info.parameter_abbreviation='T' order by rangestart
         */
        Object[] soundingTimeAry = null;
        List<Object> reSoundingTimeAry = new ArrayList<Object>();
        String queryStr = new String(
                "Select Distinct rangestart FROM grid FULL JOIN grid_info ON grid.info_id=grid_info.id where grid.reftime = '"
                        + refTimeStr
                        + ":00:00' AND grid.rangestart = grid.rangeend AND grid_info.datasetid='"
                        + mdlType
                        + "' AND grid_info.parameter_abbreviation='T' order by rangestart");
        // System.out.println("queryStr  " + queryStr);

        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        soundingTimeAry = (Object[]) dao.executeSQLQuery(queryStr);
        for (int i = 0; i < soundingTimeAry.length; i++) {
            /*
             * Chin: make sure the time line has more than 5 T(temp) values at
             * pressure (levelone) greater/equal than/to 100 hPa (mbar) use this
             * SQL : Select count(rangestart) FROM (select rangestart FROM grid
             * FULL JOIN grid_info ON grid.info_id=grid_info.id FULL JOIN level
             * ON grid_info.level_id= level.id where grid.rangestart =
             * '2012-01-26 03:00:00.0' AND grid.rangestart = grid.rangeend AND
             * grid_info.datasetid='mesoEta212' AND
             * grid_info.parameter_abbreviation='T' AND level.levelonevalue >
             * 99) X HAVING count(X.rangestart) >5
             */
            String queryStr1 = new String(
                    "Select count(rangestart) FROM (select   rangestart FROM grid FULL JOIN grid_info ON grid.info_id=grid_info.id FULL JOIN level ON grid_info.level_id= level.id where grid.rangestart = '"
                            + soundingTimeAry[i]
                            + "' AND grid.rangestart = grid.rangeend AND grid_info.datasetid='"
                            + mdlType
                            + "' AND grid_info.parameter_abbreviation='T' AND level.levelonevalue > 99) X HAVING count(X.rangestart) >2");
            Object[] countAry = null;
            // System.out.println("queryStr1  " + queryStr1);
            countAry = (Object[]) dao.executeSQLQuery(queryStr1);
            java.math.BigInteger count = new java.math.BigInteger("0");
            if (countAry.length > 0) {
                // System.out.println("rangestart ="
                // +soundingTimeAry[i]+" number="+countAry[0]);
                count = (java.math.BigInteger) countAry[0];
            }
            // else{
            // System.out.println("rangestart ="
            // +soundingTimeAry[i]+" return null");
            // }
            if (count.intValue() > 2) {
                Object timeLine = soundingTimeAry[i];
                reSoundingTimeAry.add(timeLine);
            }
        }

        tl.setTimeLines(reSoundingTimeAry.toArray());

        // }
        return tl;
    } // public static NcSoundingProfile getMdlSndData(double lat, double lon,

    // String stn, long refTimeL, long validTimeL, String sndTypeStr,
    // SndQueryKeyType queryType, String mdlName) {
    // //*System.out.println("getPfcSndData input ref time = "+
    // refTimeL+" valid time is " + validTimeL);
    // Calendar refTimeCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    // refTimeCal.setTimeInMillis(refTimeL);
    // Calendar validTimeCal =
    // Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    // validTimeCal.setTimeInMillis(validTimeL);
    // return getMdlSndData( lat, lon, refTimeCal, validTimeCal, "ncgrib",
    // mdlName);
    // }

    /**
     * Returns a list of profile for location (lat,lon) array, time, and model
     * for grib or ncgrib data.
     * 
     * @param double[][] latLonArray, e.g. at nth element, lat=[n][0],
     *        lon=[n][1]
     * @param refTimeCal
     *            data record reference time
     * @param validTimeCal
     *            data record valid time
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param mdlName
     *            the name of the model
     * @return the profile created @ 3/28/2012
     */
    public static List<NcSoundingProfile> getMdlSndDataProfileList(
            double[][] latLonArray, String refTime, String validTime,
            String pluginName, String mdlName) {
        double lat, lon;
        // System.out.println("getMdlSndData lat=" + lat + " lon="+lon);
        long t01 = System.currentTimeMillis();
        NcSoundingProfile pf = new NcSoundingProfile();
        // NcSoundingCube cube = new NcSoundingCube();
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        List<?> levels = getModelLevels(refTime, validTime, pluginName, mdlName);
        if (levels.size() == 0) {
            System.out.println("getModelLevels return 0;  file=" + refTime
                    + " stime=" + validTime + " gribtype=" + pluginName
                    + " modeltype=" + mdlName);
            return soundingProfileList;
        }
        // System.out.println("getModelLevels = "+
        // levels.size()+" levels, took "+ (System.currentTimeMillis()-t01) +
        // " ms");

        List<Point> points = new ArrayList<Point>();
        for (int k = 0; k < latLonArray.length; k++) {
            lat = latLonArray[k][0];
            lon = latLonArray[k][1];
            Point pnt = getLatLonIndices(lat, lon, refTime, validTime, levels
                    .get(0).toString(), pluginName, mdlName);
            if (pnt == null) {
                System.out.println("getLatLonIndices return 0; lat=" + lat
                        + " lon=" + lon + " stime=" + validTime + " gribtype="
                        + pluginName + " modeltype=" + mdlName);
            } else {
                points.add(pnt);
            }
        }
        if (points.size() == 0) {
            return soundingProfileList;
        }
        long t011 = System.currentTimeMillis();
        soundingProfileList = queryProfileListByPointGroup(points, refTime,
                validTime, pluginName, mdlName, levels);
        System.out.println("queryProfileListByPointGroup took "
                + (System.currentTimeMillis() - t011) + " ms");

        return soundingProfileList;
        /*
         * The floowing should be done in queryProfileListByPointGroup()
         * //System.out.println("getModelSoundingLayerList= "+ layerList.size()+
         * " layers, took "+ (System.currentTimeMillis()-t012) + " ms");
         * //pf.setStationLatitude( lat); //pf.setStationLongitude( lon);
         * //Float sfcPressure = getModelSfcPressure(pnt, refTime, validTime, //
         * pluginName, mdlName);
         * //System.out.println("getModelSfcPressure took "+
         * (System.currentTimeMillis()-t013) + " ms"); //if (sfcPressure ==
         * null) { // pf.setSfcPress(-9999.f); //} //else { // if
         * (pluginName.equalsIgnoreCase(D2DGRIB_TBL_NAME)) //
         * pf.setSfcPress(sfcPressure/100F); // else //
         * pf.setSfcPress(sfcPressure); //}
         * //System.out.println("surface pressure ="+pf.getSfcPress()+
         * " lat= "+lat+ " lon="+lon); //calculate dew point if necessary long
         * t014 = System.currentTimeMillis(); MergeSounding ms = new
         * MergeSounding(); //ms.spfhToDewpoint(layerList);
         * ms.rhToDewpoint(layerList); System.out.println("MergeSounding took "+
         * (System.currentTimeMillis()-t014) + " ms");
         * 
         * 
         * pf.setSoundingLyLst(layerList);
         * 
         * 
         * soundingProfileList.add(pf);
         * //cube.setSoundingProfileList(soundingProfileList);
         * //cube.setRtnStatus(NcSoundingCube.QueryStatus.OK); long t02 =
         * System.currentTimeMillis();
         * System.out.println("MDL cube retreival took " + (t02 - t01)); return
         * pf;
         */

    }

    /**
     * Returns a profile for a specified location (lat,lon), time, and model for
     * grib or ncgrib data.
     * 
     * @param lat
     *            location latitude
     * @param lon
     *            location longitude
     * @param refTimeCal
     *            data record reference time
     * @param validTimeCal
     *            data record valid time
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param mdlName
     *            the name of the model
     * @return the profile
     * 
     *         public static NcSoundingProfile getMdlSndData(double lat, double
     *         lon, String refTime, String validTime, String pluginName, String
     *         mdlName) { System.out.println("getMdlSndData lat=" + lat +
     *         " lon="+lon); long t01 = System.currentTimeMillis();
     *         NcSoundingProfile pf = new NcSoundingProfile(); List<?> levels =
     *         getModelLevels(refTime, validTime, pluginName, mdlName); if
     *         (levels.size() == 0) {
     *         System.out.println("getModelLevels return 0;  file=" + refTime+
     *         " stime="+validTime + " gribtype="+ pluginName +
     *         " modeltype="+mdlName);
     *         pf.setRtnStatus(NcSoundingCube.QueryStatus.FAILED); return pf; }
     *         System.out.println("getModelLevels = "+
     *         levels.size()+" levels, took "+ (System.currentTimeMillis()-t01)
     *         + " ms"); long t011 = System.currentTimeMillis(); Point pnt =
     *         getLatLonIndices(lat, lon, refTime, validTime,
     *         levels.get(0).toString(), pluginName, mdlName); if (pnt == null)
     *         { System.out.println("getLatLonIndices return 0; lat=" + lat +
     *         " lon="+lon+" stime="+validTime + " gribtype="+ pluginName +
     *         " modeltype="+mdlName);
     * 
     *         pf.setRtnStatus(NcSoundingCube.QueryStatus.LOCATION_NOT_FOUND);
     *         return pf;
     * 
     *         } System.out.println("getLatLonIndices pntX=" + pnt.getX()+
     *         " pntY=" + pnt.getY()+ " took "+
     *         (System.currentTimeMillis()-t011) + " ms"); long t012 =
     *         System.currentTimeMillis(); List<NcSoundingLayer> layerList =
     *         getModelSoundingLayerList(pnt, refTime, validTime, pluginName,
     *         mdlName, levels); if (layerList.size() == 0) {
     *         System.out.println("getModelSoundingLayerList return 0; lat=" +
     *         lat + " lon="+lon+" stime="+validTime + " gribtype="+ pluginName
     *         + " modeltype="+mdlName);
     * 
     * 
     *         pf.setRtnStatus(NcSoundingCube.QueryStatus.FAILED); return pf; }
     * 
     *         System.out.println("getModelSoundingLayerList= "+
     *         layerList.size()+ " layers, took "+
     *         (System.currentTimeMillis()-t012) + " ms");
     * 
     *         pf.setStationLatitude( lat); pf.setStationLongitude( lon); Float
     *         sfcPressure = getModelSfcPressure(pnt, refTime, validTime,
     *         pluginName, mdlName);
     *         //System.out.println("getModelSfcPressure took "+
     *         (System.currentTimeMillis()-t013) + " ms"); if (sfcPressure ==
     *         null) { pf.setSfcPress(-9999.f); } else { if
     *         (pluginName.equalsIgnoreCase(D2DGRIB_TBL_NAME))
     *         pf.setSfcPress(sfcPressure/100F); else
     *         pf.setSfcPress(sfcPressure); }
     *         //System.out.println("surface pressure ="+pf.getSfcPress()+
     *         " lat= "+lat+ " lon="+lon); //calculate dew point if necessary
     *         long t014 = System.currentTimeMillis(); MergeSounding ms = new
     *         MergeSounding(); //ms.spfhToDewpoint(layerList);
     *         ms.rhToDewpoint(layerList);
     *         System.out.println("MergeSounding took "+
     *         (System.currentTimeMillis()-t014) + " ms");
     * 
     * 
     *         pf.setSoundingLyLst(layerList);
     * 
     *         long t02 = System.currentTimeMillis();
     *         System.out.println("MDL cube retreival took " + (t02 - t01));
     *         return pf;
     * 
     *         }
     */

    public static NcSoundingModel getMdls(String pluginName) {
        NcSoundingModel mdls = new NcSoundingModel();
        Object[] mdlName = null;
        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
            CoreDao dao = new CoreDao(DaoConfig.forClass(GridInfoRecord.class));
            String queryStr = new String(
                    "Select Distinct modelname FROM grib_models ORDER BY modelname");
            mdlName = (Object[]) dao.executeSQLQuery(queryStr);
        }
        if (mdlName != null && mdlName.length > 0) {
            List<String> mdlList = new ArrayList<String>();
            for (Object mn : mdlName) {
                mdlList.add((String) mn);
            }
            mdls.setMdlList(mdlList);
        }
        return mdls;
    }

    public static boolean isPointWithinGridGeometry(double lat, double lon,
            String refTime, String validTime, String pluginName,
            String modelName) {

        ISpatialObject spatialArea = null;
        MathTransform crsFromLatLon = null;
        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
            CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
            DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());

            query.setMaxResults(new Integer(1));
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);

            try {
                List<GridRecord> recList = ((List<GridRecord>) dao
                        .queryByCriteria(query));
                if (recList.size() == 0) {
                    return false;
                } else {
                    GridRecord rec = recList.get(0);
                    spatialArea = rec.getSpatialObject();
                }

            } catch (DataAccessLayerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }

        try {
            crsFromLatLon = MapUtil
                    .getTransformFromLatLon(spatialArea.getCrs());
        } catch (FactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        DirectPosition lowerCorner = MapUtil.getGridGeometry(spatialArea)
                .getEnvelope().getLowerCorner();
        DirectPosition upperCorner = MapUtil.getGridGeometry(spatialArea)
                .getEnvelope().getUpperCorner();

        GeometryFactory gf = new GeometryFactory();

        Coordinate p1 = new Coordinate(lowerCorner.getOrdinate(0),
                lowerCorner.getOrdinate(1));
        Coordinate p2 = new Coordinate(lowerCorner.getOrdinate(0),
                upperCorner.getOrdinate(1));
        Coordinate p3 = new Coordinate(upperCorner.getOrdinate(0),
                upperCorner.getOrdinate(1));
        Coordinate p4 = new Coordinate(upperCorner.getOrdinate(0),
                lowerCorner.getOrdinate(1));

        LinearRing lr = gf.createLinearRing(new Coordinate[] { p1, p2, p3, p4,
                p1 });

        Polygon gridGeometry = gf.createPolygon(lr, null);

        DirectPosition ll = new GeneralDirectPosition(MapUtil.LATLON_PROJECTION);

        Coordinate coord = new Coordinate(lon, lat);
        ll.setOrdinate(0, coord.x);
        ll.setOrdinate(1, coord.y);
        // DirectPosition crs = new GeneralDirectPosition(spatialArea.getCrs());
        // try {
        // crsFromLatLon.transform(ll, crs);
        // } catch (MismatchedDimensionException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (TransformException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        //
        // Coordinate newC = new Coordinate(crs.getOrdinate(0),
        // crs.getOrdinate(1));
        Coordinate newC = new Coordinate(ll.getOrdinate(0), ll.getOrdinate(1));

        com.vividsolutions.jts.geom.Point p = gf.createPoint(newC);

        return gridGeometry.contains(p);

    }

    public static boolean isPointWithinGridGeometry2(double lat, double lon,
            String refTime, String validTime, String pluginName,
            String modelName) {

        ISpatialObject spatialArea = null;
        MathTransform crsFromLatLon = null;
        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
            CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
            DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());

            query.setMaxResults(new Integer(1));
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);

            try {
                List<GridRecord> recList = ((List<GridRecord>) dao
                        .queryByCriteria(query));
                if (recList.size() == 0) {
                    return false;
                } else {
                    GridRecord rec = recList.get(0);
                    spatialArea = rec.getSpatialObject();
                }

            } catch (DataAccessLayerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }

        Geometry g = spatialArea.getGeometry();

        GeometryFactory geometryFactory = new GeometryFactory();
        CoordinateSequence sequence = new CoordinateArraySequence(
                g.getCoordinates());

        Coordinate[] oldCoords = sequence.toCoordinateArray();
        Coordinate[] newCoords = new Coordinate[oldCoords.length];
        /*
         * adjust longitude for global grids whose lon span goes from 0 to 360
         * and the asked lon is negative.
         */
        for (Coordinate c : oldCoords) {
            double x = c.x;
            double y = c.y;
            double z = c.z;
            if (x >= 180.0 && x <= 360.0 && lon < 0.0) {
                lon = lon + 360.0;
                break;
            }
        }
        Coordinate coord = new Coordinate(lon, lat);

        LinearRing ring = new LinearRing(sequence, geometryFactory);
        Polygon gridGeometry = new Polygon(ring, null, geometryFactory);
        com.vividsolutions.jts.geom.Point p = geometryFactory
                .createPoint(coord);

        return gridGeometry.contains(p);

    }

    /**
     * Returns the value of surface layer for a specified location, time, and
     * model for grib or ncgrib data.
     * 
     * @param pnt
     *            location
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param modelName
     *            the name of the model
     * @return surface pressure
     * 
     *  DR17084
     */
    @SuppressWarnings("unchecked")
	public static NcSoundingLayer getModelSfcLayer(Point pnt, String refTime,
            String validTime, String pluginName, String modelName, Coordinate coordinate) {

        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
        	NcSoundingLayer soundingLy = new NcSoundingLayer();
            TableQuery query;
            try {
            	query = new TableQuery("metadata", GridRecord.class.getName());
            	query.addParameter(GridConstants.LEVEL_ONE, "0.0");
            	query.addParameter(GridConstants.LEVEL_TWO, "-999999.0");
            	query.addParameter(GridConstants.MASTER_LEVEL_NAME, "SFC");
            	query.addList(GridConstants.PARAMETER_ABBREVIATION, "P, GH");
            	query.addParameter(GridConstants.DATASET_ID, modelName);
            	query.addParameter("dataTime.refTime", refTime);
            	query.addParameter("dataTime.validPeriod.start", validTime);
            	List<GridRecord> recList = (List<GridRecord>) query.execute();
            	boolean presureAvailable=false, heightAvailable=false;
            	if (recList!=null && recList.size() > 0) {   		
            		for(GridRecord rec:recList ){
            			PointIn pointIn = new PointIn(pluginName, rec, pnt.x, pnt.y);                		
            			try {

            				float fdata = pointIn.getPointData();
            				String parm = rec.getParameter().getAbbreviation();
            				if(parm.equals("P")){
            					soundingLy.setPressure(fdata/100);
            					presureAvailable = true;
            				} if(parm.equals("GH")){
            					soundingLy.setGeoHeight(fdata);
            					heightAvailable = true;
            				} 
            				//System.out.println("prm="+rec.getParameter().getAbbreviation()+" value="+fdata);

            			} catch (PluginException e) {
            				// TODO Auto-generated catch block
            				e.printStackTrace();
            				return null;
            			}
            		}
            		recList.clear();
            	}        	
            	query = new TableQuery("metadata", GridRecord.class.getName());
            	query.addParameter(GridConstants.LEVEL_ONE, "2.0");
            	query.addParameter(GridConstants.LEVEL_TWO, "-999999.0");
            	query.addParameter(GridConstants.MASTER_LEVEL_NAME, "FHAG");
            	query.addList(GridConstants.PARAMETER_ABBREVIATION, "T, RH");
            	query.addParameter(GridConstants.DATASET_ID, modelName);
            	query.addParameter("dataTime.refTime", refTime);
            	query.addParameter("dataTime.validPeriod.start", validTime);
            	recList = (List<GridRecord>) query.execute();
            	if (recList!=null && recList.size() > 0) {
            		for(GridRecord rec:recList ){
            			PointIn pointIn = new PointIn(pluginName, rec, pnt.x, pnt.y);                		
            			try {

            				float fdata = pointIn.getPointData();
            				String parm = rec.getParameter().getAbbreviation();
            				if(parm.equals("T")){
            					soundingLy.setTemperature((float) kelvinToCelsius
            							.convert(fdata));
            				} if(parm.equals("RH")){
            					soundingLy.setRelativeHumidity(fdata);
            				} 
            				//System.out.println("prm="+rec.getParameter().getAbbreviation()+" value="+fdata);

            			} catch (PluginException e) {
            				// TODO Auto-generated catch block
            				e.printStackTrace();
            				return null;
            			}
            		}
            		recList.clear();
            	} 

            	query = new TableQuery("metadata", GridRecord.class.getName());
            	query.addParameter(GridConstants.LEVEL_ONE, "10.0");
            	query.addParameter(GridConstants.LEVEL_TWO, "-999999.0");
            	query.addParameter(GridConstants.MASTER_LEVEL_NAME, "FHAG");
            	query.addList(GridConstants.PARAMETER_ABBREVIATION, "vW, uW");
            	query.addParameter(GridConstants.DATASET_ID, modelName);
            	query.addParameter("dataTime.refTime", refTime);
            	query.addParameter("dataTime.validPeriod.start", validTime);
            	recList = (List<GridRecord>) query.execute();
            	if (recList!=null && recList.size() > 0) {
            		for(GridRecord rec:recList ){
            			PointIn pointIn = new PointIn(pluginName, rec, pnt.x, pnt.y);                		
            			try {

            				float fdata = pointIn.getPointData();
            				String parm = rec.getParameter().getAbbreviation();
            				if(parm.equals("vW")){
            					soundingLy.setWindV((float) metersPerSecondToKnots
            							.convert(fdata));
            				} if(parm.equals("uW")){
            					soundingLy.setWindU((float) metersPerSecondToKnots
            							.convert(fdata));
            				} 
            				//System.out.println("prm="+rec.getParameter().getAbbreviation()+" value="+fdata);

            			} catch (PluginException e) {
            				// TODO Auto-generated catch block
            				e.printStackTrace();
            				return null;
            			}
            		}
            	} 
            	if (presureAvailable==false || heightAvailable==false) {
            		float surfaceElevation	= NcSoundingProfile.MISSING;
            		TopoQuery topoQuery = TopoQuery.getInstance();
            		if (topoQuery != null) {
            			//System.out.println("Nsharp coordinate.x="+coordinate.x);
            			surfaceElevation = (float) topoQuery
            					.getHeight(coordinate);
            			if(surfaceElevation >=0)
            				soundingLy.setGeoHeight(surfaceElevation);
            			else {
                			if (presureAvailable==false)
                				//no pressure and no height, no hope to continue.
                				return null;             			
                		}
            		}
            		else {
            			if (presureAvailable==false)
            				//no pressure and no height, no hope to continue.
            				return null;             			
            		}
            	}
            	return soundingLy;          
            } catch (DataAccessLayerException e) {
            	// TODO Auto-generated catch block
            	e.printStackTrace();
            	return null;
            } catch (Exception e1) {
            	// TODO Auto-generated catch block
            	e1.printStackTrace();
            }
        }
        return null;

    }

    /**
     * Returns a list of NcSoundingProfile for a group of Point with specific
     * ref and range time, and model for grib or ncgrib data.
     * 
     * @param pnt
     *            location
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param modelName
     *            the name of the model
     * @param levels
     *            list of vertical levels
     * @return list of NcSoundingLayer objects
     * 
     *         Created @ 3/28/2012
     */

    private static List<NcSoundingProfile> queryProfileListByPointGroup(
            List<Point> points, String refTime, String validTime,
            String pluginName, String modelName, List<?> levels) {

        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        List<float[]> fdataArrayList = new ArrayList<float[]>();
        // long t01 = System.currentTimeMillis();
        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
            List<GridRecord> recList = new ArrayList<GridRecord>();
            ;
            TableQuery query;
            try {
                query = new TableQuery("metadata", GridRecord.class.getName());
                query.addParameter(GridConstants.MASTER_LEVEL_NAME, "MB");
                query.addParameter(GridConstants.DATASET_ID, modelName);
                query.addList(GridConstants.PARAMETER_ABBREVIATION, GRID_PARMS);
                query.addParameter("dataTime.refTime", refTime);
                query.addParameter("dataTime.validPeriod.start", validTime);
                query.setSortBy(GridConstants.LEVEL_ONE, false);
                recList = (List<GridRecord>) query.execute();
                if (recList.size() != 0) {
                    PointIn pointIn = new PointIn(pluginName, recList.get(0));
                    fdataArrayList = pointIn.getHDF5GroupDataPoints(
                            recList.toArray(), points);
                }
            } catch (DataAccessLayerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            int index = 0;
            GridGeometry2D geom = MapUtil.getGridGeometry(spatialArea);
            CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
            Coordinate coord = new Coordinate(45, 45);

            for (float[] fdataArray : fdataArrayList) {
                // one fdataArray is for one Point or say one profile
                NcSoundingProfile pf = new NcSoundingProfile();
                List<NcSoundingLayer> soundLyList = new ArrayList<NcSoundingLayer>();
                Point pnt = points.get(index);
                Object[] recArray = recList.toArray();
                for (Object level : levels) {
                    NcSoundingLayer soundingLy = new NcSoundingLayer();
                    double pressure = (Double) level;
                    soundingLy.setPressure((float) pressure);
                    for (int i = 0; i < recArray.length; i++) {
                        GridRecord rec1 = (GridRecord) recArray[i];
                        float fdata = fdataArray[i];
                        if (rec1.getLevel().getLevelonevalue() == pressure) {
                            String prm = rec1.getParameter().getAbbreviation();
                            //System.out.println("prm="+prm+" value="+fdata);
                            switch (GridParmNames.valueOf(prm)) {
                            case GH:
                                soundingLy.setGeoHeight(fdata);
                                break;
                            case uW:
                                // HDF5 data in unit of m/s, convert to Knots
                                // 4/12/2012
                                soundingLy
                                        .setWindU((float) metersPerSecondToKnots
                                                .convert(fdata));
                                break;
                            case vW:
                                // HDF5 data in unit of m/s, convert to Knots
                                // 4/12/2012
                                soundingLy
                                        .setWindV((float) metersPerSecondToKnots
                                                .convert(fdata));
                                break;
                            case T:
                                soundingLy
                                        .setTemperature((float) kelvinToCelsius
                                                .convert(fdata));
                                break;
                            case DWPK:
                                soundingLy.setDewpoint((float) kelvinToCelsius
                                        .convert(fdata));
                                break;
                            case OMEG:
                                soundingLy.setOmega(fdata);
                                break;
                            case RH:
                                soundingLy.setRelativeHumidity(fdata);
                                break;
                            case DpD:
                                soundingLy.setDpd(fdata);
                                break;
                            case DpT:
                            	soundingLy.setDewpoint((float) kelvinToCelsius
                                        .convert(fdata));
                                break;
                            case SPFH:
                            	soundingLy.setSpecHumidity(fdata);
                            	break;
                            default: 
                            	break;
                            }
                        }
                    }
                    soundLyList.add(soundingLy);
                }
                try {
                    coord = PointUtil.determineLatLon(pnt, crs, geom);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //System.out.println(" point coord.y="+coord.y+ " coord.x="+
                // coord.x);
                pf.setStationLatitude(coord.y);
                pf.setStationLongitude(coord.x);
                // DR17084
                NcSoundingLayer sfcLayer = getModelSfcLayer(pnt, refTime,
                                     validTime,pluginName, modelName, coord);
                if (sfcLayer != null) {
                	if(sfcLayer.getPressure()== NcSoundingLayer.MISSING && 
                			sfcLayer.getGeoHeight()!= NcSoundingLayer.MISSING){
                		//surface layer does not have pressure, but surface height is available
                		//see if we can interpolate surface pressure from upper and lower layer pressure
                		for (int i = 0; i < soundLyList.size(); i++) {
                			if (soundLyList.get(i).getGeoHeight() > sfcLayer.getGeoHeight()) {
                				if (i > 0) {
                					float p1 = soundLyList.get(i - 1).getPressure();
                					float p2 = soundLyList.get(i).getPressure();
                					float h1 = soundLyList.get(i - 1).getGeoHeight();
                					float h2 = soundLyList.get(i).getGeoHeight();
                					float h = sfcLayer.getGeoHeight();
                					float p = p1 + (h - h1) * (p1 - p2) / (h1 - h2);
                					sfcLayer.setPressure(p);
                				}
                				break;
                			}
                		}
                	}
                	if(sfcLayer.getPressure()!= NcSoundingLayer.MISSING){
                		// cut sounding layer under ground, i.e. below surface layer
                		for(int i=  soundLyList.size()-1; i>=0 ; i--){
                			NcSoundingLayer ly = soundLyList.get(i);
                			if(ly.getPressure() >= sfcLayer.getPressure()){
                				soundLyList.remove(i);
                			}
                		}
                		soundLyList.add(0, sfcLayer);
                	}
                	pf.setSfcPress(sfcLayer.getPressure());
                	pf.setStationElevation(sfcLayer.getGeoHeight());
                }
                //System.out.println("surface pressure ="+pf.getSfcPress());
                //end DR17084
                // calculate dew point if necessary
                MergeSounding ms = new MergeSounding();
                // ms.spfhToDewpoint(layerList);
                ms.rhToDewpoint(soundLyList);
                ms.dpdToDewpoint(soundLyList);
                pf.setSoundingLyLst(soundLyList);
                soundingProfileList.add(pf);
                index++;
            }
        }
        return soundingProfileList;
    }

    /**
     * Returns a list of NcSoundingLayer for a specified location, time, and
     * model for grib or ncgrib data.
     * 
     * @param pnt
     *            location
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param modelName
     *            the name of the model
     * @param levels
     *            list of vertical levels
     * @return list of NcSoundingLayer objects
     * 
     * 
     *         private static List<NcSoundingLayer>
     *         getModelSoundingLayerList(Point pnt, String refTime, String
     *         validTime, String pluginName, String modelName, List<?> levels) {
     *         List<NcSoundingLayer> soundLyList = new
     *         ArrayList<NcSoundingLayer>();
     * 
     *         //long t01 = System.currentTimeMillis(); if
     *         (pluginName.equalsIgnoreCase(NCGRIB_TBL_NAME)) {
     * 
     *         TableQuery query; try { query = new TableQuery("metadata",
     *         NcgribRecord.class.getName()); query.addParameter("vcord",
     *         "PRES"); query.addParameter("modelName", modelName);
     *         query.addList("parm",NC_PARMS);//parmList.toString()); //
     *         query.addParameter("dataTime.refTime", refTime);
     *         query.addParameter("dataTime.validPeriod.start", validTime);
     *         //query.addParameter("glevel1", level.toString());
     *         query.setSortBy("glevel1", false);
     * 
     * 
     *         List<NcgribRecord> recList = (List<NcgribRecord>)
     *         query.execute();
     *         System.out.println("Ncgrib group query0 result size ="+
     *         recList.size());
     * 
     *         if (recList.size() != 0) {
     * 
     *         PointIn pointIn = new PointIn(pluginName, recList.get(0), pnt.x,
     *         pnt.y); //Chin note: // We query all levels (pressure) and all
     *         parameters (at that level) at once. // The return array
     *         (fdataArray) are listed in the same order as query array
     *         (recList.toArray()) //However, returned array does not tell you
     *         which parameter itself is. //Therefore, we have to use
     *         information in query array to find out returned value's type
     *         (which parameter it is) // Further, we have to sort and store
     *         returned values to NcSoundingLayer based on its level (pressure)
     *         // Parameters in same level should be stored in one same
     *         NcSoundingLayer float[] fdataArray =
     *         pointIn.getHDF5GroupDataPoint(recList.toArray()); Object[]
     *         recArray = recList.toArray(); for (Object level : levels){
     *         NcSoundingLayer soundingLy = new NcSoundingLayer(); int pressure=
     *         (Integer)level; soundingLy.setPressure( pressure);
     * 
     *         for (int i=0; i < recArray.length; i++) { NcgribRecord rec1 =
     *         (NcgribRecord)recArray[i]; float fdata = fdataArray[i];
     *         if(rec1.getGlevel1() == pressure){ String prm = rec1.getParm();
     *         //System.out.println("point.x="+ pnt.x +
     *         " .y="+pnt.y+"pressure="+rec1 // .getGlevel1()+ " Parm="+prm );
     *         //long t01 = System.currentTimeMillis(); switch
     *         (NcParmNames.valueOf(prm)) { case HGHT:
     *         soundingLy.setGeoHeight(fdata); break; case UREL: // HDF5 data in
     *         unit of Knots, no conversion needed soundingLy.setWindU(fdata);
     *         break; case VREL: // HDF5 data in unit of Knots, no conversion
     *         needed soundingLy.setWindV(fdata); break; case TMPK:
     *         soundingLy.setTemperature((float) kelvinToCelsius
     *         .convert(fdata)); break; case DWPK:
     *         soundingLy.setDewpoint((float) kelvinToCelsius .convert(fdata));
     *         break; case SPFH: soundingLy.setSpecHumidity(fdata); break; case
     *         OMEG: soundingLy.setOmega(fdata); break; case RELH:
     *         soundingLy.setRelativeHumidity(fdata); break; } } }
     *         soundLyList.add(soundingLy); } }
     * 
     *         } catch (DataAccessLayerException e) { // TODO Auto-generated
     *         catch block e.printStackTrace(); } catch (Exception e) { // TODO
     *         Auto-generated catch block e.printStackTrace(); }
     *         //System.out.println("getModelSoundingLayerList:total level = "+
     *         totalLevel + " total records= "+totalRecords );
     * 
     *         } else if (pluginName.equalsIgnoreCase(D2DGRIB_TBL_NAME)) { try {
     *         TableQuery query = new TableQuery("metadata",
     *         GribRecord.class.getName());
     *         //query.addParameter("modelInfo.level.levelonevalue", //
     *         level.toString());
     *         //query.addParameter("modelInfo.level.leveltwovalue", //
     *         "-999999.0");
     *         query.addParameter("modelInfo.level.masterLevel.name", "MB");
     *         query.addParameter("modelInfo.modelName", modelName);
     *         query.addList("modelInfo.parameterAbbreviation", D2D_PARMS);
     *         query.addParameter("dataTime.refTime", refTime);
     *         query.addParameter("dataTime.validPeriod.start", validTime);
     *         query.setSortBy("modelInfo.level.levelonevalue", false);
     *         //System.out.println("level = "+ level.toString());
     * 
     *         List<GribRecord> recList = (List<GribRecord>) query.execute();
     *         System.out.println("Grib group query0 result size ="+
     *         recList.size());
     * 
     *         if (recList.size() > 0) { PointIn pointIn = new
     *         PointIn(pluginName, recList.get(0), pnt.x, pnt.y); float[]
     *         fdataArray = pointIn.getHDF5GroupDataPoint(recList.toArray());
     *         Object[] recArray = recList.toArray(); for (Object level :
     *         levels){ NcSoundingLayer soundingLy = new NcSoundingLayer();
     *         double pressure= (Double)level; soundingLy.setPressure(
     *         (float)pressure);
     * 
     *         for (int i=0; i < recArray.length; i++) { GribRecord rec1 =
     *         (GribRecord)recArray[i]; float fdata = fdataArray[i];
     *         if(rec1.getModelInfo().getLevelOneValue() == pressure){ String
     *         prm = rec1.getModelInfo().getParameterAbbreviation();
     *         //System.out.println("point.x="+ pnt.x +
     *         " .y="+pnt.y+"pressure="+pressure+ " Parm="+prm ); //long t01 =
     *         System.currentTimeMillis(); switch (D2DParmNames.valueOf(prm)) {
     *         case GH: soundingLy.setGeoHeight(fdata); break; case uW: // HDF5
     *         data in unit of Knots, no conversion needed
     *         soundingLy.setWindU(fdata); break; case vW: // HDF5 data in unit
     *         of Knots, no conversion needed soundingLy.setWindV(fdata); break;
     *         case T: soundingLy.setTemperature((float) kelvinToCelsius
     *         .convert(fdata)); break; case DWPK:
     *         soundingLy.setDewpoint((float) kelvinToCelsius .convert(fdata));
     *         break; case OMEG: soundingLy.setOmega(fdata); break; case RH:
     *         soundingLy.setRelativeHumidity(fdata); break; } } }
     *         soundLyList.add(soundingLy); } } } catch
     *         (DataAccessLayerException e) { // TODO Auto-generated catch block
     *         e.printStackTrace(); } catch (Exception e) { // TODO
     *         Auto-generated catch block e.printStackTrace(); } }
     * 
     *         //long t02 = System.currentTimeMillis();
     *         //System.out.println("MDL profile retreival took " + (t02 -
     *         t01));
     * 
     *         for(NcSoundingLayer layer: soundLyList){
     *         System.out.println("pre="+ layer.getPressure()+
     *         " h="+layer.getGeoHeight()+ " T="+layer.getTemperature()+" D="+
     *         layer.getDewpoint()+ " WS="+layer.getWindSpeed()+
     *         " WD="+layer.getWindDirection() + " SH="+layer.getSpecHumidity()+
     *         " RH="+layer.getRelativeHumidity()); } return soundLyList; }
     */
    /**
     * Return a list of data vertical levels for a specified time and model for
     * grib or ncgrib data.
     * 
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param modelName
     *            the name of the model
     * @return list of vertical levels
     */
    public static List<?> getModelLevels(String refTime, String validTime,
            String pluginName, String modelName) {

        // List<?>vals = null;
        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
            CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
            DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());
            query.addDistinctParameter(GridConstants.LEVEL_ONE);
            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION, "GH");
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "MB");

            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            query.addOrder(GridConstants.LEVEL_ONE, false);

            try {
                return (List<?>) dao.queryByCriteria(query);

            } catch (DataAccessLayerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }

        return null;

    }

    private static ISpatialObject spatialArea = null;

    /**
     * Returns the indices of the model grid of the closest point to the
     * specified latitude, longitude.
     * 
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @param level
     *            vertical level
     * @param pluginName
     *            the name of the data table ('grib' or 'ncgrib')
     * @param modelName
     *            the name of the model
     * @return the point indices
     */
    public static Point getLatLonIndices(double lat, double lon,
            String refTime, String validTime, String level, String pluginName,
            String modelName) {
        // ISpatialObject spatialArea = null;

        Point pnt = null;

        if (pluginName.equalsIgnoreCase(GRID_TBL_NAME)) {
            CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
            DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());

            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION, "GH");
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "MB");
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            query.addQueryParam(GridConstants.LEVEL_ONE, level);
            query.addQueryParam(GridConstants.LEVEL_TWO, "-999999.0");

            GridRecord rec;
            try {
                List<GridRecord> recList = ((List<GridRecord>) dao
                        .queryByCriteria(query));
                if (recList.size() == 0) {
                    return null;
                } else {
                    rec = recList.get(0);
                    spatialArea = rec.getSpatialObject();
                }
            } catch (DataAccessLayerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }

        } else
            return null;

        GridGeometry2D geom = MapUtil.getGridGeometry(spatialArea);

        CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
        Coordinate coord = new Coordinate(lon, lat);

        try {
            pnt = PointUtil.determineIndex(coord, crs, geom);
            Integer nx = spatialArea.getNx();
            Integer ny = spatialArea.getNy();

            if (pnt.x > nx || pnt.y > ny) {
                return null;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pnt;

    }

}
