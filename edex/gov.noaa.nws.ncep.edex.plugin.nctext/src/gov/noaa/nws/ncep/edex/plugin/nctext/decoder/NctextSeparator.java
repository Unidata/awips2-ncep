/**
 * 
 * NctextSeparator 
 * 
 * This java class performs text data separating function
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 10/23/2009               Chin Chen   Initial coding
 * 07/10/2014               Chin Chen  .fixed month adjustment
 * 02/21/2017   R28184      Chin Chen   fixed slow loading time for Observed TAF Data Products issue
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

package gov.noaa.nws.ncep.edex.plugin.nctext.decoder;

import gov.noaa.nws.ncep.edex.plugin.nctext.common.NctextRecord;
import gov.noaa.nws.ncep.edex.plugin.nctext.common.NctextTafStn;
import gov.noaa.nws.ncep.edex.plugin.nctext.common.dao.NctextInputFileTypeDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.plugin.AbstractRecordSeparator;
import com.raytheon.edex.util.Util;
import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.dataquery.db.QueryResultRow;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.decodertools.core.IDecoderConstants;

public class NctextSeparator extends AbstractRecordSeparator {

    Log logger = LogFactory.getLog(getClass());

    public static final String WMO_HEADER = "(([A-Z]{4})(\\d{0,2}|[A-Z]{0,2})) ([A-Z]{4}) (\\d{6})[^\\r\\n]*[\\r\\n]+";

    private static final int WMOIDGROUP_NUMBER = 1;

    private static final int CCCCGROUP_NUMBER = 4;

    private static final int YYGGggGROUP_NUMBER = 5;

    public static final String AWIPS_ID = "([A-Z0-9]{4,6})";

    public static final String BBBID = "[A-Z]{3}";
    
    public static final String STATION_ID = "[A-Z]{4}";

    public static final String R_TYPE_SEPARATOR = "[A-Z]{3,4}+//";

    public static final String R_TYPE_SEPARATOR2 = "00///";

    public static final String R_TYPE_SEPARATOR3 = "[A-Z]{3,4}";

    public static String[][] gdStnGp = { { "PWM", "BGR", "CAR", "CON", "AFA" },
            { "ALB", "BOS", "PHL", "BTV", "LGA", "IPT" },
            { "DCA", "RDU", "ILM", "ORF", "HAT" },
            { "CAE", "MIA", "TLH", "SAV", "LAL" },
            { "BUF", "CLE", "CRW", "PIT", "DAY", "IND" },
            { "STL", "MEM", "TYS", "SDF", "BNA", "ATL" },
            { "BHM", "JAN", "SHV", "MOB", "MSY", "LIT" },
            { "DTW", "MKE", "INL", "SSM", "MSP", "ORD" },
            { "DSM", "DDC", "LBF", "TOP", "OMA", "BFF" },
            { "OKC", "SAT", "BRO", "DFW", "IAH", "DRT" },
            { "BIS", "RAP", "BIL", "FSD", "GTF", "MSO" },
            { "LBB", "ABQ", "DEN", "ELP", "PHX", "CYS" },
            { "SEA", "PDX", "BOI", "GEG", "MFR", "PIH" },
            { "SFO", "LAX", "SLC", "FAT", "RNO", "CDC" },
            { "YQB", "YOW", "YYB", "YQT", "YMW", "YLH" },
            { "YWG", "YQR", "YYC", "YQD", "YPA", "YEG" },
            { "MCD", "YXC", "YVR", "YRV", "YCG", "YXS" },
            { "X68", "EDW", "LWS", "UCC", "BTNM", "LGIN" },
            { "SYR", "ROA", "ZZV", "AOO", "AVL" },
            { "SGF", "GRB", "GJT", "JKL", "MLI", "FAR", "LND" },
            { "MAF", "LCH", "TUL", "DHT", "DHN", "DAB" },
            { "GGW", "RDD", "LAS", "PDT", "EKO", "FLG" },
            { "AKN", "CZF", "EDF", "EHM", "SVW", "TLJ" },
            { "EIL", "GAL", "LUR", "TNC", "UTO" },
            { "ANC", "ANN", "CDV", "JNU", "ORT", "YAK" },
            { "ADQ", "BET", "CDB", "MCG", "SNP" },
            { "BRW", "BTI", "BTT", "FAI", "OME", "OTZ" },
            { "TJSJ", "TJMZ", "TJPS", "TJBQ", "TJGU", "TJAD" },
            { "TIST", "TISX", "TNCM", "TKPK", "TJNR", "TISJ" },
            { "PHNL", "PHLI", "PHOG", "PHTO", "HIB1", "HIB4" } };

    public static final byte ASCII_RS = 0x1E; // record separator "^^"

    public static final byte ASCII_SP = 0x20; // SPACE

    private String cccc = null;

    private String YYGGgg = null;

    private static final int MIN_AWIPS_HDR_LENGTH = 25;

    private String BBBIndicator;

    private static final int BBB_SIZE = 3;

    private int messageDataStart = 0; /*
                                       * start of text record data after
                                       * WMOHeader and AWIPSId
                                       */

    private int recordStart = 0; /* start of text record */

    private enum ParseError {
        NO_CTLA_TO_END, NO_CTLA_IN_BULLETIN, NO_CTLC_TO_END, NO_AWIPS_HDR, NO_AWIPS_HDR_IN_BULLETIN, GENERAL_ERR, NO_ERR
    }

    private ParseError parseErr;

    private List<NctextRecord> reports = null;

    String traceId = null; /* ingest file name */

    private int currentReport = -1;

    private String awipsId;

    private String WMOId = null; // TTAAii

    private String ingestFileExt = null; // ingest file extension has
                                         // information of product type

    private NctextInputFileTypeDao nctextInputFileTypeDao;

    private static Set<String> tafStnIdSet = null;

    /**
     * 
     * @param traceId
     *            : most case would be ingest file name
     */
    public NctextSeparator(String traceId) {
        this.traceId = traceId;
        nctextInputFileTypeDao = new NctextInputFileTypeDao();
        int ind = traceId.indexOf('.');
        if (ind > 0)
            ingestFileExt = traceId.substring(ind + 1);// use ingest file name
                                                       // extension as product
                                                       // type
        else
            ingestFileExt = "NA";
    }

    @Override
    public NctextRecord next() {
        NctextRecord data = null;
        if (hasNext()) {
            data = reports.get(currentReport++);
        }
        return data;
    }

    /**
     * @return Is there another record available?
     */
    @Override
    public boolean hasNext() {
        return ((reports != null) && (reports.size() > 0) && (currentReport < reports
                .size()));
    }

    public List<NctextRecord> getRecordList() {
        return reports;
    }

    private synchronized boolean parseRcdHeader(String sInputMessageData) {
        // Assume not valid until proven otherwise!
        boolean isValid = false;
        parseErr = ParseError.GENERAL_ERR;
        BBBIndicator = "REG";
        awipsId = "NA";

        if (sInputMessageData != null) {
            // CtlA is the start of a record bulletin and Ctl-C is the end of it
            int CtlAPos = sInputMessageData
                    .indexOf(IDecoderConstants.ASCII_SOM);
            
            if ((CtlAPos == -1)
                    && (sInputMessageData.length() <= MIN_AWIPS_HDR_LENGTH)) {
                parseErr = ParseError.NO_ERR; // end of file
                return isValid;
            }
            if (CtlAPos == -1) {
                parseErr = ParseError.NO_CTLA_TO_END;
                return isValid;
            }
            String sMessageData = sInputMessageData.substring(CtlAPos);

            Pattern p = Pattern.compile(WMO_HEADER);
            Matcher m = p.matcher(sMessageData);
            if (m.find()) {

                messageDataStart = m.end();
                WMOId = m.group(WMOIDGROUP_NUMBER);
                cccc = m.group(CCCCGROUP_NUMBER);

                YYGGgg = m.group(YYGGggGROUP_NUMBER);
                if ((m.end() - m.end(YYGGggGROUP_NUMBER)) >= BBB_SIZE) {
                    String sBBB = sMessageData.substring(
                            m.end(YYGGggGROUP_NUMBER), m.end());
                    Pattern pBBB = Pattern.compile(BBBID);
                    Matcher mBBB = pBBB.matcher(sBBB);

                    // If there is an optional BBB;
                    if (mBBB.find()) {
                        BBBIndicator = mBBB.group();
                    }
                }
                // parse 2nd line to get AWIPSId nnnxxx
                String sl = sMessageData.substring(m.end());/*
                                                             * String from 2nd
                                                             * line down to end
                                                             */
                int endLineCR = sl.indexOf(IDecoderConstants.ASCII_CR);
                int endLineLF = sl.indexOf(IDecoderConstants.ASCII_LF);
                sl = sl.substring(0, Math.min(endLineCR, endLineLF));// String
                                                                     // of 2nd
                                                                     // line
                                                                     // only
                Pattern pAWIPS = Pattern.compile(AWIPS_ID);
                Matcher ml = pAWIPS.matcher(sl);
                if (ml.find()) {
                    awipsId = ml.group();
                }
                // set Record start pointer (recordStart), remove CR, LN and Soh
                // (Ctl-A) from the record head
                recordStart = 0;
                int msgleng = sMessageData.length();
                while (msgleng > 0) {
                    if ((sMessageData.charAt(recordStart) == IDecoderConstants.ASCII_SOM)
                            || (sMessageData.charAt(recordStart) == IDecoderConstants.ASCII_LF)
                            || (sMessageData.charAt(recordStart) == IDecoderConstants.ASCII_CR)) {
                        // skip ASCII_SOM (it is a typo in imported
                        // package, should be SOH, Ctl-A )
                        // also skip CR and LN
                        msgleng--;
                        recordStart++;
                    } else {
                        break;
                    }

                }
                isValid = true;

            } else {
                parseErr = ParseError.NO_AWIPS_HDR;
            }
        }
        return isValid;
    }

    private String[] getGdStnGp(String stnId) {
        for (String[] stnGp : gdStnGp) {
            for (int i = 0; i < stnGp.length; i++) {
                if (stnId.equals(stnGp[i])) {
                    return stnGp;
                }
            }
        }
        return null;
    }
    /*
     * Create a new tafStnIdSet if it is not already created.
     * A tafStnIdSet contains all distinct station id from nwx.tafstn
     * and nwx.taf tables. 
     */
    private Set<String> getTafStnSet(){
        if (tafStnIdSet == null){
            String queryStr ="Select stnid FROM nwx.tafstn";
            CoreDao dao = new CoreDao(DaoConfig.forDatabase("ncep"));
            QueryResult result = (QueryResult) dao.executeMappedSQLQuery(queryStr,
                    null);
            tafStnIdSet = new HashSet<>();
            for(QueryResultRow row : result.getRows()){
                Object[] rowObj = row.getColumnValues();
                tafStnIdSet.add((String)rowObj[0]);
            }
            queryStr ="Select stnid FROM nwx.taf";
            result=null;
            result = (QueryResult) dao.executeMappedSQLQuery(queryStr,
                    null);
            for(QueryResultRow row : result.getRows()){
                Object[] rowObj = row.getColumnValues();
                tafStnIdSet.add((String)rowObj[0]);
            }
        }
        return tafStnIdSet;
    }

    /**
     * Set the raw message data and invoke the internal message separation
     * process.
     * 
     * @param rawMessage
     *            The raw weather text message.
     */

    public void setRecordData(byte[] rawMessage) {
        currentReport = -1;
        // Now check for some binary data types, Stop decoding, if it is binary
        String sRawMessage = new String(rawMessage);
        // some reports contains null char which will cause DB persistence
        // error. Replace all null with
        // Space.
        sRawMessage = sRawMessage.replace((char) 0x0, (char) 0x20);
        int pos = sRawMessage.indexOf("BUFR");
        boolean notStored = false;
        notStored = notStored || ((pos >= 0) && (pos < 20));
        pos = sRawMessage.indexOf("GRIB");
        notStored = notStored || ((pos >= 0) && (pos < 20));
        pos = sRawMessage.indexOf("JPG");
        notStored = notStored || ((pos >= 0) && (pos < 20));
        pos = sRawMessage.indexOf("PNG");
        notStored = notStored || ((pos >= 0) && (pos < 20));
        pos = sRawMessage.indexOf("GIF87");
        notStored = notStored || ((pos >= 0) && (pos < 20));

        int recordId = 1; /* to record number of records with same AWIPS header */
        reports = new ArrayList<NctextRecord>();
        int endPos;
        int rsPos, nextRsPos, curPos;
        if (!notStored) {
            String fileType = nctextInputFileTypeDao
                    .getFiletypeType(ingestFileExt);
            String ctlC = new String();
            char[] data = { IDecoderConstants.ASCII_ETX };
            ctlC = String.copyValueOf(data);
            String[] strRcdArray = sRawMessage.split(ctlC);
            logger.info("number of rcds " + strRcdArray.length);
            for (String strRcd : strRcdArray) {

                if (parseRcdHeader(strRcd)) {
                    endPos = strRcd.length();
                    if (fileType.equals("M")) {
                        // M type data has several records in one "section".
                        // They are separated by Record Separator "^^".
                        rsPos = strRcd.indexOf(ASCII_RS); // find first RS
                        if ((rsPos >= 0) && (rsPos < endPos)) {
                            int stnidEnd;

                            stnidEnd = strRcd.substring(rsPos + 1).indexOf(
                                    ASCII_SP) + 1; 

                            String stnId = strRcd.substring(rsPos + 1, rsPos
                                    + stnidEnd);
                            curPos = rsPos + 1;
                            nextRsPos = strRcd.indexOf(ASCII_RS, curPos); // 2nd
                                                                          // RS
                            while ((nextRsPos >= 0) && (nextRsPos < endPos)) {
                                NctextRecord nctextrecord = new NctextRecord();
                                setNctextRecord(nctextrecord);
                                nctextrecord.setRawRecord(strRcd.substring(
                                        curPos, nextRsPos - 1));
                                nctextrecord.setRecordId(recordId++);
                                nctextrecord.setIssueSite(stnId); // replace
                                                                  // cccc with
                                                                  // stndId
                                                                  // found for
                                                                  // this record
                                if (stnId.length() <= 8){ // to make sure
                                                         // we do not get
                                                         // unwanted/bad record
                                                         // with longer than 8
                                                         // chars stnid
                                    reports.add(nctextrecord);
                                }
                                stnidEnd = strRcd.substring(nextRsPos + 1)
                                        .indexOf(ASCII_SP) + 1; 
                                stnId = strRcd.substring(nextRsPos + 1,
                                        nextRsPos + stnidEnd);
                                curPos = nextRsPos + 1;

                                nextRsPos = strRcd.indexOf(ASCII_RS,
                                        nextRsPos + 1);// new next RS
                            }
                            // Handle the last record which is NOT saved in the
                            // while loop
                            NctextRecord nctextrecord = new NctextRecord();
                            setNctextRecord(nctextrecord);
                            nctextrecord.setRawRecord(strRcd.substring(curPos,
                                    endPos - 1));
                            nctextrecord.setRecordId(recordId++);
                            nctextrecord.setIssueSite(stnId); // replace cccc
                                                              // with stndId
                                                              // found for this
                                                              // record
                            if (stnId.length() <= 8){
                                reports.add(nctextrecord);
                            }
                        } 
                    } else if (fileType.equals("R")) {
                        Pattern p = Pattern.compile(R_TYPE_SEPARATOR);
                        Matcher m = p.matcher(strRcd);
                        String stnIdFound = "NA", subStr;
                        String[] stnGp = null;
                        boolean saveit = false;
                        if (m.find()) {
                            stnIdFound = m.group();
                            stnIdFound = stnIdFound.substring(0,
                                    stnIdFound.length() - 2);
                            stnGp = getGdStnGp(stnIdFound);
                            if (stnGp != null){
                                saveit = true;
                            }
                        } else { // This record may have different format, Its
                                 // Stn ID is one line before "00///"
                            p = Pattern.compile(R_TYPE_SEPARATOR2);
                            m = p.matcher(strRcd);
                            if (m.find()) {
                                subStr = strRcd.substring(m.start());
                                // find the first "00///" and move str to here
                                // from subStr, find a first stn id of this gp
                                // stns
                                // this stn is actually not the first stn in the
                                // record. but it is ok, we only need one stn id
                                // in the gp.
                                p = Pattern.compile(R_TYPE_SEPARATOR3);
                                m = p.matcher(subStr);
                                if (m.find()) {
                                    stnIdFound = m.group();
                                    stnGp = getGdStnGp(stnIdFound);
                                    if (stnGp != null){
                                        saveit = true;
                                    }
                                } else {
                                    logger.info("Could not find stn id in RGD file record !!!");
                                }
                            } else {
                                logger.info("Could not find stn id in RGD file record!");
                            }
                        }
                        if (saveit == true && stnGp != null) {
                            // For consistent with all other text record, we
                            // have to save smae record for each stn in the gp,
                            // so appication can query record based on stnId
                            for (String stn : stnGp) {
                                NctextRecord nctextrecord = new NctextRecord();
                                setNctextRecord(nctextrecord);
                                nctextrecord.setRawRecord(strRcd.substring(
                                        recordStart, endPos - 1));
                                nctextrecord.setRecordId(recordId++);
                                nctextrecord.setIssueSite(stn);
                                // replace cccc with group stndId found for this
                                // record
                                reports.add(nctextrecord);
                            }
                        } else{
                            logger.info("stn id " + stnIdFound
                                    + " But gp Stn is not found");
                        }
                    } else if (fileType.equals("O")) {
                        logger.info("Observer data is not supported now!!!");
                    } else {
                        // other data types - I.e. B, W, Z, F types
                        NctextRecord nctextrecord = new NctextRecord();
                        setNctextRecord(nctextrecord);
                        nctextrecord.setRawRecord(strRcd.substring(recordStart,
                                endPos - 1));
                        nctextrecord.setRecordId(recordId++);
                        if (fileType.equals("F")) {
                            // use AWIPS id as issue set for this type of file,
                            // e.g. FFG file
                            nctextrecord.setIssueSite(awipsId);
                        }
                        else if (fileType.equals("RFTS")) {
                            Pattern p = Pattern.compile(STATION_ID);
                            String messageData = strRcd.substring(messageDataStart,endPos - 1);
                            Matcher m = p.matcher(messageData);
                            Set<String> stnSet = getTafStnSet();
                            while (m.find()) {
                                String stnId = messageData.substring(m.start(),m.end());
                                if(stnSet.contains(stnId)){
                                    NctextTafStn tafStn = new NctextTafStn();
                                    tafStn.setStnId(stnId);
                                    nctextrecord.addTafStnElement(tafStn);
                                }
                            }
                        } 
                        reports.add(nctextrecord);
                    }
                    resetVaraable();

                } else {
                    logger.info("setRecordData : find wmoHeader "
                            + (recordId - 1) + ": " + WMOId + " " + cccc + " "
                            + YYGGgg + " " + awipsId
                            + " BUT, some issues found! ");
                    switch (parseErr) {
                    case NO_AWIPS_HDR:
                        logger.info("setRecordData : no header found. Stop here! ");
                        break;
                    case NO_AWIPS_HDR_IN_BULLETIN:
                        logger.info("setRecordData : no header found in a bulletin. ");
                        // Skip this part of data. adjust buffer to next
                        // possible bulletin
                        break;
                    case NO_CTLA_IN_BULLETIN:
                        logger.info("setRecordData : no ctl-A found in a bulletin. ");
                        // Skip this part of data. adjust buffer to next
                        // possible bulletin
                        break;
                    case NO_CTLA_TO_END:
                        logger.info("setRecordData : no ctl-A found from this point down. Stop here! ");
                        // Skip this part of data. adjust buffer to next
                        // possible bulletin
                        break;
                    case NO_CTLC_TO_END:
                        logger.info("setRecordData : no ctl-C found from this point down. Stop here! ");
                        // Skip this part of data. adjust buffer to next
                        // possible bulletin
                        break;
                    case NO_ERR:
                        logger.info("setRecordData : end of file. Stop here! ");
                        break;
                    default:
                        logger.info("setRecordData : general error! Stop here! ");
                        break;
                    }
                }
            }
        }

        if ((reports != null) && (reports.size() > 0)) {
            currentReport = 0;
        } else {
            logger.info(traceId
                    + " - setRecordData():No reports found in data.");
        }
    }

    private void resetVaraable() {
        BBBIndicator = null;
        WMOId = null;
        awipsId = null;
        cccc = null;
        YYGGgg = null;
    }

    private void setNctextRecord(NctextRecord nctextrecord) {
        nctextrecord.setAwipsId(awipsId);
        nctextrecord.setBbbInd(BBBIndicator);
        nctextrecord.setWmoId(WMOId);
        nctextrecord.setIssueSite(cccc);
        Calendar cal = null;
        try {
            cal = Util.findCurrentTime(YYGGgg);

        } catch (DataFormatException e) {
            logger.error("DataFormatException happned");
        }

        nctextrecord.setIssueTime(cal);
        nctextrecord.setDataTime(new DataTime(cal));
        // set data productType,
        nctextrecord.setProductType(ingestFileExt);
    }

    /**
     * @return the traceId
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * @param traceId
     *            the traceId to set
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public void setData(byte[] data, Headers headers) {
        // do nothing
    }
}
