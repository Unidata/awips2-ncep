package gov.noaa.nws.ncep.viz.rsc.ncscat.rsc;

import javax.measure.UnitConverter;

import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode.LongitudeCoding;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatMode.WindDirectionSense;
import gov.noaa.nws.ncep.common.dataplugin.ncscat.NcscatPoint;
import si.uom.SI;
import systems.uom.common.USCustomary;

/**
 * NcscatPointData - Class to hold numerical and boolean data for a single
 * point, or wind vector cell (WVC).
 * 
 * By the time the data are stored here, they have been normalized somewhat,
 * mainly to remove instrument-specific idiosyncrasies (like whether the
 * "direction" of wind is "from" or "to") so the resource can handle things
 * consistently. Note that no drawable graphical objects are stored here.
 * 
 * This code has been developed by NCEP for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 01/27/2016   R10155     B. Hebbard  Spun off from NcscatResource (was formerly
 *                                     an inner class)
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
class NcscatPointData {

    // @formatter:off
    
    private static final UnitConverter metersPerSecondToKnots = SI.METRE_PER_SECOND
            .getConverterTo(USCustomary.KNOT);

    private Coordinate location;        // lat/lon of this point
    private float direction;            // "from" direction in bulletin
    private float speed;                // in knots
    private boolean rainQcFlag;         // is point marked 'rain' or 'QC fail'?
    private boolean highWindSpeedFlag;  // is point marked 'high wind speed'?
    private boolean lowWindSpeedFlag;   // is point marked 'low wind speed'?
    private boolean availRedunFlag;     // is point marked 'some data unavailable' or 'redundant'?

    // Constructor to convert an existing NcscatPoint (common to decoder)
    // into a NcscatPointData (local class here), in which things are
    // minimized to -- and optimized for -- what we need for display.
    // (For example, don't want to flip direction at every paint().)

    public NcscatPointData(NcscatPoint point, NcscatMode ncscatMode) {
        int scaledLatitude = point.getLat();   // signed int from signed short
        int scaledLongitude = point.getLon();  // signed int from either signed short, OR...
        if (ncscatMode.getLongitudeCoding() == LongitudeCoding.UNSIGNED) {
            if (scaledLongitude < 0) {         // ...*unsigned* short...
                scaledLongitude += 65536;      // ...add 2^16 to convert to intended positive value
            }
            if (scaledLongitude >  18000) {  // unsigned longitude comes in as 0--360 east of Greenwich
                scaledLongitude -= 36000;    // convert to -180 -- +180
            }
        }
        location = new Coordinate(scaledLongitude / 100.0,  // de-scale and float
                                  scaledLatitude  / 100.0);
        
        direction = point.getIdr() / 100.0f;  // de-scale and float
        // some (but not all) data sources require us to... 
        if (ncscatMode.getWindDirectionSense() == WindDirectionSense.METEOROLOGICAL) { 
            direction = direction > 180.0f ? // ...reverse direction sense
                        direction - 180.0f
                      : direction + 180.0f;
        }
        
        speed = point.getIsp() / 100.0f;  // de-scale and float
        speed = (float) metersPerSecondToKnots.convert(speed);  // m/s --> kt
        
        int qualityBits = point.getIql();

        switch (ncscatMode) {
        case QUIKSCAT:
        case QUIKSCAT_HI:
            rainQcFlag =       !getBit(qualityBits, 12) &&  // bit 12 == 0 AND
                                getBit(qualityBits, 13);    // bit 13 == 1   rain
            highWindSpeedFlag = getBit(qualityBits, 10);    // bit 10 == 1
            lowWindSpeedFlag =  getBit(qualityBits, 11);    // bit 11 == 1
            availRedunFlag =    getBit(qualityBits, 14);    // bit 14 == 1   availability
            break;
        case ASCAT:
        case ASCAT_HI:
        case EXASCT:
        case EXASCT_HI:
            rainQcFlag =        getBit(qualityBits,  5);    // bit  5 == 1
            highWindSpeedFlag = getBit(qualityBits, 10);    // bit 10 == 1
            lowWindSpeedFlag =  getBit(qualityBits, 11);    // bit 11 == 1
            availRedunFlag =    getBit(qualityBits, 15);    // bit 15 == 1   redundancy
            break;
        case WSCAT:
            rainQcFlag = getBit(qualityBits,  0) ||
                         getBit(qualityBits, 15);           // bits 0 OR 15 == 1   rain
            highWindSpeedFlag = false;
            lowWindSpeedFlag =  false;
            availRedunFlag =    false;
            break;
        case OSCAT:     // Don't have...
        case OSCAT_HI:  // ...these flags
        case UNKNOWN:
        default:
            rainQcFlag = false;
            highWindSpeedFlag = false;
            lowWindSpeedFlag =  false;
            availRedunFlag =    false;
            break;
        }
    }
    
    private boolean getBit (int bits, int bitNum) {
        // Little endian bit numbering:
        // bit 0 is LSB; bit 15 is MSB
        final int masks[] = {0x0001, 0x0002, 0x0004, 0x0008,
                             0x0010, 0x0020, 0x0040, 0x0080,
                             0x0100, 0x0200, 0x0400, 0x0800,
                             0x1000, 0x2000, 0x4000, 0x8000};
        return (bits & masks[bitNum]) != 0;
    }
    
    /**
     * 
     * @param resourceData
     * @return whether or not to display this point given criteria in resourceData
     */
    public boolean pointMeetsSelectedCriteria (NcscatResourceData resourceData) {
        return (!availRedunFlag || resourceData.availabilityFlagEnable)
                && (!rainQcFlag || resourceData.rainFlagEnable)
                && (!highWindSpeedFlag || resourceData.highWindSpeedEnable)
                && (!lowWindSpeedFlag || resourceData.lowWindSpeedEnable)
                && speed > 0.0f;
    }
    /**
     * @return the location
     */
    public Coordinate getLocation() {
        return location;
    }

    /**
     * @return the direction
     */
    public float getDirection() {
        return direction;
    }

    /**
     * @return the speed
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * @return the rainQcFlag
     */
    public boolean getRainQcFlag() {
        return rainQcFlag;
    }
}
// @formatter:on