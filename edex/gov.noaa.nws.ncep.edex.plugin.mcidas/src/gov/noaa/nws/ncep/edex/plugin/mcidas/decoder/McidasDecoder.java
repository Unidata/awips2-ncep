package gov.noaa.nws.ncep.edex.plugin.mcidas.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;

import org.geotools.referencing.datum.DefaultEllipsoid;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.exception.DecoderException;
import com.raytheon.edex.plugin.AbstractDecoder;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.decodertools.core.DecoderTools;
import com.raytheon.uf.edex.decodertools.time.TimeTools;

import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasMapCoverage;
import gov.noaa.nws.ncep.common.dataplugin.mcidas.McidasRecord;
import gov.noaa.nws.ncep.edex.plugin.mcidas.McidasSpatialFactory;
import gov.noaa.nws.ncep.edex.plugin.mcidas.dao.McidasDao;
import si.uom.SI;

/**
 * This java class decodes McIDAS satellite plug-in image and creates area names
 * from area file number in the header.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * -----------  ----------  ----------- --------------------------
 * 9/2009       144         T. Lee      Creation
 * 11/2009      144         T. Lee      Added geographic area names
 * 12/2009      144         T. Lee      Set calType, satelliteId,
 *                                      and imageTypeNumber
 * 12/2009      144         T. Lee      Renamed proj type for resource
 *                                      rendering
 * 05/2010      144         L. Lin      Migration to TO11DR11.
 * 11/2011                  T. Lee      Enhanced for ntbn
 * Aug 30, 2013 2298        rjpeter     Make getPluginName abstract
 * 10/2015      7190        R.Reynolds  Send data directly to Mcidas database/no routing through mapping tables
 * Jul 26, 2016 R19277      bsteffen    Convert RECT navigation to an equidistant cylindrical projection.
 * Sep 23, 2021 8608        mapeters    Handle PDO.traceId changes
 *
 * </pre>
 *
 * @author tlee
 */
public class McidasDecoder extends AbstractDecoder {
    private static final int RADIUS = 6_371_200;

    private static final int SIZE_OF_AREA = 256;

    private static final double PI = 3.14159265;

    private static final double HALFPI = PI / 2.;

    private static final double RTD = 180. / PI;

    private static final double DTR = PI / 180.;

    private static final String traceId = "";

    private McidasDao dao;

    private final McidasRecord mr = new McidasRecord();

