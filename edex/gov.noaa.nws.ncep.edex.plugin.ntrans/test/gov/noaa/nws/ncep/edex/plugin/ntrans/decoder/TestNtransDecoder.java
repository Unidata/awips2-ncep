package gov.noaa.nws.ncep.edex.plugin.ntrans.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.raytheon.edex.exception.DecoderException;
import com.raytheon.uf.common.time.DataTime;

/**
 * JUnit test class for the NTRANS Decoder class (NtransDecoder)
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Author      Description
 * ------------ -------- ----------- -------------------------------------
 * 08/08/2014            bhebbard    Initial creation
 * 05/22/2017   R32628   mkean       added test cases for reftime calculation
 * </pre>
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * @author bhebbard
 * @version 1.0
 */

public class TestNtransDecoder {

    private NtransDecoder ntransdecoder = null;

    private Calendar decodeTime = null;

    private String frameTimeString = null;

    private String metafileName = null;

    private String actualFileName = null;

    private String expectedFileName = null;

    private DataTime actual = null;

    private DataTime expected = null;

    private Field reflectedFields[] = null;

    private Class<?> reflectedClass = null;

    private static String YEAR_FROM_FILE_NAME = "yearFromFileName";

    private static String MONTH_FROM_FILE_NAME = "monthFromFileName";

    private static String DATE_FROM_FILE_NAME = "dateFromFileName";

    private static String HOUR_FROM_FILE_NAME = "hourFromFileName";

    private static String BAD_FRAME_HEADER = "BAD_FRAME_HEADER";

    private static String METAFILE_NAME_NO_TIME =
            "METAFILE_NAME_CONTAINS_NO_TIME_STRING";

    private static ArrayList<CreateDataTimeTestCase> allCreateDataTimeTestCases =
            new ArrayList<CreateDataTimeTestCase>();

    private static ArrayList<CreateDataTimeTestCase> createDataTimeTestCases0330 =
            new ArrayList<CreateDataTimeTestCase>();

    //////// for storing test case data
    static class CreateDataTimeTestCase {

        private String filename = "";

        private String expectedResult = "";

        private DataTime expectedDataTime = null;

        private ArrayList<String> frameHeaders = new ArrayList<String>();

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getExpectedResult() {
            return expectedResult;
        }

        public void setExpectedResult(String expectedResult) {
            this.expectedResult = expectedResult;
        }

        public ArrayList<String> getFrameHeaders() {
            return frameHeaders;
        }

        public void setFrameHeaders(ArrayList<String> frameHeaders) {
            this.frameHeaders = frameHeaders;
        }

        public DataTime getExpectedDataTime() {
            return expectedDataTime;
        }

        public void setExpectedDataTime(DataTime expectedDataTime) {
            this.expectedDataTime = expectedDataTime;
        }
    }

