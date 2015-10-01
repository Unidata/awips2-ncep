package gov.noaa.nws.ncep.edex.plugin.geomag;

import gov.noaa.nws.ncep.common.dataplugin.geomag.calculation.CalcUtil;
import gov.noaa.nws.ncep.common.dataplugin.geomag.dao.GeoMagDao;
import gov.noaa.nws.ncep.common.dataplugin.geomag.exception.GeoMagException;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.GeoMagStation;
import gov.noaa.nws.ncep.common.dataplugin.geomag.table.Group;
import gov.noaa.nws.ncep.common.dataplugin.geomag.util.MissingValueCodeLookup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.edex.plugin.AbstractDecoder;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;

/**
 * This java class decodes geomagnetic data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 *                   
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 03/27/2013   975         sgurung     Initial Creation
 * 04/26/2013   975         qzhou       Added unit checkup. Declared missingVal.
 * 06/07/2013   975         qzhou       Fixed error on conversion
 * 07/16/2013   975         qzhou       Decoder redesign:  
 *                                      Changed the data entries in postgreSQL to minute(1440 entries). 
 *                                      Changed data overwrite to insert new data. Added insertion loop.
 *                                      Changed the write to from hdf5 to post. Added 5 columns: H,D,Z,F and badData.
 *                                      Removed source and sourcePreference tables.  
 *                                      Get source priority from GeoMagStaiton.xml
 *                                      Added handles for same stations but with or without header
 *                                      Fixed HAD, NGK, CNB default value
 * Aug 30, 2013 2298       rjpeter      Make getPluginName abstract
 * Dec 23, 2014 R5412      sgurung      Change float to double, add code changes for "debug mode"
 * 10/07/2015   R11429     sgurung,jtravis  Replaced hard-coded missing value codes, implemented conditions to check
 *                                          for missing values and perform appropriate actions, refactored method decode()
 * 
 * </pre>
 * 
 * @author sgurung, qzhou
 * @version 1
 */

