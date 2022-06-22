package gov.noaa.nws.ncep.edex.plugin.nctext.common.dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.edex.database.DataAccessLayerException;

import gov.noaa.nws.ncep.edex.common.dao.NcepDefaultPluginDao;
import gov.noaa.nws.ncep.edex.plugin.nctext.common.NctextRecord;

/**
 * This Data Access Object implements database query methods to get NTEXT
 * Records
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 10/29/2009       TBD     Chin Chen   Initial coding
 * Jun 22, 2022 8865        mapeters    Remove populateDataStore override, cleanup
 *                                      exception logging
 *
 * </pre>
 *
 * @author Chin Chen
 */
public class NctextRecordDao extends NcepDefaultPluginDao {

    /**
     * @param pluginName
     * @throws PluginException
     */
    public NctextRecordDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByDatauri(String datauri) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        fields.add(PluginDataObject.DATAURI_ID);
        values.add(datauri);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error(
                    "Error retrieving nctext records for data URI: " + datauri,
                    e);
        }
        return lNtextRecord;
    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataById(int id) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // the field name defined in NctextRecord
        fields.add("id");
        values.add(id);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for ID: " + id, e);
        }
        return lNtextRecord;
    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByWmoid(String wmoid) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // the field name defined in NctextRecord
        fields.add("wmoId");
        values.add(wmoid);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for WMO ID: " + wmoid,
                    e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataBySite(String site) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // the field name defined in NctextRecord
        fields.add("issueSite");
        values.add(site);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for site: " + site,
                    e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByProductType(String pType) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // the field name defined in NctextRecord
        fields.add("productType");
        values.add(pType);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for product type: "
                    + pType, e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByBBBind(String bbbInd) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // the field name defined in NctextRecord
        fields.add("bbbInd");
        values.add(bbbInd);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error(
                    "Error retrieving nctext records for bbbInd: " + bbbInd, e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByAwipsId(String awipsId) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // the field name defined in NctextRecord
        fields.add("awipsId");
        values.add(awipsId);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error(
                    "Error retrieving nctext records for AWIPS ID: " + awipsId,
                    e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByMultiFields(List<String> fields,
            List<Object> values) {
        List<NctextRecord> lNtextRecord = null;
        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for fields: " + fields
                    + ", values: " + values, e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByITime(String issueTime) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // IssueTime format should be yyyy-MM-dd hh:mm:ss
        int iYear = Integer.parseInt(issueTime.substring(0, 4).trim());
        int iMon = Integer.parseInt(issueTime.substring(5, 7).trim());
        int iDay = Integer.parseInt(issueTime.substring(8, 10).trim());
        int iHour = Integer.parseInt(issueTime.substring(11, 13).trim());
        int iMin = Integer.parseInt(issueTime.substring(14, 16).trim());
        int iSec = Integer.parseInt(issueTime.substring(17, 19).trim());
        Calendar ctime = Calendar.getInstance();
        /*
         * to clear msec field, otherwise will have problem to retrieve right
         * record
         */
        ctime.clear();
        ctime.set(iYear, iMon - 1, iDay, iHour, iMin, iSec);
        // the field name defined in NctextRecord
        fields.add("issueTime");
        values.add(ctime);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for issue time: "
                    + issueTime, e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByITime(String issueTime,
            List<String> fields, List<Object> values) {
        List<NctextRecord> lNtextRecord = null;
        // IssueTime format should be yyyy-MM-dd hh:mm:ss
        int iYear = Integer.parseInt(issueTime.substring(0, 4).trim());
        int iMon = Integer.parseInt(issueTime.substring(5, 7).trim());
        int iDay = Integer.parseInt(issueTime.substring(8, 10).trim());
        int iHour = Integer.parseInt(issueTime.substring(11, 13).trim());
        int iMin = Integer.parseInt(issueTime.substring(14, 16).trim());
        int iSec = Integer.parseInt(issueTime.substring(17, 19).trim());
        Calendar ctime = Calendar.getInstance();
        /*
         * to clear msec field, otherwise will have problem to retrieve right
         * record
         */
        ctime.clear();
        ctime.set(iYear, iMon - 1, iDay, iHour, iMin, iSec);
        // the field name defined in NctextRecord
        fields.add("issueTime");
        values.add(ctime);

        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for issue time: "
                    + issueTime + ", fields: " + fields + ", values: " + values,
                    e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByIssueTimeRange(String issueTimeStart,
            String issueTimeEnd, List<String> fields, List<Object> values,
            List<String> operands) {
        List<NctextRecord> lNtextRecord = null;
        // IssueTime format should be yyyy-MM-dd hh:mm:ss
        int iYear = Integer.parseInt(issueTimeStart.substring(0, 4).trim());
        int iMon = Integer.parseInt(issueTimeStart.substring(5, 7).trim());
        int iDay = Integer.parseInt(issueTimeStart.substring(8, 10).trim());
        int iHour = Integer.parseInt(issueTimeStart.substring(11, 13).trim());
        int iMin = Integer.parseInt(issueTimeStart.substring(14, 16).trim());
        int iSec = Integer.parseInt(issueTimeStart.substring(17, 19).trim());
        Calendar cstime = Calendar.getInstance();
        /*
         * to clear msec field, otherwise will have problem to retrieve right
         * record
         */
        cstime.clear();
        cstime.set(iYear, iMon - 1, iDay, iHour, iMin, iSec);
        iYear = Integer.parseInt(issueTimeEnd.substring(0, 4).trim());
        iMon = Integer.parseInt(issueTimeEnd.substring(5, 7).trim());
        iDay = Integer.parseInt(issueTimeEnd.substring(8, 10).trim());
        iHour = Integer.parseInt(issueTimeEnd.substring(11, 13).trim());
        iMin = Integer.parseInt(issueTimeEnd.substring(14, 16).trim());
        iSec = Integer.parseInt(issueTimeEnd.substring(17, 19).trim());
        Calendar cetime = Calendar.getInstance();
        /*
         * to clear msec field, otherwise will have problem to retrieve right
         * record
         */
        cetime.clear();
        cetime.set(iYear, iMon - 1, iDay, iHour, iMin, iSec);
        // the field name defined in NctextRecord
        fields.add("issueTime");
        values.add(cstime);
        operands.add(">=");
        // the field name defined in NctextRecord
        fields.add("issueTime");
        values.add(cetime);
        operands.add("<=");
        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values,
                    operands);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for issue time: "
                    + issueTimeStart + " - " + issueTimeEnd + ", fields: "
                    + fields + ", values: " + values + ", operand: " + operands,
                    e);
        }
        return lNtextRecord;

    }

    @SuppressWarnings("unchecked")
    public List<NctextRecord> getDataByIssueTimeRange(String issueTimeStart,
            String issueTimeEnd) {
        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<String> operands = new ArrayList<>();
        List<NctextRecord> lNtextRecord = null;
        // IssueTime format should be yyyy-MM-dd hh:mm:ss
        int iYear = Integer.parseInt(issueTimeStart.substring(0, 4).trim());
        int iMon = Integer.parseInt(issueTimeStart.substring(5, 7).trim());
        int iDay = Integer.parseInt(issueTimeStart.substring(8, 10).trim());
        int iHour = Integer.parseInt(issueTimeStart.substring(11, 13).trim());
        int iMin = Integer.parseInt(issueTimeStart.substring(14, 16).trim());
        int iSec = Integer.parseInt(issueTimeStart.substring(17, 19).trim());
        Calendar cstime = Calendar.getInstance();
        /*
         * to clear msec field, otherwise will have problem to retrieve right
         * record
         */
        cstime.clear();
        cstime.set(iYear, iMon - 1, iDay, iHour, iMin, iSec);
        iYear = Integer.parseInt(issueTimeEnd.substring(0, 4).trim());
        iMon = Integer.parseInt(issueTimeEnd.substring(5, 7).trim());
        iDay = Integer.parseInt(issueTimeEnd.substring(8, 10).trim());
        iHour = Integer.parseInt(issueTimeEnd.substring(11, 13).trim());
        iMin = Integer.parseInt(issueTimeEnd.substring(14, 16).trim());
        iSec = Integer.parseInt(issueTimeEnd.substring(17, 19).trim());
        Calendar cetime = Calendar.getInstance();
        /*
         * to clear msec field, otherwise will have problem to retrieve right
         * record
         */
        cetime.clear();
        cetime.set(iYear, iMon - 1, iDay, iHour, iMin, iSec);
        // the field name defined in NctextRecord
        fields.add("issueTime");
        values.add(cstime);
        operands.add(">=");
        // the field name defined in NctextRecord
        fields.add("issueTime");
        values.add(cetime);
        operands.add("<=");
        try {
            lNtextRecord = (List<NctextRecord>) queryByCriteria(fields, values,
                    operands);
        } catch (DataAccessLayerException e) {
            logger.error("Error retrieving nctext records for issue time: "
                    + issueTimeStart + " - " + issueTimeEnd, e);
        }
        return lNtextRecord;

    }

}
