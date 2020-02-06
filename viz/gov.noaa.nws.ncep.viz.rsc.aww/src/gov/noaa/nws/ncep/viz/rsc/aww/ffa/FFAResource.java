package gov.noaa.nws.ncep.viz.rsc.aww.ffa;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.RGB;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.edex.decodertools.core.LatLonPoint;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.core.rsc.jts.JTSCompiler;
import com.raytheon.viz.core.rsc.jts.JTSCompiler.PointStyle;
import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwFips;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwHVtec;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwLatlons;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord.AwwReportType;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwUgc;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwVtec;
import gov.noaa.nws.ncep.edex.common.stationTables.IStationField;
import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.edex.common.stationTables.StationTable;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsResource;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;
import gov.noaa.nws.ncep.viz.rsc.aww.query.AwwQueryResult;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.AwwImmediateCauseUtil;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.FFAConstant;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.StringUtil;
import gov.noaa.nws.ncep.viz.rsc.aww.utils.UGCUtil;
import gov.noaa.nws.ncep.viz.ui.display.NCMapDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * FFA resourceResource - Display Flash Flood data from aww data.
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer         Description
 * ------------ ---------- -----------      --------------------------
 * 21 June 2010  254    M. Gao  	    Initial creation.
 * 04 Oct  2010  307    G. Hull         timeMatch FfaRscDataObjs
 * 10 Jan. 2011  N/A    M. Gao          Event Time display includes date + time now since some 
 *                                      events start and then end in different dates    
 * 16 Feb 2012    555   S. Gurung       Added call to setAllFramesAsPopulated() in queryRecords()                                 
 * 05/23/12       785   Q. Zhou         Added getName for legend.
 * 17 Aug 2012    655   B. Hebbard      Added paintProps as parameter to IDisplayable draw
 * 09/11/12      852    Q. Zhou         Modified time string and alignment in drawLabel().
 * 02/01/13      972    G. Hull         define on NcMapDescriptor instead of IMapDescriptor
 * 08/14/13     1028    G. Hull         Move to aww project. Use AwwReportType enum.
 * 12/14                B. Yin          Remove ScriptCreator, use Thrift Client.
 * 1/15/2015     5770   Kris K          Code cleaned and FFA display issues with issuestime, start tiem and end times are fixed.
 * 11/05/2015    5070       randerso    Adjust font sizes for dpi scaling
 * 03/15/2016   R15560  K. Bugenhagen   Refactored common code into AwwQueryResult 
 *                                      class, removing need for FfaZoneQueryResult 
 *                                      class.  Also cleanup.
 * </pre>
 * 
 * @author mgao
 * @version 1.0
 */

