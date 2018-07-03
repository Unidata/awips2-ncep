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
 * 07/28/2017   RM#34795    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Added output for the "large hail parameter" and 
 *                                      the "modified SHERBE" parameter,..etc.
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 * 
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.ArrayList;
import java.util.List;

public class NsharpCloudInfo {
    public enum CloudType {
        OVC, BKN, SCT, FEW, UNDEFINED
    };

    public class CloudLayer {
        float pressureStart;

        float pressureEnd;

        CloudType cloudType;

        public CloudLayer(float pressureStart, float pressureEnd,
                CloudType cloudType) {
            super();
            this.pressureStart = pressureStart;
            this.pressureEnd = pressureEnd;
            this.cloudType = cloudType;
        }

        public float getPressureStart() {
            return pressureStart;
        }

        public float getPressureEnd() {
            return pressureEnd;
        }

        public CloudType getCloudType() {
            return cloudType;
        }

    };

    /* FM: Fred Mosher's Algorithm */
    private List<CloudLayer> fmCloudLys = new ArrayList<>();

    /* CE: Chernykh and Eskridge Algorithm */
    private List<CloudLayer> ceCloudLys = new ArrayList<>();

    private List<NcSoundingLayer> soundingLys;

    public NsharpCloudInfo(List<NcSoundingLayer> soundingLys) {
        super();
        this.soundingLys = soundingLys;
        draw_Clouds();
    }

    private float cloudTypeOvc(float x) {
        if (x >= -10) {
            return 1.0f;
        } else {
            return (-.1f * (x + 70) + 7);
        }
    }

    private float cloudTypeBkn(float x) {
        if (x >= 0) {
            return 2.0f;
        } else if (x >= -10 && x < 0) {
            return (-.025f * (x + 10) + 2.5f);
        } else {
            return (-.125f * (x + 70) + 10.0f);
        }
    }

    private float cloudTypeSct(float x) {
        if (x >= 0) {
            return 3.0f;
        } else if (x >= -10 && x < 0) {
            return (-0.1f * (x + 10) + 4.0f);
        } else {
            return (-0.15f * (x + 50) + 10.0f);
        }
    }

    private CloudType getCloudType(float temp, float dd) {
        if (dd < cloudTypeOvc(temp)) {
            return CloudType.OVC;
        } else if (dd < cloudTypeBkn(temp)) {
            return CloudType.BKN;
        } else if (dd < cloudTypeSct(temp)) {
            return CloudType.SCT;
        } else {
            return CloudType.FEW;
        }
    }

