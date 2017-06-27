/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.edex.plugin.asdi.decoder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;

/**
 * TODO Add Description
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 24, 2017            rcreynolds     Initial creation
 *
 * </pre>
 *
 * @author rcreynolds
 */

public class TestAsdiDecoder {

    AsdiDecoder decoder;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        decoder = new AsdiDecoder();

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.asdi.decoder.AsdiDecoder#AsdiDecoder()}
     * .
     */
    @Test
    public void testAsdiDecoder() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link gov.noaa.nws.ncep.edex.plugin.asdi.decoder.AsdiDecoder#decode(java.io.File)}
     * .
     */
    @Test
    public void testDecode() {
        // fail("Not yet implemented");
    }

    @Test
    public void testGoodAreLastReportAgeAndStatusValid() {
        assertTrue(decoder.areLastReportAgeAndStatusValid("CUR", "E"));
    }

    @Test
    public void testBadAreLastReportAgeAndStatusValid1() {
        assertFalse(decoder.areLastReportAgeAndStatusValid("CUR", "X"));
    }

    @Test
    public void testBadAreLastReportAgeAndStatusValid2() {
        assertFalse(decoder.areLastReportAgeAndStatusValid("X", "E"));
    }

    @Test
    public void testBadAreLastReportAgeAndStatusValid3() {
        assertFalse(decoder.areLastReportAgeAndStatusValid("X", "X"));
    }

    @Test
    public void testGoodIsFlightNumberValid() {
        assertTrue(decoder.isFlightNumberValid("X"));
    }

    @Test
    public void testBadIsFlightNumberValid() {
        assertFalse(decoder.isFlightNumberValid(""));
    }

    @Test
    public void testGoodAreLatitudeAndLongitudeValid() {
        assertTrue(decoder.areLatitudeAndLongitudeValid((float) 43.7022,
                (float) -72.2896));
    }

    @Test
    public void testBadAreLatitudeAndLongitudeValid1() {
        assertFalse(decoder.areLatitudeAndLongitudeValid(
                IDecoderConstantsN.FLOAT_MISSING, (float) -72.2896));
    }

    @Test
    public void testBadAreLatitudeAndLongitudeValid2() {
        assertFalse(decoder.areLatitudeAndLongitudeValid(
                IDecoderConstantsN.FLOAT_MISSING,
                IDecoderConstantsN.FLOAT_MISSING));
    }

    @Test
    public void testBadAreLatitudeAndLongitudeValid3() {
        assertFalse(decoder.areLatitudeAndLongitudeValid((float) 43.7022,
                IDecoderConstantsN.FLOAT_MISSING));
    }

    @Test
    public void testGoodisDepartureOrArrivalAirportPresent() {
        assertTrue(decoder.isDepartureOrArrivalAirportPresent("X", "X"));
    }

    @Test
    public void testBadisDepartureOrArrivalAirportPresent1() {
        assertFalse(decoder.isDepartureOrArrivalAirportPresent("", "X"));
    }

    @Test
    public void testBadisDepartureOrArrivalAirportPresent2() {
        assertFalse(decoder.isDepartureOrArrivalAirportPresent("", ""));
    }

    @Test
    public void testBadisDepartureOrArrivalAirportPresent3() {
        assertFalse(decoder.isDepartureOrArrivalAirportPresent("X", ""));
    }

}
