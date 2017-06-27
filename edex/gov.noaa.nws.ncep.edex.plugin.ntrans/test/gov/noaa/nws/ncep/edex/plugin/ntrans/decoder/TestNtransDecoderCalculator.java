package gov.noaa.nws.ncep.edex.plugin.ntrans.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.raytheon.edex.exception.DecoderException;

public class TestNtransDecoderCalculator {

    private NtransDecoderCalculator ntransdecodercalculator = null;

    private NtransDecoder ntransdecoder;

    private Calendar decodeTime = null;

    private Calendar actual = null;

    private Calendar expected = null;

    private static ArrayList<CalulateInitializationHourTestCase> allCalulateInitializationHourTestCases =
            new ArrayList<CalulateInitializationHourTestCase>();

    //////// for storing test case data
    static class CalulateInitializationHourTestCase {

        public CalulateInitializationHourTestCase() {
        }

        public CalulateInitializationHourTestCase(Integer forecastHour,
                Integer hourFromFrameHeader, Integer initializationHour) {

            this.setForecastHour(forecastHour);
            this.setHourFromFrameHeader(hourFromFrameHeader);
            this.setInitializationHour(initializationHour);
        }

        public Integer getForecastHour() {
            return forecastHour;
        }

        public void setForecastHour(Integer forecastHour) {
            this.forecastHour = forecastHour;
        }

        public Integer getHourFromFrameHeader() {
            return hourFromFrameHeader;
        }

        public void setHourFromFrameHeader(Integer hourFromFrameHeader) {
            this.hourFromFrameHeader = hourFromFrameHeader;
        }

        public Integer getInitializationHour() {
            return initializationHour;
        }

        public void setInitializationHour(Integer initializationHour) {
            this.initializationHour = initializationHour;
        }

        public Integer getExpectedResult() {
            return initializationHour;
        }

        public void setExpectedResult(Integer initializationHour) {
            this.initializationHour = initializationHour;
        }

        private Integer forecastHour;

        private Integer hourFromFrameHeader;

        // expected result initializationHour
        private Integer initializationHour;

