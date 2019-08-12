package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.media.jai.InterpolationBilinear;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
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
 * Date          Ticket#  Engineer        Description
 * ------------- -------- --------------- --------------------------------------
 * Apr 04, 2011  301      Chin Chen       Initial coding
 * Feb 28, 2012           Chin Chen       modify several sounding query
 *                                        algorithms for better performance
 * Mar 28, 2012           Chin Chen       Add new API to support query multiple
 *                                        Points at one shoot and using
 *                                        dataStore.retrieveGroups()
 * Oct 15, 2012  2473     bsteffen        Remove ncgrib
 * 03/2014       1116     T. Lee          Added DpD
 * 01/2015       16959    Chin Chen       Added DpT support to fix DR 16959
 *                                        NSHARP freezes when loading a sounding
 *                                        from HiRes-ARW/NMM models
 * Feb 03, 2015  17084    Chin Chen       Model soundings being interpolated
 *                                        below the surface for elevated sites
 * Feb 27, 2015  6641     Chin Chen       Retrieving model 2m FHAG Dew Point
 * Mar 16, 2015  6674     Chin Chen       support model sounding query data
 *                                        interpolation and nearest point option
 * Apr 29, 2015  7782     Chin Chen       NSHARP - Gridded wind direction
 *                                        correction
 * May 06, 2015  7783     Chin Chen       NSHARP - Some models are not returning
 *                                        dew point
 * May 15, 2015  Rm#8160  Chin Chen       NSHARP - Add vertical velocity from
 *                                        model grids
 * May 26, 2015  8306     Chin Chen       eliminate NSHARP dependence on
 *                                        uEngine. Copy whole file
 *                                        mdlSoundingQuery.java from uEngine
 *                                        project to this serverRequestService
 *                                        project. "refactor" and clean up
 *                                        unused code for this ticket.
 * Apr 10, 2017  30518    nabowle         Load surface data for multiple points
 *                                        simultaneously to speed up requests.
 * Apr 18, 2018  17341    mgamazaychikov  If surface elevation is missing and
 *                                        topo query returns NaN set surface
 *                                        elevation to 0.0; cleaned up code.
 * May 07, 2018  7283     mapeters        Fully eliminate uEngine dependence
 * Jun 22, 2018  17341    mgamazaychikov  Reconcile differences with 30518.
 * Jul 12, 2019           tjensen         Updated to support multiple H5s per
 *                                        model
 *
 * </pre>
 *
 * @author Chin Chen
 */
public class ModelsSoundingQuery {
    private static final Logger logger = LoggerFactory
            .getLogger(ModelsSoundingQuery.class);

    // Change specific humidity from SPFH to SH as defined in "parameter" table
    // Also remove DWPK and OMEG as they are not defined in "parameter" table
    private static String GRID_PARMS = "GH, uW, vW,T, SH, RH, DpD, DpT, PVV";

    private enum GridParmNames {
        GH, uW, vW, T, SH, RH, DpD, DpT, PVV
    }

    private static UnitConverter kelvinToCelsius = SI.KELVIN
            .getConverterTo(SI.CELSIUS);

    private static final UnitConverter metersPerSecondToKnots = SI.METERS_PER_SECOND
            .getConverterTo(NonSI.KNOT);

    private static float FRAC_ERROR = -9999;

    // Note; we are using NCInventory now. So, this api is actually not used
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

