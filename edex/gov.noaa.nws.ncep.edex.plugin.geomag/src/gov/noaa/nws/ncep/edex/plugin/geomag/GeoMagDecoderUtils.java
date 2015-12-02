/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.edex.plugin.geomag;

import gov.noaa.nws.ncep.common.dataplugin.geomag.GeoMagRecord;
import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcUtil;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.exception.GeoMagException;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.GeoMagSource;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.GeoMagStation;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.Group;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.GeoMagStationLookup;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.TableTimeStamp;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

/**
 * GeoMagDecoderUtils provides methods that when used in the appropriate
 * sequence all for decoding geomagnetic data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 29, 2015            sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public abstract class GeoMagDecoderUtils {

    public static final String UNIT = "unit";
    public static final String SOURCE = "source";
    public static final String STATION_CODE = "stationCode";
    public static final String OBS_DATE = "obsDate";
    public static final String OBS_YEAR = "obsYear";
    public static final String OBS_TIME = "obsTime";
    public static final String OBS_HOUR = "obsHour";
    public static final String OBS_MINUTE = "obsMinute";
    public static final String OBS_MINUTE_NUM = "obsMinuteNum";
    public static final String OBS_DAY_OF_YEAR = "obsDayOfYear";
    public static final String COMPONENT_1 = "component1";
    public static final String COMPONENT_2 = "component2";
    public static final String COMPONENT_3 = "component3";
    public static final String COMPONENT_4 = "component4";
    public static final String H_VALUE = "H";
    public static final String D_VALUE = "D";
    public static final String ABNORMAL_HEADER_UNIT = "0.01nT";
    public static final String REPORT_TYPE = "GEOMAG";

    /**
     * Convert the list of GeoMagData objects to the persistable
     * PluginDataObject type.
     * 
     * @param header
     * @param data
     * @param dataUriSuffix
     * @param dao
     * 
     * @return PluginDataObject[]
     */
    public static PluginDataObject[] convertGeoMagDataToGeoMagRecord(
            GeoMagHeader header, List<GeoMagData> data, String dataUriSuffix,
            GeoMagDao dao) {

        List<PluginDataObject> pdo = new ArrayList<PluginDataObject>();
        GeoMagRecord record = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss.s");

        for (int i = 0; i < data.size(); i++) {

            record = new GeoMagRecord();

            Date time = data.get(i).getObservationTime().getTime();
            String newUriTime = new String(sdf.format(time));
            String newUri = null;

            if (dataUriSuffix == null) {
                newUri = File.separator
                        + GeoMagDecoderUtils.REPORT_TYPE.toLowerCase()
                        + File.separator + newUriTime + File.separator
                        + header.getStationCode() + File.separator
                        + header.getSourceId() + File.separator
                        + GeoMagDecoderUtils.REPORT_TYPE;
            } else {
                newUri = File.separator
                        + GeoMagDecoderUtils.REPORT_TYPE.toLowerCase()
                        + File.separator + newUriTime + File.separator
                        + header.getStationCode() + File.separator
                        + header.getSourceId() + File.separator
                        + GeoMagDecoderUtils.REPORT_TYPE + dataUriSuffix;
            }

            // check to see if a record with this dataURI already exists in the
            // database
            List<?> resultsList = findUriFromDb(newUri, dao);

            // if record does not exist, set the fields of the record and
            // add it to the list of records to be persisted
            if ((resultsList == null) || resultsList.isEmpty()) {
                record.setStationCode(header.getStationCode());

                record.setSourceId(header.getSourceId());

                record.setDataURI(newUri);

                record.setComponent_1(data.get(i).getComp1Val());

                record.setComponent_2(data.get(i).getComp2Val());

                record.setComponent_3(data.get(i).getComp3Val());

                record.setComponent_4(data.get(i).getComp4Val());

                record.setDataTime(new DataTime(time));

                if (dataUriSuffix == null) {
                    record.setReportType(GeoMagDecoderUtils.REPORT_TYPE);
                } else {
                    record.setReportType(GeoMagDecoderUtils.REPORT_TYPE
                            + dataUriSuffix);
                }

                record.setOverwriteAllowed(false);
                record.setDataURI(record.getDataURI());
                pdo.add(record);
            }
        }

        return pdo.toArray(new PluginDataObject[pdo.size()]);

    }

    /**
     * Abnormal values, values where the header indicates 0.01nT, contain data
     * value that must be divided by 100.
     * 
     * @param header
     * @param data
     * 
     * @return GeoMagData
     */
    public static GeoMagData processAbnormalValues(GeoMagHeader header,
            GeoMagData data) {

        if (header.isUnitsAssigned()
                && header.getUnit().compareToIgnoreCase(
                        GeoMagDecoderUtils.ABNORMAL_HEADER_UNIT) == 0) {

            if (data.getComp1Val() != CalcUtil.missingVal) {
                data.setComp1Val(data.getComp1Val() / 100);
            }

            if (data.getComp2Val() != CalcUtil.missingVal) {
                data.setComp2Val(data.getComp2Val() / 100);
            }

            if (data.getComp3Val() != CalcUtil.missingVal) {
                data.setComp3Val(data.getComp3Val() / 100);
            }

            if (data.getComp4Val() != CalcUtil.missingVal) {
                data.setComp4Val(data.getComp4Val() / 100);
            }
        }

        return data;
    }

    /**
     * Parse the raw data from the data file
     * 
     * @param inputHeader
     * @param pattern
     * @param dataGroup
     * 
     * @return GeoMagData
     * 
     * @throws ParseException
     * @throws GeoMagException
     */
    public static GeoMagData parseData(Matcher dataMatcher,
            HashMap<String, Group> dataGroup, Calendar headerTime)
            throws ParseException, GeoMagException {

        int groupId = -1;
        GeoMagData data = new GeoMagData();

        // parse the observation time for the minute data
        data.setObservationTime(parseObservationTime(dataMatcher, dataGroup,
                headerTime));

        // parse the component values (h or x, d or y ,z and f)
        groupId = (dataGroup.get(COMPONENT_1) != null) ? dataGroup.get(
                COMPONENT_1).getId() : -1;
        if (groupId != -1) {
            data.setComp1RefersTo(dataGroup.get(COMPONENT_1).getRefersTo());
            data.setComp1Val(Double.parseDouble(dataMatcher.group(groupId)));
        }

        groupId = (dataGroup.get(COMPONENT_2) != null) ? dataGroup.get(
                COMPONENT_2).getId() : -1;
        if (groupId != -1) {
            data.setComp2RefersTo(dataGroup.get(COMPONENT_2).getRefersTo());
            data.setComp2Val(Double.parseDouble(dataMatcher.group(groupId)));
        }

        groupId = (dataGroup.get(COMPONENT_3) != null) ? dataGroup.get(
                COMPONENT_3).getId() : -1;
        if (groupId != -1) {
            data.setComp3RefersTo(dataGroup.get(COMPONENT_3).getRefersTo());
            Double comp3Val = Double.parseDouble(dataMatcher.group(groupId));
            if (comp3Val == null) {
                comp3Val = CalcUtil.missingVal;
            }
            data.setComp3Val(comp3Val);
        }

        groupId = (dataGroup.get(COMPONENT_4) != null) ? dataGroup.get(
                COMPONENT_4).getId() : -1;
        if (groupId != -1) {
            data.setComp4RefersTo(dataGroup.get(COMPONENT_4).getRefersTo());
            Double comp4Val = Double.parseDouble(dataMatcher.group(groupId));
            if (comp4Val == null) {
                comp4Val = CalcUtil.missingVal;
            }
            data.setComp4Val(comp4Val);
        }

        return data;

    }

    /**
     * Parse the raw header from the data file
     * 
     * @param inputHeader
     * @param pattern
     * @param headerGroup
     * 
     * @return GeoMagHeader
     * 
     * @throws ParseException
     * @throws GeoMagException
     */
    public static GeoMagHeader parseHeader(String inputHeader, Pattern pattern,
            HashMap<String, Group> headerGroup) throws ParseException,
            GeoMagException {

        int groupId = -1;
        GeoMagHeader header = new GeoMagHeader();

        /*
         * if this is the first line and header exists, parse the header
         * information
         */

        Matcher headerMatcher = pattern.matcher(inputHeader);

        if (headerMatcher.find()) {
            // set the station code
            groupId = (headerGroup.get(STATION_CODE) != null) ? headerGroup
                    .get(STATION_CODE).getId() : -1;
            if (groupId != -1) {
                header.setStationCode(headerMatcher.group(groupId));
            }

            // set the source
            groupId = (headerGroup.get(SOURCE) != null) ? headerGroup.get(
                    SOURCE).getId() : -1;

            if (groupId != -1) {
                String source = headerMatcher.group(groupId);
                ArrayList<GeoMagSource> src = GeoMagDecoderUtils
                        .getStationDetail(header.getStationCode(), true)
                        .getSource();
                for (int i = 0; i < src.size(); i++) {
                    String name = src.get(i).getName();
                    if (name.equalsIgnoreCase(source)) {
                        header.setSourceId(src.get(i).getPriority());
                    }
                }
            }

            // get the unit
            groupId = (headerGroup.get(UNIT) != null) ? headerGroup.get(UNIT)
                    .getId() : -1;
            if (groupId != -1) {
                header.setUnit(headerMatcher.group(groupId));
            }

            // get the time
            header.setHeaderTime(GeoMagDecoderUtils.getRecordDataTime(
                    headerMatcher, headerGroup));
        }

        return header;
    }

    /**
     * Determine if the station has a header associated with data files provided
     * by that station
     * 
     * @param station
     * 
     * @return boolean
     */
    public static boolean hasHeader(GeoMagStation station) {

        boolean hasHeader = false;

        if (station.getRawDataFormat().getHeaderFormat() != null) {
            hasHeader = true;
        }

        return hasHeader;
    }

    /**
     * Determine if the station have data that needs to be parsed
     * 
     * @param station
     * 
     * @return boolean
     */
    public static boolean hasData(GeoMagStation station) {

        boolean hasData = false;

        if (station.getRawDataFormat().getDataFormat() != null) {
            hasData = true;
        }

        return hasData;
    }

    /**
     * Obtain the geomag station
     * 
     * @param fileName
     * @return GeoMagStation
     * 
     * @throws GeoMagException
     */
    public static GeoMagStation getGeomagStation(String fileName)
            throws GeoMagException {

        GeoMagStation station = null;
        String stationCode = null;

        stationCode = fileName.substring(0, 3).toUpperCase();

        // for Hartland (HAD), Korea (JEJ) data, filename does not have full
        // station code
        if (stationCode.startsWith("HA")) {
            stationCode = "HAD";
        } else if (stationCode.startsWith("MEA")) {
            stationCode = "MEA";
        } else if (stationCode.startsWith("M")) {
            stationCode = "JEJ";
        }

        // get the station detail from metadata file 'geoMagStations.xml'
        // File has header & end with min. File has no header & end with
        // min. File has no header & not end with min.
        if (!fileName.endsWith(".min")) {
            station = GeoMagDecoderUtils.getStationDetail(stationCode, false);
        } else {
            station = GeoMagDecoderUtils.getStationDetail(stationCode, true);
        }

        return station;

    }

    /**
     * Obtain the station specific details from the geomagStations xml
     * configuration file
     * 
     * @param stnCode
     * @param hasHeader
     * @return GeoMagStation
     * 
     * @throws GeoMagException
     */
    private static GeoMagStation getStationDetail(String stnCode,
            boolean hasHeader) throws GeoMagException {
        GeoMagStation station = null;

        if (stnCode != null) {
            TableTimeStamp.updateXmlTables();
            station = GeoMagStationLookup.getInstance().getStationByCode(
                    stnCode, hasHeader);
        }

        return station;
    }

    /**
     * Obtain the data-time
     * 
     * @param matcher
     * @param groupMap
     * 
     * @return DataTime
     * 
     * @throws ParseException
     */
    public static DataTime getRecordDataTime(Matcher matcher,
            HashMap<String, Group> groupMap) throws ParseException {

        int groupId = -1;

        String obsDateStr = null;
        String obsYearStr = null;
        String obsDayOfYearStr = null;

        String format = "dd-MMM-yy";
        SimpleDateFormat obsTimeDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Calendar cal = Calendar.getInstance();
        Date obsDate = cal.getTime();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat(format);

        groupId = (groupMap.get(OBS_DATE) != null) ? groupMap.get(OBS_DATE)
                .getId() : -1;
        if (groupId != -1) {
            obsDateStr = matcher.group(groupId);
            format = (groupMap.get(OBS_DATE).getFormat() != null) ? groupMap
                    .get(OBS_DATE).getFormat() : format;
        }

        groupId = (groupMap.get(OBS_YEAR) != null) ? groupMap.get(OBS_YEAR)
                .getId() : -1;
        if (groupId != -1) {
            obsYearStr = matcher.group(groupId);
        }

        groupId = (groupMap.get(OBS_DAY_OF_YEAR) != null) ? groupMap.get(
                OBS_DAY_OF_YEAR).getId() : -1;
        if (groupId != -1) {
            obsDayOfYearStr = matcher.group(groupId);
        }

        // get Observation Date using obsDate
        if (obsDateStr != null) {
            inputDateFormat = new SimpleDateFormat(format);
            obsDate = obsTimeDateFormat.parse(obsTimeDateFormat
                    .format(inputDateFormat.parse(obsDateStr)));
        }

        // get Observation Date using obsYear and obsDayOfYear
        if ((obsYearStr != null) && (obsDayOfYearStr != null)) {
            Calendar tmpCal = Calendar.getInstance();
            tmpCal.set(Calendar.YEAR, Integer.parseInt(obsYearStr));
            tmpCal.set(Calendar.DAY_OF_YEAR, Integer.parseInt(obsDayOfYearStr));

            obsDate = obsTimeDateFormat.parse(obsTimeDateFormat.format(tmpCal
                    .getTime()));
        }

        cal.setTime(obsDate);

        DataTime dataTime = new DataTime(cal);

        return dataTime;
    }

    /**
     * Obtain the observation time
     * 
     * @param matcher
     * @param groupMap
     * @param time
     * 
     * @return Calendar
     * 
     * @throws ParseException
     */
    private static Calendar parseObservationTime(Matcher matcher,
            HashMap<String, Group> groupMap, Calendar time)
            throws ParseException {

        int groupId = -1;

        String obsDateStr = null;
        String obsTimeStr = null;
        String obsHourStr = null;
        String obsMinuteStr = null;
        String obsMinuteNumStr = null;
        String dateFormat = "dd-MMM-yy";
        String timeFormat = "HH:mm:ss";
        SimpleDateFormat inputDateFormat = new SimpleDateFormat(dateFormat
                + " " + timeFormat);

        Calendar obsTime = time;

        groupId = (groupMap.get(OBS_DATE) != null) ? groupMap.get(OBS_DATE)
                .getId() : -1;
        if (groupId != -1) {
            obsDateStr = matcher.group(groupId);
            dateFormat = (groupMap.get(OBS_DATE).getFormat() != null) ? groupMap
                    .get(OBS_DATE).getFormat() : dateFormat;

        }

        groupId = (groupMap.get(OBS_TIME) != null) ? groupMap.get(OBS_TIME)
                .getId() : -1;
        if (groupId != -1) {
            obsTimeStr = matcher.group(groupId);
            timeFormat = (groupMap.get(OBS_TIME).getFormat() != null) ? groupMap
                    .get(OBS_TIME).getFormat() : timeFormat;
        }

        groupId = (groupMap.get(OBS_MINUTE_NUM) != null) ? groupMap.get(
                OBS_MINUTE_NUM).getId() : -1;
        if (groupId != -1) {
            obsMinuteNumStr = matcher.group(groupId);

        }

        groupId = (groupMap.get(OBS_HOUR) != null) ? groupMap.get(OBS_HOUR)
                .getId() : -1;
        if (groupId != -1) {
            obsHourStr = matcher.group(groupId);
        }

        groupId = (groupMap.get(OBS_MINUTE) != null) ? groupMap.get(OBS_MINUTE)
                .getId() : -1;
        if (groupId != -1) {
            obsMinuteStr = matcher.group(groupId);
        }

        // get obsTime using obsMinuteNum
        if (obsMinuteNumStr != null) {
            obsTime.add(
                    Calendar.MINUTE,
                    (obsMinuteNumStr != null) ? Integer
                            .parseInt(obsMinuteNumStr) : 1);
        }
        // get obsTime using obsHour and obsMinute
        else if ((obsHourStr != null) && (obsMinuteStr != null)) {
            int minutes = (Integer.parseInt(obsHourStr) * 60)
                    + Integer.parseInt(obsMinuteStr);
            obsTime.add(Calendar.MINUTE, minutes);
        }

        // get obsTime using obsDate and obsTime
        else if ((obsDateStr != null) && (obsTimeStr != null)) {
            String obsDateTimeStr = obsDateStr + " " + obsTimeStr;
            inputDateFormat = new SimpleDateFormat(dateFormat + " "
                    + timeFormat);

            Date obsDateTime = inputDateFormat.parse(obsDateTimeStr);
            obsTime.setTime(obsDateTime);
        }

        return obsTime;
    }

    /**
     * Obtain the URI from the database instance
     * 
     * @param newUri
     * @param dao
     * 
     * @return List<?>
     */
    private static List<?> findUriFromDb(String newUri, GeoMagDao dao) {

        DatabaseQuery query = new DatabaseQuery(GeoMagRecord.class.getName());
        query.addQueryParam("dataURI", newUri);

        List<?> resultsList = null;
        try {
            resultsList = dao.queryByCriteria(query);
        } catch (DataAccessLayerException e1) {
            e1.printStackTrace();
        }

        return resultsList;
    }

}
