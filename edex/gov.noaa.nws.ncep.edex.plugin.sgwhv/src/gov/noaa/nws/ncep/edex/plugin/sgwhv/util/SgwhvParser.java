/**
 * 
 * Parser for significant wave height data
 * 
 * SOFTWARE HISTORSgwhvParserY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 08/23/11                 Chin J Chen Initial coding from BufrSgwhvParser
 * May 14, 2014 2536        bclement    removed TimeTools usage
 * 12/29/2016   R11514      A. Su       Recognized 5 descriptors of CryoSat-2 SIRAL altimeter
 *                                      (0 5 1), (0 6 1), (0 10 81), (0 22 156), & (0 21 93)
 *                                      to be assigned to a sgwhv record.
 * 
 * </pre>
 * 
 * @author Chin J. Chen
 * @version 1.0
 */

package gov.noaa.nws.ncep.edex.plugin.sgwhv.util;

import java.util.Calendar;
import java.util.List;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.bufrtools.BUFRDataDocument;
import com.raytheon.uf.edex.bufrtools.descriptors.BUFRDescriptor;
import com.raytheon.uf.edex.bufrtools.packets.IBUFRDataPacket;

import gov.noaa.nws.ncep.common.dataplugin.sgwhv.SgwhvRecord;
import gov.noaa.nws.ncep.edex.plugin.sgwhv.decoder.SgwhvSeparator;

public class SgwhvParser {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SgwhvParser.class);

    /**
     * process BUFR SGWHV.
     * 
     * @param sep
     *            the BufrSgwhv separator
     */
    public static SgwhvRecord processSgwhv(SgwhvSeparator sep, int subsetNum) {
        int year = -1;
        int month = -1;
        int day = -1;
        int hour = -1;
        int min = -1;
        int sec = -1;
        SgwhvRecord sgwhvRec = null;
        BUFRDataDocument record = (BUFRDataDocument) sep.next();
        if (record != null) {
            List<IBUFRDataPacket> parameterList = record.getList();
            if (parameterList != null) {
                sgwhvRec = new SgwhvRecord();
                for (IBUFRDataPacket dataPkg : parameterList) {
                    int descriptor = dataPkg.getReferencingDescriptor()
                            .getDescriptor();
                    if (descriptor == BUFRDescriptor.createDescriptor(0, 1,
                            7)) {
                        if (dataPkg.getValue() != null) {

                            Long satelliteId = (Long) dataPkg.getValue();
                            sgwhvRec.setSatelliteId(satelliteId);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            4, 1)) {
                        year = ((Double) dataPkg.getValue()).intValue();
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            4, 2)) {
                        month = ((Double) dataPkg.getValue()).intValue();
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            4, 3)) {
                        day = ((Double) dataPkg.getValue()).intValue();
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            4, 4)) {
                        hour = ((Double) dataPkg.getValue()).intValue();
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            4, 5)) {
                        min = ((Double) dataPkg.getValue()).intValue();
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            4, 6)) {
                        if (dataPkg.getValue() != null) {
                            sec = ((Double) dataPkg.getValue()).intValue();
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            5, 1)) {
                        if (dataPkg.getValue() != null) {
                            sgwhvRec.setLat(((Double) dataPkg.getValue()));
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            5, 2)) {
                        if (dataPkg.getValue() != null) {
                            sgwhvRec.setLat(((Double) dataPkg.getValue()));
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            6, 1)) {
                        if (dataPkg.getValue() != null) {
                            sgwhvRec.setLon(((Double) dataPkg.getValue()));
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            6, 2)) {
                        if (dataPkg.getValue() != null) {
                            sgwhvRec.setLon(((Double) dataPkg.getValue()));
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            11, 12)) {
                        if (dataPkg.getValue() != null) {
                            Double wspd10 = (Double) dataPkg.getValue();
                            sgwhvRec.setWspd10(wspd10);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            22, 21)) {
                        if (dataPkg.getValue() != null) {
                            Double htwaves = (Double) dataPkg.getValue();
                            sgwhvRec.setHtwaves(htwaves);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            22, 156)) {
                        if (dataPkg.getValue() != null) {
                            Double htwaves = (Double) dataPkg.getValue();
                            sgwhvRec.setHtwaves(htwaves);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            22, 26)) {
                        if (dataPkg.getValue() != null) {
                            Double sgwhSd = (Double) dataPkg.getValue();
                            sgwhvRec.setSgwhSd(sgwhSd);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            10, 33)) {
                        if (dataPkg.getValue() != null) {
                            Double altitude = (Double) dataPkg.getValue();
                            sgwhvRec.setAltitude(altitude);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            10, 81)) {
                        if (dataPkg.getValue() != null) {
                            Double altitude = (Double) dataPkg.getValue();
                            sgwhvRec.setAltitude(altitude);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 71)) {
                        if (dataPkg.getValue() != null) {
                            Long peak = (Long) dataPkg.getValue();
                            sgwhvRec.setPeak(peak);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 93)) {
                        if (dataPkg.getValue() != null) {
                            Long peak = (Long) dataPkg.getValue();
                            sgwhvRec.setPeak(peak);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 77)) {
                        if (dataPkg.getValue() != null) {
                            Double altCorrI = (Double) dataPkg.getValue();
                            sgwhvRec.setAltCorrI(altCorrI);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 78)) {
                        if (dataPkg.getValue() != null) {
                            Double altCorrD = (Double) dataPkg.getValue();
                            sgwhvRec.setAltCorrD(altCorrD);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 79)) {
                        if (dataPkg.getValue() != null) {
                            Double altCorrW = (Double) dataPkg.getValue();
                            sgwhvRec.setAltCorrW(altCorrW);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 82)) {
                        if (dataPkg.getValue() != null) {
                            Double loopCorr = (Double) dataPkg.getValue();
                            sgwhvRec.setLoopCorr(loopCorr);
                        }
                    } else if (descriptor == BUFRDescriptor.createDescriptor(0,
                            21, 62)) {
                        if (dataPkg.getValue() != null) {
                            Double bkst = (Double) dataPkg.getValue();
                            sgwhvRec.setBkst(bkst);
                        }
                    }
                }

                /*
                 * Create time stamp.
                 */
                if ((year > 0) && (month > 0) && (day > 0) && (hour >= 0)) {
                    Calendar baseTime = TimeUtil.newGmtCalendar(year, month,
                            day);
                    baseTime.set(Calendar.HOUR_OF_DAY, hour);
                    baseTime.set(Calendar.MINUTE, min);
                    baseTime.set(Calendar.SECOND, sec);
                    Calendar obstime = (Calendar) baseTime.clone();
                    sgwhvRec.setObsTime(obstime);
                    DataTime dt = new DataTime(obstime);
                    sgwhvRec.setDataTime(dt);
                } else {
                    statusHandler.handle(Priority.WARN,
                            "Cannot create a proper time stamp");
                }
            } else {
                statusHandler.handle(Priority.WARN,
                        " There is no data in bulletin ");
            }
        }
        return sgwhvRec;
    }

}
