package gov.noaa.nws.ncep.edex.plugin.ntrans.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.edex.exception.DecoderException;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.common.dataplugin.ntrans.NtransRecord;

/**
 * NTRANS Decoder
 * 
 * This class decodes legacy NTRANS Metafiles.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Author      Description
 * ------------ -------- ----------- -------------------------------------
 * 03/2013               B. Hebbard  Initial creation
 * 04/2013               B. Hebbard  IOC version (for OB13.4.1)
 * 10/2013               B. Hebbard  Modify model name inference from metafile name
 * 08/30/2013   2298     rjpeter     Make getPluginName abstract
 * 6/2014                T. Lee      Added HYSPLIT and fixed "other" modelName
 * 08/2014               B. Hebbard  Revise createDataTime() to correct end-of-month boundary bug
 * 08/2014               B. Hebbard  Enhance to use (cycle) time info from metafile name, if available
 * 09/2014               B. Hebbard  Normalize (shorten) metafile name to remove directory artifacts added during dataflow, so user will see that they're used to and will fit selection dialog column
 * 11/20/2015   R12988   J. Lopez    Fixed "Unable to create property map" error
 * 01/12/2016   R13542   S. Russell  Updated normalizeMetafileName() to handle
 *                                   file names that might have a period and 
 *                                   duplicate number on the end
 * 05/22/2017   R32628   JHuber/MKean Added initialization hour logic to new
 *                                   NtransDecoderCalculator class, and updated
 *                                   the date algorithms in createDataTime() 
 *                                   to prevent inconsistent reftime calculations
 * </pre>
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
public class NtransDecoder {

    private Log logger = LogFactory.getLog(getClass());

    private final static int NTRANS_FILE_TITLE_SIZE = 32; // bytes

    private final static int NTRANS_FRAME_LABEL_SIZE = 64;

    private final static int NTRANS_RESERVED_SPACE_SIZE = 38;

    private final static String REGEX_ALL_DIGITS = "^\\d+$";

    private final Pattern PATTERN_ALL_DIGITS =
            Pattern.compile(REGEX_ALL_DIGITS);

    private final static String REGEX_ALPHA_NUMERIC = "^[a-zA-Z0-9]*$";

    private final Pattern pattern_all_alphaNumberic =
            Pattern.compile(REGEX_ALPHA_NUMERIC);

    private final String FILENAME_DELIM = ".";

    private final String FILENAME_DELIM_SPLITTER = "\\.";

    private final String FILENAME_DELIM_2 = "_";

    private final static String REGEX_SHORT_FORM = "((\\d\\d){1,4})/(\\d\\d)";

    private final static String REGEX_LONG_FORM =
            "((\\d\\d){1,4})/(\\d\\d)(F|V)(\\d{1,3})";

    private final static String REGEX_DATETIME_FORM =
            "((\\d\\d){1,4})/(\\d\\d)(F|V)(\\d{1,4})";

    private final Pattern PATTERN_LONGFORM = Pattern.compile(REGEX_LONG_FORM);

    private final Pattern PATTERN_SHORTFORM = Pattern.compile(REGEX_SHORT_FORM);

    private final Pattern PATTERN_DATETIMEFORM =
            Pattern.compile(REGEX_DATETIME_FORM);

    private Calendar decodeTime = null;

    private String normalizedMetafileName;

    private Integer yearFromFileName = null;

    private Integer monthFromFileName = null;

    private Integer dateFromFileName = null;

    private Integer hourFromFileName = null;

    // The file name to parse
    private String fileName;

    private NtransDecoderCalculator ntransDecoderCalculator = null;

    /**
     * Constructor
     * 
     * @throws DecoderException
     */
    public NtransDecoder() throws DecoderException {

        // default to current date
        decodeTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        ntransDecoderCalculator = new NtransDecoderCalculator();
    }

    private class FrameHeader {
        String frameHeaderTimeString;

        String productNameString;

        long startPos;

        long endPos;
    }

    /**
     * process ntrans data in given metafile (File inputFile), load frames into
     * NtransRecord and return as PluginDataObject[] array
     * 
     * @param File
     *            inputFile : ingest file to be decoded
     * @return PluginDataObject[]
     * @throws DecoderException
     * @throws PluginException
     * 
     */
    public PluginDataObject[] decode(File inputFile)
            throws DecoderException, PluginException {

        InputStream inputStream = null;
        int fileMaxFrame = 0;
        int frameSizeX = 0;
        int frameSizeY = 0;
        List<FrameHeader> frameHeaders = new ArrayList<FrameHeader>();
        List<NtransRecord> records = new ArrayList<NtransRecord>();

        try {

            decodeTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            fileName = inputFile.getName();

            normalizedMetafileName = normalizeMetafileName(fileName);

            getTimeFromMetafileName(normalizedMetafileName);

            inputStream = new FileInputStream(inputFile);

            // Read the entire file

            // TODO: Wish we didn't have to do that, but decode method
            // wants to return all PDOs at once, so it's all got
            // to sit in memory anyway. Propose architecture change (?)
            // say, decode(File,IDataReturners) to allow sending up
            // PDOs as completed, so memory can be released?
            // Anyway, this allows us to use NIO buffer tricks...
            // NOTE: Watch #34602 has been added for this issue

            long inputFileCount = inputFile.length();

            if (inputFileCount > Integer.MAX_VALUE) {
                throw new DecoderException(
                        "Input file too big! " + inputFileCount + " bytes");
            }

            byte[] fileBytes = new byte[(int) inputFileCount];
            int fileBytesReadIn = inputStream.read(fileBytes);

            if (fileBytesReadIn != inputFileCount) {
                throw new DecoderException(
                        "Input file not read correctly/completely! "
                                + fileBytesReadIn + " of " + inputFileCount
                                + " bytes read");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(fileBytes);

            byteBuffer.order(determineEndianess(byteBuffer));

            byteBuffer.rewind();

            // Read NTRANS metafile header

            byte[] fileTitleBytes = new byte[NTRANS_FILE_TITLE_SIZE];
            byteBuffer.get(fileTitleBytes);
            fileMaxFrame =
                    ntransDecoderCalculator.toUnsigned(byteBuffer.getShort());

            // Advance the position in the byteBuffer past the file version
            byteBuffer.getShort();
            // Now advance the position in the byteBuffer past fileMachineType
            byteBuffer.getShort();

            frameSizeX =
                    ntransDecoderCalculator.toUnsigned(byteBuffer.getShort());
            frameSizeY =
                    ntransDecoderCalculator.toUnsigned(byteBuffer.getShort());

            byte[] fileReserved = new byte[NTRANS_RESERVED_SPACE_SIZE];
            byteBuffer.get(fileReserved);

            // Read NTRANS frame headers (follow file header; precede frame
            // contents)

            for (int frame = 0; frame < fileMaxFrame; frame++) {

                byte[] labelTitleBytes = new byte[NTRANS_FRAME_LABEL_SIZE];
                byteBuffer.get(labelTitleBytes);
                StringBuffer sb = new StringBuffer();

                for (int i = 0; (i < NTRANS_FRAME_LABEL_SIZE)
                        && (labelTitleBytes[i] != 0x00); i++) {
                    sb.append((char) labelTitleBytes[i]);
                }
                String labelTitle = new String(sb);
                long startPos =
                        ntransDecoderCalculator.toUnsigned(byteBuffer.getInt());
                long endPos =
                        ntransDecoderCalculator.toUnsigned(byteBuffer.getInt());

                if ((startPos == 0) && (endPos == 0)) {
                    break;
                }

                // frame header processing - split Timestamp and Product Name
                // add start and end positions of associated frame in the file

                FrameHeader frameHeader = new FrameHeader();
                createHeaderTimeAndNameString(labelTitle, frameHeader);
                frameHeader.startPos = startPos;
                frameHeader.endPos = endPos;
                frameHeaders.add(frameHeader);
            }

            // Read NTRANS frames (CGM coded images)

            for (int frame = 0; frame < frameHeaders.size(); frame++) {

                FrameHeader frameHeader1 = frameHeaders.get(frame);
                int startPos = (int) (frameHeader1.startPos - 4);
                int endPos = (int) (frameHeader1.endPos + 0);
                int imageLength = (endPos - startPos) + 0;
                byte[] frameImage = new byte[imageLength];

                byteBuffer.position(startPos);
                byteBuffer.get(frameImage);

                // Create NTRANS record (PDO)

                NtransRecord record = new NtransRecord();

                record.setReportType("NTRANS");
                record.setModelName(
                        inferModel(inputFile.getName()).replaceAll("_", "-"));
                record.setMetafileName(
                        normalizedMetafileName.replaceAll("_", "-"));

                record.setProductName(ntransDecoderCalculator
                        .handleProductNameSpecialCharacters(
                                frameHeader1.productNameString.trim()));

                record.setDataTime(
                        createDataTime(frameHeader1.frameHeaderTimeString));
                record.setValidTimeString(frameHeader1.frameHeaderTimeString);
                record.setImageData(frameImage);
                record.setImageSizeX(frameSizeX);
                record.setImageSizeY(frameSizeY);
                record.setImageByteCount(frameImage.length);
                record.setFrameNumberInFile(frame);
                record.setTotalFramesInFile(frameHeaders.size());

                // Data URI gets set, in the process of calling getDataURI
                record.getDataURI();

                records.add(record);
            }

        } catch (IOException ioe) {
            logger.error("Error reading input file " + inputFile.getName(),
                    ioe);

        } catch (Exception e) {
            logger.error(
                    "Error decoding NTrans input file " + inputFile.getName(),
                    e);
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

        if (records.isEmpty()) {
            return new PluginDataObject[0];

        } else {
            PluginDataObject[] pdos = records.toArray(new PluginDataObject[0]);
            return pdos;
        }

    }

    /**
     * Splits the labelTile into frameHeaderTimeString and productNameString so
     * that they can be properly parsed during frame processing. Values are
     * returned via the updated FrameHeader object.
     * 
     * @param labelTitle
     *            The label title string read from the frame header in the file.
     * @param frameheader
     *            the FrameHeader object that's being filled with data.
     * @throws DecoderException
     *             In the case of an unknown time stamp format, throw exception
     *             up the chain. This will stop the processing of the file and
     *             drop it before any frames can be processed.
     */
    private void createHeaderTimeAndNameString(String labelTitle,
            FrameHeader frameheader) throws DecoderException {

        // patterns for matching time stamps
        // maximum expected is "DD/HHVnnn", which is 9 characters long
        // minimum expected is "DD/HH" which is 5 characters long.
        Matcher longMatch = PATTERN_LONGFORM.matcher(labelTitle);
        Matcher shortMatch = PATTERN_SHORTFORM.matcher(labelTitle);

        if (longMatch.find()) {
            frameheader.frameHeaderTimeString = longMatch.group(0);
            frameheader.productNameString =
                    labelTitle.substring(longMatch.end(0)).trim();

        } else if (shortMatch.find()) {
            frameheader.frameHeaderTimeString = shortMatch.group(0) + "V000";
            frameheader.productNameString =
                    labelTitle.substring(shortMatch.end(0)).trim();
        } else {
            throw new DecoderException(
                    "Input file not read correctly/completely! "
                            + "Frame Header " + labelTitle
                            + " has unknown date format.");
        }
    }

    /**
     * Reduce the size of (the raw) fileName.
     * 
     * 1. If the file name does not have periods: do nothing
     * 
     * 2. If the file name has periods, take the right most token that isn't all
     * numbers.
     * 
     * 3. If the remaining token is all numbers before the first underscore,
     * remove everything up to and including the first underscore
     * 
     * 4. Lastly, if the remaining token starts with a non-alphanumberic
     * character remove it.
     * 
     * Example: Given something like..."gfs_gfs.20140901_gfs_20140901_12_ak.3"
     * Return............... "gfs_20140901_12_ak"
     * 
     * wave_wave.20140902_nww3_20140902_00_akw
     * gfs_gfs.20160114_gfsver_20160114_1
     * 
     * Note: for the two raw filenames above the correct model names are indeed
     * nww3 and gfsver
     * 
     * As of this update there is no authoritative list of Ntrans file name
     * formats, raw or meta file names
     * 
     * @param String
     *            filName, raw file name
     * @return the fileName if there are no periods in it, otherwise the last
     *         token
     */

    public String normalizeMetafileName(String fileNameIn) {

        // If fileName contains a "."
        if (fileNameIn.contains(FILENAME_DELIM)) {

            String lastSplit = null;
            Matcher matcher = null;
            boolean foundAcceptableToken = false;

            try {
                // Tokenize fileName on the "."
                String[] splits = fileNameIn.split(FILENAME_DELIM_SPLITTER);

                // Starting from the last token
                for (int i = splits.length - 1; i >= 0; i--) {
                    lastSplit = splits[i];

                    // Is the token all digits, aka a duplicate number ?
                    matcher = PATTERN_ALL_DIGITS.matcher(lastSplit);

                    // If not
                    if (!matcher.matches()) {
                        foundAcceptableToken = true;
                        break;
                    }
                }

                if (foundAcceptableToken) {

                    // If the token has at least one underscore
                    if (lastSplit.indexOf("_") > 0) {

                        // Split the last token on a "_"
                        String[] split2 =
                                lastSplit.split(this.FILENAME_DELIM_2);
                        String t1 = split2[0];

                        matcher = this.PATTERN_ALL_DIGITS.matcher(t1);

                        // if t1 is all numbers, remove it up to the _
                        if (matcher.matches()) {
                            lastSplit = lastSplit.replaceFirst("^.*?_", "");

                        }
                    }

                    // Is the first char alphanumberic?
                    matcher = this.pattern_all_alphaNumberic
                            .matcher(lastSplit.substring(0, 1));

                    // If not
                    if (!matcher.matches()) {
                        // remove it
                        lastSplit = lastSplit.substring(1, lastSplit.length());
                    }

                } else { // no acceptable token found in fileName
                    throw new DecoderException("Could not find an acceptable "
                            + "meta file name within the raw NTrans file name");
                }

            } catch (Exception e) {
                logger.error("Error normalizing fileName: " + fileNameIn
                        + " into a metafile name ", e);
                lastSplit = fileNameIn;
            }

            return lastSplit;

        } else {
            // fileName does NOT have a "." in it
            return fileNameIn;
        }
    }

    /**
     * Return normalized year for the given in shortYear
     * 
     * Year can be 2 digits. If so, select century to make it the one closest to
     * the current year. (Yeah, I know it's overkill. But that's what we thought
     * *last* century...)
     * 
     * @param shortYear
     * @return
     */
    private int normalizeYear(int shortYear) {

        if (shortYear > 99) {
            return shortYear;

        } else {

            if (decodeTime == null) {
                decodeTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            }

            int currYear = decodeTime.get(Calendar.YEAR);
            int currCentury = currYear / 100;
            int breakpoint = (currYear + 50) % 100;
            int derivedYear = currCentury * 100 + shortYear;

            if (shortYear > breakpoint) {
                derivedYear++;
            }
            return derivedYear;
        }
    }

    /*-
     * Parse the date elements (yearFromFileName, monthFromFileName,
     * dateFromFileName, hourFromFileName) from the given metafile (fileName
     * String).
     * 
     * NTRANS metafile names (almost?) always contain date and (sometimes)
     * hour information. We assume that this refers to the cycle time of the
     * model run which produced the images it contains. Since individual
     * frame headers (contained within the body of the file) currently
     * (2014-08) provide only valid times with date -- and no month or year
     * -- taking these 'hints' from the file name, if available, allows us
     * to handle legacy data properly, even if months or years old.
     *   
     * This method operates on instance variables for both input and output.
     *   
     * Input is fileName, which is assumed to have been set to the full name
     * of the metafile currently being ingested, containing a substring of
     * one of the following forms:
     * @formatter:off
     * YYYYMMDD
     * YYYYMMDDHH
     * YYYYMMDD_HH
     * YYMMDD
     * YYMMDD_HH
     * @formatter:on
     *   
     * Output takes the form of...
     * yearFromFileName, monthFromFileName,
     * dateFromFileName, hourFromFileName
     * These are (boxed) Integer variables; a null value indicates no value
     * is available for that field.
     * 
     * @param String fileName
     */
    private void getTimeFromMetafileName(String fileNameIn) {

        final Pattern pattern = Pattern.compile("((\\d\\d){3,4})_?(\\d\\d)?");
        Matcher matcher = pattern.matcher(fileNameIn);

        String matchString = "";
        String hourString = "";

        String year = "";
        String month = "";
        String date = "";

        while (matcher.find()) {

            if (matcher.group(0).length() >= matchString.length()) {
                matchString = matcher.group(0);

                String dateString = matcher.group(1).replaceFirst("_", "");

                // skip parsing date fields if source dateString is empty
                if (dateString.isEmpty()) {
                    logger.error("No date string found");

                } else {
                    date = dateString.substring(dateString.length() - 2);
                    dateString =
                            dateString.substring(0, dateString.length() - 2);
                    month = dateString.substring(dateString.length() - 2);
                    dateString =
                            dateString.substring(0, dateString.length() - 2);
                    year = dateString.substring(dateString.length() - 2);
                    dateString =
                            dateString.substring(0, dateString.length() - 2);

                    hourString = matcher.group(3);
                }
            }
        }

        if (!matchString.isEmpty()) {

            try {
                yearFromFileName = normalizeYear(Integer.parseInt(year));
                monthFromFileName = Integer.parseInt(month);
                dateFromFileName = Integer.parseInt(date);

                if (hourString != null && !hourString.isEmpty()) {

                    hourFromFileName = Integer.parseInt(hourString);
                }

            } catch (Exception e) {
                logger.error("Error extracting date from the metafile name", e);
            }
        }
    }

    /**
     * 
     * Overloads the createDataTime() method and allows for testing with a
     * simulatedFileName (for setting the metafile), and a simulatedDecodeTime
     * for setting a decodeTime
     * 
     * @param frameHeaderTimeString
     * @param simulatedFileName
     * @param simulatedDecodeTime
     * @return com.raytheon.uf.common.time.DataTime
     */
    DataTime createDataTime(String frameHeaderTimeString,
            String simulatedFileName, Calendar simulatedDecodeTime) {

        decodeTime = simulatedDecodeTime;

        return createDataTime(frameHeaderTimeString, simulatedFileName);
    }

    /**
     * Overloads the createDataTime() method and allows for testing with a
     * simulatedFileName for setting the metafile
     * 
     * @param frameHeaderTimeString
     * @param simulatedFileName
     * @return com.raytheon.uf.common.time.DataTime
     */
    DataTime createDataTime(String frameHeaderTimeString,
            String simulatedFileName) {

        fileName = simulatedFileName;
        normalizedMetafileName = normalizeMetafileName(fileName);
        getTimeFromMetafileName(normalizedMetafileName);

        return createDataTime(frameHeaderTimeString);
    }

    /*-
     * 
     * Create a standard DataTime object, with proper timing determined from
     * given time string (e.g., "27/06V042") (from the frame header), AND fields
     * parsed earlier from the metafile name (and stored in instance variables),
     * if available.
     * 
     * Get components of validframeHeaderTimeString as 'int's
     *
     * @formatter:off
     *    Be able to decode...
     *    8, 6, 4, or 2 digits, followed by...
     *    "/" followed by...
     *    2 digits followed by...
     *    "F" or "V" followed by...
     *    any number of digits? 2-3? 1-6?
     *    NOT with intervening spaces?
     * @formatter:on
     * 
     * @param String frameHeaderTimeString
     * @return com.raytheon.uf.common.time.DataTime
     */
    DataTime createDataTime(String frameHeaderTimeString) {

        Matcher matcher = PATTERN_DATETIMEFORM.matcher(frameHeaderTimeString);

        boolean isVframeType = false;

        Integer centuryFromFrameHeader = null;
        Integer yearFromFrameHeader = null;
        Integer monthFromFrameHeader = null;
        Integer dateFromFrameHeader = null;
        Integer hourFromFrameHeader = null;
        Integer forecastHour = null;

        String dateString;
        String hourString;
        String frameTypeFV;
        String forecastHourString;

        if (matcher.find()) {

            dateString = matcher.group(1);
            hourString = matcher.group(3);
            frameTypeFV = matcher.group(4);
            forecastHourString = matcher.group(5);
        } else {

            decodeTime.set(Calendar.SECOND, 0);
            decodeTime.set(Calendar.MILLISECOND, 0);

            // log error, but do not throw an exception
            logger.error("Error : " + frameHeaderTimeString
                    + " is not a valid frame header time string.  DataTime being set to decode time.");
            return new DataTime(decodeTime, 0);
        }

        try {
            switch (dateString.length()) {
            case 8:
                centuryFromFrameHeader =
                        Integer.parseInt(dateString.substring(0, 2));
                dateString = dateString.substring(2);
                // NO break;

            case 6:
                yearFromFrameHeader =
                        Integer.parseInt(dateString.substring(0, 2));
                dateString = dateString.substring(2);

                if (centuryFromFrameHeader == null) {
                    yearFromFrameHeader = normalizeYear(yearFromFrameHeader);
                } else {
                    yearFromFrameHeader += centuryFromFrameHeader * 100;
                }
                // NO break;

            case 4:
                monthFromFrameHeader =
                        Integer.parseInt(dateString.substring(0, 2));
                dateString = dateString.substring(2);
                // NO break;

            case 2:
                dateFromFrameHeader =
                        Integer.parseInt(dateString.substring(0, 2));
                dateString = dateString.substring(2);
                assert (dateString.isEmpty());
                break;

            default:
                // ERROR
                break;
            }

            hourFromFrameHeader = Integer.parseInt(hourString);
            isVframeType = frameTypeFV.equalsIgnoreCase("V");
            forecastHour = Integer.parseInt(forecastHourString);

        } catch (Exception e) {
            logger.error("DataTime parsing error. Replacing with current GMT.");

            return new DataTime(
                    Calendar.getInstance(TimeZone.getTimeZone("GMT")), 0);
        }

        // Establish upper bound on what the specified time means.
        Calendar upperBoundTime =
                ntransDecoderCalculator.calculateUpperBoundTime(
                        yearFromFrameHeader, monthFromFrameHeader, this);

        // If we're dealing with a V-type string, we're determining the VALID
        // time... Set the initialization hour and add forecast hours.
        if (isVframeType) {

            int initializationHour =
                    ntransDecoderCalculator.calulateInitializationHour(
                            forecastHour, hourFromFrameHeader);

            upperBoundTime.set(Calendar.HOUR_OF_DAY, initializationHour);

            upperBoundTime =
                    ntransDecoderCalculator.setSubHourToZero(upperBoundTime);

            upperBoundTime.add(Calendar.HOUR_OF_DAY, forecastHour);
        }

        // Now calculate the actual valid time, starting with the latest
        // possible...
        Calendar calculatedCycleTime = ntransDecoderCalculator
                .calculateCycleTime(monthFromFrameHeader, dateFromFrameHeader,
                        hourFromFrameHeader, upperBoundTime);

        // Subtract forecast hours to get initial (reference) or cycle time
        if (isVframeType) {
            calculatedCycleTime = ntransDecoderCalculator
                    .setSubHourToZero(calculatedCycleTime);

            calculatedCycleTime.add(Calendar.HOUR_OF_DAY, -forecastHour);
        }

        // Sanity check against file name date and hour (if known)

        int calculatedCycleDate =
                calculatedCycleTime.get(Calendar.DAY_OF_MONTH);
        int calculatedCycleHour = calculatedCycleTime.get(Calendar.HOUR_OF_DAY);

        if (dateFromFileName != null
                && dateFromFileName != calculatedCycleDate) {

            // WARNING!!
            logger.warn("Cycle date " + dateFromFileName
                    + " from metafile name " + fileName + " differs from "
                    + calculatedCycleDate + " inferred from frame header "
                    + frameHeaderTimeString);

        } else if (hourFromFileName != null
                && hourFromFileName != calculatedCycleHour) {

            // WARNING!!
            logger.warn("Cycle hour " + hourFromFileName
                    + " from metafile name " + fileName + " differs from "
                    + calculatedCycleHour + " inferred from frame header "
                    + frameHeaderTimeString);
        }

        // Return DataTime, constructed from cycle time and forecast hour.
        DataTime dataTime =
                new DataTime(calculatedCycleTime, forecastHour * 3600);

        return dataTime;
    }

    // enum Model is not used anywhere else. It can conceivably be redesigned
    // out of this class by redesigning ResourceSelectControl
    private enum Model {

        // TODO - Remove this, to make decoder agnostic w.r.t. list of available
        // models.

        // We do this temporarily because we don't yet know the possible formats
        // of filename strings we're going to be fed, so for now we just look
        // for known model names appearing anywhere in the file name.
        // NOTE: Sequence is important only insofar as any model name must
        // appear after all model names of which it is a proper substring.
        // Also, OPC_ENC comes first, since its metafiles may contain other
        // model substrings

        // @formatter:off
        OPC_ENS, 
        CMCE_AVGSPR, 
        CMCE, 
        CMCVER, 
        CMC, 
        CPC, 
        DGEX, 
        ECENS_AVGSPR, 
        ECENS, 
        ECMWFVER, 
        ECMWF_HR, 
        ECMWF, 
        ENSVER, 
        FNMOCWAVE, 
        GDAS, 
        GEFS_AVGSPR, 
        GEFS, 
        GFSP, 
        GFSVERP,
        GFSVER,
        GFS,
        GHM,
        HPCQPF,
        HPCVER,
        HWRF, 
        ICEACCR,
        JMAP,
        JMA,
        MEDRT,
        NAEFS,
        NAM20,
        NAM44,
        NAMVER,
        NAM, 
        NAVGEM,
        NOGAPS,
        NWW3P,
        NWW3, 
        RAPP, 
        RAP, 
        SREFX,
        SST,
        UKMETVER,
        UKMET,
        VAFTAD
        // @formatter:on
    };

    /**
     * Infer the model name from the file name (Use heuristics gleaned from
     * $NTRANS_META contents)
     * 
     * @param String
     *            fileName
     * @return String modelName
     */
    private String inferModel(String fileName) {

        // TODO "default" model...?
        String modelName = "other";

        if (fileName.startsWith("ecens_prob")) {
            modelName = "ecens";

        } else if (fileName.startsWith("Day") || fileName.startsWith("Night")
                || fileName.startsWith("Official")) {
            modelName = "medrt";

        } else if (fileName.startsWith("meta_sst")) {
            modelName = "sst";
        } else if (fileName.startsWith("hysplit")) {
            modelName = "HYSPLIT";
        } else if (fileName.contains("_GFS")) {
            modelName = "vaftad";
        } else {

            for (Model model : Model.values()) {

                if (fileName.toLowerCase()
                        .contains(model.name().toLowerCase())) {

                    modelName = model.name().toLowerCase();
                    break;
                }
            }

            if (modelName.equals("jma")) {
                modelName = "jmap";
            }
            return modelName;
        }
        return modelName;
    }

    /**
     * Return ByteOrder (Endianess) for given ByteBuffer (byteBuffer)
     * 
     * @param ByteBuffer
     *            byteBuffer
     * @return ByteOrder
     */
    public ByteOrder determineEndianess(ByteBuffer byteBuffer) {

        // TODO Review This -- I think this is the same criterion
        // used in legacy; see scan_hmeta in process_meta.c
        // (where doflip is set)
        byte lowAddressByteOfTheMachineTypeField = byteBuffer.get(36);

        if (lowAddressByteOfTheMachineTypeField == 0) {
            return ByteOrder.BIG_ENDIAN;

        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    public Calendar getDecodeTime() {
        return decodeTime;
    }

    public Integer getYearFromFileName() {
        return yearFromFileName;
    }

    public Integer getMonthFromFileName() {
        return monthFromFileName;
    }

    public Integer getDateFromFileName() {
        return dateFromFileName;
    }

    public Integer getHourFromFileName() {
        return hourFromFileName;
    }
}