    /**
     * load test case data
     * 
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        // load test cases for createDataTime() method
        CreateDataTimeTestCase createDataTimetestcase;
        String testFrames[];

        ////// load first test case /////////////////////////////////
        createDataTimetestcase = new CreateDataTimeTestCase();

        createDataTimetestcase.setFilename("gfs_20170330_12_us_bwx");
        createDataTimetestcase.setExpectedResult("2017-03-30 12:00:00.0 (.*)");
        createDataTimetestcase
                .setExpectedDataTime(new DataTime("2017-03-30 12:00:00.0 (0)"));

        createDataTimetestcase.setFrameHeaders(new ArrayList<String>());

        testFrames = new String[] { "01/00V036", "01/06V042", "01/12V048",
                "01/18V054", "02/00V060", "02/06V066", "02/12V072", "02/18V078",
                "03/00V084", "03/06V090", "03/12V096", "03/18V102", "04/00V108",
                "04/06V114", "04/12V120", "04/18V126", "05/00V132", "05/06V138",
                "05/12V144", "05/18V150", "06/00V156", "06/06V162", "06/12V168",
                "06/18V174", "07/00V180", "30/12V000", "30/18V006", "31/00V012",
                "31/06V018", "31/12V024", "31/18V030" };

        for (String frame1 : testFrames) {
            createDataTimetestcase.getFrameHeaders().add(frame1);
        }
        allCreateDataTimeTestCases.add(createDataTimetestcase);

        // test cases specific to 03/30 create date
        createDataTimeTestCases0330.add(createDataTimetestcase);

        // load second test case ////////////////////////////////////

        createDataTimetestcase = new CreateDataTimeTestCase();

        createDataTimetestcase.setFilename("cpc_20170429_ecmwf");
        createDataTimetestcase.setExpectedResult("2017-04-29 00:00:00.0 (.*)");
        createDataTimetestcase
                .setExpectedDataTime(new DataTime("2017-04-29 00:00:00.0 (0)"));

        createDataTimetestcase.setFrameHeaders(new ArrayList<String>());

        createDataTimetestcase.getFrameHeaders().add("07/00V192");

        allCreateDataTimeTestCases.add(createDataTimetestcase);

        // load third test case /////////////////////////////////////

        createDataTimetestcase = new CreateDataTimeTestCase();

        createDataTimetestcase.setFilename("hpcqpf_2017040200");
        createDataTimetestcase.setExpectedResult("2017-04-02 00:00:00.0 (.*)");
        createDataTimetestcase
                .setExpectedDataTime(new DataTime("2017-04-02 00:00:00.0 (0)"));

        createDataTimetestcase.setFrameHeaders(new ArrayList<String>());

        testFrames = new String[] { "02/06V006", "02/18V018", "03/12V036",
                "03/18V042", "02/12V012", "04/12V060", "03/06V030", "04/00V048",
                "03/00V024", "04/06V054" };

        for (String frame1 : testFrames) {
            createDataTimetestcase.getFrameHeaders().add(frame1);
        }

        allCreateDataTimeTestCases.add(createDataTimetestcase);

        // load fourth test case ////////////////////////////////////

        createDataTimetestcase = new CreateDataTimeTestCase();

        createDataTimetestcase.setFilename("srefx_20170426_03_arw");
        createDataTimetestcase.setExpectedResult("2017-04-26 03:00:00.0 (.*)");
        createDataTimetestcase
                .setExpectedDataTime(new DataTime("2017-04-26 03:00:00.0 (0)"));

        createDataTimetestcase.setFrameHeaders(new ArrayList<String>());

        testFrames = new String[] { "27/00V021", "27/00V021", "27/12V033",
                "29/18V087", "28/12V057", "28/06V051", "26/12V009", "29/06V075",
                "28/00V045", "26/18V015", "29/00V069", "29/12V081", "27/18V039",
                "26/06V003", "27/06V027", "28/18V063" };

        for (String frame1 : testFrames) {
            createDataTimetestcase.getFrameHeaders().add(frame1);
        }

        allCreateDataTimeTestCases.add(createDataTimetestcase);
    }

    /**
     * after all the tests in the class have run
     * 
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        ntransdecoder = new NtransDecoder();

        decodeTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        // no nonsense here
        decodeTime.setLenient(false);

        // default simulated decode time: 2014-08-16 23:57:59.327
        decodeTime.set(2014, Calendar.AUGUST, 16, 23, 57, 59);

        decodeTime.set(Calendar.MILLISECOND, 327);

        // set some other defaults
        frameTimeString = "";

        metafileName = METAFILE_NAME_NO_TIME;

        // load reflection fields (if not yet loaded)
        if (reflectedFields == null) {

            reflectedClass = ntransdecoder.getClass();
            reflectedFields = reflectedClass.getDeclaredFields();
        }
    }

    /**
     * After each test runs
     * 
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

        ntransdecoder = null;
        decodeTime = null;
        frameTimeString = null;
        metafileName = null;
    }

    // @formatter:off
    /*-
     * Now, the individual test cases!
     * 
     * In the comments which follow... 
     *    C = cycle (or initial) time 
     *    D = decode (or system) time 
     *    V = valid time 
     *    F = forecast hour (V - C) 
     *    M = month boundary
     *
     */
    // @formatter:on

