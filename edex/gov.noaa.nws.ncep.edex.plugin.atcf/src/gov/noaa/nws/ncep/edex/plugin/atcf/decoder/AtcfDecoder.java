package gov.noaa.nws.ncep.edex.plugin.atcf.decoder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.exception.DecoderException;
import com.raytheon.edex.plugin.AbstractDecoder;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ncep.common.dataplugin.atcf.AtcfRecord;
import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;
import gov.noaa.nws.ncep.edex.plugin.atcf.util.AtcfParser;

/**
 *
 * AtcfDecoder
 *
 * Decoder implementation for ATCF Plug-In
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#  Engineer     Description
 * ------------ -------- -----------  --------------------------
 * 06/23/2010   208      F. J. Yen   Initial creation
 * 08/30/2013   2298     rjpeter      Make getPluginName abstract
 * 06/  /2014            T. Lee      Batch processing
 * 07/25/2015   R19869   B. Hebbard  Change ATCF_DATA regex to allow null value
 *                                   (common in B-deck) in column 4 TECHNUM/MIN
 * 08/10/2016   R19869   B. Hebbard  Per code review, share out regex ATCF_DATA so
 *                                   AtcfSeparator needn't have a duplicate expression;
 *                                   clean comments.
 * Sep 23, 2021 8608     mapeters    Handle PDO.traceId changes
 * </pre>
 *
 * @author Fee Jing Yen, SIB
 */
public class AtcfDecoder extends AbstractDecoder {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AtcfDecoder.class);

    public static final String ATCF_DATA = "(WP|IO|SH|CP|EP|AL), +\\d{1,2}, +\\d{10}, +\\d{0,2}, +\\w{1,4}, +(-|\\d)\\d{0,2}, +\\d{1,3}(N|S), +\\d{1,4}(E|W), +.*?\\x0a";

    protected Matcher regexMatcher;

    protected Pattern regexPattern;

    public Calendar observTime = null;

    /**
     * Constructor
     *
     * @throws DecoderException
     */
    public AtcfDecoder() throws DecoderException {
    }

    public PluginDataObject[] decode(byte[] data, Headers headers)
            throws DecoderException {
        String traceId = "";
        byte[] messageData = null;
        // ATCF_DATA is REGEX for a ATCF record
        final Pattern atcfPattern = Pattern.compile(ATCF_DATA);
        if (headers != null) {
            traceId = (String) headers.get("traceId");
        }
        /*
         * Check if there are more records
         */
        AtcfRecord record = null;
        List<AtcfRecord> records = new ArrayList<>();
        AtcfSeparator sep = AtcfSeparator.separate(data, headers);

        while (sep.hasNext()) {
            messageData = sep.next();
            String theMessage = new String(messageData);

            try {
                Matcher atcfMatcher = atcfPattern.matcher(theMessage);

                if (atcfMatcher.find()) {

                } else {
                    statusHandler.warn(
                            "ATCF:  Ignored invalid record:  " + theMessage);
                }
            } catch (Exception e) {
                statusHandler.error(
                        "ATCF exception:  Unable to decode:  " + theMessage, e);
            }

            /*
             * Process the ATCF fields
             */
            record = AtcfParser.processFields(theMessage);

            /*
             * Check the ATCF record object
             */
            if (record != null) {
                try {
                    record.setSourceTraceId(traceId);
                    record.setReportType("ATCF");
                    record.constructDataURI();
                    if ((record.getClat() != IDecoderConstantsN.FLOAT_MISSING)
                            && (record
                                    .getClon() != IDecoderConstantsN.FLOAT_MISSING)) {
                        records.add(record);
                    }

                } catch (PluginException e) {
                    throw new DecoderException(
                            "ATCF WARNING:  Unable to construct dataURI--exception:  ",
                            e);
                }
            }
        }
        /*
         * Return ATCF record object if not null
         */
        if (record == null) {
            return new PluginDataObject[0];
        } else {
            return records.toArray(new PluginDataObject[records.size()]);
        }
    }
}
