package gov.noaa.nws.ncep.standalone.testConverter;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gov.noaa.nws.ncep.viz.common.dbQuery.NcDirectDbQuery;

public class TestXmlConvertTest {

    TestXmlConvert convert;

    public static String HTTP_SERVER = "";

    @Before
    public void setUp() throws Exception {
        convert = new TestXmlConvert();
        NcDirectDbQuery.setHttpServer(HTTP_SERVER);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLinesXml1() {
        // blue line, OH state
        String v = convert.foo(true);
        assertEquals("yes", v);
    }

    @Test
    public void testBlueFronts() {
        assertEquals("testing foo", "no", convert.foo(false));
    }

    @Test
    public void testSum() {
        // two parameters
        assertEquals(3, convert.sum(1, 2));
        assertEquals(48, convert.sum(1, 2, 45));
        assertEquals(106, convert.sum(1, 2, 100, 3));
    }

    public static void main(String[] args) {

        TestXmlConvert convert;
        HTTP_SERVER = "http://localhost:2222/";
    }
}
