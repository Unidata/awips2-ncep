package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.media.jai.InterpolationBilinear;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

import gov.noaa.nws.ncep.common.dataplugin.soundingrequest.SoundingServiceRequest;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingCube;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingModel;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingProfile;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTimeLines;

/**
 *
 * gov.noaa.nws.ncep.edex.uengine.tasks.profile.MdlSoundingQuery
 *
 * This java class performs the Grid model sounding data query functions. This
 * code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 04/04/2011   301         Chin Chen   Initial coding
 * 02/28/2012               Chin Chen   modify several sounding query algorithms for better performance
 * 03/28/2012               Chin Chen   Add new API to support query multiple Points at one shoot and using
 *                                      dataStore.retrieveGroups()
 * Oct 15, 2012 2473        bsteffen    Remove ncgrib
 * 03/2014      1116        T. Lee      Added DpD
 * 01/2015      DR#16959    Chin Chen   Added DpT support to fix DR 16959 NSHARP freezes when loading a sounding from
 *                                      HiRes-ARW/NMM models
 * 02/03/2015   DR#17084    Chin Chen   Model soundings being interpolated below the surface for elevated sites
 * 02/27/2015   RM#6641     Chin Chen   Retrieving model 2m FHAG Dew Point
 * 03/16/2015   RM#6674     Chin Chen   support model sounding query data interpolation and nearest point option
 * 04/29/2015   RM#7782     Chin Chen   NSHARP - Gridded wind direction correction
 * 05/06/2015   RM#7783     Chin Chen   NSHARP - Some models are not returning dew point
 * 05/15/2015   Rm#8160     Chin Chen   NSHARP - Add vertical velocity from model grids
 * 05/26/2015   RM#8306     Chin Chen   eliminate NSHARP dependence on uEngine.
 *                                      Copy whole file mdlSoundingQuery.java from uEngine project to this serverRequestService project.
 *                                      "refactor" and clean up unused code for this ticket.
 * Apr 18, 2018   17341 mgamazaychikov  If surface elevation is missing and topo query returns NaN
 *                                      set surface elevation to 0.0; cleaned up code.
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 */
public class ModelsSoundingQuery {
    private static final String GRID_TBL_NAME = "grid";
    // 7783: change specific humidity from SPFH to SH as defined in "parameter"
    // table.
    // Also remove DWPK and OMEG as they are not defined in "parameter" table
    private static String GRID_PARMS = "GH, uW, vW,T, SH, RH, DpD, DpT, PVV";

    private enum GridParmNames {
        GH, uW, vW, T, SH, RH, DpD, DpT, PVV
    };

    private static UnitConverter kelvinToCelsius = SI.KELVIN.getConverterTo(SI.CELSIUS);

    private static final UnitConverter metersPerSecondToKnots = SI.METERS_PER_SECOND.getConverterTo(NonSI.KNOT);

    private static float FRAC_ERROR = -9999;

    // Note; we are using NCInventory now. So, this api is actually not used.
    public static NcSoundingTimeLines getMdlSndTimeLine(String mdlType) {
        NcSoundingTimeLines tl = new NcSoundingTimeLines();
        Object[] refTimeAry = null;
        String queryStr = new String(
                "Select Distinct reftime FROM grid FULL JOIN grid_info ON grid.info_id=grid_info.id where grid_info.datasetid='"
                        + mdlType + "' ORDER BY reftime DESC");

        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        refTimeAry = dao.executeSQLQuery(queryStr);
        tl.setTimeLines(refTimeAry);

        return tl;
    }