    /**
     * VALID CASES
     */

    /**
     * A "typical" scenario: C < D < V (Here F = 36)
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_01_C_D_V_F036_A()
            throws IllegalArgumentException, IllegalAccessException {

        // default simulated decode time: 2014-08-16 23:57:59.327
        frameTimeString = "18/00V036";

        expected = new DataTime("2014-08-16 12:00:00.0 (36)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * C = V < D (F = 0)
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_02_C_V_D_F000_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2014, Calendar.AUGUST, 18, 04, 22, 13);
        frameTimeString = "18/00V000";

        expected = new DataTime("2014-08-18 00:00:00.0 (0)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * C < M < V < D (F = 6)
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_03_C_M_V_D_F006_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2014, Calendar.SEPTEMBER, 01, 04, 22, 13);
        frameTimeString = "01/03V006";

        expected = new DataTime("2014-08-31 21:00:00.0 (6)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * C < D < M < V (F = 96)
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_04_C_D_M_V_F096_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2014, Calendar.OCTOBER, 31, 04, 22, 13);

        // V = 2014-11-03 12:00:00
        frameTimeString = "03/12V096";

        expected = new DataTime("2014-10-30 12:00:00.0 (96)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * C < M < D < V (F = 240)
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_05_C_M_D_V_F240_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2017, Calendar.JULY, 01, 04, 22, 13);

        // V = 2017-07-02 18:00:00
        frameTimeString = "02/18V240";

        expected = new DataTime("2017-06-22 18:00:00.0 (240)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * C < V < M < D (F = 240)
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_06_C_V_M_D_F240_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        // V = 2019-01-21 18:00:00
        frameTimeString = "21/18V240";

        expected = new DataTime("2019-01-11 18:00:00.0 (240)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: YYYYMMDDHH
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_10_YYYYMMDDHH_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "ecens_prob_2013042712_atl";

        // V = 2013-05-07 12:00:00
        frameTimeString = "07/12V240";

        expected = new DataTime("2013-04-27 12:00:00.0 (240)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2013,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 04,
                getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 27, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals((Integer) 12, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: YYYYMMDD_HH
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_11_YYYYMMDD_HH_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "cmc_20130429_00_chi_sta";

        // V = 2013-05-09 00:00:00
        frameTimeString = "09/00V240";

        expected = new DataTime("2013-04-29 00:00:00.0 (240)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2013,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 04,
                getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 29, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals((Integer) 00, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: YYMMDD_HH
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_12_YYMMDD_HH_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "iceaccr_130428_18";

        // V = 2013-05-08 18:00:00
        frameTimeString = "08/18V240";

        expected = new DataTime("2013-04-28 18:00:00.0 (240)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2013,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 04,
                getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 28, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals((Integer) 18, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: YYYYMMDD
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_13_YYYYMMDD_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "cpc_20130419_ecmwf";

        // V = 2013-05-04 18:00:00 - F = 15 days
        frameTimeString = "04/18V360";

        expected = new DataTime("2013-04-19 18:00:00.0 (360)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2013,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 04,
                getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 19, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: YYMMDD
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_14_YYMMDD_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "cpc_130419_ecmwf";

        // V = 2013-05-04 18:00:00 - F = 15 days
        frameTimeString = "04/18V360";

        expected = new DataTime("2013-04-19 18:00:00.0 (360)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2013,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 04,
                getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 19, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: Date without hour, followed later by date
     * with hour
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_15_YYYYMMDD_YYYYMMDD_HH_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "gfs_gfs.20140829_gfs_20140829_18";

        // V = 2013-05-04 18:00:00 - F = 15 days
        frameTimeString = "29/18V000";

        expected = new DataTime("2014-08-29 18:00:00.0 (0)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2014,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 8, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 29, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals((Integer) 18, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Time spec in metafile name: Date with hour, followed later by date
     * without hour
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTime_16_YYYYMMDD_HH_YYYYMMDD_A()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2019, Calendar.FEBRUARY, 01, 04, 22, 13);

        metafileName = "gfs_gfs.20140829_gfs_20140829_18";

        // V = 2013-05-04 18:00:00 - F = 15 days
        frameTimeString = "29/18V000";

        expected = new DataTime("2014-08-29 18:00:00.0 (0)");

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals((Integer) 2014,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 8, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 29, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals((Integer) 18, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTimeString1()
            throws IllegalArgumentException, IllegalAccessException {

        metafileName = "GFS_DUMMY_20140713_10";

        frameTimeString = "14/22V036";

        actual = ntransdecoder.createDataTime(frameTimeString, metafileName);

        expected = new DataTime("2014-07-13 10:00:00.0 (36)");

        assertEquals((Integer) 2014,
                getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals((Integer) 07,
                getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals((Integer) 13, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals((Integer) 10, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTimeDecodeTime1()
            throws IllegalArgumentException, IllegalAccessException {

        decodeTime.set(2014, Calendar.AUGUST, 12, 20, 01, 01);

        frameTimeString = "20140714/22V036";

        expected = new DataTime("2014-07-13 10:00:00.0 (36)");

        // does this need a decodeTime ??
        actual = ntransdecoder.createDataTime("20140714/22V036", metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        assertEquals(expected, actual);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String)}
     * .
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public final void testCreateDataTimeString2()
            throws IllegalArgumentException, IllegalAccessException {

        frameTimeString = "20140714/22V036";

        // test with default decode time and metafile
        actual = ntransdecoder.createDataTime(frameTimeString, metafileName,
                decodeTime);

        assertEquals(null, getFieldWithReflection(YEAR_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(MONTH_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(DATE_FROM_FILE_NAME));
        assertEquals(null, getFieldWithReflection(HOUR_FROM_FILE_NAME));

        expected = new DataTime("2014-07-13 10:00:00.0 (36)");

        assertEquals(expected, actual);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName01() {

        metafileName = "gfs_gfs.20140901_gfs_20140901_12_ak";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gfs_20140901_12_ak";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName02() {

        metafileName = "gdas_gdas.20140901_gdas_20140901_12_na";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gdas_20140901_12_na";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName03() {

        metafileName = "gefs_gefs.20140831_gefs_20140831_18_spag";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gefs_20140831_18_spag";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName04() {

        metafileName = "gefs_gefs.20140902_gefs_avgspr_20140902_06_natl";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gefs_avgspr_20140902_06_natl";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName05() {

        metafileName = "gfs_gfs.20140901_gfs_20140901_00_mar_skewt";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gfs_20140901_00_mar_skewt";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName06() {

        metafileName = "gfs_gfs.20140901_gfsver_20140901_00";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gfsver_20140901_00";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName07() {

        metafileName = "gfs_gfs.20140901_gfsver_20140901_18_na_mar";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "gfsver_20140901_18_na_mar";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName08() {

        metafileName = "ghm_ghm.20140901_ghm_20140901_00_invest99l";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "ghm_20140901_00_invest99l";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName09() {

        metafileName = "ghm_ghm.20140902_ghm_20140902_06_dolly05l_nest";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "ghm_20140902_06_dolly05l_nest";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName10() {

        metafileName = "hwrf_hwrf.20140901_hwrf_20140901_06_invest99l_nest";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "hwrf_20140901_06_invest99l_nest";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName11() {

        metafileName = "nam_nam.20140901_nam_20140901_00";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "nam_20140901_00";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName12() {

        metafileName = "nam_nam.20140901_nam_20140901_00_bwx";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "nam_20140901_00_bwx";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName13() {

        metafileName = "nam_nam.20140901_nam_20140901_00_mar_ver";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "nam_20140901_00_mar_ver";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName14() {

        metafileName = "rap_rap.20140831_rap_20140831_23_anlloop";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "rap_20140831_23_anlloop";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName15() {

        metafileName = "rap_rap.20140901_rap_20140901_01";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "rap_20140901_01";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName16() {

        metafileName = "ukmet.2014090_ukmet.2014090._ukmetver_20140901_00";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        // now this one is tricky!
        expectedFileName = "ukmetver_20140901_00";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName17() {

        metafileName = "ukmet_ukmet.20140902_ukmet_20140902_00_trop";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "ukmet_20140902_00_trop";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName18() {

        metafileName = "wave_wave.20140901_nww3_20140901_12";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        // yes, "wave" disappears here...
        expectedFileName = "nww3_20140901_12";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName19() {

        metafileName = "wave_wave.20140902_nww3_20140902_00_akw";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        expectedFileName = "nww3_20140902_00_akw";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public final void testNormalizeFileName20() {

        metafileName = "opc_ens_20140901_18_gefs_prob_lo_spd";

        actualFileName = ntransdecoder.normalizeMetafileName(metafileName);

        // should leave this one alone!
        expectedFileName = "opc_ens_20140901_18_gefs_prob_lo_spd";

        assertEquals(expectedFileName, actualFileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     */
    @Test
    public void testCreateDataTime_IceFile() {

        metafileName = "iceaccr_170507_18";

        String testFrameHeaderTime = "12/00V102";

        actual = ntransdecoder.createDataTime(testFrameHeaderTime,
                metafileName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     */
    @Test
    public void testCreateDataTime_0330_1() {

        // test end of month
        decodeTime.set(2017, Calendar.MARCH, 30, 04, 22, 13);

        // testing for file without time stamp
        metafileName = METAFILE_NAME_NO_TIME;

        for (CreateDataTimeTestCase currentTestCase : createDataTimeTestCases0330) {

            // loop through all test frame headers
            for (String currentFrameHeader : currentTestCase.frameHeaders) {

                actual = ntransdecoder.createDataTime(currentFrameHeader,
                        metafileName, decodeTime);

                assertEquals(actual.getRefTime(),
                        currentTestCase.getExpectedDataTime().getRefTime());
            }
        }
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     */
    @Test
    public void testCreateDataTime_0330_2() {

        // test beginning of following month
        decodeTime.set(2017, Calendar.APRIL, 01, 04, 22, 13);

        // testing for file without time stamp
        metafileName = METAFILE_NAME_NO_TIME;

        for (CreateDataTimeTestCase currentTestCase : createDataTimeTestCases0330) {

            // loop through all test frame headers
            for (String currentFrameHeader : currentTestCase.frameHeaders) {

                actual = ntransdecoder.createDataTime(currentFrameHeader,
                        metafileName, decodeTime);

                assertEquals(actual.getRefTime(),
                        currentTestCase.getExpectedDataTime().getRefTime());
            }
        }
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     */
    @Test
    public void testCreateDataTime_0330_3() {

        // test edge case -- this will only work up to 28th (<1 month?)
        decodeTime.set(2017, Calendar.APRIL, 28, 04, 22, 13);

        // testing for file without time stamp
        metafileName = METAFILE_NAME_NO_TIME;

        for (CreateDataTimeTestCase currentTestCase : createDataTimeTestCases0330) {

            // loop through all test frame headers
            for (String currentFrameHeader : currentTestCase.frameHeaders) {

                actual = ntransdecoder.createDataTime(currentFrameHeader,
                        metafileName, decodeTime);

                assertEquals(actual.getRefTime(),
                        currentTestCase.getExpectedDataTime().getRefTime());
            }
        }
    }

    /**
     * Test that createDataTime() will generate consistent dataTime (refTime)
     * results across frames, when there is no YYMM date elements given in the
     * frame or the metafile, and the metafile is beyond a month old.
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     */
    @Test
    public void testCreateDataTime_consistency() {

        decodeTime.set(2017, Calendar.SEPTEMBER, 01, 01, 01, 01);

        // testing for file without time stamp
        metafileName = METAFILE_NAME_NO_TIME;

        for (CreateDataTimeTestCase currentTestCase : allCreateDataTimeTestCases) {

            expected = null;

            // loop through all test frame headers
            for (String currentFrameHeader : currentTestCase.frameHeaders) {

                actual = ntransdecoder.createDataTime(currentFrameHeader,
                        metafileName, decodeTime);

                // save the first actual result for subsequent tests
                if (expected == null) {
                    expected = actual;
                } else {
                    assertEquals(expected.getRefTime(), actual.getRefTime());
                }
            }
        }
    }

    /**
     * Test that createDataTime() will generate consistent dataTime (refTime)
     * results across frames, when the date is included on the metafile but
     * there is no YYMM date elements given in the frame.
     * 
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     * 
     * @throws DecoderException
     */
    @Test
    public void testCreateDataTimeWithFile_consistency()
            throws DecoderException {

        decodeTime.set(2017, Calendar.SEPTEMBER, 01, 01, 01, 01);

        for (CreateDataTimeTestCase currentTestCase : allCreateDataTimeTestCases) {

            // populate the test metafile
            metafileName = currentTestCase.getFilename();

            expected = null;

            // :NtransDecoder for next test case
            ntransdecoder = new NtransDecoder();

            // loop through all test frame headers
            for (String currentFrameHeader : currentTestCase.frameHeaders) {

                actual = ntransdecoder.createDataTime(currentFrameHeader,
                        metafileName, decodeTime);

                // save the first actual result for subsequent tests
                if (expected == null) {
                    expected = actual;
                } else {
                    assertEquals(expected.getRefTime(), actual.getRefTime());
                }
            }
        }
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(java.lang.String, java.lang.String, java.util.Calendar)}
     * .
     */
    @Test
    public void testCreateDataTimeGoodFramesWithFile() {

        for (CreateDataTimeTestCase currentTestCase : allCreateDataTimeTestCases) {

            // populate the test metafile
            metafileName = currentTestCase.getFilename();

            // loop through all test frame headers
            for (String currentFrameHeader : currentTestCase.frameHeaders) {

                // test with simulated metafile and decodeTime
                actual = ntransdecoder.createDataTime(currentFrameHeader,
                        metafileName, decodeTime);

                assertEquals(actual.getRefTime(),
                        currentTestCase.getExpectedDataTime().getRefTime());
            }
        }
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(String)}
     * .
     */
    @Test
    public void testCreateDataTimeBadFrame() {

        for (CreateDataTimeTestCase currentCreateDataTimeTestCase : allCreateDataTimeTestCases) {

            // test an invalid frame header value
            actual = ntransdecoder.createDataTime(BAD_FRAME_HEADER);

            // bad frame should not produce valid refTime
            assertNotEquals(actual.getRefTime(), currentCreateDataTimeTestCase
                    .getExpectedDataTime().getRefTime());
        }
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoder#createDataTime(String)}
     * .
     */
    @Test
    public void testCreateDataTimeBadFrameWithFile() {

        for (CreateDataTimeTestCase currentCreateDataTimeTestCase : allCreateDataTimeTestCases) {

            // test with metafile populated
            metafileName = currentCreateDataTimeTestCase.getFilename();

            // test an invalid frame header value
            actual = ntransdecoder.createDataTime(BAD_FRAME_HEADER);

            // bad frame should not produce valid refTime
            assertNotEquals(actual.getRefTime(), currentCreateDataTimeTestCase
                    .getExpectedDataTime().getRefTime());
        }
    }

    /**
     * 
     * Allows test methods to access and verify NtransDecoder class (ie:
     * private) data
     * 
     * @param fieldName
     * @return Object
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Object getFieldWithReflection(String fieldName)
            throws IllegalArgumentException, IllegalAccessException {

        // declare Object because we dont know the return type yet
        Object returnObject = null;
        boolean foundField = false;

        // iterate to find the requested field
        for (Field field1 : reflectedFields) {

            if (field1.getName().equals(fieldName)) {

                foundField = true;

                // make accessible in case its private
                field1.setAccessible(true);

                returnObject = field1.get(ntransdecoder);

                break;
            }
        }

        // no match found for field name?
        if (!foundField) {

            throw new IllegalArgumentException(
                    "reflection could not find fieldName: " + fieldName);

        }

        return returnObject;
    }

    public static String getMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
}
