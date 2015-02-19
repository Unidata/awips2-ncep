/**
 * This software was modified from Raytheon's taf plugin by
 * NOAA/NWS/NCEP/NCO in order to output point data in HDF5.
 **/

package gov.noaa.nws.ncep.common.dataplugin.nctaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Set of tools to parse TAF weather conditions. The public parseWeather() calls
 * various helper methods to properly parse the data following certain rules
 * that are defined in the TAF standard and in NWS coding standards.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 09/09/2011   458			sgurung	    Initial Creation from Raytheon's taf plugin
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */
public class NcTafWeatherTools {

    private static String[] tsTypes = { "TSRA", "TSSN", "TSPL", "TSGS", "TSGR", };

    private static String[] shTypes = { "SHRA", "SHSN", "SHPL", "SHGS", "SHGR", };

    private static String [] wxTypes = { "DZ", "RA", "SN", "SG", "IC", "PL", "GR", "GS", }; 

    private static String[] vcTypes = { "VCTS", "VCSH", "VCFG", };
    
    private static String[] fzTypes = { "FZRA", "FZDZ", "FZFG", };
    
    // Freezing fog will get picked up in the FZ category!
    // Vicinity fog will get picked up in the VC category!
    private static String [] drTypes = { "DRSN", "DRDU", "DRSA", }; 
    
    private static String [] blTypes = { "BLSN", "BLDU", "BLSA", "BLPY", }; 
    
    private static String [] obTypes = { "BR", "MIFG", "PRFG", "BCFG", "FG", "FU", "VA", "DU", "SA", "HZ", "PY", }; 

    private static String [] otTypes = { "PO", "SQ", "FC", "SS", "DS", }; 

    private List<NcTafWxElements> elements = null;

    /**
     * Parse Vicinity weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Vicinity found.
     */
    private boolean doVC(StringBuilder data) {
        
        boolean foundVC = false;
        int posx = -1;
        for(String s : vcTypes) {
            posx = data.indexOf(s);
            if (posx >= 0) {
                foundVC = true;

                String ss = data.substring(posx+2,posx+4);
                data.delete(posx, posx + 4);

                NcTafWxElements e = new NcTafWxElements();
                e.setIntensity("VC");
                if("FG".equals(ss)) {
                    e.setObscuration(ss);
                } else {
                    e.setDescriptor(ss);
                }
                elements.add(e);
                break;
            }
        }
        return foundVC;
    }
    