    public static NcSoundingTimeLines getMdlSndRangeTimeLine(String mdlType, String refTimeStr) {
        NcSoundingTimeLines tl = new NcSoundingTimeLines();
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
                        + refTimeStr + ":00:00' AND grid.rangestart = grid.rangeend AND grid_info.datasetid='" + mdlType
                        + "' AND grid_info.parameter_abbreviation='T' order by rangestart");

        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        soundingTimeAry = dao.executeSQLQuery(queryStr);
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
                            + soundingTimeAry[i] + "' AND grid.rangestart = grid.rangeend AND grid_info.datasetid='"
                            + mdlType
                            + "' AND grid_info.parameter_abbreviation='T' AND level.levelonevalue > 99) X HAVING count(X.rangestart) >2");
            Object[] countAry = null;
            countAry = dao.executeSQLQuery(queryStr1);
            java.math.BigInteger count = new java.math.BigInteger("0");
            if (countAry.length > 0) {
                count = (java.math.BigInteger) countAry[0];
            }
            if (count.intValue() > 2) {
                Object timeLine = soundingTimeAry[i];
                reSoundingTimeAry.add(timeLine);
            }
        }

        tl.setTimeLines(reSoundingTimeAry.toArray());

        // }
        return tl;
    }

    /**
     * Returns a list of profile for location (lat,lon) array, time, and model
     * for grib data at 4 surrounding grib points and interpolate to request
     * location (lat/lon).
     *
     * @param double[][]
     *            latLonArray, e.g. at nth element, lat=[n][0], lon=[n][1]
     * @param refTimeCal
     *            data record reference time
     * @param validTimeCal
     *            data record valid time
     * @param mdlName
     *            the name of the model
     * @return the profile
     *
     *         RM#6674: Chin created @ 03/16/2015
     */
    private static List<NcSoundingProfile> getMdlSndInterpolatedDataProfileList(Coordinate[] latLonArray,
            String refTime, String validTime, String mdlName) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        List<?> levels = getModelLevels(refTime, validTime, mdlName);
        if (levels.size() == 0) {
            return soundingProfileList;
        }
        soundingProfileList = queryInterpolationProfileListByPointGroup(refTime, validTime, mdlName, levels,
                latLonArray);
        return soundingProfileList;

    }

    /**
     * Returns a list of profile for location (lat,lon) array, time, and model
     * for grib data at nearest point.
     *
     * @param double[][]
     *            latLonArray, e.g. at nth element, lat=[n][0], lon=[n][1]
     * @param refTimeCal
     *            data record reference time
     * @param validTimeCal
     *            data record valid time
     * @param mdlName
     *            the name of the model
     * @return the profile created @ 3/28/2012
     */
    private static List<NcSoundingProfile> getMdlSndNearestPtDataProfileList(Coordinate[] latLonArray, String refTime,
            String validTime, String mdlName) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        List<?> levels = getModelLevels(refTime, validTime, mdlName);
        if (levels.size() == 0) {
            return soundingProfileList;
        }
        soundingProfileList = queryNearestPtProfileListByPointGroup(refTime, validTime, mdlName, levels, latLonArray);

        return soundingProfileList;

    }

    // Note; we are using NCInventory now. So, this api is actually not used.
    public static NcSoundingModel getMdls() {
        NcSoundingModel mdls = new NcSoundingModel();
        CoreDao dao = new CoreDao(DaoConfig.forClass(GridInfoRecord.class));
        String queryStr = new String("Select Distinct modelname FROM grib_models ORDER BY modelname");
        Object[] mdlName = dao.executeSQLQuery(queryStr);

        if (mdlName != null && mdlName.length > 0) {
            List<String> mdlList = new ArrayList<String>();
            for (Object mn : mdlName) {
                mdlList.add((String) mn);
            }
            mdls.setMdlList(mdlList);
        }
        return mdls;
    }

    /**
     * Returns the value of surface layer for a specified location, time, and
     * model for grib or ncgrib data.
     *
     * @param pnt
     *            location
     * @param modelName
     *            the name of the model
     * @return surface pressure
     *
     *         DR17084
     */
    @SuppressWarnings("unchecked")
    private static NcSoundingLayer getModelSfcLayer(Point pnt, String refTime, String validTime, String modelName,
            ISpatialObject spatialArea) {

        NcSoundingLayer soundingLy = new NcSoundingLayer();
        TableQuery query;
        try {
            query = new TableQuery("metadata", GridRecord.class.getName());
            query.addParameter(GridConstants.LEVEL_ONE, "0.0");
            query.addParameter(GridConstants.LEVEL_TWO, "-999999.0");
            query.addParameter(GridConstants.MASTER_LEVEL_NAME, "SFC");
            // 7783, we intend to get P and GH for most models, but model
            // ECMWF-HiRes saves its T, DpT, vW, uW at SFC data set
            query.addList(GridConstants.PARAMETER_ABBREVIATION, "P, GH,  T, DpT, vW, uW");
            query.addParameter(GridConstants.DATASET_ID, modelName);
            query.addParameter("dataTime.refTime", refTime);
            query.addParameter("dataTime.validPeriod.start", validTime);
            List<GridRecord> recList = (List<GridRecord>) query.execute();
            boolean presAvailable = false, heightAvailable = false;
            if (recList != null && recList.size() > 0) {
                for (GridRecord rec : recList) {
                    PointIn pointIn = new PointIn(GRID_TBL_NAME, rec, pnt.x, pnt.y);
                    try {

                        float fdata = pointIn.getPointData();
                        String parm = rec.getParameter().getAbbreviation();
                        if (parm.equals("P")) {
                            soundingLy.setPressure(fdata / 100);
                            presAvailable = true;
                        } else if (parm.equals("GH")) {
                            soundingLy.setGeoHeight(fdata);
                            heightAvailable = true;
                        } else if (parm.equals("T")) {
                            soundingLy.setTemperature((float) kelvinToCelsius.convert(fdata));
                        } else if (parm.equals("DpT")) {
                            soundingLy.setDewpoint((float) kelvinToCelsius.convert(fdata));
                        } else if (parm.equals("vW")) {
                            soundingLy.setWindV((float) metersPerSecondToKnots.convert(fdata));
                        } else if (parm.equals("uW")) {
                            soundingLy.setWindU((float) metersPerSecondToKnots.convert(fdata));
                        }
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
            query.addList(GridConstants.PARAMETER_ABBREVIATION, "T, RH, DpT");
            query.addParameter(GridConstants.DATASET_ID, modelName);
            query.addParameter("dataTime.refTime", refTime);
            query.addParameter("dataTime.validPeriod.start", validTime);
            recList = (List<GridRecord>) query.execute();
            if (recList != null && recList.size() > 0) {
                for (GridRecord rec : recList) {
                    PointIn pointIn = new PointIn(GRID_TBL_NAME, rec, pnt.x, pnt.y);
                    try {

                        float fdata = pointIn.getPointData();
                        String parm = rec.getParameter().getAbbreviation();
                        if (parm.equals("T")) {
                            soundingLy.setTemperature((float) kelvinToCelsius.convert(fdata));
                        } else if (parm.equals("DpT")) {
                            soundingLy.setDewpoint((float) kelvinToCelsius.convert(fdata));
                        } else if (parm.equals("RH")) {
                            soundingLy.setRelativeHumidity(fdata);
                        }
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
            if (recList != null && recList.size() > 0) {
                for (GridRecord rec : recList) {
                    PointIn pointIn = new PointIn(GRID_TBL_NAME, rec, pnt.x, pnt.y);
                    try {

                        float fdata = pointIn.getPointData();
                        String parm = rec.getParameter().getAbbreviation();
                        if (parm.equals("vW")) {
                            soundingLy.setWindV((float) metersPerSecondToKnots.convert(fdata));
                        } else if (parm.equals("uW")) {
                            soundingLy.setWindU((float) metersPerSecondToKnots.convert(fdata));
                        }
                    } catch (PluginException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return null;
                    }
                }
            }
            if (presAvailable == false || heightAvailable == false) {
                float surfaceElevation = NcSoundingProfile.MISSING;
                TopoQuery topoQuery = TopoQuery.getInstance();
                if (topoQuery != null) {
                    GridGeometry2D geom = MapUtil.getGridGeometry(spatialArea);
                    CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
                    Coordinate coord = new Coordinate(45, 45);
                    try {
                        coord = PointUtil.determineLatLon(pnt, crs, geom);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    surfaceElevation = (float) topoQuery.getHeight(coord);
                    if (Float.isNaN(surfaceElevation)) {
                        surfaceElevation = 0.0f;
                    }
                    if (surfaceElevation >= 0) {
                        soundingLy.setGeoHeight(surfaceElevation);
                    } else {
                        if (presAvailable == false) {
                            // no pressure and no height, no hope to
                            // continue.
                            return null;
                        }
                    }
                } else {
                    if (presAvailable == false) {
                        // no pressure and no height, no hope to continue.
                        return null;
                    }
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

        return null;

    }

    /**
     * Returns a list of NcSoundingProfile for a group of Points (latLonArray)
     * with specific ref and range time, and model for grib or ncgrib data.
     *
     * @param pnt
     *            location
     * @param modelName
     *            the name of the model
     * @param levels
     *            list of vertical levels
     * @return list of NcSoundingLayer objects
     *
     *         Created @ 3/28/2012 #6674 update to supoort interpolation
     */

    private static List<NcSoundingProfile> queryNearestPtProfileListByPointGroup(String refTime, String validTime,
            String modelName, List<?> levels, Coordinate[] latLonArray) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        List<Point> points = new ArrayList<Point>();
        List<float[]> fdataArrayList = new ArrayList<float[]>();

        List<GridRecord> recList = new ArrayList<GridRecord>();
        ISpatialObject spatialArea = null;
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
            if (recList.size() > 0) {
                // #6674
                GridRecord gridRec = recList.get(0); // use any one GridRecord
                                                     // for all points..
                spatialArea = gridRec.getSpatialObject();
                double lat, lon;
                for (int k = 0; k < latLonArray.length; k++) {
                    lat = latLonArray[k].y;
                    lon = latLonArray[k].x;
                    Point pt = getNearestPt(lat, lon, spatialArea);
                    if (pt != null) {
                        points.add(pt);
                    }
                }
                // end #6674
                PointIn pointIn = new PointIn(GRID_TBL_NAME, gridRec);
                fdataArrayList = pointIn.getHDF5GroupDataPoints(recList.toArray(), points);
            } else {
                return soundingProfileList;
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
                        switch (GridParmNames.valueOf(prm)) {
                        case GH:
                            soundingLy.setGeoHeight(fdata);
                            break;
                        case uW:
                            // HDF5 data in unit of m/s, convert to Knots
                            // 4/12/2012
                            soundingLy.setWindU((float) metersPerSecondToKnots.convert(fdata));
                            break;
                        case vW:
                            // HDF5 data in unit of m/s, convert to Knots
                            // 4/12/2012
                            soundingLy.setWindV((float) metersPerSecondToKnots.convert(fdata));
                            break;
                        case T:
                            soundingLy.setTemperature((float) kelvinToCelsius.convert(fdata));
                            break;
                        case PVV:
                            soundingLy.setOmega(fdata);
                            break;
                        case DpT:
                            soundingLy.setDewpoint((float) kelvinToCelsius.convert(fdata));
                            break;
                        case RH:
                            soundingLy.setRelativeHumidity(fdata);
                            break;
                        case DpD:
                            soundingLy.setDpd(fdata);
                            break;
                        case SH:
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
            pf.setStationLatitude(coord.y);
            pf.setStationLongitude(coord.x);

            NcSoundingLayer sfcLayer = getModelSfcLayer(pnt, refTime, validTime, modelName, spatialArea);
            if (sfcLayer != null) {
                if (sfcLayer.getPressure() == NcSoundingLayer.MISSING
                        && sfcLayer.getGeoHeight() != NcSoundingLayer.MISSING) {
                    // surface layer does not have pressure, but surface
                    // height is available
                    // see if we can interpolate surface pressure from upper
                    // and lower layer pressure
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
                if (sfcLayer.getPressure() != NcSoundingLayer.MISSING) {
                    // cut sounding layer under ground, i.e. below surface
                    // layer
                    for (int i = soundLyList.size() - 1; i >= 0; i--) {
                        NcSoundingLayer ly = soundLyList.get(i);
                        if (ly.getPressure() >= sfcLayer.getPressure()) {
                            soundLyList.remove(i);
                        }
                    }
                    soundLyList.add(0, sfcLayer);
                }
                pf.setSfcPress(sfcLayer.getPressure());
                pf.setStationElevation(sfcLayer.getGeoHeight());
            }
            // convert dew point if necessary
            MergeSounding ms = new MergeSounding();
            // RM#7783
            ms.convertDewpoint(soundLyList);
            pf.setSoundingLyLst(soundLyList);
            // wind direction correction
            adjustWindDirectionToEarthRelative(coord.y, coord.x, spatialArea, pf);
            soundingProfileList.add(pf);
            index++;
        }

        return soundingProfileList;
    }

    private static NcSoundingProfile performInterpolation(List<NcSoundingProfile> soundingProfileList, float xfrac,
            float yfrac) {
        // the soundingProfileList now contains 1, 2, or 4 profiles depending
        // on the size of
        // surroundPoints of the requested location.
        // do interpolation to interpolate those profiles to one profile
        if (soundingProfileList.size() == 1) {
            // interpolation is not necessary
            return soundingProfileList.get(0);
        }
        InterpolationBilinear intpLinear = new InterpolationBilinear();
        NcSoundingProfile intpedPf = new NcSoundingProfile();
        if (soundingProfileList.size() == 4) {
            NcSoundingProfile pf1 = soundingProfileList.get(0);
            NcSoundingProfile pf2 = soundingProfileList.get(1);
            NcSoundingProfile pf3 = soundingProfileList.get(2);
            NcSoundingProfile pf4 = soundingProfileList.get(3);

            // Note: the 4 profiles should have same number of layers
            for (int ii = 0; ii < pf1.getSoundingLyLst().size(); ii++) {
                NcSoundingLayer pf1Ly = pf1.getSoundingLyLst().get(ii); // s00
                NcSoundingLayer pf2Ly = pf2.getSoundingLyLst().get(ii); // s01
                NcSoundingLayer pf3Ly = pf3.getSoundingLyLst().get(ii); // s10
                NcSoundingLayer pf4Ly = pf4.getSoundingLyLst().get(ii); // s11
                NcSoundingLayer intpedLy = new NcSoundingLayer();
                intpedLy.setPressure(intpLinear.interpolate(pf1Ly.getPressure(), pf2Ly.getPressure(),
                        pf3Ly.getPressure(), pf4Ly.getPressure(), xfrac, yfrac));
                intpedLy.setTemperature(intpLinear.interpolate(pf1Ly.getTemperature(), pf2Ly.getTemperature(),
                        pf3Ly.getTemperature(), pf4Ly.getTemperature(), xfrac, yfrac));
                intpedLy.setDewpoint(intpLinear.interpolate(pf1Ly.getDewpoint(), pf2Ly.getDewpoint(),
                        pf3Ly.getDewpoint(), pf4Ly.getDewpoint(), xfrac, yfrac));
                intpedLy.setGeoHeight(intpLinear.interpolate(pf1Ly.getGeoHeight(), pf2Ly.getGeoHeight(),
                        pf3Ly.getGeoHeight(), pf4Ly.getGeoHeight(), xfrac, yfrac));
                intpedLy.setOmega(intpLinear.interpolate(pf1Ly.getOmega(), pf2Ly.getOmega(), pf3Ly.getOmega(),
                        pf4Ly.getOmega(), xfrac, yfrac));
                // for wind, we interpolate u and v components first and then
                // convert them to speed and direction.
                float windV = intpLinear.interpolate(pf1Ly.getWindV(), pf2Ly.getWindV(), pf3Ly.getWindV(),
                        pf4Ly.getWindV(), xfrac, yfrac);
                float windU = intpLinear.interpolate(pf1Ly.getWindU(), pf2Ly.getWindU(), pf3Ly.getWindU(),
                        pf4Ly.getWindU(), xfrac, yfrac);
                intpedLy.updateWindSpdDir(windV, windU);
                intpedPf.getSoundingLyLst().add(intpedLy);
            }
        } else if (soundingProfileList.size() == 2) {
            NcSoundingProfile pf1 = soundingProfileList.get(0);
            NcSoundingProfile pf2 = soundingProfileList.get(1);
            for (int ii = 0; ii < pf1.getSoundingLyLst().size(); ii++) {
                NcSoundingLayer pf1Ly = pf1.getSoundingLyLst().get(ii); // s00
                NcSoundingLayer pf2Ly = pf2.getSoundingLyLst().get(ii); // s01
                                                                        // or
                                                                        // s10
                NcSoundingLayer intpedLy = new NcSoundingLayer();
                float frac;
                if (xfrac == FRAC_ERROR) {
                    frac = yfrac;
                } else {
                    frac = xfrac;
                }
                // Note: it is ok to use either interpolateH or interpolateV
                intpedLy.setPressure(intpLinear.interpolateH(pf1Ly.getPressure(), pf2Ly.getPressure(), frac));
                intpedLy.setTemperature(intpLinear.interpolateH(pf1Ly.getTemperature(), pf2Ly.getTemperature(), frac));
                intpedLy.setDewpoint(intpLinear.interpolateH(pf1Ly.getDewpoint(), pf2Ly.getDewpoint(), frac));
                intpedLy.setGeoHeight(intpLinear.interpolateH(pf1Ly.getGeoHeight(), pf2Ly.getGeoHeight(), frac));
                intpedLy.setOmega(intpLinear.interpolateH(pf1Ly.getOmega(), pf2Ly.getOmega(), frac));
                float windV = intpLinear.interpolateH(pf1Ly.getWindV(), pf2Ly.getWindV(), frac);
                float windU = intpLinear.interpolateH(pf1Ly.getWindU(), pf2Ly.getWindU(), frac);
                intpedLy.updateWindSpdDir(windV, windU);
                intpedPf.getSoundingLyLst().add(intpedLy);
            }
        }
        return intpedPf;
    }

    /**
     * Returns a list of NcSoundingProfile for a group of points (latLonArray)
     * with specific ref and range time, and model for grib or ncgrib data.
     * Linear interpolated with the 4 surrounding grid points for each request
     * loction (lat/lon).
     *
     * @param latLonArray
     *            location lat/lon array
     * @param modelName
     *            the name of the model
     * @param levels
     *            list of vertical levels
     * @return list of NcSoundingLayer objects
     *
     *         Chin Created @ 3/11/2015 for ticket #6674
     *
     */
    private static List<NcSoundingProfile> queryInterpolationProfileListByPointGroup(String refTime, String validTime,
            String modelName, List<?> levels, Coordinate[] latLonArray) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<NcSoundingProfile>();
        List<NcSoundingProfile> returnProfileList = new ArrayList<NcSoundingProfile>();
        List<float[]> fdataArrayList = new ArrayList<float[]>();
        List<List<Point>> pntLstLst = new ArrayList<List<Point>>(); // one point
                                                                    // list
                                                                    // represent
                                                                    // for one
                                                                    // request
                                                                    // location(lat/lon)
        List<GridRecord> recList = new ArrayList<GridRecord>();
        ISpatialObject spatialArea = null;
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
            if (recList.size() > 0) {
                GridRecord gridRec = recList.get(0); // use any one GridRecord
                                                     // for all points..
                spatialArea = gridRec.getSpatialObject();
                double lat, lon;
                float[] xyfrac = { 0f, 0f }; // {xfrac, yfrac}
                for (int k = 0; k < latLonArray.length; k++) {
                    // FOR each request location
                    lat = latLonArray[k].y;
                    lon = latLonArray[k].x;
                    List<Point> surroundPoints = getSurroundingPoints(lat, lon, spatialArea, xyfrac);
                    if (surroundPoints != null) {
                        // the returned "surroundPoints" should contain 1, 2, or
                        // 4 points surrounding the request point,
                        // and they are used for interpolation to the one
                        // requested location (lat/lon) later
                        pntLstLst.add(surroundPoints);
                        PointIn pointIn = new PointIn(GRID_TBL_NAME, gridRec);
                        fdataArrayList = pointIn.getHDF5GroupDataPoints(recList.toArray(), surroundPoints);
                        int index = 0;
                        Object[] recArray = recList.toArray();
                        for (float[] fdataArray : fdataArrayList) {
                            // one fdataArray is for one grid Point profile
                            NcSoundingProfile pf = new NcSoundingProfile();
                            List<NcSoundingLayer> soundLyList = new ArrayList<NcSoundingLayer>();
                            Point pnt = surroundPoints.get(index);
                            for (Object level : levels) {
                                NcSoundingLayer soundingLy = new NcSoundingLayer();
                                double pressure = (Double) level;
                                soundingLy.setPressure((float) pressure);
                                for (int i = 0; i < recArray.length; i++) {
                                    GridRecord rec1 = (GridRecord) recArray[i];
                                    float fdata = fdataArray[i];
                                    if (rec1.getLevel().getLevelonevalue() == pressure) {
                                        String prm = rec1.getParameter().getAbbreviation();
                                        switch (GridParmNames.valueOf(prm)) {
                                        case GH:
                                            soundingLy.setGeoHeight(fdata);
                                            break;
                                        case uW:
                                            // HDF5 data in unit of m/s, convert
                                            // to Knots
                                            // 4/12/2012
                                            soundingLy.updateWindU((float) metersPerSecondToKnots.convert(fdata));
                                            break;
                                        case vW:
                                            // HDF5 data in unit of m/s, convert
                                            // to Knots
                                            // 4/12/2012
                                            soundingLy.updateWindV((float) metersPerSecondToKnots.convert(fdata));
                                            break;
                                        case T:
                                            soundingLy.setTemperature((float) kelvinToCelsius.convert(fdata));
                                            break;
                                        case PVV:
                                            soundingLy.setOmega(fdata);
                                            break;
                                        case DpT:
                                            soundingLy.setDewpoint((float) kelvinToCelsius.convert(fdata));
                                            break;
                                        case RH:
                                            soundingLy.setRelativeHumidity(fdata);
                                            break;
                                        case DpD:
                                            soundingLy.setDpd(fdata);
                                            break;
                                        case SH:
                                            soundingLy.setSpecHumidity(fdata);
                                            break;
                                        default:
                                            break;
                                        }
                                    }
                                }
                                soundLyList.add(soundingLy);
                            }
                            NcSoundingLayer sfcLayer = getModelSfcLayer(pnt, refTime, validTime, modelName,
                                    spatialArea);

                            if (sfcLayer != null) {
                                if (sfcLayer.getPressure() == NcSoundingLayer.MISSING
                                        && sfcLayer.getGeoHeight() != NcSoundingLayer.MISSING) {
                                    // surface layer does not have pressure, but
                                    // surface
                                    // height is available
                                    // see if we can interpolate surface
                                    // pressure from upper
                                    // and lower layer pressure
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
                                if (sfcLayer.getPressure() != NcSoundingLayer.MISSING) {
                                    // add sfc layer as first element
                                    soundLyList.add(0, sfcLayer);
                                }

                            }
                            // calculate dew point if necessary
                            MergeSounding ms = new MergeSounding();
                            // RM#7783
                            ms.convertDewpoint(soundLyList);
                            pf.setSoundingLyLst(soundLyList);
                            soundingProfileList.add(pf);
                            index++;
                        }

                        // the soundingProfileList noew contains 1, 2, or 4
                        // profiles depending on the size of
                        // surroundPoints of the requested location.
                        // do interpolation to interpolate those profiles to one
                        // profile
                        NcSoundingProfile intpedPf = performInterpolation(soundingProfileList, xyfrac[0], xyfrac[1]);
                        // cut sounding layer under ground, i.e. removed layers
                        // below surface layer,
                        // note that surface layer was added at layer 0
                        List<NcSoundingLayer> intpedSoundLyList = intpedPf.getSoundingLyLst();
                        NcSoundingLayer intpedSfcLyr = intpedSoundLyList.get(0);
                        for (int i = intpedSoundLyList.size() - 1; i > 0; i--) {
                            NcSoundingLayer ly = intpedSoundLyList.get(i);
                            if (ly.getPressure() >= intpedSfcLyr.getPressure()) {
                                intpedSoundLyList.remove(i);
                            }
                        }
                        intpedPf.setSfcPress(intpedSfcLyr.getPressure());
                        intpedPf.setStationElevation(intpedSfcLyr.getGeoHeight());
                        intpedPf.setStationLatitude(lat);
                        intpedPf.setStationLongitude(lon);
                        // wind direction correction
                        adjustWindDirectionToEarthRelative(lat, lon, spatialArea, intpedPf);
                        returnProfileList.add(intpedPf);
                    }
                }
            }
        } catch (DataAccessLayerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnProfileList;
    }

    /**
     * Return a list of data vertical levels for a specified time and model for
     * grib or ncgrib data.
     *
     * @param modelName
     *            the name of the model
     * @return list of vertical levels
     */
    private static List<?> getModelLevels(String refTime, String validTime, String modelName) {
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
            return dao.queryByCriteria(query);

        } catch (DataAccessLayerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the 1, 2, or 4 surrounding grid points of the model of the
     * specified latitude, longitude.
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @param rec
     *            a GridRecord
     * @param xyfrac
     *            return value, { x fraction of the requested point, y fraction
     *            of the requested point}
     * @return the list of the 4 surrounding points defined by the following
     *         graph p is the request point. s00, s01, s10, s11 are the
     *         surrounding 4 points
     *
     *         xfloor xceil s00 s01 yfloor
     *
     *         p < yfrac
     *
     *         s10 s11 yceil ^ xfrac
     *
     *         Note 1:: the 4 points will be used for interpolation by using
     *         javax.media.jai.InterpolationBilinear class Note 2:: In case the
     *         request location is a "point" itself, e.g p=s00, then just return
     *         one point Note 3:: In case the request location is on the "line"
     *         of the 4 points, then only return 2 points, (s00, s01) OR (s00,
     *         s10) Note 4:: In case the request location is within the "area"
     *         of the 4 points, then return 4 points, in the order s00, s01,
     *         s10, s11
     * @author cchen 03/11/2015 for RM#6674
     */
    private static List<Point> getSurroundingPoints(double lat, double lon, ISpatialObject spatialArea,
            float[] xyfrac) {
        List<Point> points = new ArrayList<Point>();
        float xfrac, yfrac;

        GridGeometry2D geom = MapUtil.getGridGeometry(spatialArea);

        CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
        Coordinate coord = new Coordinate(lon, lat);

        try {
            DirectPosition2D exactPt = PointUtil.determineExactIndex(coord, crs, geom);
            Integer nx = spatialArea.getNx();
            Integer ny = spatialArea.getNy();
            if (exactPt.x > nx || exactPt.y > ny) {
                return null;
            }
            int xfloor = (int) Math.floor(exactPt.x);
            int yfloor = (int) Math.floor(exactPt.y);
            int xceil = (int) Math.ceil(exactPt.x);
            int yceil = (int) Math.ceil(exactPt.y);
            xfrac = (float) ((exactPt.x - xfloor) / (xceil - xfloor));
            yfrac = (float) ((exactPt.y - yfloor) / (yceil - yfloor));

            if (xfloor == xceil) {
                if (yfloor == yceil) {
                    // the request location is a "point" itself, just return one
                    // point
                    points.add(new Point(xfloor, yfloor));
                    xfrac = FRAC_ERROR;
                    yfrac = FRAC_ERROR;
                } else {
                    // the request location is located at s00-s10 line, return
                    // two points
                    points.add(new Point(xfloor, yfloor)); // s00
                    points.add(new Point(xfloor, yceil)); // s10
                    xfrac = FRAC_ERROR;
                }
            } else {
                if (yfloor == yceil) {
                    // the request location is located at s00-s01 line, return
                    // two points
                    points.add(new Point(xfloor, yfloor)); // s00
                    points.add(new Point(xceil, yfloor)); // s01
                    yfrac = FRAC_ERROR;
                } else {
                    // the request point is inside the 4 points "area", return 4
                    // points
                    points.add(new Point(xfloor, yfloor)); // s00
                    points.add(new Point(xceil, yfloor)); // s01
                    points.add(new Point(xfloor, yceil)); // s10
                    points.add(new Point(xceil, yceil)); // s11
                }
            }
            xyfrac[0] = xfrac;
            xyfrac[1] = yfrac;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return points;

    }

    /**
     * Returns the points of the model grid of the closest point to the
     * specified latitude, longitude.
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @param ISpatialObject
     *            spatialArea
     * @return the point indices
     */

    private static Point getNearestPt(double lat, double lon, ISpatialObject spatialArea) {
        Point pnt = null;

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

    /**
     * Adjust wind direction from grid relative to earth relative of each layer
     * in profile's sounding list
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @param ISpatialObject
     *            spatialArea
     * @param NcSoundingProfile
     *            profile
     * @author cchen created 04/29/2015
     */
    private static void adjustWindDirectionToEarthRelative(double lat, double lon, ISpatialObject so,
            NcSoundingProfile pf) {
        Coordinate coord = new Coordinate(lon, lat);
        double angle = MapUtil.rotation(coord, so);
        // if angle is positive X degree, then UP (grid's North) direction is
        // 360-X degrees and X degrees to the right of
        // UP is earth north (or 360 degrees).
        List<NcSoundingLayer> soundLyList = pf.getSoundingLyLst();
        for (NcSoundingLayer ly : soundLyList) {
            float correctedWindDir = ly.getWindDirection() + (float) angle;
            if (correctedWindDir >= 360) {
                correctedWindDir = correctedWindDir - 360;
            }
            ly.setWindDirection(correctedWindDir);
        }
    }

    public static NcSoundingCube handleGridModelDataRequest(SoundingServiceRequest request) {
        String[] refTimeStrArr = request.getRefTimeStrAry();
        if (refTimeStrArr == null && request.getRefTimeAry() != null) {
            refTimeStrArr = QueryMiscTools.convertTimeLongArrayToStrArray(request.getRefTimeAry());
        }
        String[] rangeTimeStrArr = request.getRangeStartTimeStrAry();
        if (rangeTimeStrArr == null && request.getRangeStartTimeAry() != null) {
            rangeTimeStrArr = QueryMiscTools.convertTimeLongArrayToStrArray(request.getRangeStartTimeAry());
        }
        if (request.getLatLonAry() != null && refTimeStrArr != null && refTimeStrArr.length > 0
                && rangeTimeStrArr != null && rangeTimeStrArr.length > 0) {
            NcSoundingCube cube = new NcSoundingCube();
            cube.setRtnStatus(NcSoundingCube.QueryStatus.FAILED); // assume
                                                                  // query
                                                                  // failure
            List<NcSoundingProfile> soundingProfileList;
            List<NcSoundingProfile> finalSoundingProfileList = new ArrayList<NcSoundingProfile>();
            String refTime = refTimeStrArr[0];
            for (String validTimeStartStr : rangeTimeStrArr) {
                if (request.isInterpolation() == true) {
                    // get interpolation point
                    soundingProfileList = getMdlSndInterpolatedDataProfileList(request.getLatLonAry(), refTime,
                            validTimeStartStr, request.getModelType());
                } else {
                    // get nearest point data
                    soundingProfileList = getMdlSndNearestPtDataProfileList(request.getLatLonAry(), refTime,
                            validTimeStartStr, request.getModelType());
                }
                finalSoundingProfileList.addAll(soundingProfileList);
            }
            if (finalSoundingProfileList.size() > 0) {
                cube.setRtnStatus(NcSoundingCube.QueryStatus.OK); // as long as
            }
            // one query
            // successful,
            // set it to
            // OK
            cube.setSoundingProfileList(finalSoundingProfileList);
            return cube;
        } else {
            return null;
        }
    }
}