    public static NcSoundingTimeLines getMdlSndRangeTimeLine(String mdlType,
            String refTimeStr) {
        NcSoundingTimeLines tl = new NcSoundingTimeLines();
        /*
         * Chin: modified for Unified Grid DB make sure data in DB is not just
         * nHour data, as those data are not used by Nsharp. And when query to
         * it, the returned will be null. We do not want to show such sounding
         * time line to user. use this SQL query string for gfs as example.
         */
        /*
         * Select Distinct rangestart FROM grid FULL JOIN grid_info ON
         * grid.info_id=grid_info.id where grid.reftime = '2012-01-26 00:00:00'
         * AND grid.rangestart = grid.rangeend AND
         * grid_info.datasetid='mesoEta212' AND
         * grid_info.parameter_abbreviation='T' order by rangestart
         */
        Object[] soundingTimeAry = null;
        List<Object> reSoundingTimeAry = new ArrayList<>();
        String queryStr = new String(
                "Select Distinct rangestart FROM grid FULL JOIN grid_info ON grid.info_id=grid_info.id where grid.reftime = '"
                        + refTimeStr
                        + ":00:00' AND grid.rangestart = grid.rangeend AND grid_info.datasetid='"
                        + mdlType
                        + "' AND grid_info.parameter_abbreviation='T' order by rangestart");

        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        soundingTimeAry = dao.executeSQLQuery(queryStr);
        for (Object timeLine : soundingTimeAry) {
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
                            + timeLine
                            + "' AND grid.rangestart = grid.rangeend AND grid_info.datasetid='"
                            + mdlType
                            + "' AND grid_info.parameter_abbreviation='T' AND level.levelonevalue > 99) X HAVING count(X.rangestart) >2");
            Object[] countAry = null;
            countAry = dao.executeSQLQuery(queryStr1);
            java.math.BigInteger count = new java.math.BigInteger("0");
            if (countAry.length > 0) {
                count = (java.math.BigInteger) countAry[0];
            }
            if (count.intValue() > 2) {
                reSoundingTimeAry.add(timeLine);
            }
        }