    public PluginDataObject[] decode(byte[] data, Headers headers)
            throws Exception {
        int endian = 0;
        byte[] area = null;
        byte[] nonAreaBlock = new byte[data.length - SIZE_OF_AREA];
        McidasRecord record = new McidasRecord();
        // String areaName = null;

        /*
         * Separate area file and non-area block.
         */
        record.setSizeRecords(data.length);

        area = new byte[SIZE_OF_AREA];
        System.arraycopy(data, 0, area, 0, SIZE_OF_AREA);
        System.arraycopy(data, SIZE_OF_AREA, nonAreaBlock, 0,
                nonAreaBlock.length);

        /*
         * First word contains all zero for a valid record
         */
        if (byteArrayToInt(area, 0, 0) == 0) {

            /*
             * Check endians for 2nd word, if it is not 4, i.e., big endian,
             * swapping bytes.
             */
            if (byteArrayToInt(area, 4, 0) != 4) {
                endian = 1;
            }

            /*
             * Satellite identification number (SID)
             */
            String sid = byteArrayToInt(area, 8, endian).toString();
            record.setSatelliteId(sid);

            /*
             * Nominal year and Julian day
             */
            int yyddd = byteArrayToInt(area, 12, endian);

            /*
             * Nominal image time
             */
            int hhmmss = byteArrayToInt(area, 16, endian);

            /*
             * Set nominal time as data time and set seconds/millid
             */
            Calendar cal = convertJulianToCalendar(yyddd, hhmmss);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            record.setDataTime(new DataTime(cal));

            /*
             * Upper-left line in satellite coordinates
             */
            int ulline = byteArrayToInt(area, 20, endian);

            /*
             * Upper-left element in satellite coordinates
             */
            int ulelem = byteArrayToInt(area, 24, endian);

            /*
             * Number of lines in y-axis
             */
            Integer ny = byteArrayToInt(area, 32, endian);

            /*
             * Number of pixels in x-axis
             */
            Integer nx = byteArrayToInt(area, 36, endian);

            /*
             * Number of bytes each element (1, 2 or 4) int zdim =
             * byteArrayToInt (area, 40, endian);
             */

            /*
             * Line resolution
             */
            int yres = byteArrayToInt(area, 44, endian);

            /*
             * Element (pixel) resolution
             */
            int xres = byteArrayToInt(area, 48, endian);

            /*
             * Maximum number of bands per scan line int zres = byteArrayToInt
             * (area, 52, endian);
             */

            /*
             * Length of the data block line prefix
             */
            int prefix = byteArrayToInt(area, 56, endian);
            record.setPrefix(prefix);

            /*
             * User project number under which the area is created int task =
             * byteArrayToInt (area, 60, endian);
             */

            /*
             * Get and set the area creation time
             */
            yyddd = byteArrayToInt(area, 64, endian);
            hhmmss = byteArrayToInt(area, 68, endian);

            if (hhmmss != 0) {
                cal = convertJulianToCalendar(yyddd, hhmmss);
            }
            record.setCreationTime(cal);

            /*
             * Get and set image type, e.g., VIS, IR, IR2 from satellite name
             * and image type number
             */
            String imageTypeId = byteArrayToInt(area, 72, endian).toString();

            record.setImageTypeId(imageTypeId);

            String areaId = byteArrayToInt(area, 128, endian).toString();
            /*
             * Set the area ID.
             */

            record.setAreaId(areaId);
            String fileName = "";
            if (headers != null) {

                File ingestFile = new File(
                        (String) headers.get(DecoderTools.INGEST_FILE_NAME));
                fileName = ingestFile.getName();
            }
            record.setInputFileName(fileName);

            /*
             * Data offset: byte offset to the start of the data block
             */
            int dataoff = byteArrayToInt(area, 132, endian);
            byte[] header = new byte[dataoff];
            record.setHeaderBlock(header);

            /*
             * Navigation offset: byte offset to the start of the navigation
             * block
             */
            int navoff = byteArrayToInt(area, 136, endian);

            /*
             * Validity code: if these bytes are non-zero, they must match the
             * first four bytes of each DATA block line prefix or the line's
             * data is ignored.
             */
            int validcode = byteArrayToInt(area, 140, endian);
            record.setValidCode(validcode);

            /*
             * Get and set actual image scan time
             */
            yyddd = byteArrayToInt(area, 180, endian);
            hhmmss = byteArrayToInt(area, 184, endian);

            if (hhmmss != 0) {
                cal = convertJulianToCalendar(yyddd, hhmmss);
            }
            record.setImageTime(cal);

            /*
             * Actual starting scan int ascan = byteArrayToInt (area, 188,
             * endian);
             *
             * Line prefix documentation section length in bytes int predoc =
             * byteArrayToInt (area, 192, endian);
             *
             * Line prefix calibration section length in bytes int precal =
             * byteArrayToInt (area, 196, endian);
             *
             * Line prefix level map section length in bytes int prelvl =
             * byteArrayToInt (area, 200, endian);
             *
             * Image source type: "VISR', "VAS', 'AAA', ERBE', "AVHR' String
             * srctyp = byteArrayToString (area, 204, endian);
             */

            /*
             * Calibration type: 'RAW', "TEMP', 'BRIT'
             */
            String caltyp = byteArrayToString(area, 208, endian);
            record.setCalType(caltyp);

            /*
             * Processing type int prctyp = byteArrayToInt (area, 212, endian);
             *
             * POES signal type int sigtyp = byteArrayToInt (area, 216, endian);
             *
             * POES ascending/descending int phase = byteArrayToInt (area, 220,
             * endian);
             *
             * Original source srctyp String orgtyp = byteArrayToString (area,
             * 224, endian);
             */

            /*
             * Byte offset to the start of the calibration block
             */
            int offcal = byteArrayToInt(area, 248, endian);

            /*
             * Number of card image comments
             */
            int icomment = byteArrayToInt(area, 252, endian);

            /*
             * Create navigation block
             */
            int navsize;
            if (offcal == 0) {
                navsize = dataoff - navoff;
            } else {
                navsize = offcal - navoff;
            }
            byte[] navigation = new byte[navsize];
            System.arraycopy(nonAreaBlock, 0, navigation, 0, navsize);

            /*
             * Set data block.
             */
            byte[] imageData = new byte[data.length - dataoff
                    - (80 * icomment)];
            System.arraycopy(data, dataoff, imageData, 0, imageData.length);
            record.setMessageData(imageData);

            /*
             * Projection type
             */
            String navtyp = byteArrayToString(navigation, 0, endian);

            /*
             * For map coverage compliance: 1: Mecator (MERC), 3: Lamber
             * Conformal (LAMB), 5: Polar Steoreographic (PS)
             */
            int resolution = 0;
            Integer iproj = 0;
            if ("PS".equals(navtyp.trim()) || "MERC".equals(navtyp)
                    || "MET".equals(navtyp.trim())) {
                resolution = byteArrayToInt(navigation, 16, endian) / 1000;

                if ("MERC".equals(navtyp)) {
                    iproj = 1;
                } else if ("PS".equals(navtyp.trim())) {
                    iproj = 5;
                }
            } else if ("LAMB".equals(navtyp) || "TANC".equals(navtyp)) {
                resolution = byteArrayToInt(navigation, 20, endian) / 1000;
                if ("TANC".equals(navtyp)) {
                    resolution = resolution / 1000;
                }
                iproj = 3;
            } else if ("RECT".equals(navtyp)) {
                resolution = (yres < xres) ? yres : xres;
                iproj = 7;
            } else {
                // native satellite projections ( not remapped )
                iproj = 7585;
                resolution = (yres < xres) ? yres : xres;
            }
            record.setResolution(resolution);

            /*
             * Create map coverage. n4: standard latitude or spacing for TANC
             */
            int n4 = byteArrayToInt(navigation, 12, endian);
            int angdd = n4 / 10000;
            int angmm = (n4 / 100) - (angdd * 100);
            int angss = n4 - (angdd * 10000) - (angmm * 100);
            Float stdlat1 = (float) (angdd + (angmm / 60.) + (angss / 3600.));
            Float stdlat2 = stdlat1;
            double phi0r = stdlat1 * DTR;
            double sign = 1.;
            if (phi0r < 0.) {
                sign = -1.;
            }

            /*
             * Central longitude
             */
            int n6 = byteArrayToInt(navigation, 20, endian);
            angdd = n6 / 10000;
            angmm = (n6 / 100) - (angdd * 100);
            angss = n6 - (angdd * 10000) - (angmm * 100);
            Float clon = (float) (angdd + (angmm / 60.) + (angss / 3600.));
            if (byteArrayToInt(navigation, 36, endian) >= 0) {
                clon = -clon;
            }

            /*
             * Set pixel/grid spacing and earth radius and eccentricity. For
             * TANC, n5 is the standard latitude.
             */
            int spacing = byteArrayToInt(navigation, 16, endian);
            Float dx = (float) (spacing * xres);

            /*
             * Earth radius
             */
            int re = byteArrayToInt(navigation, 24, endian);

            /*
             * Eccentricity int n8 = byteArrayToInt (navigation, 28, endian);
             * double ecc = n8 / 1000000.;
             */

            /*
             * Image y-coordinate of north pole
             */
            int n2 = byteArrayToInt(navigation, 4, endian);

            /*
             * Image x-coordinate of north pole
             */
            int n3 = byteArrayToInt(navigation, 8, endian);

            /*
             * location of pole point (rxp, ryp); (1,1) at lower-left corner.
             */
            double rxp = (((double) (n3 - ulelem) / xres) + 1.);
            double ryp = (ny - ((double) (n2 - ulline) / yres));

            /*
             * Polar steoreographic projection (PS)
             */
            Float dy = 0.f;
            Float lllat = 0.f, lllon = 0.f, urlat = 0.f, urlon = 0.f;
            String proj = null;
            if (iproj == 5) {
                proj = "STR";
                dy = (float) spacing * yres;

                /*
                 * Compute lat/lon of the lower-left corner
                 */
                double dxp = (1. - rxp) * dx;
                double dyp = (1. - ryp) * dy;
                double alpha = 1. + Math.sin(Math.abs(phi0r));
                double rm = Math.sqrt(((dxp * dxp) + (dyp * dyp))) / alpha;
                lllat = (float) (sign * ((HALFPI - (2. * Math.atan(rm / re))))
                        * RTD);
                double thta;
                if (dyp != 0) {
                    dyp = (-dyp) * sign;
                    thta = (Math.atan2(dxp, dyp)) * RTD;
                    lllon = prnlon((float) (clon + thta));
                } else {
                    lllon = (float) clon;
                }

                /*
                 * Compute lat/lon of the upper-right corner
                 */
                dxp = (nx - rxp) * dx;
                dyp = (ny - ryp) * dy;
                rm = Math.sqrt(((dxp * dxp) + (dyp * dyp))) / alpha;
                urlat = (float) (sign * ((HALFPI - (2. * Math.atan(rm / re))))
                        * RTD);

                if (dyp != 0) {
                    dyp = (-dyp) * sign;
                    thta = (Math.atan2(dxp, dyp)) * RTD;
                    urlon = prnlon((float) (clon + thta));
                } else {
                    urlon = (float) clon;
                }
            }

            /*
             * Mercator projection
             */
            else if (iproj == 1) {
                proj = "MER";
                dy = (float) spacing * yres;

                /*
                 * compute lat/lon of the lower-left corner
                 */
                double dxp = 1. - rxp;
                double dyp = 1. - ryp;
                double rm = dx * dyp;
                double rcos = re * Math.cos(phi0r);
                double arg = Math.exp(rm / rcos);
                lllat = (float) (((2. * Math.atan(arg)) - HALFPI) * RTD);
                lllon = prnlon((float) (clon + (((dx * dxp) / rcos) * RTD)));

                /*
                 * compute lat/lon of the upper-right corner
                 */
                dxp = nx - rxp;
                dyp = ny - ryp;
                rm = dx * dyp;
                arg = Math.exp(rm / rcos);
                urlat = (float) (((2. * Math.atan(arg)) - HALFPI) * RTD);
                urlon = prnlon((float) (clon + (((dx * dxp) / rcos) * RTD)));
                urlon = prnlon(urlon);

                /*
                 * Lamber conformal conic projection (LAMB)
                 */
            } else if (iproj == 3) {
                if (stdlat1 > 0) {
                    proj = "LCC";
                } else {
                    proj = "SCC";
                }

                if ("LAMB".equals(navtyp)) {
                    // earth radius
                    re = byteArrayToInt(navigation, 28, endian);

                    if (re <= 6200000.) {
                        re = RADIUS;
                    }

                    /*
                     * Standard latitude 2
                     */
                    int n5 = byteArrayToInt(navigation, 16, endian);
                    angdd = n5 / 10000;
                    angmm = (n5 / 100) - (angdd * 100);
                    angss = n5 - (angdd * 10000) - (angmm * 100);
                    stdlat2 = (float) (angdd + (angmm / 60.) + (angss / 3600.));

                    /*
                     * Central longitude. If west positive, make west negative.
                     */
                    int n7 = byteArrayToInt(navigation, 24, endian);
                    angdd = n7 / 10000;
                    angmm = (n7 / 100) - (angdd * 100);
                    angss = n7 - (angdd * 10000) - (angmm * 100);
                    clon = (float) (angdd + (angmm / 60.) + (angss / 3600.));

                    if (byteArrayToInt(navigation, 36, endian) >= 0) {
                        clon = -clon;
                    }

                    /*
                     * compute pixel/grid spacing and colatitudes
                     */
                    n6 = byteArrayToInt(navigation, 20, endian);
                    dx = (float) n6 * xres;
                    dy = (float) n6 * yres;
                } else if ("TANC".equals(navtyp)) {

                    /*
                     * McIDAS uses Earth Radius 6371.1 (RADIUS). Navigation
                     * block km per pixel scaled by 10000., convert to meters
                     */
                    re = RADIUS;
                    dx = ((n4 / 10000.f) * xres) * 1000.f;
                    dy = ((n4 / 10000.f) * yres) * 1000.f;
                    rxp = ((((n3 / 10000.) - ulelem) / xres) + 1.);
                    ryp = (ny - (((n2 / 10000.) - ulline) / yres));

                    /*
                     * Standard angles are in decimal degree for TANC only
                     */
                    int n5 = byteArrayToInt(navigation, 16, endian);
                    stdlat1 = n5 / 10000.f;
                    stdlat2 = stdlat1;
                    phi0r = stdlat1 * DTR;
                    if (phi0r < 0.) {
                        sign = -1.;
                        proj = "SCC";
                    } else {
                        proj = "LCC";
                    }
                }

                /*
                 * compute pixel/grid spacing and colatitude.
                 */
                double psi1 = HALFPI - (Math.abs(stdlat1) * DTR);
                double psi2 = HALFPI - (Math.abs(stdlat2) * DTR);

                /*
                 * compute cone constant
                 */
                double ccone;
                if (psi1 == psi2) {
                    ccone = Math.cos(psi1);
                } else {
                    double tmp1 = Math.log(Math.sin(psi2) / Math.sin(psi1));
                    double tmp2 = Math
                            .log(Math.tan(psi2 / 2.) / Math.tan(psi1 / 2.));
                    ccone = tmp1 / tmp2;
                }

                /*
                 * Compute lat/lon of the lower-left corner. Sing = 1/-1 denotes
                 * NH/SH
                 */
                double dxp = 1. - rxp;
                double dyp = 1. - ryp;
                double rm = dx * Math.sqrt(((dxp * dxp) + (dyp * dyp)));
                double tmp = ccone / (re * Math.sin(psi1));
                double arg = Math.pow(rm * tmp, 1. / ccone)
                        * Math.tan(psi1 / 2.);
                lllat = (float) (sign * (HALFPI - (2. * Math.atan(arg))) * RTD);

                double thta;
                if (dyp != 0) {
                    dyp = -dyp;
                    thta = ((Math.atan2(dxp, dyp)) * RTD) / ccone;
                    lllon = prnlon((float) (clon + thta));
                } else {
                    lllon = (float) clon;
                }

                /*
                 * compute lat/lon of the upper-right corner
                 */
                dxp = nx - rxp;
                dyp = ny - ryp;
                rm = dx * Math.sqrt(((dxp * dxp) + (dyp * dyp)));
                arg = Math.pow(rm * tmp, 1. / ccone) * Math.tan(psi1 / 2.);
                urlat = (float) (sign * ((HALFPI - (2. * Math.atan(arg))))
                        * RTD);

                if (dyp != 0) {
                    dyp = -dyp;
                    thta = ((Math.atan2(dxp, dyp)) * RTD) / ccone;
                    urlon = (float) (clon + thta);
                    urlon = prnlon(urlon);
                } else {
                    urlon = (float) clon;
                }
            } else if (iproj == 7) {
                proj = navtyp;

                stdlat1 = stdlat2 = 0.0f;

                /*
                 * Everything is encoded as an int but for most of these values
                 * we want a decimal value so it is encoded with two ints, one
                 * as a base value and another is a scale factor.
                 */
                int anchor_row = byteArrayToInt(navigation, 4, endian);
                int unscaled_anchor_lat = byteArrayToInt(navigation, 8, endian);
                int anchor_col = byteArrayToInt(navigation, 12, endian);
                int unscaled_anchor_lon = byteArrayToInt(navigation, 16,
                        endian);
                int unscaled_lat_spacing = byteArrayToInt(navigation, 20,
                        endian);
                int unscaled_lon_spacing = byteArrayToInt(navigation, 24,
                        endian);
                int unscaled_earth_radius = byteArrayToInt(navigation, 28,
                        endian);
                int swap_lon_sign = byteArrayToInt(navigation, 40, endian);

                int anchor_lat_scale = byteArrayToInt(navigation, 44, endian);
                int anchor_lon_scale = byteArrayToInt(navigation, 48, endian);
                int lat_spacing_scale = byteArrayToInt(navigation, 52, endian);
                int lon_spacing_scale = byteArrayToInt(navigation, 56, endian);
                int earth_radius_scale = byteArrayToInt(navigation, 60, endian);

                /*
                 * Each value has a different default scale value that is used
                 * if the scale is set to 0.
                 */
                if (anchor_lat_scale == 0) {
                    anchor_lat_scale = 4;
                }
                if (anchor_lon_scale == 0) {
                    anchor_lon_scale = 4;
                }
                if (lat_spacing_scale == 0) {
                    lat_spacing_scale = 4;
                }
                if (lon_spacing_scale == 0) {
                    lon_spacing_scale = 4;
                }
                if (earth_radius_scale == 0) {
                    earth_radius_scale = 3;
                }

                double anchor_lat = unscaled_anchor_lat
                        / Math.pow(10.0, anchor_lat_scale);
                double anchor_lon = unscaled_anchor_lon
                        / Math.pow(10.0, anchor_lon_scale);

                /* This dx/dy is in degree spacing */
                dx = (float) (unscaled_lat_spacing
                        / Math.pow(10.0, lat_spacing_scale));
                dy = (float) (unscaled_lon_spacing
                        / Math.pow(10.0, lon_spacing_scale));

                if (swap_lon_sign >= 0) {
                    dx = -1 * dx;
                    anchor_lon = -1 * anchor_lon;
                }

                urlon = (float) (anchor_lon + (anchor_col - ulelem) * dx);
                urlat = (float) (anchor_lat + (anchor_row - ulline) * dy);
                lllon = urlon - nx * dx;
                lllat = urlat - ny * dy;

                clon = (lllon + urlon) / 2;

                double scaled_re_in_km = unscaled_earth_radius
                        / Math.pow(10.0, earth_radius_scale);
                re = (int) (scaled_re_in_km) * 1000;

                /*
                 * Need to recalculate dx/dy in a meter spacing since it will be
                 * used with an Equidistant Cylindrical CRS.
                 */
                DefaultEllipsoid ellipsoid = DefaultEllipsoid
                        .createEllipsoid("Mcidas", re, re, SI.METRE);
                dx = (float) ellipsoid.orthodromicDistance(clon, 0, clon + dx,
                        0);
                dy = (float) ellipsoid.orthodromicDistance(clon, 0, clon, dy);

            } else if (iproj == 7585) {
                // native satellite projections ( not remapped )
                proj = navtyp;
                int ilonrad = byteArrayToInt(navigation, 20, endian);
                clon = ilonrad / 10000000.f;
                clon = (float) Math.toDegrees(clon);

            }
            record.setProjection(proj);
            record.setOverwriteAllowed(true);

            /*
             * Create map coverage.
             */
            McidasMapCoverage mapCoverage = null;
            try {
                if (iproj <= 7) {
                    mapCoverage = McidasSpatialFactory.getInstance()
                            .getMapCoverage(iproj, nx, ny, dx, dy, clon,
                                    stdlat1, stdlat2, lllat, lllon, urlat,
                                    urlon, re);
                } else {
                    // non-remapped Navigations
                    mapCoverage = McidasSpatialFactory.getInstance()
                            .getMapCoverage(iproj, nx, ny, clon, ulelem, ulline,
                                    xres, yres, navigation);
                }
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                if (iproj <= 7) {
                    sb.append(
                            "Error getting or constructing SatMapCoverage for values: ")
                            .append("\n\t");
                    sb.append("mapProjection=").append(iproj).append("\n\t");
                    sb.append("nx=").append(nx).append("\n\t");
                    sb.append("ny=").append(ny).append("\n\t");
                    sb.append("dx=").append(dx).append("\n\t");
                    sb.append("dy=").append(dy).append("\n\t");
                    sb.append("clon=").append(clon).append("\n\t");
                    sb.append("stdlat1=").append(stdlat1).append("\n\t");
                    sb.append("stdlat2=").append(stdlat2).append("\n\t");
                    sb.append("la1=").append(lllat).append("\n\t");
                    sb.append("lo1=").append(lllon).append("\n\t");
                    sb.append("la2=").append(urlat).append("\n\t");
                    sb.append("lo2=").append(urlon).append("\n");
                } else {
                    sb.append(
                            "Error getting or constructing SatMapCoverage for Navigation Type ")
                            .append(proj).append("\n");
                }
                throw new DecoderException(sb.toString(), e);
            }

            record.setReportType("mcidas");
            if (record != null) {
                record.setSourceTraceId(traceId);
                record.setCoverage(mapCoverage);
                record.setPersistenceTime(TimeTools.getSystemCalendar());

            }
            return new PluginDataObject[] { record };
        } else {
            return new PluginDataObject[0];
        }
    }