public class FFAResource extends
        AbstractNatlCntrsResource<FFAResourceData, NCMapDescriptor> implements
        INatlCntrsResource, IStationField {

    private Logger logger = Logger.getLogger(this.getClass());

    private final static String QUERY_PREFIX = "select AsBinary(the_geom) G, AsBinary(the_geom_0_001) G1, state,name,state_zone from mapdata.zonelowres where ";

    private final static String QUERY_COLUMN_NAME = "state_zone";

    private IFont font;

    private StationTable countyStationTable, zoneStationTable;

    private FFAResourceData ffaRscData;

    // for pre-query the database
    private AwwQueryResult queryResult = new AwwQueryResult();

    // for storing result of pre-calculation
    private IWireframeShape outlineShape;

    // for pre-calculate the IWiredframeShape
    private ZoneResultJob zrJob = new ZoneResultJob("");

    // Area change flag
    private boolean areaChangeFlag = false;

    public class FfaRscDataObj implements IRscDataObject {

        String dataUri, vtecline; // used as a key string

        DataTime issueTime; // issue time from bulletin

        DataTime eventTime; // Event start time of Vtec with validPeriod

        DataTime endTime; // Event end time of of Vtec

        String reportType, actionType, officeId, eTrackingNo, phenomena,
                significance;

        int polyNumPoints;

        int ugcIndicator;

        float[] polyLatArray, countyOrZoneOrStateOrLakeLatArray;

        float[] polyLonArray, countyOrZoneOrStateOrLakeLonArray;

        LatLonPoint[] polygonLatLonPointArray;

        List<LatLonPoint> countyOrZoneOrStateOrLakeLatLonPointList;

        Set<String> ugcCodeStringSet, allStateAbbreviationSet,
                greatLakeNameSet;

        List<String> countyOrZoneOrStateOrLakeNameList; // last one;

        public List<String> fips;

        String immediateCause;

        @Override
        public DataTime getDataTime() {
            return eventTime;
        }

        public String getKey() {
            return getFfaRscDataObjKey(this);
        }

    }

    protected class FrameData extends AbstractFrameData {
        HashMap<String, FfaRscDataObj> ffaDataMap;

        public FrameData(DataTime frameTime, int timeInt) {
            super(frameTime, timeInt);
            ffaDataMap = new HashMap<String, FfaRscDataObj>();
        }

        @Override
        public boolean updateFrameData(IRscDataObject rscDataObj) {
            if (!(rscDataObj instanceof FfaRscDataObj)) {
                logger.error("FFAResource:updateFrameData() processing.....\n"
                        + "Data belongs to a different class :"
                        + rscDataObj.getClass().toString());
                return false;
            }
            FfaRscDataObj ffaRscDataObj = (FfaRscDataObj) rscDataObj;
            updateFfaDataMap(ffaDataMap, ffaRscDataObj);

            return true;
        }

        /**
         * 1.if the eventTime of the FfaRscDataObj is null, the FfaRscDataObj is
         * considered to be invalid record and thus will not be added to the
         * map. 2. If the imported FfaRscDataObj is valid, a. If existing
         * FfaRscDataObj is null, the imported ffaRscDataObj is added to the
         * map. b. If existing FfaRscDataObj is not null, the imported
         * ffaRscDataObj is added to the map only when the issueTime of the
         * imported ffaRscDataObj is newer than the issueTime of the existing
         * FfaRscDataObj be skipped
         * 
         * @param ffaDataMap
         * @param ffaRscDataObj
         */
        private void updateFfaDataMap(
                HashMap<String, FfaRscDataObj> ffaDataMap,
                FfaRscDataObj ffaRscDataObj) {
            if (isFfaRscDataObjValid(ffaRscDataObj)) {
                FfaRscDataObj existingFFAData = ffaDataMap
                        .get(ffaRscDataObj.dataUri);
                if (existingFFAData == null
                        || newIssueTimeGreaterThanExistingIssueTime(
                                ffaRscDataObj.issueTime,
                                existingFFAData.issueTime)) {
                    ffaDataMap.put(ffaRscDataObj.dataUri, ffaRscDataObj);
                }
            }
        }

        private boolean newIssueTimeGreaterThanExistingIssueTime(
                DataTime newIssueTime, DataTime existingIssueTime) {
            boolean isNewIssueTimeGreaterThanExistingIssueTime = false;
            if (newIssueTime != null && existingIssueTime != null) {
                if (newIssueTime.greaterThan(existingIssueTime))
                    isNewIssueTimeGreaterThanExistingIssueTime = true;
            }
            return isNewIssueTimeGreaterThanExistingIssueTime;
        }

        private boolean isFfaRscDataObjValid(FfaRscDataObj newFfaRscDataObj) {
            boolean isFfaRscDataObjValid = false;
            if (newFfaRscDataObj != null && newFfaRscDataObj.eventTime != null)
                isFfaRscDataObjValid = true;
            return isFfaRscDataObjValid;
        }

        public Map<String, FfaRscDataObj> getFfaDataMap() {
            return ffaDataMap;
        }
    }

    /**
     * This overrides the default which works for PluginDataObjects. This method
     * is called by queryRecords to turn the records from the database into
     * FfaRscDataObj objects.
     */
    @Override
    public IRscDataObject[] processRecord(Object awwObj) {
        if (!(awwObj instanceof AwwRecord)) {
            logger.error("FFAResource.processRecord: object is not a "
                    + "AwwRecord: " + awwObj.getClass().getName());
            return new IRscDataObject[] {};
        }

        FfaRscDataObj ffaRscDataObj = getFFAData((AwwRecord) awwObj);
        if (!isRetrievedFfaRscDataObjValid(ffaRscDataObj)) {
            return new IRscDataObject[] {};
        }

        return new FfaRscDataObj[] { ffaRscDataObj };
    }

    /*
     * Conditions that a FfaRscDataObj to be invalid object 1. FfaRscDataObj is
     * NULL 2. FfaRscDataObj.eventTime is NULL
     */
    private boolean isRetrievedFfaRscDataObjValid(FfaRscDataObj ffaRscDataObj) {
        boolean isFfaRscDataObjValid = false;
        if (ffaRscDataObj != null && ffaRscDataObj.eventTime != null)
            isFfaRscDataObjValid = true;
        return isFfaRscDataObjValid;
    }

    private FfaRscDataObj getFFAData(AwwRecord awwRecord) {
        FfaRscDataObj ffaData = null;

        ffaData = new FfaRscDataObj();
        ffaData.issueTime = new DataTime(awwRecord.getIssueTime());
        ffaData.reportType = awwRecord.getReportType();
        ffaData.dataUri = awwRecord.getDataURI();

        Set<AwwUgc> awwUgcSet = awwRecord.getAwwUGC();
        for (AwwUgc eachAwwUgc : awwUgcSet) {
            if (eachAwwUgc.getAwwVtecLine() != null) {
                /*
                 * To obtain the event start and end time. This will be looped
                 * only once since the relationship between tables is one to
                 * one.
                 */
                for (AwwVtec awwVtec : eachAwwUgc.getAwwVtecLine()) {
                    fillEventStartAndEndTime(awwVtec, awwRecord, ffaData);
                    /*
                     * retrieve and then set ImmediateCause value to FFAData
                     */
                    ffaData.immediateCause = getImmediateCauseValue(awwVtec);
                }
            }

            ffaData.fips = getFips(eachAwwUgc);

            String ugcLine = eachAwwUgc.getUgc();
            if (!StringUtil.isStringEmpty(ugcLine)) {
                ffaData.ugcCodeStringSet = getCountyUgcSet(eachAwwUgc
                        .getAwwFIPS());
                ffaData = populateCountyOrZoneOrStateOrLakeInfo(ffaData);
            }

            ffaData.polyNumPoints = eachAwwUgc.getAwwLatLon().size();
            if (ffaData.polyNumPoints > 0) {
                ffaData.polygonLatLonPointArray = new LatLonPoint[ffaData.polyNumPoints];
                ffaData.polyLatArray = new float[ffaData.polyNumPoints];
                ffaData.polyLonArray = new float[ffaData.polyNumPoints];
                int index;
                for (AwwLatlons awwLatLon : eachAwwUgc.getAwwLatLon()) {
                    LatLonPoint point = new LatLonPoint(awwLatLon.getLat(),
                            awwLatLon.getLon(), LatLonPoint.INDEGREES);
                    index = awwLatLon.getIndex();
                    ffaData.polyLatArray[index - 1] = awwLatLon.getLat();
                    ffaData.polyLonArray[index - 1] = awwLatLon.getLon();
                    logger.debug("the index of this lat lon is " + index);

                    ffaData.polygonLatLonPointArray[index - 1] = point;
                }
            }
        }

        return ffaData;
    }

    private void fillEventStartAndEndTime(AwwVtec awwVtec, AwwRecord awwRecord,
            FfaRscDataObj ffaData) {
        /*
         * In the real data, sometimes EventStartTime or EventEndTime is missed
         */
        Calendar eventStartCalendar = getEventStartTimeFromAwwVtec(awwVtec);
        Calendar eventEndCalendar = getEventEndTimeFromAwwVtec(awwVtec);

        /*
         * If startEventTime is still NULL, use the issueTime of AwwRecord to
         * substitute it This solution may not be 100% accurate for some
         * scenarios.
         */
        if (eventStartCalendar == null && awwRecord.getIssueTime() != null)
            eventStartCalendar = awwRecord.getIssueTime();

        Calendar issueTime = awwRecord.getIssueTime();

        if (issueTime != null && eventEndCalendar != null) {
            ffaData.endTime = new DataTime(eventEndCalendar);
            ffaData.eventTime = new DataTime(eventStartCalendar, new TimeRange(
                    issueTime, eventEndCalendar));
        }
    }

    private Calendar getEventStartTimeFromAwwVtec(AwwVtec awwVtec) {
        Calendar startTime = null;
        if (awwVtec != null)
            startTime = awwVtec.getEventStartTime();
        return startTime;
    }

    private Calendar getEventEndTimeFromAwwVtec(AwwVtec awwVtec) {
        Calendar endTime = null;
        if (awwVtec != null)
            endTime = awwVtec.getEventEndTime();
        return endTime;
    }

    private Set<String> getCountyUgcSet(Set<AwwFips> awwFipsSet) {
        Set<String> countyUgcSet = null;
        if (awwFipsSet == null)
            countyUgcSet = new HashSet<String>();
        else {
            countyUgcSet = new HashSet<String>(awwFipsSet.size());
            for (AwwFips eachAwwFips : awwFipsSet) {
                String eachFips = eachAwwFips.getFips();
                countyUgcSet.add(eachFips);
            }
        }

        return countyUgcSet;
    }

    private String getImmediateCauseValue(AwwVtec awwVtec) {
        StringBuilder causeBuilder = new StringBuilder();
        if (awwVtec.getAwwHVtecLine() != null) {
            for (AwwHVtec eachHVtec : awwVtec.getAwwHVtecLine()) {
                if (!StringUtil.isStringEmpty(eachHVtec.getImmediateCause()))
                    causeBuilder.append(eachHVtec.getImmediateCause()).append(
                            "; ");
            }
        }
        return causeBuilder.toString();
    }

    private FfaRscDataObj populateCountyOrZoneOrStateOrLakeInfo(
            FfaRscDataObj ffaData) {
        ffaData.countyOrZoneOrStateOrLakeLatLonPointList = new ArrayList<LatLonPoint>();
        ffaData.countyOrZoneOrStateOrLakeNameList = new ArrayList<String>();
        ffaData.allStateAbbreviationSet = new HashSet<String>();
        ffaData.greatLakeNameSet = new HashSet<String>();
        ffaData.countyOrZoneOrStateOrLakeLatArray = new float[ffaData.ugcCodeStringSet
                .size()];
        ffaData.countyOrZoneOrStateOrLakeLonArray = new float[ffaData.ugcCodeStringSet
                .size()];

        int arrayIndex = 0;
        for (String eachCountyOrZoneOrStateOrLakeUgcString : ffaData.ugcCodeStringSet) {
            int ugcIndicator = UGCUtil
                    .getUgcIndicator(eachCountyOrZoneOrStateOrLakeUgcString);
            ffaData.ugcIndicator = ugcIndicator;
            switch (ugcIndicator) {
            case FFAConstant.UGC_GREAT_LAKE_INDICATOR:
                String greatLakeAbbreviation = eachCountyOrZoneOrStateOrLakeUgcString
                        .substring(0, 2);
                if (!ffaData.greatLakeNameSet.contains(greatLakeAbbreviation)) {
                    List<Object[]> objectArrayList = getGreatLakeInfo(greatLakeAbbreviation);
                    if (!objectArrayList.isEmpty()) {
                        Object[] objectArray = objectArrayList.get(0);
                        String lakeAreaName = (String) objectArray[0];
                        String greatLakeName = getGreatLakeName(lakeAreaName);
                        Double[] latLonArrayOfLake = getLatLonArrayOfLake((String) objectArray[1]);
                        if (latLonArrayOfLake != null) {
                            ffaData.greatLakeNameSet.add(greatLakeAbbreviation);

                            LatLonPoint greatLakePoint = new LatLonPoint(
                                    latLonArrayOfLake[0], latLonArrayOfLake[1],
                                    LatLonPoint.INDEGREES);

                            ffaData.countyOrZoneOrStateOrLakeLatLonPointList
                                    .add(greatLakePoint);
                            ffaData.countyOrZoneOrStateOrLakeNameList
                                    .add(getCountyOrZoneAndStateName(
                                            greatLakeName,
                                            "",
                                            FFAConstant.UGC_GREAT_LAKE_INDICATOR));
                            ffaData.countyOrZoneOrStateOrLakeLatArray[arrayIndex] = latLonArrayOfLake[0]
                                    .floatValue();
                            ffaData.countyOrZoneOrStateOrLakeLonArray[arrayIndex] = latLonArrayOfLake[1]
                                    .floatValue();
                            arrayIndex++;
                        }
                    }
                }
                break;
            case FFAConstant.UGC_ALL_STATE_ZONE_INDICATOR:
                String stateAbbreviation = eachCountyOrZoneOrStateOrLakeUgcString
                        .substring(0, 2);
                if (!ffaData.allStateAbbreviationSet
                        .contains(stateAbbreviation)) {
                    List<Object[]> objectArrayList = getStateInfo(stateAbbreviation);
                    if (!objectArrayList.isEmpty()) {
                        ffaData.allStateAbbreviationSet.add(stateAbbreviation);

                        Object[] objectArray = objectArrayList.get(0);

                        String stateFullName = (String) objectArray[0];
                        BigDecimal stateLatitude = (BigDecimal) objectArray[1];
                        BigDecimal stateLongitude = (BigDecimal) objectArray[2];
                        LatLonPoint statePoint = new LatLonPoint(
                                stateLatitude.doubleValue(),
                                stateLongitude.doubleValue(),
                                LatLonPoint.INDEGREES);

                        ffaData.countyOrZoneOrStateOrLakeLatLonPointList
                                .add(statePoint);
                        ffaData.countyOrZoneOrStateOrLakeNameList
                                .add(getCountyOrZoneAndStateName(
                                        stateFullName,
                                        "",
                                        FFAConstant.UGC_ALL_STATE_ZONE_INDICATOR));
                        ffaData.countyOrZoneOrStateOrLakeLatArray[arrayIndex] = stateLatitude
                                .floatValue();
                        ffaData.countyOrZoneOrStateOrLakeLonArray[arrayIndex] = stateLongitude
                                .floatValue();
                        arrayIndex++;
                    }
                }
                break;
            case FFAConstant.UGC_ZONE_INDICATOR:
                Station zoneStation = zoneStationTable.getStation(
                        StationField.STID,
                        eachCountyOrZoneOrStateOrLakeUgcString);
                if (zoneStation != null) {
                    String zoneAndStateName = getCountyOrZoneAndStateName(
                            zoneStation.getStnname(), zoneStation.getState(),
                            FFAConstant.UGC_ZONE_INDICATOR);
                    if (!ffaData.countyOrZoneOrStateOrLakeNameList
                            .contains(zoneAndStateName)) {
                        LatLonPoint point = new LatLonPoint(
                                zoneStation.getLatitude(),
                                zoneStation.getLongitude(),
                                LatLonPoint.INDEGREES);
                        ffaData.countyOrZoneOrStateOrLakeLatLonPointList
                                .add(point);
                        ffaData.countyOrZoneOrStateOrLakeNameList
                                .add(zoneAndStateName);
                        ffaData.countyOrZoneOrStateOrLakeLatArray[arrayIndex] = zoneStation
                                .getLatitude();
                        ffaData.countyOrZoneOrStateOrLakeLonArray[arrayIndex] = zoneStation
                                .getLongitude();
                        arrayIndex++;
                    }
                }
                break;
            case FFAConstant.UGC_NEW_PATTERN_INDICATOR:
                eachCountyOrZoneOrStateOrLakeUgcString = convertToOldUgcStringPattern(eachCountyOrZoneOrStateOrLakeUgcString);
            case FFAConstant.UGC_COUNTY_INDICATOR:
                Station countyStation = countyStationTable.getStation(
                        StationField.STID,
                        eachCountyOrZoneOrStateOrLakeUgcString);
                if (countyStation != null) {
                    String countyAndStateName = getCountyOrZoneAndStateName(
                            countyStation.getStnname(),
                            countyStation.getState(),
                            FFAConstant.UGC_COUNTY_INDICATOR);
                    if (!ffaData.countyOrZoneOrStateOrLakeNameList
                            .contains(countyAndStateName)) {
                        LatLonPoint point = new LatLonPoint(
                                countyStation.getLatitude(),
                                countyStation.getLongitude(),
                                LatLonPoint.INDEGREES);
                        ffaData.countyOrZoneOrStateOrLakeLatLonPointList
                                .add(point);
                        ffaData.countyOrZoneOrStateOrLakeNameList
                                .add(countyAndStateName);
                        ffaData.countyOrZoneOrStateOrLakeLatArray[arrayIndex] = countyStation
                                .getLatitude();
                        ffaData.countyOrZoneOrStateOrLakeLonArray[arrayIndex] = countyStation
                                .getLongitude();
                        arrayIndex++;
                    }
                }
                break;
            default:
                logger.error("Unknown ugc string pattern, UGC string ="
                        + eachCountyOrZoneOrStateOrLakeUgcString);
                break;
            }

        }

        return ffaData;
    }

    private List<Object[]> getStateInfo(String stateAbbreviation) {
        StringBuilder query = new StringBuilder(
                "select name, lat, lon from mapdata.states where state ='");
        query.append(stateAbbreviation).append("';");
        List<Object[]> results = new ArrayList<Object[]>();
        try {
            results = DirectDbQuery.executeQuery(query.toString(), "maps",
                    QueryLanguage.SQL);
        } catch (VizException e1) {
            logger.debug("VizException is thrown when trying to do DirectDbQuery.executeQuery to query mapdata.states table, error="
                    + e1.getMessage());
            e1.printStackTrace();
        }
        return results;
    }

    private List<Object[]> getGreatLakeInfo(String greatLakeAbbreviation) {
        StringBuilder query = new StringBuilder(
                "select area, ctrloc from bounds.greatlakesbnds where id ='");
        query.append(greatLakeAbbreviation).append("';");
        List<Object[]> results = new ArrayList<Object[]>();
        try {
            results = DirectDbQuery.executeQuery(query.toString(), "ncep",
                    QueryLanguage.SQL);
        } catch (VizException e1) {
            logger.debug("VizException is thrown when trying to do DirectDbQuery.executeQuery to query bounds.greatlakesbnds table, error="
                    + e1.getMessage());
            e1.printStackTrace();
        }
        return results;
    }

    private String getCountyOrZoneAndStateName(
            String countyOrZoneOrOrStateLakeName, String stateName,
            int zoneRangeCategoryIndicator) {
        String countyOrZoneAndStateName = "unknown_countyOrZoneName";
        if (!StringUtil.isStringEmpty(countyOrZoneOrOrStateLakeName)
                && stateName != null) {
            switch (zoneRangeCategoryIndicator) {
            case FFAConstant.UGC_ALL_STATE_ZONE_INDICATOR:
                countyOrZoneAndStateName = countyOrZoneOrOrStateLakeName
                        + FFAConstant.HYPHEN + FFAConstant.ALL_STATES_MARKER;
                break;
            case FFAConstant.UGC_COUNTY_INDICATOR:
                countyOrZoneAndStateName = countyOrZoneOrOrStateLakeName
                        + FFAConstant.UNDERSTORE + stateName
                        + FFAConstant.HYPHEN + FFAConstant.COUNTY_MARKER;
                break;
            case FFAConstant.UGC_ZONE_INDICATOR:
                countyOrZoneAndStateName = countyOrZoneOrOrStateLakeName
                        + FFAConstant.UNDERSTORE + stateName
                        + FFAConstant.HYPHEN + FFAConstant.ZONE_MARKER;
                break;
            case FFAConstant.UGC_GREAT_LAKE_INDICATOR:
                countyOrZoneAndStateName = countyOrZoneOrOrStateLakeName
                        + FFAConstant.HYPHEN + FFAConstant.GREAT_LAKES_MARKER;
                break;
            }
        }
        return countyOrZoneAndStateName;
    }

    /**
     * Create a FFA resource.
     * 
     * @throws VizException
     */
    public FFAResource(FFAResourceData rscData, LoadProperties loadProperties)
            throws VizException {
        super(rscData, loadProperties);
        ffaRscData = (FFAResourceData) resourceData;
    }

    protected AbstractFrameData createNewFrame(DataTime frameTime, int timeInt) {
        return (AbstractFrameData) new FrameData(frameTime, timeInt);
    }

    public void initResource(IGraphicsTarget grphTarget) throws VizException {
        font = grphTarget.initializeFont("Monospace", 12,
                new IFont.Style[] { IFont.Style.BOLD });
        countyStationTable = new StationTable(NcPathManager.getInstance()
                .getStaticFile(NcPathConstants.COUNTY_STN_TBL)
                .getAbsolutePath());
        zoneStationTable = new StationTable(NcPathManager.getInstance()
                .getStaticFile(NcPathConstants.FFG_ZONES_STN_TBL)
                .getAbsolutePath());
        queryRecords();
        populateQueryResultMap();
    }

    private void populateQueryResultMap() {

        Iterator<IRscDataObject> iter = newRscDataObjsQueue.iterator();
        while (iter.hasNext()) {
            IRscDataObject dataObject = (IRscDataObject) iter.next();
            FfaRscDataObj ffaRscDataObj = (FfaRscDataObj) dataObject;
            queryResult.buildQueryPart(ffaRscDataObj.fips, QUERY_COLUMN_NAME);
        }
        queryResult.populateMap(QUERY_PREFIX);
        setAllFramesAsPopulated();
    }

    @Override
    public void disposeInternal() {

    }

    public void paintFrame(AbstractFrameData frameData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (paintProps == null) {
            return;
        }

        if (areaChangeFlag) {
            areaChangeFlag = false;
            postProcessFrameUpdate();
        } // TODO: dispose old outlineShape

        FrameData currFrameData = (FrameData) frameData;

        RGB color = new RGB(155, 155, 155);
        LineStyle lineStyle = LineStyle.SOLID;
        int outlineWidth = 2;
        int symbolSize = 2;

        Collection<FfaRscDataObj> ffaDataValues = currFrameData.ffaDataMap
                .values();

        for (FfaRscDataObj eachFFAData : ffaDataValues) {

            AwwReportType rptyp = AwwReportType
                    .getReportType(eachFFAData.reportType);

            if (rptyp == AwwReportType.FLASH_FLOOD_WATCH) {
                color = ffaRscData.getFlashFloodWatchColor();
                outlineWidth = ffaRscData.getFlashFloodWatchSymbolWidth();
                symbolSize = ffaRscData.getFlashFloodWatchSymbolSize();
                drawingFFAData(target, paintProps, eachFFAData,
                        ffaRscData.getFlashFloodWatchEnable(), color,
                        outlineWidth, lineStyle, symbolSize);
            } else if (rptyp == AwwReportType.FLOOD_WATCH) {
                color = ffaRscData.getFlashFloodStatementColor();
                outlineWidth = ffaRscData.getFlashFloodStatementSymbolWidth();
                symbolSize = ffaRscData.getFlashFloodStatementSymbolSize();
                drawingFFAData(target, paintProps, eachFFAData,
                        ffaRscData.getFlashFloodStatementEnable(), color,
                        outlineWidth, lineStyle, symbolSize);
            }
        }
    }

    private void drawingFFAData(IGraphicsTarget target,
            PaintProperties paintProps, FfaRscDataObj ffaData,
            Boolean drawingEnabled, RGB rgbColor, int outlineWidth,
            LineStyle lineStyle, int symbolSize) throws VizException {
        if (!drawingEnabled)
            return;
        if (ffaRscData.getOutlineEnable())
            drawOutline(ffaData, target, rgbColor, outlineWidth, lineStyle,
                    paintProps);
        drawLabel(ffaData, target, paintProps, rgbColor, symbolSize, symbolSize);
    }

    public void drawOutline(FfaRscDataObj ffaData, IGraphicsTarget target,
            RGB color, int outlineWidth, LineStyle lineStyle,
            PaintProperties paintProps) throws VizException {
        Envelope env = null;
        try {
            PixelExtent extent = (PixelExtent) paintProps.getView().getExtent();
            Envelope e = getNcMapDescriptor().pixelToWorld(extent,
                    descriptor.getCRS());
            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(e,
                    descriptor.getCRS());
            env = referencedEnvelope.transform(MapUtil.LATLON_PROJECTION, true);
        } catch (Exception e) {
            throw new VizException("Error transforming extent", e);
        }
        /*
         * draw county outline if there is any
         */
        drawZoneOutline2(ffaData, target, color, outlineWidth, lineStyle,
                paintProps, env);

    }

    public void drawLabel(FfaRscDataObj ffaData,
            IGraphicsTarget graphicsTarget, PaintProperties paintProps,
            RGB color, float symbolLineWidth, double symbolSizeScale) {
        try {
            int index = 0;
            for (String eachCountyOrZoneAndStateNameWithMaker : ffaData.countyOrZoneOrStateOrLakeNameList) {
                String eachCountyOrZoneAndStateName = removeMarker(eachCountyOrZoneAndStateNameWithMaker);
                double[] labelLatLon = {
                        ffaData.countyOrZoneOrStateOrLakeLonArray[index],
                        ffaData.countyOrZoneOrStateOrLakeLatArray[index] };
                double[] labelPix = descriptor.worldToPixel(labelLatLon);

                if (labelPix != null) {
                    /*
                     * If outline is not enabled, draw a small circle above the
                     * strings
                     */
                    if (!ffaRscData.getOutlineEnable()) {
                        drawSymbol(FFAConstant.FILLED_DIAMOND_SYMBOL,
                                graphicsTarget, paintProps, labelLatLon, color,
                                symbolLineWidth, symbolSizeScale * 0.4);
                    }

                    String[] text = new String[3];
                    List<String> enabledText = new ArrayList<String>();

                    if (ffaRscData.getCountyOrZoneNameEnable()) {
                        enabledText
                                .add(getCountyOrZoneAndStateNameValue(eachCountyOrZoneAndStateName));
                    }

                    if (ffaRscData.getTimeEnable()) {
                        enabledText.add(getEventTimeStringValue(
                                ffaData.eventTime, ffaData.endTime));
                    }

                    if (ffaRscData.getImmediateCauseEnable()) {
                        enabledText
                                .add(getImmediateCauseDesc(ffaData.immediateCause));
                    }

                    for (int j = enabledText.size(); j < 3; j++)
                        enabledText.add("");

                    text = enabledText.toArray(text);

                    IExtent screenExtentInPixels = paintProps.getView()
                            .getExtent();
                    double ratio = screenExtentInPixels.getWidth()
                            / paintProps.getCanvasBounds().width;

                    graphicsTarget.drawStrings(font, text, labelPix[0],
                            labelPix[1] + 3 * ratio, 0.0, TextStyle.NORMAL,
                            new RGB[] { color, color, color },
                            HorizontalAlignment.LEFT, VerticalAlignment.TOP);
                }
                index++;
            }
        } catch (VizException vize) {
            logger.error("VizException is thrown when trying to drawLabel, error="
                    + vize.getMessage());
        }
    }

    private String getImmediateCauseDesc(String immediateCauseAbbreviation) {
        String desc = "";
        if (!StringUtil.isStringEmpty(immediateCauseAbbreviation)) {
            String[] abbreviationArray = immediateCauseAbbreviation.split(";");
            StringBuilder builder = new StringBuilder(
                    immediateCauseAbbreviation.length());
            for (String eachAbbreviation : abbreviationArray) {
                builder.append(
                        AwwImmediateCauseUtil
                                .getImmediateCauseDesc(eachAbbreviation))
                        .append("  ");
            }
            desc = builder.toString();
        }
        return desc;
    }

    private void drawSymbol(String symbolType, IGraphicsTarget graphicsTarget,
            PaintProperties paintProps, double[] latLonArray, RGB color,
            float symbolLineWidth, double symbolSizeScale) {
        Color symbolColor = new Color(color.red, color.green, color.blue);
        Coordinate coordinate = new Coordinate(
                latLonArray[0], latLonArray[1]);
        Symbol symbol = new Symbol(null, new Color[] { symbolColor },
                symbolLineWidth,// lineWidth, same as arrow's
                symbolSizeScale, true, coordinate, "Symbol", symbolType);

        DisplayElementFactory df = new DisplayElementFactory(graphicsTarget,
                getNcMapDescriptor());
        ArrayList<IDisplayable> displayElsPoint = df.createDisplayElements(
                symbol, paintProps);
        for (IDisplayable each : displayElsPoint) {
            each.draw(graphicsTarget, paintProps);
            each.dispose();
        }
    }

    private String convertToOldUgcStringPattern(String newUgcStringPattern) {
        /*
         * Convert the new string pattern to old county pattern
         */
        StringBuilder builder = new StringBuilder();
        if (!StringUtil.isStringEmpty(newUgcStringPattern)
                && newUgcStringPattern.trim().length() == 6) {
            builder.append(newUgcStringPattern.subSequence(0, 2)).append("C")
                    .append(newUgcStringPattern.substring(3));
        }
        return builder.toString();
    }

    private String removeMarker(String countyOrZoneAndStateNameWithMaker) {
        String[] countyOrZoneAndStateNameArray = countyOrZoneAndStateNameWithMaker
                .split(FFAConstant.HYPHEN);
        return countyOrZoneAndStateNameArray[0];
    }

    private String getCountyOrZoneAndStateNameValue(String combinedName) {
        StringBuilder builder = new StringBuilder();
        if (!StringUtil.isStringEmpty(combinedName)) {
            String[] countyOrZoneAndStateNameArray = combinedName
                    .split(FFAConstant.UNDERSTORE);
            builder.append(" ").append(countyOrZoneAndStateNameArray[0]);
            if (countyOrZoneAndStateNameArray.length == 2) {
                builder.append(", ").append(countyOrZoneAndStateNameArray[1]);
            }
        }
        return builder.toString();
    }

    private String getEventTimeStringValue(DataTime eventStartTime,
            DataTime eventEndTime) {
        StringBuilder builder = new StringBuilder(16);

        if (eventStartTime != null)
            builder.append(eventStartTime.toString().substring(8, 10) + "/"
                    + eventStartTime.toString().substring(11, 13)
                    + eventStartTime.toString().substring(14, 16));
        else
            builder.append("-");
        builder.append("-");
        if (eventEndTime != null)
            builder.append(eventEndTime.toString().substring(8, 10) + "/"
                    + eventEndTime.toString().substring(11, 13)
                    + eventEndTime.toString().substring(14, 16));
        else
            builder.append("-");
        return builder.toString();
    }

    private String getGreatLakeName(String lakeAreaName) {
        String greatLakeName = "Unknown great lake name";
        if (!StringUtil.isStringEmpty(lakeAreaName))
            greatLakeName = lakeAreaName.trim().replace(FFAConstant.UNDERSTORE,
                    " ");
        return greatLakeName;
    }

    private Double[] getLatLonArrayOfLake(String lakeAreaStringValue) {
        Double[] latLonDoubleArray = null;
        if (isLakeAreaStringValid(lakeAreaStringValue)) {
            String trimmedString = lakeAreaStringValue.trim();
            int stringLength = trimmedString.length();
            String[] latLonStringArray = trimmedString.substring(1,
                    stringLength - 1).split(",");
            if (latLonStringArray != null && latLonStringArray.length == 2) {
                try {
                    latLonDoubleArray = new Double[2];
                    latLonDoubleArray[0] = Double
                            .parseDouble(latLonStringArray[0]);
                    latLonDoubleArray[1] = Double
                            .parseDouble(latLonStringArray[1]);
                } catch (NumberFormatException nfe) {
                    latLonDoubleArray = null;
                }
            }
        }
        return latLonDoubleArray;
    }

    private boolean isLakeAreaStringValid(String lakeAreaStringValue) {
        boolean isValid = false;
        if (!StringUtil.isStringEmpty(lakeAreaStringValue)) {
            int strLength = lakeAreaStringValue.trim().length();
            if (lakeAreaStringValue.trim().indexOf('(') == 0
                    && lakeAreaStringValue.indexOf(')') == (strLength - 1)
                    && lakeAreaStringValue.indexOf(',') > 0)
                isValid = true;
        }
        return isValid;
    }

    @Override
    protected boolean postProcessFrameUpdate() {

        AbstractEditor ncme = NcDisplayMngr.getActiveNatlCntrsEditor();

        zrJob.setRequest(ncme.getActiveDisplayPane().getTarget(),
                getNcMapDescriptor(), null, false, false, null);

        return true;
    }

    public String getFfaRscDataObjKey(FfaRscDataObj f) {
        if (f == null)
            return "";
        return f.dataUri;

    }

    public List<String> getFips(AwwUgc eachAwwUgc) {
        List<String> list = new ArrayList<String>();

        if (eachAwwUgc == null || eachAwwUgc.getAwwFIPS() == null)
            return list;

        for (AwwFips afips : eachAwwUgc.getAwwFIPS()) {
            StringBuilder sb = new StringBuilder(afips.getFips()
                    .substring(0, 2));
            list.add(sb.append(afips.getFips().substring(3)).toString());//
        }

        return list;
    }

    // see comment in the else block for the Map values
    public boolean updateFrameData2(IRscDataObject rscDataObj,
            Map<String, ArrayList<FfaRscDataObj>> fDataMap) {

        if (!(rscDataObj instanceof FfaRscDataObj))
            return false;

        FfaRscDataObj frdo = (FfaRscDataObj) rscDataObj;

        String key = frdo.getKey();

        if (fDataMap.containsKey(key)) {

            fDataMap.get(key).add(frdo);
        } else {

            ArrayList<FfaRscDataObj> list = new ArrayList<FfaRscDataObj>();
            list.add(frdo);
            fDataMap.put(key, list);// NOT datauri
        }

        // }

        return true;
    }

    /**
     * handles the IWireframeShape pre-calculation
     * 
     * @author gzhang
     */
    private class ZoneResultJob extends org.eclipse.core.runtime.jobs.Job {

        private Map<String, Result> keyResultMap = new java.util.concurrent.ConcurrentHashMap<String, Result>();

        private IGraphicsTarget target;

        private IMapDescriptor descriptor;

        private RGB symbolColor = new RGB(155, 155, 155);

        public class Result {

            public IWireframeShape outlineShape;

            private Result(IWireframeShape outlineShape,
                    IWireframeShape nuShape, IShadedShape shadedShape,
                    Map<Object, RGB> colorMap) {

                this.outlineShape = outlineShape;

            }
        }

        public ZoneResultJob(String name) {
            super(name);
        }

        public void setRequest(IGraphicsTarget target,
                IMapDescriptor descriptor, String query, boolean labeled,
                boolean shaded, Map<Object, RGB> colorMap) {

            this.target = target;
            this.descriptor = descriptor;
            this.run(null);

        }

        @Override
        protected org.eclipse.core.runtime.IStatus run(
                org.eclipse.core.runtime.IProgressMonitor monitor) {

            for (AbstractFrameData afd : frameDataMap.values()) {

                FrameData fd = (FrameData) afd;

                /*
                 * list elements FfaRscDataObj.getKey() are all the same since
                 * they are just different action types: NEW, EXA/EXB
                 */
                for (FfaRscDataObj frdo : fd.ffaDataMap.values()) {// list){
                    Collection<Geometry> gw = new ArrayList<Geometry>();
                    for (int i = 0; i < frdo.fips.size(); i++) {
                        for (ArrayList<Object[]> zones : queryResult
                                .getZoneResult(frdo.fips.get(i))) {
                            if (zones == null)
                                continue;
                            WKBReader wkbReader = new WKBReader();
                            for (Object[] result : zones) {
                                int k = 0;
                                byte[] wkb1 = (byte[]) result[k];
                                MultiPolygon countyGeo = null;
                                try {
                                    countyGeo = (MultiPolygon) wkbReader
                                            .read(wkb1);
                                    if (countyGeo != null
                                            && !countyGeo.isEmpty()) {
                                        gw.add(countyGeo);
                                    }
                                } catch (Exception e) {
                                    logger.info("Exception: " + e.getMessage());
                                }
                            }
                        }

                    }
                    if (gw.size() == 0)
                        continue;
                    else
                        keyResultMap.put(frdo.getKey(), new Result(
                                getEachWrdoShape(gw), null, null, null));
                }
            }

            return org.eclipse.core.runtime.Status.OK_STATUS;
        }

        public IWireframeShape getEachWrdoShape(Collection<Geometry> gw) {

            IWireframeShape newOutlineShape = target.createWireframeShape(
                    false, descriptor, 0.0f);

            JTSCompiler jtsCompiler = new JTSCompiler(null, newOutlineShape,
                    descriptor, PointStyle.CROSS);

            GeometryCollection gColl = (GeometryCollection) new GeometryFactory()
                    .buildGeometry(gw);

            try {
                gColl.normalize();

                jtsCompiler.handle(gColl, symbolColor);

                newOutlineShape.compile();

            } catch (Exception e) {
                logger.info("_____Error: " + e.getMessage());
            }

            return newOutlineShape;
        }
    }

    private void drawZoneOutline2(FfaRscDataObj ffaData,
            IGraphicsTarget target, RGB color, int outlineWidth,
            LineStyle lineStyle, PaintProperties paintProps, Envelope env) {

        ZoneResultJob.Result result = zrJob.keyResultMap.get(ffaData.getKey());

        if (result != null) {
            if (outlineShape == null) {
                outlineShape = result.outlineShape;
            } else {
                outlineShape = result.outlineShape;
            }
        } else {
            return;
        }

        if (outlineShape != null && outlineShape.isDrawable()) {
            try {
                target.drawWireframeShape(outlineShape, color, outlineWidth,
                        lineStyle);
            } catch (VizException e) {
                logger.info("VizException in drawCountyOutline2() of FFAResource");
            }

        }
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        areaChangeFlag = true;
    }

    /**
     * avoid null pointers exception in super class
     */
    @Override
    protected long getDataTimeMs(IRscDataObject rscDataObj) {

        if (rscDataObj == null)
            return 0;

        java.util.Calendar validTimeInCalendar = null;
        DataTime dataTime = rscDataObj.getDataTime();
        if (dataTime != null) {
            validTimeInCalendar = dataTime.getValidTime();

        } else {
            logger.info("===== find IRscDataObject rscDataObj.getDataTime() return NULL!!!");
        }
        long dataTimeInMs = 0;
        if (validTimeInCalendar != null)
            dataTimeInMs = validTimeInCalendar.getTimeInMillis();
        return dataTimeInMs;
    }

    @Override
    public String getName() {
        String legendString = super.getName();
        FrameData fd = (FrameData) getCurrentFrame();
        if (fd == null || fd.getFrameTime() == null
                || fd.ffaDataMap.size() == 0) {
            return legendString + "-No Data";
        }
        return legendString + " "
                + NmapCommon.getTimeStringFromDataTime(fd.getFrameTime(), "/");
    }
}
