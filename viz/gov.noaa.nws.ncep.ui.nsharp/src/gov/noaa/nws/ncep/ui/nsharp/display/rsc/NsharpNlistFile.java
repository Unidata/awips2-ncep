package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system. 
 * 
 * All methods developed in this class are based on the algorithm developed in BigSharp 
 * native C file, basics.c , by John A. Hart/SPC.
 * All methods name are defined with same name as the C function name defined in native code.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 * 
 *
 *
 * Nsharp nlist.txt file contains previous weather data with hail history. It is used for SARS (Sounding Analog 
 * Retrieval System) computations. The file is formatted like the following.
 * DATE/RAOB ELEV REPORT  MUCAPE  MUMR  500TEMP 300T 7-5LR  5-3LR  0-3SH  0-6SH 0-9SH  SRH3  SHIP MODELb
 * 95052300.DDC  791 6.00  4181  15.3    -9.6    -36.2   7.5 7.1 23.4    19.4    26.8    325 1.9 3.0
 * 91051100.MAF  873 6.00  4692  14.9    -10.8   -37.3   8.8 7.2 14.2    13.2    18.1    -37 1.9 2.7
 * .............
 * 
 */
import gov.noaa.nws.ncep.ui.nsharp.NsharpConfigManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class NsharpNlistFile {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpNlistFile.class);

    private static List<NlistLineInfo> nlistLineList = new ArrayList<>();

    public class NlistLineInfo {
        private String dateStnStr;

        private float size;

        private float cape;

        private float mixingRatio;

        private float temp500;

        private float lapseRate75;

        private float shear3km;

        private float shear6km;

        private float shear9km;

        private float helicity3km;

        private boolean significantMatch;

        public String getDateStnStr() {
            return dateStnStr;
        }

        public void setDateStnStr(String dateStnStr) {
            this.dateStnStr = dateStnStr;
        }

        public float getSize() {
            return size;
        }

        public void setSize(float size) {
            this.size = size;
        }

        public float getCape() {
            return cape;
        }

        public void setCape(float cape) {
            this.cape = cape;
        }

        public float getMixingRatio() {
            return mixingRatio;
        }

        public void setMixingRatio(float mixingRatio) {
            this.mixingRatio = mixingRatio;
        }

        public float getTemp500() {
            return temp500;
        }

        public void setTemp500(float temp500) {
            this.temp500 = temp500;
        }

        public float getLapseRate75() {
            return lapseRate75;
        }

        public void setLapseRate75(float lapseRate75) {
            this.lapseRate75 = lapseRate75;
        }

        public float getShear3km() {
            return shear3km;
        }

        public void setShear3km(float shear3km) {
            this.shear3km = shear3km;
        }

        public float getShear6km() {
            return shear6km;
        }

        public void setShear6km(float shear6km) {
            this.shear6km = shear6km;
        }

        public float getShear9km() {
            return shear9km;
        }

        public void setShear9km(float shear9km) {
            this.shear9km = shear9km;
        }

        public float getHelicity3km() {
            return helicity3km;
        }

        public void setHelicity3km(float helicity3km) {
            this.helicity3km = helicity3km;
        }

        public boolean isSignificantMatch() {
            return significantMatch;
        }

        public void setSignificantMatch(boolean significantMatch) {
            this.significantMatch = significantMatch;
        }

    }

    public static List<NlistLineInfo> readNlistFile() {
        if (nlistLineList.size() <= 0) {
            NsharpConfigManager configMgr = NsharpConfigManager.getInstance();
            String nlistFilePath = configMgr
                    .getBigNsharpFlPath(NcPathConstants.NSHARP_HAILDATA_FILE);
            if (nlistFilePath != null) {
                try {

                    NsharpNlistFile nlistFile = new NsharpNlistFile();
                    InputStream is = new FileInputStream(nlistFilePath);
                    StringBuilder strContent = new StringBuilder("");
                    int byteread;
                    while ((byteread = is.read()) != -1) {
                        strContent.append((char) byteread);
                    }
                    is.close();

                    String headerEndStr = "MODELb";
                    int headerEndIndex = strContent.indexOf(headerEndStr)
                            + headerEndStr.length();
                    String dataString = strContent.substring(headerEndIndex);
                    dataString = dataString.trim();
                    StringTokenizer st = new StringTokenizer(dataString);
                    /*
                     * nlist.txt file contains n lines of previous hail
                     * information. Each line contains 15 parameters: DATE/RAOB
                     * ELEV REPORT MUCAPE MUMR 500TEMP 300T 7-5LR 5-3LR 0-3SH
                     * 0-6SH 0-9SH SRH3 SHIP MODELb
                     * 
                     * We only use the following for sars computations.
                     * DATE/RAOB, REPORT, MUCAPE, MUMR, 500TEMP, 7-5LR, 0-3SH,
                     * 0-6SH, 0-9SH, SRH3
                     */
                    int dataCount = 0;
                    int dataCycleLength = 15;
                    NlistLineInfo line = null;
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken();
                        int paramIndex = dataCount % dataCycleLength;
                        switch (paramIndex) {
                        case 0: // DATE/RAOB
                            line = nlistFile.new NlistLineInfo();
                            nlistLineList.add(line);
                            line.setDateStnStr(tok);
                            break;
                        case 2: // REPORT i.e. size
                            float size = Float.valueOf(tok);
                            line.setSize(size);
                            if (size >= 2) {
                                line.setSignificantMatch(true);
                            } else {
                                line.setSignificantMatch(false);
                            }
                            break;
                        case 3: // MUCAPE
                            float cape = Float.valueOf(tok);
                            line.setCape(cape);
                            break;
                        case 4: // MUMR
                            float mixingRatio = Float.valueOf(tok);
                            line.setMixingRatio(mixingRatio);
                            break;
                        case 5: // 500TEMP
                            float temp500 = Float.valueOf(tok);
                            line.setTemp500(temp500);
                            break;
                        case 7: // 7-5LR
                            float lapseRate75 = Float.valueOf(tok);
                            line.setLapseRate75(lapseRate75);
                            break;
                        case 9: // 0-3SH
                            float shear3km = Float.valueOf(tok);
                            line.setShear3km(shear3km);
                            break;
                        case 10: // 0-6SH
                            float shear6km = Float.valueOf(tok);
                            line.setShear6km(shear6km);
                            break;
                        case 11: // 0-9SH
                            float shear9km = Float.valueOf(tok);
                            line.setShear9km(shear9km);
                            break;
                        case 12: // SRH3
                            float helicity3km = Float.valueOf(tok);
                            line.setHelicity3km(helicity3km);
                            break;
                        default:
                            break;
                        }
                        dataCount++;
                    }

                } catch (FileNotFoundException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                } catch (IOException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                }
            }
        }
        return nlistLineList;
    }
}