        tl.setTimeLines(reSoundingTimeAry.toArray());
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
    private static List<NcSoundingProfile> getMdlSndInterpolatedDataProfileList(
            Coordinate[] latLonArray, String refTime, String validTime,
            String mdlName) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<>();
        List<?> levels = getModelLevels(refTime, validTime, mdlName);
        if (levels.isEmpty()) {
            logger.info("getModelLevels return 0;  file=" + refTime + " stime="
                    + validTime + " modeltype=" + mdlName);
            return soundingProfileList;
        }
        soundingProfileList = queryInterpolationProfileListByPointGroup(refTime,
                validTime, mdlName, levels, latLonArray);
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
    private static List<NcSoundingProfile> getMdlSndNearestPtDataProfileList(
            Coordinate[] latLonArray, String refTime, String validTime,
            String mdlName) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<>();
        List<?> levels = getModelLevels(refTime, validTime, mdlName);
        if (levels.isEmpty()) {
            logger.info("getModelLevels return 0;  file=" + refTime + " stime="
                    + validTime + " modeltype=" + mdlName);
            return soundingProfileList;
        }
        soundingProfileList = queryNearestPtProfileListByPointGroup(refTime,
                validTime, mdlName, levels, latLonArray);
        return soundingProfileList;
    }

    // Note: we are using NCInventory now. So, this api is actually not used.
    public static NcSoundingModel getMdls() {
        NcSoundingModel mdls = new NcSoundingModel();
        CoreDao dao = new CoreDao(DaoConfig.forClass(GridInfoRecord.class));
        String queryStr = new String(
                "Select Distinct modelname FROM grib_models ORDER BY modelname");
        Object[] mdlName = dao.executeSQLQuery(queryStr);

        if (mdlName != null && mdlName.length > 0) {
            List<String> mdlList = new ArrayList<>();
            for (Object mn : mdlName) {
                mdlList.add((String) mn);
            }
            mdls.setMdlList(mdlList);
        }
        return mdls;
    }

    /**
     * Returns the surface layers for specified locations, time, and model for
     * grib or ncgrib data.
     *
     * @param points
     *            The locations of the layers.
     * @param refTime
     *            The reftime
     * @param validTime
     *            The valid time start.
     * @param modelName
     *            The name of the model.
     * @param spatialArea
     *            The spatial area.
     * @return The computed surface layers.
     */
    @SuppressWarnings("unchecked")
    private static List<NcSoundingLayer> getModelSfcLayers(List<Point> points,
            String refTime, String validTime, String modelName,
            ISpatialObject spatialArea) {

        List<NcSoundingLayer> layers = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            layers.add(new NcSoundingLayer());
        }

        boolean[] presAvailable = new boolean[layers.size()];
        boolean[] heightAvailable = new boolean[layers.size()];

        // Create the surface sounding layer from SFC and/or FHAG grid records
        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());
        try {
            query.addQueryParam(GridConstants.LEVEL_ONE, "0.0");
            query.addQueryParam(GridConstants.LEVEL_TWO, "-999999.0");
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "SFC");
            /*
             * 7783, we intend to get P and GH for most models, but model
             * ECMWF-HiRes saves its T, DpT, vW, uW at SFC data set
             */
            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION,
                    "P, GH,  T, DpT, vW, uW", "in");
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            List<GridRecord> recList = (List<GridRecord>) dao
                    .queryByCriteria(query);
            updateLayers(points, layers, presAvailable, heightAvailable,
                    recList);

            query = new DatabaseQuery(GridRecord.class.getName());
            query.addQueryParam(GridConstants.LEVEL_ONE, "2.0");
            query.addQueryParam(GridConstants.LEVEL_TWO, "-999999.0");
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "FHAG");
            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION,
                    "T, RH, DpT", "in");
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            recList = (List<GridRecord>) dao.queryByCriteria(query);
            /*
             * combine the two lists of FHAG records to read from the datastore
             * only once
             */
            List<GridRecord> fhagRecList = new ArrayList<>();
            if (recList != null) {
                fhagRecList.addAll(recList);
            }

            query = new DatabaseQuery(GridRecord.class.getName());
            query.addQueryParam(GridConstants.LEVEL_ONE, "10.0");
            query.addQueryParam(GridConstants.LEVEL_TWO, "-999999.0");
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "FHAG");
            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION, "vW, uW",
                    "in");
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            recList = (List<GridRecord>) dao.queryByCriteria(query);
            if (recList != null) {
                fhagRecList.addAll(recList);
            }
            updateLayers(points, layers, presAvailable, heightAvailable,
                    fhagRecList);

            /*
             * Validate that each surface level has a pressure level or
             * geoheight
             */
            for (int i = 0; i < points.size(); i++) {
                NcSoundingLayer soundingLy = layers.get(i);
                if (!presAvailable[i] || !heightAvailable[i]) {
                    float surfaceElevation = NcSoundingProfile.MISSING;
                    TopoQuery topoQuery = TopoQuery.getInstance();
                    if (topoQuery != null) {
                        GridGeometry2D geom = MapUtil
                                .getGridGeometry(spatialArea);
                        CoordinateReferenceSystem crs = geom
                                .getCoordinateReferenceSystem();
                        Coordinate coord = new Coordinate(45, 45);
                        try {
                            coord = PointUtil.determineLatLon(points.get(i),
                                    crs, geom);
                        } catch (Exception e) {
                            logger.warn(
                                    "Unable to determine the Lat/Lon for point "
                                            + points.get(i),
                                    e);
                        }
                        surfaceElevation = (float) topoQuery.getHeight(coord);
                        if (surfaceElevation >= 0) {
                            soundingLy.setGeoHeight(surfaceElevation);
                        } else if (!presAvailable[i]) {
                            // no pressure and no height, no hope to continue
                            layers.set(i, null);
                        }
                    } else if (!presAvailable[i]) {
                        // no pressure and no height, no hope to continue
                        layers.set(i, null);
                    }
                }
            }
            return layers;
        } catch (Exception e) {
            logger.warn("Unable to update the surface layers.", e);
        }

        // cannot create any surface layers. return a null layer at each point
        return Arrays.asList(new NcSoundingLayer[points.size()]);
    }

    /**
     * Updates the surface sounding layer at each of the surrounding points with
     * the data values for the records at those points.
     *
     * @param surroundPoints
     *            The list of points.
     * @param layers
     *            The list of surface layers, each associated with a point.
     * @param presAvailable
     *            The array tracking whether each layer has a pressure
     *            available.
     * @param heightAvailable
     *            The array tracking whether each layer has a height available.
     * @param recList
     *            The list of GridRecords to retrieve data for and update the
     *            layers using.
     * @throws PluginException
     *             If there is an exception retrieving the data for the records.
     */
    private static void updateLayers(List<Point> surroundPoints,
            List<NcSoundingLayer> layers, boolean[] presAvailable,
            boolean[] heightAvailable, List<GridRecord> recList)
            throws PluginException {
        if (recList != null && !recList.isEmpty()) {
            List<PluginDataObject> newOrderedRecs = new ArrayList<>();
            List<float[]> values = PointIn.getHDF5GroupDataPoints(
                    recList.toArray(), surroundPoints, newOrderedRecs);
            for (int i = 0; i < surroundPoints.size(); i++) {
                float[] fdata = values.get(i);
                NcSoundingLayer soundingLy = layers.get(i);
                layers.set(i, soundingLy);
                for (int j = 0; j < newOrderedRecs.size(); j++) {
                    GridRecord rec = (GridRecord) newOrderedRecs.get(j);
                    String parm = rec.getParameter().getAbbreviation();
                    if ("P".equals(parm)) {
                        soundingLy.setPressure(fdata[j] / 100);
                        presAvailable[i] = true;
                    } else if ("GH".equals(parm)) {
                        soundingLy.setGeoHeight(fdata[j]);
                        heightAvailable[i] = true;
                    } else if ("T".equals(parm)) {
                        soundingLy.setTemperature(
                                (float) kelvinToCelsius.convert(fdata[j]));
                    } else if ("DpT".equals(parm)) {
                        soundingLy.setDewpoint(
                                (float) kelvinToCelsius.convert(fdata[j]));
                    } else if ("vW".equals(parm)) {
                        soundingLy.setWindV((float) metersPerSecondToKnots
                                .convert(fdata[j]));
                    } else if ("uW".equals(parm)) {
                        soundingLy.setWindU((float) metersPerSecondToKnots
                                .convert(fdata[j]));
                    } else if ("RH".equals(parm)) {
                        soundingLy.setRelativeHumidity(fdata[j]);
                    }
                }
            }
        }
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
     *         Created @ 3/28/2012 #6674 update to support interpolation
     */

    private static List<NcSoundingProfile> queryNearestPtProfileListByPointGroup(
            String refTime, String validTime, String modelName, List<?> levels,
            Coordinate[] latLonArray) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<>();
        List<Point> points = new ArrayList<>();
        List<float[]> fdataArrayList = new ArrayList<>();

        List<GridRecord> recList = new ArrayList<>();
        List<PluginDataObject> newOrderedRecs = new ArrayList<>();
        ISpatialObject spatialArea = null;
        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());
        try {
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "MB");
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION,
                    GRID_PARMS, "in");
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            query.addOrder(GridConstants.LEVEL_ONE, false);
            recList = (List<GridRecord>) dao.queryByCriteria(query);
            if (!recList.isEmpty()) {
                // use any one GridRecord for all points
                GridRecord gridRec = recList.get(0);
                spatialArea = gridRec.getSpatialObject();
                double lat, lon;
                for (Coordinate latLon : latLonArray) {
                    lat = latLon.y;
                    lon = latLon.x;
                    Point pt = getNearestPt(lat, lon, spatialArea);
                    if (pt == null) {
                        logger.info("getLatLonNearestPt return 0; lat=" + lat
                                + " lon=" + lon + " stime=" + validTime
                                + " modeltype=" + modelName);
                    } else {
                        points.add(pt);
                    }
                }
                fdataArrayList = PointIn.getHDF5GroupDataPoints(
                        recList.toArray(), points, newOrderedRecs);
                logger.info("Number of record=" + recList.size());
                logger.info(
                        "Number of fdataArrayList=" + fdataArrayList.size());
            } else {
                return soundingProfileList;
            }
        } catch (Exception e) {
            logger.warn("Unable to retrieve the profile list.", e);
        }
        int index = 0;
        GridGeometry2D geom = MapUtil.getGridGeometry(spatialArea);
        CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
        Coordinate coord = new Coordinate(45, 45);
        List<NcSoundingLayer> sfcLayers = getModelSfcLayers(points, refTime,
                validTime, modelName, spatialArea);
        for (float[] fdataArray : fdataArrayList) {
            // one fdataArray is for one Point or say one profile
            logger.info("fdataArray size =" + fdataArray.length);
            NcSoundingProfile pf = new NcSoundingProfile();
            List<NcSoundingLayer> soundLyList = new ArrayList<>();
            Point pnt = points.get(index);
            Object[] recArray = newOrderedRecs.toArray();
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
                            soundingLy.setWindU((float) metersPerSecondToKnots
                                    .convert(fdata));
                            break;
                        case vW:
                            // HDF5 data in unit of m/s, convert to Knots
                            soundingLy.setWindV((float) metersPerSecondToKnots
                                    .convert(fdata));
                            break;
                        case T:
                            soundingLy.setTemperature(
                                    (float) kelvinToCelsius.convert(fdata));
                            break;
                        case PVV:
                            soundingLy.setOmega(fdata);
                            break;
                        case DpT:
                            soundingLy.setDewpoint(
                                    (float) kelvinToCelsius.convert(fdata));
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
                logger.warn("Unable to determine the Lat/Lon for point " + pnt,
                        e);
            }
            pf.setStationLatitude(coord.y);
            pf.setStationLongitude(coord.x);

            NcSoundingLayer sfcLayer = sfcLayers.get(index);
            if (sfcLayer != null) {
                if (sfcLayer.getPressure() == NcSoundingLayer.MISSING
                        && sfcLayer.getGeoHeight() != NcSoundingLayer.MISSING) {
                    /*
                     * surface layer does not have pressure, but surface height
                     * is available see if we can interpolate surface pressure
                     * from upper and lower layer pressure
                     */
                    for (int i = 0; i < soundLyList.size(); i++) {
                        if (soundLyList.get(i).getGeoHeight() > sfcLayer
                                .getGeoHeight()) {
                            if (i > 0) {
                                float p1 = soundLyList.get(i - 1).getPressure();
                                float p2 = soundLyList.get(i).getPressure();
                                float h1 = soundLyList.get(i - 1)
                                        .getGeoHeight();
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
                    // cut sounding layer under ground, i.e. below surface layer
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
            ms.convertDewpoint(soundLyList);
            pf.setSoundingLyLst(soundLyList);
            // wind direction correction
            adjustWindDirectionToEarthRelative(coord.y, coord.x, spatialArea,
                    pf);
            soundingProfileList.add(pf);
            index++;
        }

        return soundingProfileList;
    }

    /**
     * Interpolate the surrounding Sounding Profiles at a level into the single
     * Sounding Profile at the requested point.
     *
     * @param soundingProfileList
     *            The list of Sounding Profiles at the same level surrounding
     *            the point of the
     * @param xfrac
     * @param yfrac
     * @return
     */
    private static NcSoundingProfile performInterpolation(
            List<NcSoundingProfile> soundingProfileList, float xfrac,
            float yfrac) {
        /*
         * the soundingProfileList now contains 1, 2, or 4 profiles depending on
         * the size of surroundPoints of the requested location do interpolation
         * to interpolate those profiles to one profile
         */
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
                intpedLy.setPressure(intpLinear.interpolate(pf1Ly.getPressure(),
                        pf2Ly.getPressure(), pf3Ly.getPressure(),
                        pf4Ly.getPressure(), xfrac, yfrac));
                intpedLy.setTemperature(
                        intpLinear.interpolate(pf1Ly.getTemperature(),
                                pf2Ly.getTemperature(), pf3Ly.getTemperature(),
                                pf4Ly.getTemperature(), xfrac, yfrac));
                intpedLy.setDewpoint(intpLinear.interpolate(pf1Ly.getDewpoint(),
                        pf2Ly.getDewpoint(), pf3Ly.getDewpoint(),
                        pf4Ly.getDewpoint(), xfrac, yfrac));
                intpedLy.setGeoHeight(
                        intpLinear.interpolate(pf1Ly.getGeoHeight(),
                                pf2Ly.getGeoHeight(), pf3Ly.getGeoHeight(),
                                pf4Ly.getGeoHeight(), xfrac, yfrac));
                intpedLy.setOmega(intpLinear.interpolate(pf1Ly.getOmega(),
                        pf2Ly.getOmega(), pf3Ly.getOmega(), pf4Ly.getOmega(),
                        xfrac, yfrac));
                // for wind, we interpolate u and v components first and then
                // convert them to speed and direction
                float windV = intpLinear.interpolate(pf1Ly.getWindV(),
                        pf2Ly.getWindV(), pf3Ly.getWindV(), pf4Ly.getWindV(),
                        xfrac, yfrac);
                float windU = intpLinear.interpolate(pf1Ly.getWindU(),
                        pf2Ly.getWindU(), pf3Ly.getWindU(), pf4Ly.getWindU(),
                        xfrac, yfrac);
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
                intpedLy.setPressure(intpLinear.interpolateH(
                        pf1Ly.getPressure(), pf2Ly.getPressure(), frac));
                intpedLy.setTemperature(intpLinear.interpolateH(
                        pf1Ly.getTemperature(), pf2Ly.getTemperature(), frac));
                intpedLy.setDewpoint(intpLinear.interpolateH(
                        pf1Ly.getDewpoint(), pf2Ly.getDewpoint(), frac));
                intpedLy.setGeoHeight(intpLinear.interpolateH(
                        pf1Ly.getGeoHeight(), pf2Ly.getGeoHeight(), frac));
                intpedLy.setOmega(intpLinear.interpolateH(pf1Ly.getOmega(),
                        pf2Ly.getOmega(), frac));
                float windV = intpLinear.interpolateH(pf1Ly.getWindV(),
                        pf2Ly.getWindV(), frac);
                float windU = intpLinear.interpolateH(pf1Ly.getWindU(),
                        pf2Ly.getWindU(), frac);
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
    private static List<NcSoundingProfile> queryInterpolationProfileListByPointGroup(
            String refTime, String validTime, String modelName, List<?> levels,
            Coordinate[] latLonArray) {
        List<NcSoundingProfile> soundingProfileList = new ArrayList<>();
        List<NcSoundingProfile> returnProfileList = new ArrayList<>();
        List<float[]> fdataArrayList = new ArrayList<>();
        // one point list represent for one request location(lat/lon)
        List<List<Point>> pntLstLst = new ArrayList<>();
        List<GridRecord> recList = new ArrayList<>();
        ISpatialObject spatialArea = null;
        CoreDao dao = new CoreDao(DaoConfig.forClass(GridRecord.class));
        DatabaseQuery query = new DatabaseQuery(GridRecord.class.getName());
        try {
            query.addQueryParam(GridConstants.MASTER_LEVEL_NAME, "MB");
            query.addQueryParam(GridConstants.DATASET_ID, modelName);
            query.addQueryParam(GridConstants.PARAMETER_ABBREVIATION,
                    GRID_PARMS, "in");
            query.addQueryParam("dataTime.refTime", refTime);
            query.addQueryParam("dataTime.validPeriod.start", validTime);
            query.addOrder(GridConstants.LEVEL_ONE, false);
            recList = (List<GridRecord>) dao.queryByCriteria(query);
            if (!recList.isEmpty()) {
                // use any one GridRecord for all points
                GridRecord gridRec = recList.get(0);
                spatialArea = gridRec.getSpatialObject();
                double lat, lon;
                float[] xyfrac = { 0f, 0f };
                for (Coordinate latLon : latLonArray) {
                    // FOR each request location
                    lat = latLon.y;
                    lon = latLon.x;
                    List<Point> surroundPoints = getSurroundingPoints(lat, lon,
                            spatialArea, xyfrac);
                    if (surroundPoints != null) {
                        /*
                         * the returned "surroundPoints" should contain 1, 2, or
                         * 4 points surrounding the request point, and they are
                         * used for interpolation to the one requested location
                         * (lat/lon) later
                         */
                        pntLstLst.add(surroundPoints);
                        List<PluginDataObject> newOrderedRecs = new ArrayList<>();
                        fdataArrayList = PointIn.getHDF5GroupDataPoints(
                                recList.toArray(), surroundPoints,
                                newOrderedRecs);
                        List<NcSoundingLayer> sfcLayers = getModelSfcLayers(
                                surroundPoints, refTime, validTime, modelName,
                                spatialArea);
                        int index = 0;
                        Object[] recArray = newOrderedRecs.toArray();
                        for (float[] fdataArray : fdataArrayList) {
                            NcSoundingProfile pf = new NcSoundingProfile();
                            List<NcSoundingLayer> soundLyList = new ArrayList<>();
                            for (Object level : levels) {
                                NcSoundingLayer soundingLy = new NcSoundingLayer();
                                double pressure = (Double) level;
                                soundingLy.setPressure((float) pressure);
                                for (int i = 0; i < recArray.length; i++) {
                                    GridRecord rec1 = (GridRecord) recArray[i];
                                    float fdata = fdataArray[i];
                                    if (rec1.getLevel()
                                            .getLevelonevalue() == pressure) {
                                        String prm = rec1.getParameter()
                                                .getAbbreviation();
                                        switch (GridParmNames.valueOf(prm)) {
                                        case GH:
                                            soundingLy.setGeoHeight(fdata);
                                            break;
                                        case uW:
                                            // HDF5 data in unit of m/s, convert
                                            // to Knots
                                            soundingLy.updateWindU(
                                                    (float) metersPerSecondToKnots
                                                            .convert(fdata));
                                            break;
                                        case vW:
                                            // HDF5 data in unit of m/s, convert
                                            // to Knots
                                            soundingLy.updateWindV(
                                                    (float) metersPerSecondToKnots
                                                            .convert(fdata));
                                            break;
                                        case T:
                                            soundingLy.setTemperature(
                                                    (float) kelvinToCelsius
                                                            .convert(fdata));
                                            break;
                                        case PVV:
                                            soundingLy.setOmega(fdata);
                                            break;
                                        case DpT:
                                            soundingLy.setDewpoint(
                                                    (float) kelvinToCelsius
                                                            .convert(fdata));
                                            break;
                                        case RH:
                                            soundingLy
                                                    .setRelativeHumidity(fdata);
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
                            NcSoundingLayer sfcLayer = sfcLayers.get(index);

                            if (sfcLayer != null) {
                                if (sfcLayer
                                        .getPressure() == NcSoundingLayer.MISSING
                                        && sfcLayer
                                                .getGeoHeight() != NcSoundingLayer.MISSING) {
                                    /*
                                     * surface layer does not have pressure, but
                                     * surface height is available see if we can
                                     * interpolate surface pressure from upper
                                     * and lower layer pressure
                                     */
                                    for (int i = 0; i < soundLyList
                                            .size(); i++) {
                                        if (soundLyList.get(i)
                                                .getGeoHeight() > sfcLayer
                                                        .getGeoHeight()) {
                                            if (i > 0) {
                                                float p1 = soundLyList
                                                        .get(i - 1)
                                                        .getPressure();
                                                float p2 = soundLyList.get(i)
                                                        .getPressure();
                                                float h1 = soundLyList
                                                        .get(i - 1)
                                                        .getGeoHeight();
                                                float h2 = soundLyList.get(i)
                                                        .getGeoHeight();
                                                float h = sfcLayer
                                                        .getGeoHeight();
                                                float p = p1 + (h - h1)
                                                        * (p1 - p2) / (h1 - h2);
                                                sfcLayer.setPressure(p);
                                            }
                                            break;
                                        }
                                    }
                                }
                                if (sfcLayer
                                        .getPressure() != NcSoundingLayer.MISSING) {
                                    // add sfc layer as first element
                                    soundLyList.add(0, sfcLayer);
                                }

                            }
                            // calculate dew point if necessary
                            MergeSounding ms = new MergeSounding();
                            ms.convertDewpoint(soundLyList);
                            pf.setSoundingLyLst(soundLyList);
                            soundingProfileList.add(pf);
                            index++;
                        }

                        /*
                         * the soundingProfileList noew contains 1, 2, or 4
                         * profiles depending on the size of surroundPoints of
                         * the requested location. do interpolation to
                         * interpolate those profiles to one profile
                         */
                        NcSoundingProfile intpedPf = performInterpolation(
                                soundingProfileList, xyfrac[0], xyfrac[1]);
                        /*
                         * cut sounding layer under ground, i.e. removed layers
                         * below surface layer, note that surface layer was
                         * added at layer 0
                         */
                        List<NcSoundingLayer> intpedSoundLyList = intpedPf
                                .getSoundingLyLst();
                        NcSoundingLayer intpedSfcLyr = intpedSoundLyList.get(0);
                        for (int i = intpedSoundLyList.size() - 1; i > 0; i--) {
                            NcSoundingLayer ly = intpedSoundLyList.get(i);
                            if (ly.getPressure() >= intpedSfcLyr
                                    .getPressure()) {
                                intpedSoundLyList.remove(i);
                            }
                        }
                        intpedPf.setSfcPress(intpedSfcLyr.getPressure());
                        intpedPf.setStationElevation(
                                intpedSfcLyr.getGeoHeight());
                        intpedPf.setStationLatitude(lat);
                        intpedPf.setStationLongitude(lon);
                        // wind direction correction
                        adjustWindDirectionToEarthRelative(lat, lon,
                                spatialArea, intpedPf);
                        returnProfileList.add(intpedPf);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to retrieve profile list.", e);
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
    private static List<?> getModelLevels(String refTime, String validTime,
            String modelName) {
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
            logger.warn("Unable to query the model levels.", e);
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
    private static List<Point> getSurroundingPoints(double lat, double lon,
            ISpatialObject spatialArea, float[] xyfrac) {
        List<Point> points = new ArrayList<>();
        float xfrac, yfrac;

        GridGeometry2D geom = MapUtil.getGridGeometry(spatialArea);

        CoordinateReferenceSystem crs = geom.getCoordinateReferenceSystem();
        Coordinate coord = new Coordinate(lon, lat);

        try {
            DirectPosition2D exactPt = PointUtil.determineExactIndex(coord, crs,
                    geom);
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
                    /*
                     * the request location is a "point" itself, just return 1
                     * point
                     */
                    points.add(new Point(xfloor, yfloor));
                    xfrac = FRAC_ERROR;
                    yfrac = FRAC_ERROR;
                } else {
                    /*
                     * the request location is located at s00-s10 line, return 2
                     * points
                     */
                    points.add(new Point(xfloor, yfloor)); // s00
                    points.add(new Point(xfloor, yceil)); // s10
                    xfrac = FRAC_ERROR;
                }
            } else {
                if (yfloor == yceil) {
                    /*
                     * the request location is located at s00-s01 line, return 2
                     * points
                     */
                    points.add(new Point(xfloor, yfloor)); // s00
                    points.add(new Point(xceil, yfloor)); // s01
                    yfrac = FRAC_ERROR;
                } else {
                    /*
                     * the request point is inside the 4 points "area", return 4
                     * points
                     */
                    points.add(new Point(xfloor, yfloor)); // s00
                    points.add(new Point(xceil, yfloor)); // s01
                    points.add(new Point(xfloor, yceil)); // s10
                    points.add(new Point(xceil, yceil)); // s11
                }
            }
            xyfrac[0] = xfrac;
            xyfrac[1] = yfrac;
        } catch (Exception e) {
            logger.warn("Unable to determine the surrounding points.", e);
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

    private static Point getNearestPt(double lat, double lon,
            ISpatialObject spatialArea) {
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
            logger.warn("Unable to determine the nearest point.", e);
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
    private static void adjustWindDirectionToEarthRelative(double lat,
            double lon, ISpatialObject so, NcSoundingProfile pf) {
        Coordinate coord = new Coordinate(lon, lat);
        double angle = MapUtil.rotation(coord, so);
        /*
         * if angle is positive X degree, then UP (grid's North) direction is
         * 360-X degrees and X degrees to the right of UP is earth north (or 360
         * degrees)
         */
        List<NcSoundingLayer> soundLyList = pf.getSoundingLyLst();
        for (NcSoundingLayer ly : soundLyList) {
            float correctedWindDir = ly.getWindDirection() + (float) angle;
            if (correctedWindDir >= 360) {
                correctedWindDir = correctedWindDir - 360;
            }
            ly.setWindDirection(correctedWindDir);
        }
    }

    public static NcSoundingCube handleGridModelDataRequest(
            SoundingServiceRequest request) {
        String[] refTimeStrArr = request.getRefTimeStrAry();
        if (refTimeStrArr == null && request.getRefTimeAry() != null) {
            refTimeStrArr = QueryMiscTools
                    .convertTimeLongArrayToStrArray(request.getRefTimeAry());
        }
        String[] rangeTimeStrArr = request.getRangeStartTimeStrAry();
        if (rangeTimeStrArr == null && request.getRangeStartTimeAry() != null) {
            rangeTimeStrArr = QueryMiscTools.convertTimeLongArrayToStrArray(
                    request.getRangeStartTimeAry());
        }
        if (request.getLatLonAry() != null && refTimeStrArr != null
                && refTimeStrArr.length > 0 && rangeTimeStrArr != null
                && rangeTimeStrArr.length > 0) {
            NcSoundingCube cube = new NcSoundingCube();
            // assume query failure
            cube.setRtnStatus(NcSoundingCube.QueryStatus.FAILED);
            List<NcSoundingProfile> soundingProfileList;
            List<NcSoundingProfile> finalSoundingProfileList = new ArrayList<>();
            String refTime = refTimeStrArr[0];
            for (String validTimeStartStr : rangeTimeStrArr) {
                if (request.isInterpolation()) {
                    // get interpolation point
                    soundingProfileList = getMdlSndInterpolatedDataProfileList(
                            request.getLatLonAry(), refTime, validTimeStartStr,
                            request.getModelType());
                } else {
                    // get nearest point data
                    soundingProfileList = getMdlSndNearestPtDataProfileList(
                            request.getLatLonAry(), refTime, validTimeStartStr,
                            request.getModelType());
                }
                finalSoundingProfileList.addAll(soundingProfileList);
            }
            if (!finalSoundingProfileList.isEmpty()) {
                // as long as one query successful, set it to OK
                cube.setRtnStatus(NcSoundingCube.QueryStatus.OK);
            }
            cube.setSoundingProfileList(finalSoundingProfileList);
            return cube;
        }
        return null;
    }
}
