package gov.noaa.nws.ncep.edex.plugin.ncscat.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.edex.exception.DecoderException;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatRecord;
import gov.noaa.nws.ncep.edex.plugin.ncscat.util.NcscatProcessing;

/**
 * <pre>
 * Ncscat Decoder
 *
 * This Java class decodes ocean winds scatterometer/radiometer (NESDIS File)raw data.
 *
 * SOFTWARE HISTORY 
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ---------    --------------------------------------------
 * Nov 2009                Uma Josyula  Initial creation
 * Jan 2011                B. Hebbard   Handle ambiguity variants
 * Jun 2012                B. Hebbard   Handle OSCAT / OSCAT_HI
 * Oct 2014                B. Hebbard   Get reportType from NcscatMode, instead of
 *                                      if-then-else'ing over hardcoded integer lengths
 * 01/08/2016    R10434    R Reynolds   Added code to distinguish incoming ascatb/ascatd Metop ID's
 * 01/31/2017    R18596    J Beck       Extracted code for parsing filenames into method
 *                                      Extract File reading code into method
 *                                      Make many fields final and static
 *                                      Add additional sub-strings that can identify ascata/ascatb/ascatc data
 * 
 *
 * </pre>
 *
 */

public class NcscatDecoder {

    private String plugin;

    private String ambigNumber;

    private Matcher matcher;

    private String inputFileName;

    protected Log logger = LogFactory.getLog(getClass());

    private static final List<String> ASCAT_A_SUBSTRINGS = new ArrayList<>();

    private static final List<String> ASCAT_B_SUBSTRINGS = new ArrayList<>();

    private static final List<String> ASCAT_C_SUBSTRINGS = new ArrayList<>();

    private static final Map<String, List<String>> ASCAT_MAP = new HashMap<>();

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern
            .compile("\\.([a-z]{5,6})");

    private static final Pattern AMBIGUITY_SUBSTRING_PATTERN = Pattern
            .compile("amb(ig(uity_){0,1}){0,1}([1-9])");

    /**
     * Constructor
     *
     * Populates the Lists of valid substrings we will match against Populates a
     * map of <ascat type, valid ascat substrings>
     *
     * @param name
     * @throws DecoderException
     */
    public NcscatDecoder(String name) throws DecoderException {

        for (String substring : new String[] { "ascata", "ascat-a", "ascat_a",
                "ascatd", "ascat-d", "ascat_d", "metopa", "metop-a",
                "metop_a" }) {

            ASCAT_A_SUBSTRINGS.add(substring);
        }

        for (String substring : new String[] { "ascatb", "ascat-b", "ascat_b",
                "metopb", "metop-b", "metop_b" }) {

            ASCAT_B_SUBSTRINGS.add(substring);
        }

        for (String substring : new String[] { "ascatc", "ascat-c", "ascat_c",
                "metopc", "metop-c", "metop_c" }) {

            ASCAT_C_SUBSTRINGS.add(substring);
        }

        ASCAT_MAP.put("ascat-a", ASCAT_A_SUBSTRINGS);
        ASCAT_MAP.put("ascat-b", ASCAT_B_SUBSTRINGS);
        ASCAT_MAP.put("ascat-c", ASCAT_C_SUBSTRINGS);
    }

    /**
     * Decode an Ncscat Data File
     *
     * @param inputFile
     *            The file to be decoded
     *
     * @return PluginDataObject[]
     *
     * @throws DecoderException
     * @throws PluginException
     *
     */
    public synchronized PluginDataObject[] decodeNcscatInputFile(File inputFile)
            throws DecoderException, PluginException {

        /* get the input file as an array of bytes */
        byte[] fileData = readFile(inputFile);

        if (fileData != null) {

            matcher = FILE_EXTENSION_PATTERN.matcher(inputFileName);
            if (matcher.find()) {
                plugin = matcher.group(1);
            }
            /*
             * INFO: Review this temporary fix by BH to see if a broader
             * refactor might be better.
             * 
             * Problem: Without the added 'else' below, non-local variable
             * "plugin" will retain its previous value if the pattern matcher
             * fails above; this will happen if the file suffix/extension (after
             * the ".") is shorter than 5 characters.
             * 
             * Scenario: A file with suffix ".ascatx" is ingested, then later a
             * ".qsct" file. Variable "plugin" will be set to "ascatx" by the
             * first, but will retain this same value for the second; logic in
             * NcscatProcessing.doSeparate() will as a result interpret the
             * big-endian ".qsct" data as little endian format. Alternate
             * (better): Could make "plugin" local to this method
             * 
             * See also endianess fixes in NcscatProcessing
             */
            else {
                plugin = "";
            }

            /*
             * Handle ambiguity variants:
             * 
             * In addition to files containing the primary (greatest likelihood)
             * wind vector solutions, for some data types we also receive
             * (typically 2 or 4) 'ambiguity' variants for the same point
             * locations, but with (lesser likelihood) solutions.
             * 
             * Since these cannot be distinguished from the primary files based
             * on data format/content alone, we look for a (prearranged)
             * telltale pattern somewhere in the file name.
             * 
             * If found, we remember the indicated ambiguity number, for later
             * tagging of the database record.
             */

            ambigNumber = parseAmbiguity(inputFileName);
            return decode(fileData);
        }

        logger.error("decodeNcscatInputFile(): fileData is null ");
        return (new PluginDataObject[0]);

    }

