//package gov.noaa.nws.ncep.edex.plugin.ncscat;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//
//import java.util.ArrayList;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import gov.noaa.nws.ncep.edex.plugin.ncscat.decoder.NcscatDecoder;
//
///**
// * <pre>
// * 
// * Ncscat Decoder JUnit Test Class 
// * 
// * This JUnit Class tests the file parsing capabilities of the Ncscat Decoder,
// * The main benefit is that we don't have to actually create files with the desired names and ingest them.
// * 
// * -= INSTRUCTIONS FOR RUNNING THE JUNIT TEST CASES =-
// * 
// * For now (2/6/2017) we will be using a temporary method to use this prototype unit test
// * 
// * I) First, change the following NcscatDecoder methods to public:
// * 
// * parseAscatA()
// * parseAscatB()
// * parseAmbiguity()
// * 
// * II) Next, open /gov.noaa.nws.ncep.edex.plugin.ncscat/META-INF/MANIFEST.MF and add:
// *     org.junit;version="4.12.0" as the last line
// *  
// * III) Finally, open /gov.noaa.nws.ncep.edex.plugin.ncscat/.classpath and add:
// *      <classpathentry kind="lib" path="/org.hamcrest/hamcrest-all-1.3.jar"/>
// * 
// * 1. (Regression test) 
// *    Verify the parser still works after you've made a code change to NcscatDecoder.java
// *    Run the test with a right click > Run As > Junit Test
// * 
// * 2. (New parsing rules) 
// *    Verify the parser still works after you've modified the Test
// *    Change the test: add/remove valid/invalid substrings, and/or tests
// *    Run the test with a right click > Run As > Junit Test
// * 
// * 
// *    2/6/2017
// *    Here are the valid sub-strings (case-insensitive) that can be parsed. 
// *    These will identify the type of the data files to be decoded.
// * 
// *   ascat a:
// *
// *  "ascata", "ascat-a", "ascat_a" 
// *  "ascatd", "ascat-d", "ascat_d" 
// *  "metopa", "metop-a", "metop_a"
// * 
// *   ascat b:
// *
// *  "ascatb", "ascat-b", "ascat_b"
// *  "metopb", "metop-b", "metop_b"
// * 
// *
// *   ambiguity number:
// *
// *  "ambN", "ambigN", "ambiguity_N"
// *  
// * 
// * SOFTWARE HISTORY 
// * 
// * Date          Ticket#  Engineer     Description
// * ------------- -------- ---------    -------------------------
// *
// * 01/31/2017    R18596    J Beck      Initial creation
// * 
// * </pre>
// */
//
//public class TestNcscatParser {
//
//    private NcscatDecoder decoder;
//
//    private ArrayList<String> validAscataFileNames;
//
//    private ArrayList<String> validAscatbFileNames;
//
//    private ArrayList<String> validAmbigFileNames;
//
//    private ArrayList<String> inValidAscatbFileNames;
//
//    private ArrayList<String> notValidAmbigFileNames;
//
//    /**
//     * 
//     * Set Up the test harness
//     * 
//     * Simulate the data filenames we want to test. We can create what we hope
//     * to succeed, as well as what we expect to fail.
//     *
//     * @throws Exception
//     * 
//     */
//
//    @Before
//    public void setUp() throws Exception {
//
//        // Obtain an instance of the decoder
//        decoder = new NcscatDecoder("decodeNcscatInputFile");
//
//        // ASCAT A
//        validAscataFileNames = new ArrayList<>();
//
//        validAscataFileNames.add("myfile-ascata");
//        validAscataFileNames.add("myfile-ascat-a");
//        validAscataFileNames.add("myfile-metopa");
//        validAscataFileNames.add("myfile-metop-a");
//        validAscataFileNames.add("myfile-ASCATD");
//        validAscataFileNames.add("myfile-ascat-d");
//
//        // ASCAT B
//        validAscatbFileNames = new ArrayList<>();
//
//        validAscatbFileNames.add("myfile-ascatb");
//        validAscatbFileNames.add("myfile-ascat-b");
//        validAscatbFileNames.add("myfile-metopb");
//        validAscatbFileNames.add("myfile-metop-b");
//
//        // ASCAT B INVALID
//        inValidAscatbFileNames = new ArrayList<>();
//
//        inValidAscatbFileNames.add("myfile-ascat_more_filename.ascat");
//        inValidAscatbFileNames.add("myfile-ascat_more_filename.metop");
//        inValidAscatbFileNames.add("myfile-asc_more_filename.asc");
//        inValidAscatbFileNames.add("myfile-ascat_more_filename.ascat.B");
//
//        // AMBIGUITY PATTERN 1 "ambigN" VALID
//        validAmbigFileNames = new ArrayList<>();
//
//        validAmbigFileNames.add("myfile-ambig1");
//        validAmbigFileNames.add("myfile-amBIG9");
//        validAmbigFileNames.add("myfile-IAMBIG9.ascat");
//        validAmbigFileNames.add("myfile-amBIG99");
//
//        // AMBIGUITY PATTERN 2 "ambiguity_N" VALID
//        validAmbigFileNames.add("myfile-ambiguity_1");
//        validAmbigFileNames.add("myfile-amBIGuity_3");
//        validAmbigFileNames.add("myfile-ambiguity_4.ascat");
//        validAmbigFileNames.add("myfile-ambiguity_9");
//        validAmbigFileNames.add("myfile-amBIGuity_9.ascat");
//
//        // AMBIGUITY PATTERN 3 "ambN" VALID
//        validAmbigFileNames.add("myfile-amb1");
//        validAmbigFileNames.add("myfile-amB3");
//        validAmbigFileNames.add("myfile-AMB4.ascat");
//        validAmbigFileNames.add("myfile-Amb9.rtext");
//
//        // AMBIGUITY PATTERNS - NOT VALID
//        notValidAmbigFileNames = new ArrayList<>();
//        notValidAmbigFileNames.add("myfile-amBIGuity-5.ascat");
//    }
//
//    @After
//    public void tearDown() throws Exception {
//
//    }
//
//    /**
//     * Test parser for valid Ascat A filenames
//     */
//    @Test
//    public void testValidAscatAFilenames() {
//
//        for (String fileName : validAscataFileNames) {
//            boolean result = decoder.parseAscatA(fileName);
//            assertTrue(result);
//        }
//    }
//
//    /**
//     * Test parser for valid Ascat B filenames.
//     */
//    @Test
//    public void testValidAscatBFilenames() {
//
//        for (String fileName : validAscatbFileNames) {
//            boolean result = decoder.parseAscatB(fileName);
//            assertTrue(result);
//        }
//    }
//
//    /**
//     * Test parser for non-valid Ascat B filenames
//     */
//    @Test
//    public void testInValidAscatBFilenames() {
//
//        for (String fileName : inValidAscatbFileNames) {
//            boolean result = decoder.parseAscatB(fileName);
//            assertTrue(!result);
//        }
//    }
//
//    /**
//     * Test parser for valid ambiguity substrings in filenames "value" is either
//     * the empty string (fail), or 1-9 (pass)
//     */
//    @Test
//    public void testValidAmbigFilenames() {
//
//        for (String fileName : validAmbigFileNames) {
//
//            String value = decoder.parseAmbiguity(fileName);
//
//            if ("".equals(value)) {
//                fail();
//                return;
//            }
//
//            if ((Integer.valueOf(value) > 0) && (Integer.valueOf(value) < 10)) {
//                assert true;
//            }
//        }
//    }
//
//    /**
//     * Test parser for valid ambiguity substrings in filenames
//     */
//    @Test
//    public void testNotValidAmbigFilenames() {
//
//        for (String fileName : notValidAmbigFileNames) {
//
//            String value = decoder.parseAmbiguity(fileName);
//            assertEquals(value, "");
//        }
//    }
//
//    /**
//     * Template for an ignored test
//     */
//    @Ignore
//    public void someTest() {
//
//    }
//}