        /**
         * adds test cases CalulateInitializationHourTestCase to the list
         * allCalulateInitializationHourTestCases for the given
         * forecastAndFrameHours int array and expectedInitializationHour int
         * 
         * @param forecastAndFrameHours
         * @param expectedInitializationHour
         */
        public static void addCalulateInitializationHourTestCases(
                int forecastAndFrameHours[][], int expectedInitializationHour) {

            for (int testCaseInitializationHour[] : forecastAndFrameHours) {
                allCalulateInitializationHourTestCases
                        .add(new CalulateInitializationHourTestCase(
                                testCaseInitializationHour[0],
                                testCaseInitializationHour[1],
                                expectedInitializationHour));
            }
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        // load test cases for: calulateInitializationHour()

        int forecastAndFrameHours[][] = null;
        int expectedInitializationHour;

        /////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour expected result = 0
        expectedInitializationHour = 0;

        forecastAndFrameHours = new int[][] { { 108, 12 }, { 120, 0 },
                { 12, 12 }, { 132, 12 }, { 144, 0 }, { 156, 12 }, { 168, 0 },
                { 180, 12 }, { 24, 0 }, { 36, 12 }, { 48, 0 }, { 60, 12 },
                { 72, 0 }, { 84, 12 }, { 96, 0 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        /////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour for expected result = 3
        expectedInitializationHour = 3;

        forecastAndFrameHours = new int[][] { { 3, 6 }, { 9, 12 }, { 15, 18 },
                { 27, 6 }, { 33, 12 }, { 39, 18 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        /////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour for expected result = 6
        expectedInitializationHour = 6;

        forecastAndFrameHours = new int[][] { { 6, 0 }, { 12, 6 }, { 18, 12 },
                { 30, 0 }, { 36, 6 }, { 42, 12 }, { 54, 0 }, { 60, 6 },
                { 66, 12 }, { 78, 0 }, { 84, 6 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        /////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour for expected result = 9
        expectedInitializationHour = 9;

        forecastAndFrameHours = new int[][] { { 3, 12 }, { 9, 18 }, { 27, 12 },
                { 33, 18 }, { 51, 12 }, { 69, 12 }, { 81, 0 }, { 87, 6 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        /////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour expected result = 12
        expectedInitializationHour = 12;

        forecastAndFrameHours = new int[][] { { 0, 12 }, { 102, 18 },
                { 108, 0 }, { 114, 6 }, { 120, 12 }, { 126, 18 }, { 12, 0 },
                { 132, 0 }, { 138, 6 }, { 144, 12 }, { 150, 18 }, { 156, 0 },
                { 162, 6 }, { 168, 12 }, { 174, 18 }, { 180, 0 }, { 18, 6 },
                { 24, 12 }, { 30, 18 }, { 36, 0 }, { 42, 6 }, { 48, 12 },
                { 54, 18 }, { 60, 0 }, { 66, 6 }, { 6, 18 }, { 72, 12 },
                { 78, 18 }, { 84, 0 }, { 90, 6 }, { 96, 12 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        ////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour expected result = 15
        expectedInitializationHour = 15;

        forecastAndFrameHours = new int[][] { { 3, 18 }, { 21, 6 }, { 27, 18 },
                { 39, 0 }, { 45, 6 }, { 51, 18 }, { 63, 0 }, { 75, 18 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        ////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour expected result = 18
        expectedInitializationHour = 18;

        forecastAndFrameHours =
                new int[][] { { 42, 0 }, { 66, 0 }, { 360, 18 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

        /////////////////////////////////////////////////////////////////////
        //// test cases for initialization hour expected result = 21
        expectedInitializationHour = 21;

        forecastAndFrameHours = new int[][] { { 45, 0 }, { 21, 0 } };

        // add the test cases
        CalulateInitializationHourTestCase
                .addCalulateInitializationHourTestCases(forecastAndFrameHours,
                        expectedInitializationHour);

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

        decodeTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        ntransdecodercalculator = new NtransDecoderCalculator();
        ntransdecoder = new NtransDecoder();
    }

    /**
     * After each test runs
     * 
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

        ntransdecodercalculator = null;
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#calulateInitializationHour(Integer, Integer)}
     * .
     */
    @Test
    public void testCalulateInitializationHour() {

        // loop through all test cases
        for (CalulateInitializationHourTestCase calulateInitializationHourTestCase : allCalulateInitializationHourTestCases) {

            // pass the integer parameters to be tested:
            // forecastHour and hourFromFrameHeader
            Integer testResult =
                    ntransdecodercalculator.calulateInitializationHour(
                            calulateInitializationHourTestCase
                                    .getForecastHour(),
                            calulateInitializationHourTestCase
                                    .getHourFromFrameHeader());

            assertEquals(testResult,
                    calulateInitializationHourTestCase.getInitializationHour());
        }
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#calculateUpperBoundTime()}
     * .
     * 
     * @throws DecoderException
     */
    @Test
    public void testcalculateUpperBoundTime_033017() throws DecoderException {

        decodeTime.setTime(new Date(0));
        decodeTime.set(2017, Calendar.MARCH, 30, 21, 1, 0);

        ntransdecoder.createDataTime("NONE", "gfs_20170330_12_us_bwx",
                decodeTime);

        expected = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expected.setTime(new Date(0));
        expected.set(2017, Calendar.MARCH, 30, 21, 1, 0);

        // call the tested method
        actual = ntransdecodercalculator.calculateUpperBoundTime(null, null,
                ntransdecoder);

        assertEquals(expected.getTime(), actual.getTime());
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#calculateUpperBoundTime()}
     * .
     * 
     * @throws DecoderException
     */
    @Test
    public void testcalculateUpperBoundTime_042713() throws DecoderException {

        decodeTime.setTime(new Date(0));
        decodeTime.set(2013, Calendar.APRIL, 27, 12, 0, 0);

        ntransdecoder.createDataTime("NONE", "ecens_prob_2013042712_atl",
                decodeTime);

        expected = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expected.setTime(new Date(0));
        expected.set(2013, Calendar.APRIL, 27, 12, 0, 0);

        // call the tested method
        actual = ntransdecodercalculator.calculateUpperBoundTime(null, null,
                ntransdecoder);

        assertEquals(expected.getTime(), actual.getTime());
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#calculateCycleTime(Integer monthFromFrameHeader, Integer dateFromFrameHeader, Integer hourFromFrameHeader, Calendar upperBoundTime)}
     * .
     */
    @Test
    public void testCalculateCycleTime_26_6_0901_0826() {

        Calendar upperBoundTime =
                Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        upperBoundTime.set(2017, Calendar.SEPTEMBER, 1, 6, 00, 00);

        expected = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expected.setTime(new Date(0));
        expected.set(2017, Calendar.AUGUST, 26, 6, 0, 0);

        // call the tested method
        actual = ntransdecodercalculator.calculateCycleTime(null, 26, 6,
                upperBoundTime);

        assertEquals(expected.getTime(), actual.getTime());
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#calculateCycleTime(Integer monthFromFrameHeader, Integer dateFromFrameHeader, Integer hourFromFrameHeader, Calendar upperBoundTime)}
     * .
     */
    @Test
    public void testCalculateCycleTime_29_18_0904_0829() {

        Calendar upperBoundTime =
                Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        upperBoundTime.set(2017, Calendar.SEPTEMBER, 4, 14, 00, 00);

        expected = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expected.setTime(new Date(0));
        expected.set(2017, Calendar.AUGUST, 29, 12, 0, 0);

        // call the tested method
        actual = ntransdecodercalculator.calculateCycleTime(null, 29, 12,
                upperBoundTime);

        assertEquals(expected.getTime(), actual.getTime());
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#handleProductNameSpecialCharacters(String)}
     * .
     */
    @Test
    public void testHandleProductNameSpecialCharacters() {

        String actualProductName;
        String initialProductName =
                "Special_:__---Characters  //--.Product-_::&-,,,Name";
        String expectedProductName = "Special-Characters-Product-Name";

        actualProductName = ntransdecodercalculator
                .handleProductNameSpecialCharacters(initialProductName);

        assertEquals(expectedProductName, actualProductName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#handleProductNameSpecialCharacters(String)}
     * .
     */
    @Test
    public void testHandleProductNameSpecialCharacters_2() {

        String actualProductName;
        String initialProductName = "6-HR PCPN & 1000-500 THK";
        String expectedProductName = "6-HR-PCPN-1000-500-THK";

        actualProductName = ntransdecodercalculator
                .handleProductNameSpecialCharacters(initialProductName);

        assertEquals(expectedProductName, actualProductName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#handleProductNameSpecialCharacters(String)}
     * .
     */
    @Test
    public void testHandleProductNameSpecialCharacters_3() {

        String actualProductName;
        String initialProductName = "4400:10000SG RH,R/S T,VV";
        String expectedProductName = "4400-10000SG-RH-R-S-T-VV";

        actualProductName = ntransdecodercalculator
                .handleProductNameSpecialCharacters(initialProductName);

        assertEquals(expectedProductName, actualProductName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#handleProductNameSpecialCharacters(String)}
     * .
     */
    @Test
    public void testHandleProductNameSpecialCharacters_END() {

        String actualProductName;
        String initialProductName = "24-HR PRCP: 0.50in.";
        String expectedProductName = "24-HR-PRCP-0-50in";

        actualProductName = ntransdecodercalculator
                .handleProductNameSpecialCharacters(initialProductName);

        assertEquals(expectedProductName, actualProductName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#handleProductNameSpecialCharacters(String)}
     * .
     */
    @Test
    public void testHandleProductNameSpecialCharacters_BEGIN() {

        String actualProductName;
        String initialProductName = "-6-HR PCPN & 1000-500 THK";
        String expectedProductName = "6-HR-PCPN-1000-500-THK";

        actualProductName = ntransdecodercalculator
                .handleProductNameSpecialCharacters(initialProductName);

        assertEquals(expectedProductName, actualProductName);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#setSubHourToZero(java.util.Calendar)}
     * .
     */
    @Test
    public void testSetSubHourToZero() {

        decodeTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        decodeTime = ntransdecodercalculator.setSubHourToZero(decodeTime);

        // all sub hour fields should now be zero
        assertEquals(0, decodeTime.get(Calendar.MINUTE));
        assertEquals(0, decodeTime.get(Calendar.SECOND));
        assertEquals(0, decodeTime.get(Calendar.MILLISECOND));
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#CalendarMonth(int)}
     * .
     */
    @Test
    public void testCalendarMonth_January() {

        int expectedMonth = Calendar.JANUARY;
        int goodOldMonth = 1;
        int actualMonth;

        // call the method to be tested
        actualMonth = ntransdecodercalculator.CalendarMonth(goodOldMonth);

        assertEquals(actualMonth, expectedMonth);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#CalendarMonth(int)}
     * .
     */
    @Test
    public void testCalendarMonth_April() {

        int expectedMonth = Calendar.APRIL;
        int goodOldMonth = 4;
        int actualMonth;

        // call the method to be tested
        actualMonth = ntransdecodercalculator.CalendarMonth(goodOldMonth);

        assertEquals(actualMonth, expectedMonth);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#CalendarMonth(int)}
     * .
     */
    @Test
    public void testCalendarMonth_September() {

        int expectedMonth = Calendar.SEPTEMBER;
        int goodOldMonth = 9;
        int actualMonth;

        // call the method to be tested
        actualMonth = ntransdecodercalculator.CalendarMonth(goodOldMonth);

        assertEquals(actualMonth, expectedMonth);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#toUnsigned(short)}
     * .
     */
    @Test
    public void testToUnsigned_short() {

        short signedShort = -1;
        int actualInt;

        // call the method to be tested
        actualInt = ntransdecodercalculator.toUnsigned(signedShort);

        assertTrue(actualInt > 0);
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.ntrans.decoder.NtransDecoderCalculator#toUnsigned(int)}
     * .
     */
    @Test
    public void testToUnsigned_long() {

        int signedInt = -1;
        long actualLong;

        // call the method to be tested
        actualLong = ntransdecodercalculator.toUnsigned(signedInt);

        assertTrue(actualLong > 0);
    }
}
