package gov.noaa.nws.ncep.edex.plugin.aww.decoder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.exception.DecoderException;
import com.raytheon.edex.plugin.AbstractDecoder;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.edex.decodertools.core.IDecoderConstants;

import gov.noaa.nws.ncep.common.dataplugin.aww.AwwLatlons;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwRecord;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwUgc;
import gov.noaa.nws.ncep.common.dataplugin.aww.AwwVtec;
import gov.noaa.nws.ncep.edex.plugin.aww.exception.AwwDecoderException;
import gov.noaa.nws.ncep.edex.plugin.aww.util.AwwLatLonUtil;
import gov.noaa.nws.ncep.edex.plugin.aww.util.AwwParser;
import gov.noaa.nws.ncep.edex.tools.decoder.MndTime;
import gov.noaa.nws.ncep.edex.util.UtilN;

/**
 *
 * AwwDecoder
 *
 * This java class decodes advisory, watch, and warning raw data including 1.
 * flash flood, tornado and severe thunderstorm warning reports (warning -
 * dcwarn) 2. tornado and severe thunderstorm watch box reports and watch status
 * reports (watch – dcwtch) 3. watch county notification reports (wcn – dcwcn)
 * 4. winter storm reports (wstm – Dcwstm) 5. watch outline update reports (wou
 * – Dcwou) 6. flash flood watch reports (ffa – Dcffa) 7. severe local storm
 * reports (tornado and severe thunderstorm watch reports) (svrl – dcsvrl).
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#         Engineer    Description
 * ------------ ----------      ----------- --------------------------
 * 03/2009      38              L. Lin      Initial coding
 * 04/2009      38              L. Lin      Convert to TO10
 * 07/2009      38              L. Lin      Migration to TO11
 * 09/2009      38              L. Lin      Will not store bullmessage
 *                                          if length over the database size
 * 11/2009      38              L. Lin      Correctly get UGC information.
 * 11/2009      38              L. Lin      Migration to TO11 D6.
 * 05/2010      38              L. Lin      Migration to TO11DR11.
 * Jan 26, 2011 N/A             M. Gao      Refactor:
 *                                          1. if AwwParser.processWMO failed, simply
 *                                             drop the record by throwing an exception
 *                                          2. comment out the end check "if(record == null")
 *                                             because it is a dead code.    [DS- removed 8/14]
 * Aug 08, 2013 1028            G. Hull     rm underscores from reportType and set mndTime in URI
 * Aug 30, 2013 2298            rjpeter     Make getPluginName abstract
 * Mar 21, 2103 1112            S. Russell  *.WCN files, get the watch number
 * Aug 13, 2014 (none?)         D. Sushon   refactor to be sure all bulletins get decoded, removed some dead code, marked possibly dead code as such
 * Nov 7, 2014 5125             J. Huber    Cleaned up unneeded logic and deprecated method calls.
 * Aug 17, 2015 R8881           J. Huber    Fixed bug which prevented WCN ugc/vtec/fips information from being processed.
 * Sep 23, 2021 8608            mapeters    Handle PDO.traceId changes
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * @author L. Lin
 */
public class AwwDecoder extends AbstractDecoder {

    /**
     * Constructor
     *
     * @throws DecoderException
     */
    public AwwDecoder() throws DecoderException {
    }