public class GeoMagDecoder extends AbstractDecoder {
    private GeoMagDao dao;

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * Decode any valid input file containing geomag data
     * 
     * @param file
     * 
     * @return PluginDataObject[]
     * 
     * @throws Exception
     */
    public PluginDataObject[] decode(File file) throws Exception {

        BufferedReader in = null;
        int sourceId = 101;
        String dataUriSuffix = null;
        List<GeoMagData> dataList = new ArrayList<GeoMagData>();
        PluginDataObject[] pdo = new PluginDataObject[0];
        Pattern HEADER_EXP = null;
        Pattern DATA_EXP = null;
        boolean conversionRequired = false;
        HashMap<String, Group> headerGroupMap = new HashMap<String, Group>();
        HashMap<String, Group> dataGroupMap = new HashMap<String, Group>();

        logger.info("******** Start magnetometer decoder.");

        if ((file == null) || (file.length() < 1)) {
            return new PluginDataObject[0];
        }

        dataUriSuffix = CalcUtil.parseFileName(file.getName());

        try {

            GeoMagStation station = GeoMagDecoderUtils.getGeomagStation(file
                    .getName());

            if (station == null) {
                logger.error("Error decoding geomag file! Station code not found in geoMagStations.xml file.");
                return new PluginDataObject[0];
            }

            /*
             * Get regular expression for the header from the station metadata
             * file
             */
            if (GeoMagDecoderUtils.hasHeader(station)) {
                HEADER_EXP = Pattern.compile(station.getRawDataFormat()
                        .getHeaderFormat().getPattern());
                headerGroupMap = station.getRawDataFormat().getHeaderFormat()
                        .getHeaderGroups();
            }

            /*
             * Get regular expression for the data from the station metadata
             * file
             */
            if (GeoMagDecoderUtils.hasData(station)) {
                DATA_EXP = Pattern.compile(station.getRawDataFormat()
                        .getDataFormat().getPattern());

                dataGroupMap = station.getRawDataFormat().getDataFormat()
                        .getDataGroups();

                conversionRequired = station.getRawDataFormat().getDataFormat()
                        .getConversionRequired();
            }

            boolean firstLine = true;
            DataTime headerTime = null;
            GeoMagHeader header = null;
            GeoMagData geoMagData = null;

            String input = null;
            in = new BufferedReader(new FileReader(file));

            while ((input = in.readLine()) != null) {

                /*
                 * if this is the first line and header exists, parse the header
                 * information
                 */
                if (firstLine && GeoMagDecoderUtils.hasHeader(station)) {
                    header = GeoMagDecoderUtils.parseHeader(input, HEADER_EXP,
                            headerGroupMap);
                    headerTime = header.getHeaderTime();
                    sourceId = header.getSourceId();

                    firstLine = false;
                }

                if (GeoMagDecoderUtils.hasData(station)) {
                    /* if data exists, parse the data information */
                    Matcher dataMatcher = DATA_EXP.matcher(input);

                    if (dataMatcher.find()) {

                        /* if this is the first line and header does not exist */
                        if (firstLine && !GeoMagDecoderUtils.hasHeader(station)) {
                            header = new GeoMagHeader();

                            headerTime = GeoMagDecoderUtils.getRecordDataTime(
                                    dataMatcher, dataGroupMap);

                            // if no header, the sourceId is 101
                            sourceId = 101;
                            firstLine = false;

                            header.setHeaderTime(headerTime);
                            header.setSourceId(sourceId);
                            header.setStationCode(station.getStationCode());
                        }

                        // parse the data
                        geoMagData = GeoMagDecoderUtils
                                .parseData(dataMatcher, dataGroupMap,
                                        headerTime.getRefTimeAsCalendar());

                        // substitute station specific missing value codes with
                        // the default/global missing value code
                        Vector<Double> missingValueCodes = MissingValueCodeLookup
                                .getInstance().getMissingValues(
                                        station.getStationCode());

                        Vector<Double> componentValues = geoMagData
                                .getComponentValues();

                        for (int i = 0; i <= missingValueCodes.size() - 1; i++) {
                            double code = missingValueCodes.get(i);

                            for (int j = 0; j <= componentValues.size() - 1; j++) {
                                double cv = componentValues.get(j);

                                if (cv == code) {
                                    componentValues.set(j, CalcUtil.missingVal);
                                }
                            }

                        }

                        geoMagData.setComp1Val(componentValues.get(0));
                        geoMagData.setComp2Val(componentValues.get(1));
                        geoMagData.setComp3Val(componentValues.get(2));
                        geoMagData.setComp4Val(componentValues.get(3));

                        // Account for the fact that sometimes there
                        // are "abnormal" values
                        geoMagData = GeoMagDecoderUtils.processAbnormalValues(
                                header, geoMagData);

                        // Handle processing of the data based on conditions
                        if (Conditions.isCondition1Valid(geoMagData,
                                CalcUtil.missingVal)) {

                            // If the condition is true ignore the record -
                            // nothing is added to the db.
                            continue;
                        } else if (Conditions.isCondition2Valid(geoMagData,
                                CalcUtil.missingVal)) {

                            // If the condition is true add the record to the
                            // database and set component_4 to the missing value
                            // code
                            geoMagData.setComp4Val(CalcUtil.missingVal);
                        } else if (Conditions.isCondition3Valid(geoMagData,
                                CalcUtil.missingVal)) {

                            // If the condition is true add the record to
                            // the database and set component 1, component 2,
                            // and component 3 to the missing value
                            // code
                            geoMagData.setComp1Val(CalcUtil.missingVal);
                            geoMagData.setComp2Val(CalcUtil.missingVal);
                            geoMagData.setComp3Val(CalcUtil.missingVal);
                        }

                        /**
                         * Raw data from some providers might not be reported in
                         * the appropriate format/units. These data needs to be
                         * converted to northward component (X) in nT and
                         * eastward component (Y) in nT
                         */
                        if (conversionRequired
                                && (geoMagData.getComp1Val() != CalcUtil.missingVal && geoMagData
                                        .getComp2Val() != CalcUtil.missingVal)) {
                            Hashtable<String, Double> convertedValues = Conversion
                                    .convertHandD(geoMagData.getComp1Val(),
                                            geoMagData.getComp1RefersTo(),
                                            geoMagData.getComp2Val(),
                                            geoMagData.getComp2RefersTo());
                            geoMagData.setComp1Val(convertedValues
                                    .get(GeoMagDecoderUtils.COMPONENT_1));
                            geoMagData.setComp2Val(convertedValues
                                    .get(GeoMagDecoderUtils.COMPONENT_2));
                        }

                        dataList.add(geoMagData);

                    } // if (dataMatcher.find())
                } // end if containData
            } // end while

            pdo = GeoMagDecoderUtils.convertGeoMagDataToGeoMagRecord(header,
                    dataList, dataUriSuffix, dao);

            if (pdo == null || (pdo.length < 1)) {
                pdo = new PluginDataObject[0];
            }

        } catch (GeoMagException exGeoMag) {
            logger.error("Failed to decode file: [" + file.getAbsolutePath()
                    + "]", exGeoMag);
            throw new GeoMagException(exGeoMag.getMessage(), exGeoMag);

        } catch (IOException exIo) {
            throw new GeoMagException(exIo.getMessage(), exIo);

        } finally {
            in.close();
        }

        return pdo;
    }

    /**
     * @return GeoMagDao
     */
    public GeoMagDao getDao() {
        return dao;
    }

    /**
     * @param dao
     */
    public void setDao(GeoMagDao dao) {
        this.dao = dao;
    }

}