    /**
     * Parse Freezing weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Freezing found.
     */
    private boolean doFZ(StringBuilder data) {
        
        boolean foundFZ = false;
        
        String descriptor = null;
        int pos = data.indexOf("FZ");
        if(pos >= 0) {
            descriptor = "FZ";
        }
        if(descriptor != null) {

            // Going to check for TS here.
            int posx = data.indexOf("TS");
            if((posx >= 0)&&(posx < pos)) {
                data.delete(posx,pos);
                NcTafWxElements e = new NcTafWxElements();
                e.setDescriptor("TS");
                elements.add(e);
                pos -= 2;
            } else {
                posx = data.indexOf("SH");
                if((posx >= 0)&&(posx < pos)) {
                    data.delete(posx,pos);
                    NcTafWxElements e = new NcTafWxElements();
                    e.setDescriptor("SH");
                    elements.add(e);
                    pos -= 2;
                }
            }
            String intensity = " ";
            for(String s : fzTypes) {
                posx = data.indexOf(s);
                if(posx == pos) {
                    int end = posx + s.length();
                    NcTafWxElements e = new NcTafWxElements();
                    e.setDescriptor(descriptor);
                    e.setWx(data.substring(posx+2,posx+4));

                    // Check if there is an intensity
                    // Is it possible?
                    if(posx > 0) {
                        char c = data.charAt(posx-1);
                        if((c == '+')||(c == '-')) {
                            posx--;
                            intensity = data.substring(posx,posx+1);
                        }
                    }
                    e.setIntensity(intensity);
                    elements.add(e);
                    data.delete(posx, end);
                
                    // RA,SN,PL,GR, and GS can follow
                    if(data.length() > 0) {
                        // If there's a character here then we're in a compound weather
                        // group. Extract it.
                        while(data.charAt(0) != ' ') {
                            boolean breakOut = true;
                            for(String ss : wxTypes) {
                                String subGrp = data.substring(0,2);
                                if(ss.equals(subGrp)) {
                                    e = new NcTafWxElements();
                                    e.setIntensity(intensity);
                                    e.setWx(subGrp);
                                    elements.add(e);
                                    data.delete(0,2);
                                    breakOut = false;
                                }
                            }
                            if(breakOut) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return foundFZ;
    }
    
    /**
     * Parse Thunderstorm/Shower weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Thunderstorms/Showers found.
     */
    private boolean doTSSH(StringBuilder data) {

        // RA,SN,PL,GR, and GS can follow
        String [] wx = {
                "RA","SN","PL","GR","GS",
        };
        
        boolean foundTS = false;
        String [] types = null;
        
        String descriptor = null;
        int pos = data.indexOf("TS");
        if(pos >= 0) {
            descriptor = "TS";
            types = tsTypes;
        } else {
            pos = data.indexOf("SH");
            if(pos >= 0) {
                descriptor = "SH";
                types = shTypes;
            }
        }
        if(descriptor != null) {
            // Assume moderate until we find out otherwise.
            String intensity = " ";
            int posx = -1;
            for(String s : types) {
                posx = data.indexOf(s);
                if(posx >= pos) {
                    foundTS = true;
                    int end = posx + 4;
                    NcTafWxElements e = new NcTafWxElements();
                    e.setDescriptor(descriptor);
                    e.setWx(data.substring(posx+2,posx+4));

                    // Check if there is an intensity
                    // Is it possible?
                    if(posx > 0) {
                        char c = data.charAt(posx-1);
                        if((c == '+')||(c == '-')) {
                            posx--;
                            intensity = data.substring(posx,posx+1);
                            e.setIntensity(intensity);
                        }
                        elements.add(e);
                    } else {
                        e.setIntensity(intensity);
                        elements.add(e);
                    }
                    data.delete(posx, end);
                    // RA,SN,PL,GR, and GS can follow
                    // If there's a character here then we're in a compound
                    // weather
                    // group. Extract it.
                    outerLoop: while ((data.length() > 1)
                            && (data.charAt(0) != ' ')) {
                        boolean breakOut = true;
                        for (String ss : wx) {
                            String subGrp = data.substring(0, 2);
                            if (ss.equals(subGrp)) {
                                e = new NcTafWxElements();
                                e.setIntensity(intensity);
                                e.setDescriptor(descriptor);
                                e.setWx(subGrp);
                                elements.add(e);
                                data.delete(0, 2);
                                breakOut = false;
                                continue outerLoop;
                            }
                        } // for
                        // If we've gone through the entire list without finding
                        // anything, quit.
                        if(breakOut) {
                            break;
                        }
                    } // while
                    break;
                }
            }
        }
        return foundTS;
    }
    
    /**
     * Parse weather elements.
     * @param data Candidate TAF weather data.
     * @return Was weather found.
     */
    private boolean doWx(StringBuilder data) {
        
        ArrayList<NcTafWxElements> wxData = new ArrayList<NcTafWxElements>();
       
        boolean foundWx = false;

        int minPos = 9999;
        int end = -1;
        
        int pos = -1;
        for(String s : wxTypes) {
            pos = data.indexOf(s);
            if (pos >= 0) {
                minPos = Math.min(minPos,pos);
                end = Math.max(end,pos+2);

                NcTafWxElements e = new NcTafWxElements();
                e.setWx(data.substring(pos,pos+2));
                e.setPos(pos);
                wxData.add(e);
                foundWx = true;
            }
        }
        if(wxData.size() > 0) {
            Collections.sort(wxData);
            
            String intensity = " ";
            if(minPos > 0) {
                char c = data.charAt(minPos-1);
                if((c == '+')||(c == '-')) {
                    minPos--;
                    intensity = data.substring(minPos,minPos+1);
                }
            }
            data.delete(minPos, end);

            for(NcTafWxElements s : wxData) {
                s.setIntensity(intensity);
                elements.add(s);
            }
        }
        return foundWx;
    }

    /**
     * Parse Blowing weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Blowing found.
     */
    private boolean doBL(StringBuilder data) {
        
        boolean foundBL = false;
        int posx = -1;
        for(String s : blTypes) {
            posx = data.indexOf(s);
            if (posx >= 0) {
                foundBL = true;
                NcTafWxElements e = new NcTafWxElements();
                e.setDescriptor(data.substring(posx,posx+2));
                String ss = data.substring(posx+2,posx+4);
                if("SN".equals(ss)) {
                    e.setWx(ss);
                } else {
                    e.setObscuration(ss);
                }
                elements.add(e);
                data.delete(posx, posx + 4);
                break;
            }
        }
        return foundBL;
    }
    
    /**
     * Parse Drifting weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Drifting found.
     */
    private boolean doDR(StringBuilder data) {
        
        boolean foundDR = false;
        int posx = -1;
        for(String s : drTypes) {
            posx = data.indexOf(s);
            if (posx >= 0) {
                foundDR = true;
                NcTafWxElements e = new NcTafWxElements();
                String ss = data.substring(posx+2,posx+4);
                if("SN".equals(ss)) {
                    e.setWx(ss);
                } else {
                    e.setObscuration(ss);
                }
                e.setDescriptor(data.substring(posx,posx+2));
                elements.add(e);
                data.delete(posx, posx + 4);
                break;
            }
        }
        return foundDR;
    }
    
    /**
     * Parse Obscuration weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Obscuration found.
     */
    private boolean doOB(StringBuilder data) {
        
        boolean foundOB = false;
        ArrayList<NcTafWxElements> wxData = new ArrayList<NcTafWxElements>();
        
        int minPos = 9999;
        int end = -1;
        
        int pos = -1;
        for(String s : obTypes) {
            pos = data.indexOf(s);
            if (pos >= 0) {
                minPos = Math.min(minPos,pos);
                end = Math.max(end,pos+s.length());

                NcTafWxElements e = new NcTafWxElements();
                if(s.length() == 4) {
                    e.setDescriptor(data.substring(pos,pos+2));
                    e.setObscuration(data.substring(pos+2,pos+4));
                } else {
                    e.setObscuration(data.substring(pos,pos+s.length()));
                }
                int size = s.length();
                data.replace(pos,pos+size,String.format("%" + size + "s" ," "));
                e.setPos(pos);
                wxData.add(e);
                foundOB = true;
            }
        }
        if(foundOB) {
            Collections.sort(wxData);
            data.delete(minPos, end);

            for(NcTafWxElements s : wxData) {
                elements.add(s);
            }
        }
        
        return foundOB;
    }
    
    /**
     * Parse Other weather elements.
     * @param data Candidate TAF weather data.
     * @return Was Other found.
     */
    private boolean doOT(StringBuilder data) {
        
        boolean foundOT = false;
        ArrayList<NcTafWxElements> wxData = new ArrayList<NcTafWxElements>();
        
        int minPos = 9999;
        int end = -1;
        
        int pos = -1;
        for(String s : otTypes) {
            pos = data.indexOf(s);
            if (pos >= 0) {
                minPos = Math.min(minPos,pos);
                end = Math.max(end,pos+s.length());
                int size = s.length();

                NcTafWxElements e = new NcTafWxElements();

                String ss = data.substring(pos,pos+size); 
                e.setWx(ss);
                // Have to check for a special case of +FC here. (Tornado/Waterspout)
                if("FC".equals(ss) || "DS".equals(ss) || "SS".equals(ss)   ) {
                    // Could there be a plus sign?
                    if((pos > 0)&&('+' == data.charAt(pos-1))) {
                        pos--;
                        size++;
                        minPos = Math.min(minPos,pos);
                        e.setIntensity("+");
                    }
                }
                
                data.replace(pos,pos+size,String.format("%" + size + "s" ," "));
                e.setPos(pos);
                wxData.add(e);
                foundOT = true;
            }
        }
        if(foundOT) {
            Collections.sort(wxData);
            data.delete(minPos, end);

            for(NcTafWxElements s : wxData) {
                elements.add(s);
            }
        }
        
        return foundOT;
    }

    /**
     * Parse all weather elements and delete those found.
     * @param data Candidate TAF weather data.
     * @return Set of all weather element data found.
     */
    public Set<NcTafWeatherCondition> parseWeather(StringBuilder data) {
        
        Set<NcTafWeatherCondition> wxElements = null;
        
        elements = new ArrayList<NcTafWxElements>();
        
        // The order of these method calls depends on the data definition.
        // Changes should be made with caution!
        doFZ(data);
        doTSSH(data);
        doBL(data);
        doDR(data);
        doWx(data);
        doVC(data);
        doOB(data);
        doOT(data);
        
        if(elements.size() > 0) {
            wxElements = new HashSet<NcTafWeatherCondition>();
            
            int sequence = 1;
            for(NcTafWxElements e : elements) {
                NcTafWeatherCondition con = e.getWeather(sequence++);
                wxElements.add(con);
            }
        }
        
        return wxElements;
    }
}
