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
 * Nsharp sup.txt file contains previous weather data with "super cell" history.
 * It is used for SARS (Sounding Analog Retrieval System) computations. The file
 * is formatted like the following.
 * FILENAME CAT MLMIXR MLCAPE MLCIN MLCL(MAGL) 0-1SRH 0-6KT STPC 500T(C) 500DIR 7-5LR 0-3(KT) 0-9(KT) 0-3KMSRH(M2/S2)
 * 00042320.TXK    2   12.9    1702    -1  657    134 61.7    2.3 -14.0   250 6.5 32.8    76.6    166
 * 00042001.PPF    2   13.5    2614    -50 1216   227 59.9    4.7 -12.9   234 7.5 39.7    73.5    244
 * ................
 * Note that wrong formatted file will cause file reading error.
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

public class NsharpSupFile {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NsharpSupFile.class);

    private static List<SupLineInfo> supLineList = new ArrayList<>();

    public class SupLineInfo {
        private String dateStnStr;

        private int tornadoType;

        private float cape;

        private float lcl;

        private float temp500;

        private float lapseRate75;

        private float shear3km;

        private float shear6km;

        private float shear9km;

        private float helicity1km;

        private float helicity3km;

        public String getDateStnStr() {
            return dateStnStr;
        }

        public void setDateStnStr(String dateStnStr) {
            this.dateStnStr = dateStnStr;
        }

        public int getTornadoType() {
            return tornadoType;
        }

        public void setTornadoType(int tornadoType) {
            this.tornadoType = tornadoType;
        }

        public float getCape() {
            return cape;
        }

        public void setCape(float cape) {
            this.cape = cape;
        }

        public float getLcl() {
            return lcl;
        }

        public void setLcl(float lcl) {
            this.lcl = lcl;
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

        public float getHelicity1km() {
            return helicity1km;
        }

        public void setHelicity1km(float helicity1km) {
            this.helicity1km = helicity1km;
        }

        public float getHelicity3km() {
            return helicity3km;
        }

        public void setHelicity3km(float helicity3km) {
            this.helicity3km = helicity3km;
        }
    }

    public static List<SupLineInfo> readSupFile() {
        if (supLineList.size() <= 0) {
            NsharpConfigManager configMgr = NsharpConfigManager.getInstance();
            String supFilePath = configMgr
                    .getBigNsharpFlPath(NcPathConstants.NSHARP_SUP_FILE);
            if (supFilePath != null) {
                try {
                    NsharpSupFile supFile = new NsharpSupFile();
                    InputStream is = new FileInputStream(supFilePath);
                    StringBuilder strContent = new StringBuilder("");
                    int byteread;
                    while ((byteread = is.read()) != -1) {
                        strContent.append((char) byteread);
                    }
                    is.close();

                    String headerEndStr = "(M2/S2)";
                    int headerEndIndex = strContent.indexOf(headerEndStr)
                            + headerEndStr.length();
                    String dataString = strContent.substring(headerEndIndex);
                    dataString = dataString.trim();
                    StringTokenizer st = new StringTokenizer(dataString);
                    /*
                     * sup.txt file contains n lines of previous super cell
                     * information. Each line contains 15 parameters: FILENAME
                     * CAT MLMIXR MLCAPE MLCIN MLCL(MAGL) 0-1SRH 0-6KT STPC
                     * 500T(C) 500DIR 7-5LR 0-3(KT) 0-9(KT) 0-3KMSRH(M2/S2)
                     * 
                     * We only use the following for SARS computations.
                     * FILENAME(0), CAT(1), MLCAPE(3), MLCL(MAGL)(5), 0-1SRH(6),
                     * 0-6KT(7), 500T(C)(9), 7-5LR(11), 0-3(KT)(12),
                     * 0-9(KT)(13), 0-3KMSRH(M2/S2)(14)
                     */
                    int dataCount = 0;
                    int dataCycleLength = 15;
                    SupLineInfo line = null;
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken();
                        int paramIndex = dataCount % dataCycleLength;
                        switch (paramIndex) {
                        case 0: // FILENAME
                            line = supFile.new SupLineInfo();
                            supLineList.add(line);
                            line.setDateStnStr(tok);
                            break;
                        case 1: // CAT
                            int tornadoType = Integer.valueOf(tok);
                            line.setTornadoType(tornadoType);
                            break;
                        case 3: // MLCAPE
                            float cape = Float.valueOf(tok);
                            line.setCape(cape);
                            break;
                        case 5: // MLCL
                            float lcl = Float.valueOf(tok);
                            line.setLcl(lcl);
                            break;
                        case 6: // 0-1KM SRH
                            float helicity1km = Float.valueOf(tok);
                            line.setHelicity1km(helicity1km);
                        case 7: // 0-6KT SH
                            float shear6km = Float.valueOf(tok);
                            line.setShear6km(shear6km);
                            break;
                        case 9: // 500TEMP C
                            float temp500 = Float.valueOf(tok);
                            line.setTemp500(temp500);
                            break;
                        case 11: // 7-5LR
                            float lapseRate75 = Float.valueOf(tok);
                            line.setLapseRate75(lapseRate75);
                            break;
                        case 12: // 0-3KT SH
                            float shear3km = Float.valueOf(tok);
                            line.setShear3km(shear3km);
                            break;
                        case 13: // 0-9KT SH
                            float shear9km = Float.valueOf(tok);
                            line.setShear9km(shear9km);
                            break;
                        case 14: // 0-3KM SRH
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
        return supLineList;
    }
}