    private void draw_Clouds()
    /*****************************************************************
     * DRAW_CLOUDS LARRY J. HINSON AWC/KCMO ported from "AWC nsharp" and
     * modified for CAVE Compute cloud information
     *****************************************************************/
    {
        // need at least 3 sounding layers for computation
        if (soundingLys == null || soundingLys.size() <= 3) {
            return;
        }
        int s1, s2, s3;
        float temp1, temp2, temp3;
        float dewPoint1, dewPoint2, dewPoint3;
        float press1, press2, press3;
        float startpres = 0, endpres = 0;
        float tempAvg, tempDewptDiffAvg, tempDewptDiff;
        float tempAccum = 0.0f;
        float tempDewptDiffAccum = 0.0f;
        float tempCount = 0.0f;
        boolean startflag = false;
        // Fred Mosher's Algorithm
        for (int i = 1; i < soundingLys.size() - 1; i++) {
            s1 = i - 1;
            s2 = i;
            s3 = i + 1;
            NcSoundingLayer layerS1 = soundingLys.get(s1);
            NcSoundingLayer layerS2 = soundingLys.get(s2);
            NcSoundingLayer layerS3 = soundingLys.get(s3);
            temp1 = layerS1.getTemperature();
            temp2 = layerS2.getTemperature();
            temp3 = layerS3.getTemperature();

            if (NsharpLibBasics.qc(temp1) && NsharpLibBasics.qc(temp2)
                    && NsharpLibBasics.qc(temp3)) {
                float dz = layerS3.getGeoHeight() - layerS1.getGeoHeight();
                if (dz == 0.0) {
                    dz = 1;
                }
                float d2T = (temp3 - 2 * temp2 + temp1) / (dz * dz);
                press1 = layerS1.getPressure();
                press2 = layerS2.getPressure();
                press3 = layerS3.getPressure();
                dewPoint1 = layerS1.getDewpoint();
                dewPoint2 = layerS2.getDewpoint();
                dewPoint3 = layerS3.getDewpoint();

                float R1 = 100 * NsharpLibThermo.mixratio(press1, dewPoint1)
                        / NsharpLibThermo.mixratio(press1, temp1);
                float R2 = 100 * NsharpLibThermo.mixratio(press2, dewPoint2)
                        / NsharpLibThermo.mixratio(press2, temp2);
                float R3 = 100 * NsharpLibThermo.mixratio(press3, dewPoint3)
                        / NsharpLibThermo.mixratio(press3, temp3);
                float d2R = (R3 - 2 * R2 + R1) / (dz * dz);
                if (d2T >= 0 && d2R <= 0 && !startflag) {
                    startflag = true;
                    startpres = press2;
                } else if (!(d2T >= 0 && d2R <= 0) && startflag) {
                    startflag = false;
                    endpres = press2;
                    tempAvg = tempAccum / tempCount;
                    tempDewptDiffAvg = tempDewptDiffAccum / tempCount;
                    CloudType cloudType = getCloudType(tempAvg,
                            tempDewptDiffAvg);
                    tempAccum = 0.0f;
                    tempDewptDiffAccum = 0.0f;
                    tempCount = 0;
                    if (cloudType != CloudType.FEW) {
                        CloudLayer cloudLy = new CloudLayer(startpres, endpres,
                                cloudType);
                        fmCloudLys.add(cloudLy);
                    }
                }
                if ((d2T >= 0 && d2R <= 0) && startflag) {
                    tempAccum += temp2;
                    tempDewptDiff = temp2 - dewPoint2;
                    tempDewptDiffAccum += tempDewptDiff;
                    tempCount++;
                }
            }
        }

        // Chernykh and Eskridge Algorithm
        boolean top = false;
        for (int i = soundingLys.size() - 2; i > 0; i--) {
            boolean basefound = false;
            s1 = i - 1;
            s2 = i;
            s3 = i + 1;
            NcSoundingLayer layerS1 = soundingLys.get(s1);
            NcSoundingLayer layerS2 = soundingLys.get(s2);
            NcSoundingLayer layerS3 = soundingLys.get(s3);
            if (layerS3.getPressure() < 100) {
                continue;
            }
            temp1 = layerS1.getTemperature();
            temp2 = layerS2.getTemperature();
            temp3 = layerS3.getTemperature();
            if (NsharpLibBasics.qc(temp1) && NsharpLibBasics.qc(temp2)
                    && NsharpLibBasics.qc(temp3)) {
                if (!top) {
                    dewPoint1 = layerS1.getDewpoint();
                    dewPoint2 = layerS2.getDewpoint();
                    dewPoint3 = layerS3.getDewpoint();
                    float dd1 = temp1 - dewPoint1;
                    float dd2 = temp2 - dewPoint2;
                    float dd3 = temp3 - dewPoint3;
                    float dz = (layerS3.getGeoHeight() - layerS1.getGeoHeight()) / 2.0f;
                    if (dz == 0) {
                        dz = 1;
                    }
                    float d2x = (dd3 - 2 * dd2 + dd1) / (dz * dz);
                    if (d2x > 0 && dd2 < 4.5) {
                        top = true;
                        endpres = layerS2.getPressure();
                        /* Now work downward till you get moistening with height */
                        /*
                         * Do this until you reach lowest level of this
                         * condition
                         */
                        while (i > 1 && !basefound) {
                            i--;
                            s1 = i - 1;
                            s2 = i;
                            s3 = i + 1;
                            layerS1 = soundingLys.get(s1);
                            layerS2 = soundingLys.get(s2);
                            layerS3 = soundingLys.get(s3);
                            temp1 = layerS1.getTemperature();
                            temp2 = layerS2.getTemperature();
                            temp3 = layerS3.getTemperature();
                            if (NsharpLibBasics.qc(temp1)
                                    && NsharpLibBasics.qc(temp2)
                                    && NsharpLibBasics.qc(temp3)) {
                                dewPoint1 = layerS1.getDewpoint();
                                dewPoint2 = layerS2.getDewpoint();
                                dewPoint3 = layerS3.getDewpoint();
                                dd1 = temp1 - dewPoint1;
                                dd2 = temp2 - dewPoint2;
                                dd3 = temp3 - dewPoint3;
                                dz = (layerS3.getGeoHeight() - layerS1
                                        .getGeoHeight()) / 2.0f;
                                if (dz == 0) {
                                    dz = 1;
                                }
                                d2x = (dd3 - 2 * dd2 + dd1) / (dz * dz);
                                float d2xparam = 0.0000f;
                                if (d2x < -d2xparam) {
                                    while ((d2x < -d2xparam && dd2 < 4.5)
                                            && i > 1) {
                                        i--;
                                        s1 = i - 1;
                                        s2 = i;
                                        s3 = i + 1;
                                        layerS1 = soundingLys.get(s1);
                                        layerS2 = soundingLys.get(s2);
                                        layerS3 = soundingLys.get(s3);
                                        temp1 = layerS1.getTemperature();
                                        temp2 = layerS2.getTemperature();
                                        temp3 = layerS3.getTemperature();
                                        if (NsharpLibBasics.qc(temp1)
                                                && NsharpLibBasics.qc(temp2)
                                                && NsharpLibBasics.qc(temp3)) {
                                            dewPoint1 = layerS1.getDewpoint();
                                            dewPoint2 = layerS2.getDewpoint();
                                            dewPoint3 = layerS3.getDewpoint();
                                            dd1 = temp1 - dewPoint1;
                                            dd2 = temp2 - dewPoint2;
                                            dd3 = temp3 - dewPoint3;
                                            dz = (layerS3.getGeoHeight() - layerS1
                                                    .getGeoHeight()) / 2.0f;
                                            if (dz == 0) {
                                                dz = 1;
                                            }
                                            d2x = (dd3 - 2 * dd2 + dd1)
                                                    / (dz * dz);
                                        }
                                    }
                                    /*
                                     * Lowest level of drying found...compute
                                     * LCL from s1
                                     */
                                    LayerParameters lyParam = NsharpLibThermo
                                            .drylift(layerS1.getPressure(),
                                                    temp1, dewPoint1);
                                    if(lyParam!=null){
                                        startpres = lyParam.getPressure();

                                        if (startpres < endpres) {
                                            startpres = layerS1.getPressure();
                                        }
                                        top = false;
                                        basefound = true;
                                        CloudLayer cloudLy = new CloudLayer(
                                                startpres, endpres,
                                                CloudType.UNDEFINED);
                                        ceCloudLys.add(cloudLy);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public List<CloudLayer> getFmCloudLys() {
        return fmCloudLys;
    }

    public List<CloudLayer> getCeCloudLys() {
        return ceCloudLys;
    }
}