    public PluginDataObject[] decode(byte[] data, Headers headers)
            throws DecoderException {

        List<PluginDataObject> outPdo = new ArrayList<>();

        String traceId = "";
        if (headers != null) {
            traceId = (String) headers.get("traceId");
        }
        // Regular expression for the county table
        final String UGC_EXP = "([A-Z]{3}[0-9]{3}[\\-\\>].*[0-9]{6}\\-)[\\r\\n]+";

        // Pattern used for extracting the UGC line
        Pattern ugcPattern = Pattern.compile(UGC_EXP, Pattern.DOTALL);

        // String "&" ascii code is x26 in hex
        String segmentDelim = "\\x24\\x24";
        String etx = IDecoderConstants.ETX;

        ArrayList<String> watchesList = new ArrayList<>();

        byte[] messageData = null;
        String theBulletin = null;

        // Check if there are more bulletins
        AwwSeparator sep = AwwSeparator.separate(data, headers);
        messageData = sep.next();
        String theMessage = new String(messageData);

        try (Scanner cc = new Scanner(theMessage).useDelimiter(etx)) {
            if (cc.hasNext()) {
                theBulletin = cc.next();
            } else {
                theBulletin = theMessage;
            }

            do {

                // Set MND (Mass News Disseminator) time string and convert it
                // into Calendar object
                MndTime mt = new MndTime(theBulletin.getBytes());
                Calendar mndTime = mt.getMndTime();

                // Decode and set WMO line
                AwwRecord record = AwwParser.processWMO(theBulletin, mndTime);
                if (record == null) {
                    throw new AwwDecoderException(
                            "Error on decoding Aww Record");
                } else {
                    // Set MND remark before the URI is constructed
                    if ((mt.getMndTimeString() == null)
                            || mt.getMndTimeString().trim().isEmpty()) {
                        record.setMndTime("unknown");
                    } else {
                        record.setMndTime(mt.getMndTimeString());
                    }
                    // Ticket 456
                    boolean isWtchFlag = AwwDecoder.isWtch(record);

                    // Get report type
                    String reportType = AwwParser.getReportType(theBulletin);

                    ArrayList<String> segmentList = new ArrayList<>();
                    segmentList.clear();

                    boolean isWCN = false;
                    String wcnLbl = AwwRecord.AwwReportType.WATCH_COUNTY_NOTIFICATION
                            .name();
                    wcnLbl = wcnLbl.replace("_", " ");
                    if (reportType.equals(wcnLbl)) {
                        isWCN = true;
                    }

                    // Break the bulletin message into segments by a "$$"
                    try (Scanner sc = new Scanner(theBulletin)
                            .useDelimiter(segmentDelim)) {
                        while (sc.hasNext()) {
                            String segment = sc.next();
                            Matcher ugcMatcher = ugcPattern.matcher(segment);
                            boolean ugcMatch = ugcMatcher.find();
                            // discard if the segment did not have an UGC line.
                            if (ugcMatch || isWtchFlag || isWCN) {
                                segmentList.add(segment);
                            }
                        }
                    }

                    try {
                        // process each segment in a order of UGC, VTEC, H-VTEC,
                        // FIPS, LATLON...
                        for (String segment : segmentList) {
                            Matcher ugcMatcher = ugcPattern.matcher(segment);
                            boolean ugcMatch = ugcMatcher.find();
                            AwwUgc ugc = null;

                            if (isWCN) {
                                String watchNumber = AwwParser
                                        .retrieveWatchNumberFromWCN(segment);
                                if (watchNumber != null) {
                                    record.setWatchNumber(watchNumber);
                                    if (ugcMatch) {
                                        ugc = AwwParser.processUgc(
                                                ugcMatcher.group(), segment,
                                                mndTime, watchesList);
                                    }
                                }
                            }
                            // local forecast
                            else if (isWtchFlag) {
                                ugc = AwwParser.processUgcForWtch(
                                        AwwParser.WTCH_BOX_UGC_LINE, segment,
                                        mndTime, record.getIssueOffice(),
                                        watchesList);

                                String watchNumber = AwwParser
                                        .processUgcToRetrieveWatchNumberForThunderstormOrTornado(
                                                segment);
                                record.setWatchNumber(watchNumber);

                            } else if (ugcMatch) {
                                ugc = AwwParser.processUgc(ugcMatcher.group(),
                                        segment, mndTime, watchesList);
                            }

                            if (ugc != null) {
                                record.addAwwUGC(ugc);

                                if (AwwParser.isSegmentTextValid(segment)) {
                                    /*
                                     * parse and then set the Watch Number for
                                     * Status Report
                                     */
                                    String watchNumber = AwwParser
                                            .processUgcToRetrieveWatchNumberForStatusReport(
                                                    segment);
                                    record.setWatchNumber(watchNumber);

                                    AwwVtec awwVtec = AwwParser
                                            .processVtectForSevereWeatherStatus(
                                                    theBulletin,
                                                    record.getIssueTime(),
                                                    record.getIssueOffice());
                                    Set<AwwVtec> awwVtecSet = new HashSet<>();
                                    awwVtecSet.add(awwVtec);
                                    ugc.setAwwVtecLine(awwVtecSet);
                                    /*
                                     * now calculate status latlon info and then
                                     * add to ugc
                                     */
                                    List<AwwLatlons> pointAwwLatLonsList = AwwLatLonUtil
                                            .getAwwLatLonsListBySereveWeatherStatusPointLine(
                                                    awwVtec.getVtecLine());
                                    for (AwwLatlons eachAwwLatlons : pointAwwLatLonsList) {
                                        ugc.addAwwLatLon(eachAwwLatlons);
                                    }
                                }

                            }
                        }
                        record.setReportType(reportType.trim());
                        record.setSourceTraceId(traceId);

                    } catch (Exception e) {
                        logger.error("Error processing decoded segment", e);
                        record = null;
                    }

                    // Decode and set attention line
                    record.setAttentionWFO(AwwParser.processATTN(theBulletin));

                    // Replace special characters to a blank so that it may be
                    // readable.
                    if (theBulletin.length() < 40000) {
                        record.setBullMessage(
                                UtilN.removeLeadingWhiteSpaces(theBulletin
                                        .replace('\r', ' ').replace('\003', ' ')
                                        .replace('\000', ' ')
                                        .replace('\001', ' ')
                                        .replace('\036', ' ')));
                    }

                    outPdo.add(record);
                    if (cc.hasNext()) {
                        theBulletin = cc.next();
                    }
                }
            } while (cc.hasNext());
        }

        return outPdo.toArray(new PluginDataObject[] {});
    }

    /**
     * Ticket 456: for handling NO UGC lines in *.wtch2 files
     *
     * @param ar
     * @return boolean flag whether AwwRecord is WTCH
     */
    public static boolean isWtch(AwwRecord ar) {
        if (ar == null) {
            return false;
        }
        return ("WWUS30".equalsIgnoreCase(ar.getWmoHeader()));
    }

}