    /**
     * Convert from a Julian date to a Gregorian date
     *
     * @param julian
     *            The julian date
     * @return The Calendar
     */

    private Calendar convertJulianToCalendar(int julian, int hhmmss) {

        /*
         * The Julian day format is nYYDDD where n = 1, year > 2000 and n = 0,
         * year is prior to 2001. For example, 109244 -> 2009/244.
         */
        if (julian > 100000) {
            julian = julian + 1900000;
        } else {
            julian = julian + 1800000;
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.YEAR, julian / 1000);
        cal.set(Calendar.DAY_OF_YEAR, julian - ((julian / 1000) * 1000));

        int ihour = hhmmss / 10000;
        cal.set(Calendar.HOUR_OF_DAY, ihour);
        int minute = (hhmmss - (ihour * 10000)) / 100;
        cal.set(Calendar.MINUTE, minute);
        int second = hhmmss - (ihour * 10000) - (minute * 100);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b
     *            The byte array
     * @param offset
     *            The array offset
     * @param endian
     *            endian flag
     * @return The Integer
     */
    public Integer byteArrayToInt(byte[] b, int offset, int endian) {
        Integer value = 0;

        // little endian (MSB at the highest memory address)
        if (endian == 0) {
            for (int i = 0; i < 4; i++) {
                int shift = (3 - i) * 8;
                value += (b[i + offset] & 0x000000FF) << shift;
            }
        }

        // big endian (MSB at the lowest memory address)
        else if (endian == 1) {
            for (int i = 0; i < 4; i++) {
                int shift = i * 8;
                value += (b[i + offset] & 0x000000FF) << shift;
            }
        } else {
            logger.error("Illegal endian input: " + endian);
        }
        return value;
    }

    /**
     * Convert the byte array to a string starting from the given offset.
     *
     * @param b
     *            The byte array
     * @param offset
     *            The array offset
     * @param endian
     *            little and big endian flag
     * @return The integer
     */
    public String byteArrayToString(byte[] b, int offset, int endian) {
        String str = null;

        /*
         * little endian (MSB at the highest memory address)
         */
        if (endian == 0) {
            byte[] byteArray = new byte[] { b[offset + 3], b[offset + 2],
                    b[offset + 1], b[offset + 0] };
            str = new String(byteArray);
        }

        /*
         * big endian (MSB at the lowest memory address)
         */
        else if (endian == 1) {
            byte[] byteArray = new byte[] { b[offset + 0], b[offset + 1],
                    b[offset + 2], b[offset + 3] };
            str = new String(byteArray);
        } else {
            logger.error("Illegal endian input: " + endian);
        }
        return str;
    }

    /**
     * Convert a longitude in degrees which fall within the range -180 to 180.
     *
     * @param lon
     * @return
     */
    public float prnlon(float lon) {
        float dlon = lon - ((int) (lon / 360.f) * 360.f);
        if (lon < -180.) {
            dlon = lon + 360.f;
        } else if (lon > 180.) {
            dlon = (float) (lon - 360.);
        }
        return dlon;
    }

    public McidasDao getDao() {
        return dao;
    }

    public void setDao(McidasDao dao) {
        this.dao = dao;
    }

    /**
     * @param data
     * @return
     * @throws Exception
     */

    public PluginDataObject[] decodeFile(File inputFile) throws Exception {
        byte[] fileData = null;
        InputStream is = null;
        try {
            try {
                is = new FileInputStream(inputFile);

                fileData = new byte[(int) inputFile.length()];
                int bytesRead = is.read(fileData);
                // If we didn't or couldn't read all the data, signal the
                // fact by setting the data to null;
                if (bytesRead != fileData.length) {
                    fileData = null;
                }
                fileName = inputFile.getName();
                mr.setInputFileName(fileName);
            } catch (IOException ioe) {
                logger.error("Error reading input file " + inputFile.getName(),
                        ioe);
                fileData = null;
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    logger.error(
                            "Could not close input file " + inputFile.getName(),
                            ioe);
                }
            }
        }
        return decode(fileData, null);
    }
}
