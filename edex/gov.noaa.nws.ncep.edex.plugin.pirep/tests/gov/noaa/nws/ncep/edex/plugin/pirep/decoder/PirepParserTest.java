package gov.noaa.nws.ncep.edex.plugin.pirep.decoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.edex.decodertools.core.BasePoint;

/**
 * Main to test Pirep Parser
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 21, 2020 8191       randerso    Moved out of PirepParser.
 *
 * </pre>
 *
 * @author jkorman
 */
public class PirepParserTest {
    private static final Logger logger = LoggerFactory
            .getLogger(PirepParserTest.class);

    public static final void main(String[] args) {
        // test1: latlon format
        String[] latlons = { "0000N 00000W", "0000S 00000E", "9000S 00000W",
                "9000N 00000W", "0000N 09000W", "9000S 09000W", "9000N 09000W",

                "0000N 09000W", "4500S 09000W", "9000N 09000W",

                "9000N 09959W", "0000N 10000W",

                "4500S 09000W", "9000N 09000W",

                "90N 18000E", // no match
                "9000S 18000E", "9000N 18000W", "9000S 18000W", "90N 179W",
                "9000S 17959W", };

        for (int i = 0; i < latlons.length; i++) {
            String[] split = latlons[i].split(" ");
            if (split != null && split.length == 2) {
                latlons[i] = split[0] + split[1];
            }
            logger.debug("latlons[i] " + latlons[i]);
        }

        Pattern p = Pattern.compile(PirepParser.LATLON_PTRN);

        for (String s : latlons) {
            Matcher m = p.matcher(s);
            if (m.find()) {
                BasePoint b = PirepParser.parseLatLon(m.group());
                if (b != null) {
                    logger.debug(String.format("%16s %10.6f %11.6f", s,
                            b.getLatitude(), b.getLongitude()));
                } else {
                    logger.debug("Invalid parse " + s);
                }
            } else {
                logger.debug("no match for " + s);
            }
        }

        // test2 replace "[\r\n]", " "
        String str = "123 123  123 \r SCT";
        str = str.replaceAll("[\r\n]", " ");
        str = str.replaceAll(" {2,}", " ");
        logger.debug("[" + str + "]");

        // test3: parse SK. BKN pased in first regex
        String inputStr = "BKN-SCT046-TOPUNKN/OVC089/ SKC";
        String[] string = inputStr.split("/");
        for (String mystr : string) {

            mystr = mystr.trim();

            String regex = "([A-Z]{3}|UNKN?)(?:-([A-Z]{3}))?";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(mystr);

            if (matcher.find()) {
                String cloud_1 = matcher.group(1);
                String cloud_2 = matcher.group(2);
                logger.debug("FFFF " + cloud_1 + " " + cloud_2);

            }

            regex = "([0-9]{3}|UNKN?)(?:-TOP([0-9]{3}|UNKN?))?";
            pattern = Pattern.compile(regex);
            matcher = pattern.matcher(mystr);

            if (matcher.find()) {

                String baseHeight = matcher.group(1);
                String topHeight = matcher.group(2);
                logger.debug("DDDD " + baseHeight + " " + topHeight);

            }
        }

        // test4: TB
        String str1 = "SEV/LGT/MOD";
        // "INTMT MOD-SEV ABV 014"; //"TB MOD-SEV
        // CHOP 220/NEG BLO 095";
        // String strIc = "MOD RIME BLO 095";
        String[] str2 = str1.split("/");

        for (String strTmp : str2) {
            PirepTools tools = new PirepTools(strTmp);
            tools.decodeTurbulenceData();
        }
//          if (str.indexOf(NEG_ENTRY) > 0) {
//                AircraftFlightCondition at = new AircraftFlightCondition();
//
//                // NEG should be the only value! Used to indicate forecasted but not observed!
//                at.setIntensity1(NEG_ENTRY);
////                if (flightLevel != null) {
////                    at.setBaseHeight(flightLevel.getFlightLevel());
////                    theTurbulenceLayers.add(at);
////                }
//            } else {
//                pattern = Pattern.compile(TRB_REGEX);
//                // Get a Matcher based on the target string.
//                matcher = pattern.matcher(str);
//                // add each turbulence
//
//                // decodeFlightLevelData()
//                AircraftFlightLevel flightLevel = null;
//                String regex1 = "([0-9]{3})(?:-([0-9]{3}))?";
//                Pattern pattern1 = Pattern.compile(regex1);
//
//                // Get a Matcher based on the target string.
//                Matcher matcher1 = pattern1.matcher(str);
//                if (matcher1.find()) {
//                    Integer fltLevel = decodeHeight(matcher1.group(1));
//
//                    if (matcher1.group(2) != null) {
//                        Integer upperLevel = decodeHeight(matcher1.group(2));
//
//                        fltLevel = (fltLevel + upperLevel) / 2;
//                    }
//                    flightLevel = new AircraftFlightLevel(fltLevel);
//                }
//                // end decodeFlightLevelData()
//                while (matcher.find()) {
//                    //addFlightCondition(matcher, theTurbulenceLayers);
//                  for (int i=0; i<matcher.groupCount(); i++)
//                      System.out.print(matcher.group(i)+" ");
//                  if(matcher.groupCount() >= 13) {
//                        String s1 = matcher.group(1);
//                        String s2 = matcher.group(3);
//                        // Some words that may show up in group 1 or 3 that need to be
//                        // thrown away!
//                        s1 = FL_COND_WORDS.get(s1);
//                        //if (s1.equalsIgnoreCase(cont, int,ocal))
//                        if((s1 == null) || (NULL_ENTRY.equals(s1))) {
//                            return;
//                        }
//                        // Need to allow s2 to be null,
//                        if(s2 != null) {
//                            // but not null after lookup!
//                            s2 = FL_COND_WORDS.get(s2);
//                            if((s2 == null) || (NULL_ENTRY.equals(s2))) {
//                                return;
//                            }
//                        }
//                        //******************************************************************
//
//                        AircraftFlightCondition at = new AircraftFlightCondition();
//
//                        // NEG should be the only value! Used to indicate forecasted but
//                        // not observed!
//                        if(NEG_ENTRY.equals(s1)) {
//                            at.setIntensity1(s1);
//                            if(flightLevel != null) {
//                                at.setBaseHeight(flightLevel.getFlightLevel());
//                            }
//                        } else {
//                            at.setIntensity1(s1);
//                            at.setIntensity2(s2);
//
//                            s1 = matcher.group(5);
//                            s1 = COND_TYPES.get(s1);
//                            if((s1 != null) && (!s1.equals(NULL_ENTRY))) {
//                                at.setType(s1);
//                            }
//
//                            s1 = matcher.group(8);
//                            s2 = matcher.group(9);
//                            if(BLO_HGT.equals(s1)) {
//                                at.setBaseHeight(IDecoderConstantsN.UAIR_INTEGER_MISSING);
//                                at.setTopHeight(decodeHeight(s2));
//
//                            } else if (ABV_HGT.equals(s1)) {
//                                at.setBaseHeight(decodeHeight(s2));
//                                at.setTopHeight(IDecoderConstantsN.UAIR_INTEGER_MISSING);
//                            } else {
//                                // Check for one or more levels
//                                s1 = matcher.group(11);
//                                s2 = matcher.group(13);
//                                if(s1 != null) {
//                                    at.setBaseHeight(decodeHeight(s1));
//                                }
//                                if(s2 != null) {
//                                    at.setTopHeight(decodeHeight(s2));
//                                }
//                                if((s1 != null)&&(s2 != null)) {
//                                    Integer base = at.getBaseHeight();
//                                    Integer top = at.getTopHeight();
//                                    if(base != IDecoderConstantsN.UAIR_INTEGER_MISSING) {
//                                        if(top != IDecoderConstantsN.UAIR_INTEGER_MISSING) {
//                                            if(base > top) {
//                                               // logger.debug(traceId + "- BASE-TOP inversion fixed");
//                                                at.setBaseHeight(top);
//                                                at.setTopHeight(base);
//                                            }
//                                        }
//                                    }
//                                }
//                                if((s1 == null)&&(s2 == null)) {
//                                    // Use the flight level if heights are not specified.
//                                    if(flightLevel != null) {
//                                        at.setBaseHeight(flightLevel.getFlightLevel());
//                                    }
//                                }
//                            }
//                        }
//                       // layers.add(at);
//                    }
//                  //logger.debug(theTurbulenceLayers);
//                }
//            }
//        }

//        String report = "BIG UA /OV BIG095040/TM 2343/FL090/TP SR22/TA M05/IC LGT RIME 090-100/RM CWSU ZAN=";
//        String traceId = "1";
//        new PirepParser(report, traceId);
    }

}