    /**
     * Decode Ncscat byte array data
     * 
     * @param data
     * @return PluginDataObject[] NcscatRecord
     * @throws DecoderException
     */
    public PluginDataObject[] decode(byte[] data) throws DecoderException {

        NcscatRecord record = null;
        byte[] messageData = null;
        int recLength = 688;
        NcscatProcessing sProcess = new NcscatProcessing();
        sProcess.setInputFileName(plugin);
        messageData = sProcess.separate(data);
        recLength = sProcess.getRecordLength();
        boolean match = false;
        String ascatType = null;
        List<String> substrings = null;

        if (messageData != null) {

            record = new NcscatRecord();
            record.setConvertedMessage(messageData);
            record.setStartTime(sProcess.getStartTime());
            record.setEndTime(sProcess.getEndTime());
            record.setRecordLength(recLength);
            record.setDataTime(new DataTime(sProcess.getStartTime()));
            record.setReportType(sProcess.getNcscatMode().getReportType());

            /*
             * If this is a numbered 'ambiguity' variant, then add the numbered
             * suffix to the db reporttype field.
             */
            if (!ambigNumber.isEmpty()) {
                record.setReportType(
                        record.getReportType() + "-ambig" + ambigNumber);
            }

            /* Here we make a call to parse the input filename for substrings */
            for (Map.Entry<String, List<String>> entry : ASCAT_MAP.entrySet()) {

                ascatType = entry.getKey();
                substrings = entry.getValue();

                /* Here's the call to our parser */
                if (parseFilename(inputFileName.toLowerCase(), substrings)) {

                    record.setReportType(record.getReportType()
                            .replaceAll("ascat", ascatType));
                    match = true;
                    break;
                }
            }

            if (!match) {
                /* We have not found a substring match */
                logger.info(
                        "NcscatDecoder did not find an identifying substring in "
                                + inputFileName);
            }

            if (record != null) {
                record.getDataURI();
            }
        }

        if (record == null) {

            return new PluginDataObject[0];

        } else {

            return new PluginDataObject[] { record };
        }

    }

    /**
     * Here, the decoder will parse data file names, to determine if substrings
     * are found that will identify the file to be a particular data type.
     * 
     * @param filename
     *            the filename to parse
     * 
     * @param ascatSubstrings
     *            the List of valid ascat substrings
     * 
     * @return true if a substring is found, false otherwise
     */
    public boolean parseFilename(String filename,
            List<String> ascatSubstrings) {

        for (String ascatSubstring : ascatSubstrings) {
            if (filename.contains(ascatSubstring)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identify an ambiguity number in the data filename
     * 
     * @param filename
     *            The filename string to parse
     * 
     * @return A single digit if this filename is identified as ambiguity data,
     *         otherwise return an empty string
     * 
     */
    public String parseAmbiguity(String filename) {

        String file = filename.toLowerCase();

        matcher = AMBIGUITY_SUBSTRING_PATTERN.matcher(file);

        if (matcher.find()) {
            return matcher.group(3);
        }
        return "";
    }

    /**
     * Boilerplate code for reading bytes from a file input stream
     * 
     * @param inputFile
     *            the input File
     * @return a byte array representing the input file
     */
    private byte[] readFile(File inputFile) {

        byte[] fileByteData = null;
        InputStream inputStream = null;

        try {
            /* Read the input file */
            inputStream = new FileInputStream(inputFile);

            fileByteData = new byte[(int) inputFile.length()];
            int bytesRead = inputStream.read(fileByteData);

            if (bytesRead != fileByteData.length) {
                fileByteData = null;
            }

            /* set the input file name */
            inputFileName = inputFile.getName();

        } catch (IOException ioe) {
            logger.error("Error reading input file " + inputFile.getName(),
                    ioe);
            fileByteData = null;

        } finally {

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    logger.error("Could not close input file "
                            + inputFile.getName());
                }
            }
        }

        return fileByteData;
    }
}
