package gov.noaa.nws.ncep.ui.nsharp.display.rsc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSkparams;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibThermo;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibWinds;
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibXwvid;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.EffectiveLayerPressures;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LParcelValues;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LapseRateMax;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.LayerParameters;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.MixHeight;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingTools;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpHailInfo.HailInfoContainer;

/**
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * All methods developed in this class are based on the algorithm developed in
 * BigSharp native C file, basics.c , by John A. Hart/SPC. All methods name are
 * defined with same name as the C function name defined in native code.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 06/13/2017   RM#34793    Chin Chen   Add max lapse rate bar on skewT pane
 * 07/10/2017   RM#34796    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                     - Reformat the lower left data page
 * 07/28/2017   RM#34795    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Added output for the "large hail parameter" and
 *                                      the "modified SHERBE" parameter,..etc.
 * 09/1/2017   RM#34794    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                      - Update the dendritic growth layer calculations and other skewT
 *                                      updates.
 * May, 5, 2018 49896       mgamazaychikov  Fixed an NPE for muParcel (line 1140), fixed formatting
 * Sep,18, 2018 DCS20492    mgamazaychikov  Changed call to bunkers_storm_motion method
 * Dec 14, 2018 6872        bsteffen   Initialize watch warning type to none.
 * Dec 20, 2018 7575        bsteffen   Keep tack of user selected parcel pressure.
 *
 * </pre>
 *
 * @author Chin Chen
 * @version 1.0
 *
 */
public class NsharpWeatherDataStore {
    public class ParcelMiscParams {
        // mean wind at LFC-EL
        private WindComponent meanWindCompLfcToEl;

        // mean wind at LCL -EL
        private WindComponent meanWindCompLclToEl;

        // SR mean wind at LCL -EL
        private WindComponent srMeanWindCompLclToEl;

        // SR mean wind at Sfc - LFC
        private WindComponent srMeanWindCompSfcToLfc;

        // SR mean wind at LFC - LFC+4km
        private WindComponent srMeanWindCompLfcToLFCP4km;

        // SR mean wind at EL-4km - EL
        private WindComponent srMeanWindCompElM4kmToEl;

        // EL storm relative (SR) wind
        private WindComponent srWindCompEl;

        // wind shear at LCL -EL
        private float windShearLclToEl = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

        // LPL-LFC SR helicity
        private Helicity helicityLplToLfc;

        // CHI1
        private float chi1;

        // CHI2
        private float chi2;

        // MU parcel name dynamically created
        private String muName = "";

        public ParcelMiscParams() {
            super();

        }

        public WindComponent getMeanWindCompLfcToEl() {
            return meanWindCompLfcToEl;
        }

        public void setMeanWindCompLfcToEl(WindComponent meanWindCompLfcToEl) {
            this.meanWindCompLfcToEl = meanWindCompLfcToEl;
        }

        public WindComponent getMeanWindCompLclToEl() {
            return meanWindCompLclToEl;
        }

        public void setMeanWindCompLclToEl(WindComponent meanWindCompLclToEl) {
            this.meanWindCompLclToEl = meanWindCompLclToEl;
        }

        public WindComponent getSrMeanWindCompLclToEl() {
            return srMeanWindCompLclToEl;
        }

        public void setSrMeanWindCompLclToEl(WindComponent srMeanWindCompLclToEl) {
            this.srMeanWindCompLclToEl = srMeanWindCompLclToEl;
        }

        public float getWindShearLclToEl() {
            return windShearLclToEl;
        }

        public void setWindShearLclToEl(float windShearLclToEl) {
            this.windShearLclToEl = windShearLclToEl;
        }

        public Helicity getHelicityLplToLfc() {
            return helicityLplToLfc;
        }

        public void setHelicityLplToLfc(Helicity helicityLplToLfc) {
            this.helicityLplToLfc = helicityLplToLfc;
        }

        public WindComponent getSrMeanWindCompSfcToLfc() {
            return srMeanWindCompSfcToLfc;
        }

        public WindComponent getSrMeanWindCompLfcToLFCP4km() {
            return srMeanWindCompLfcToLFCP4km;
        }

        public WindComponent getSrMeanWindCompElM4kmToEl() {
            return srMeanWindCompElM4kmToEl;
        }

        public WindComponent getSrWindCompEl() {
            return srWindCompEl;
        }

        public float getChi1() {
            return chi1;
        }

        public float getChi2() {
            return chi2;
        }

        public String getMuName() {
            return muName;
        }

        public void setSrMeanWindCompSfcToLfc(WindComponent srMeanWindCompSfcToLfc) {
            this.srMeanWindCompSfcToLfc = srMeanWindCompSfcToLfc;
        }

        public void setSrMeanWindCompLfcToLFCP4km(WindComponent srMeanWindCompLfcToLFCP4km) {
            this.srMeanWindCompLfcToLFCP4km = srMeanWindCompLfcToLFCP4km;
        }

        public void setSrMeanWindCompElM4kmToEl(WindComponent srMeanWindCompElM4kmToEl) {
            this.srMeanWindCompElM4kmToEl = srMeanWindCompElM4kmToEl;
        }

        public void setSrWindCompEl(WindComponent srWindCompEl) {
            this.srWindCompEl = srWindCompEl;
        }

        public void setChi1(float chi1) {
            this.chi1 = chi1;
        }

        public void setChi2(float chi2) {
            this.chi2 = chi2;
        }

        public void setMuName(String muName) {
            this.muName = muName;
        }
    }

    /*
     * data pane PAGE2 string definitions
     */
    public static final String[] STORM_MOTION_TYPE_STR = { "SFC-1km", "SFC-2km", "SFC-3km", "Eff Inflow", "SFC-6km",
            "SFC-8km", "LCL-EL(Cloud Layer)", "Eff Shear(EBWD)" };

    private static final float[][] STORM_MOTION_HEIGHT = { { 0, 1000 }, { 0, 2000 }, { 0, 3000 }, { 0, 0 }, { 0, 6000 },
            { 0, 8000 }, { 0, 0 }, { 0, 0 } };

    public static final Map<Integer, Float> parcelToLayerPressMap = new HashMap<Integer, Float>() {
        private static final long serialVersionUID = 1L;

        {
            put(NsharpLibSndglib.PARCELTYPE_OBS_SFC, NsharpLibSndglib.OBS_LAYER_PRESS);
            put(NsharpLibSndglib.PARCELTYPE_FCST_SFC, NsharpLibSndglib.FCST_LAYER_PRESS);
            put(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING, NsharpLibSndglib.MML_LAYER_PRESS);
            put(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE, NsharpLibSndglib.MU_LAYER_PRESS);
            put(NsharpLibSndglib.PARCELTYPE_USER_DEFINED, NsharpLibSndglib.USER_LAYER_PRESS);
            put(NsharpLibSndglib.PARCELTYPE_EFF, NsharpLibSndglib.EFF_LAYER_PRESS);
        }
    };

    // Parcel structure store weather parameters that are parcel type relevant
    // and computed by parcel() method.
    // parcelMap contains 6 parcels used by Nsharp. Its key is "parcel number"
    // and value is Parcel structure.
    // parcel type and number defined in NsharpLibSndglib.java
    private Map<Integer, Parcel> parcelMap = new HashMap<>();

    // ParcelMiscParams store parcel relevant parameters that not computed by
    // parcel().
    // parcelMiscParamsMap contains 6 ParcelMiscParams used by nsharp. Its key
    // is
    // "parcel number" and value is ParcelMiscParams structure.
    private Map<Integer, ParcelMiscParams> parcelMiscParamsMap = new HashMap<>();

    /*******************************************************************
     * Data Pane Page 1 ("Sum1" page) weather parameters
     *******************************************************************/
    // All parcel relevant parameters, e.g., CAPE, CINH, LCL, LI, LFC, EL
    // etc, are saved in ParcelMap

    // mean mix ratio in g/kg
    private float meanMixRatio = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // AGL height in ft of freezing level
    private float fgzft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // height in ft of freezing level
    private float frzft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // precipitable water in inch
    private float pw = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // AGL height in ft of Wet-bulb at freezing level
    private float wbzft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // msl height in ft of Wet-bulb at freezing level
    private float wbzmslft = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // pressure in mb of Wet-bulb at freezing level
    private float wbzp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Damaging wind potential
    private float wndg = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    private float kIndex = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // downdraft instability in J/Kg
    private float dcape = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Downrush temperature at surface in F
    private float downT = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Enhanced Stretching Potential
    private float esp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // middle layers (surfacePressure-150 to surfacePressure 350) RH in %
    private float midRh = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // low layers (surfacePressure to surfacePressure-150) RH in %
    private float lowRh = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // convective temperature in F
    private float convT = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // MMP: Coniglio MCS Maintenance Parameter
    private float mmp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // maximum temperature (in F) forecast based on depth of mixing level and
    // low level temperatures.
    private float maxTemp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // NCAPE
    private float ncape = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // sfc-3km Agl Lapse Rate (C/km)
    private float sfcTo3kmLapseRate = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // sfc-3km temp delta (C)
    private float sfcTo3kmTempDelta = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 3km-6km Agl Lapse Rate (C/km)
    private float threeKmTo6kmLapseRate = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 3km-6km temp delta (C)
    private float threeKmTo6kmTempDelta = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 850-500 mb Lapse Rate (C/km)
    private float eight50To500mbLapseRate = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 850-500 mb temp delta (C)
    private float eight50To500mbTempDelta = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 700-500 mb Lapse Rate (C/km)
    private float sevenHundredTo500mbLapseRate = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 700-500 mb temp delta (C)
    private float sevenHundredTo500mbTempDelta = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // SCP: Supercell Composite Parameter
    private float scp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Significant Tornado Parameter (effective layer with CIN)
    private float stpCin = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Significant Tornado Parameter (fixed layer)
    private float stpFixed = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Significant Hail Parameter
    private float ship = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    /*******************************************************************
     * Data Pane Page 2 ("Sum2" page) weather parameters
     *******************************************************************/
    // The following parameters are parcel relevant and are saved in ParcelMap
    // BRN Shear, mean wind at LCL-EL, SR mean wind at LCL-EL, wind shear at
    // LCL-EL

    // mean wind map, key defined in STORM_MOTION_TYPE_STR[]
    Map<String, WindComponent> stormTypeToMeanWindMap = new HashMap<>();

    // SR mean wind map, key defined in STORM_MOTION_TYPE_STR[]
    private Map<String, WindComponent> stormTypeToSrMeanWindMap = new HashMap<>();

    // wind shear map, key defined in STORM_MOTION_TYPE_STR[]
    private Map<String, Float> stormTypeToWindShearMap = new HashMap<>();

    // Storm helicity map, key defined in STORM_MOTION_TYPE_STR[]
    private Map<String, Helicity> stormTypeToHelicityMap = new HashMap<>();

    // SR mean wind at 4-6 km
    private WindComponent srMeanWindComp4To6km;

    // corfidi down shear [0], up shear [1]
    private WindComponent[] cofidiShearWindComp = new WindComponent[2];

    // bunkers storm motion right@[0], left @[1]
    private WindComponent[] bunkersStormMotionWindComp = new WindComponent[2];

    // STP lR
    private float stpLr = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // large hail parameter
    private float lhp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Modified SHERBE
    private float modSherbe = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    /*******************************************************************
     * Data Pane Page 3 ("PARCEL DATA" page) weather parameters
     *******************************************************************/
    // all parameters are parcel relevant and saved at parcelMap
    /*******************************************************************
     * Data Pane Page 4 ("THERMODYNAMIC DATA" page) weather parameters
     *******************************************************************/
    // PW, meanlowRh, 700-500 mb Lapse Rate, 850-500 mb Lapse Rate, K index,
    // MaxTemp, ConvTemp, WBZ, and FGZ are listed in Page1
    // All Parcel LIs are listed in Parcel (parcelMap)

    // Top of MoistLyr pressure in mb
    private float topMoistLyrPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Top of MoistLyr height in ft
    private float topMoistLyrHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // all layers' (surfacePressure to top layer) RH in %
    private float meanRh = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // Total Totals
    private float totTots = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // sweat Index
    private float sweatIndex = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // ThetaE Diff in C
    private float thetaDiff = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    /*******************************************************************
     * Data Pane Page 5 ("OPC LOW LEVEL" page) weather parameters
     *******************************************************************/
    // 975mb level temperature in C
    private float temp975 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // 975mb level height in meter
    private float height975 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // lowest inversion height in m
    private float lowestInvHeight = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // lowest inversion pressure in mb
    private float lowestInvPressure = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // lowest inversion temp change in C
    private float lowestInvTempChange = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    /*******************************************************************
     * Data Pane Page 6 ("MIXING HEIGHT" page) weather parameters
     *******************************************************************/
    // layer Based mix height parameters
    private MixHeight mixHeightLayerBased;

    // Surface Based mix height parameters
    private MixHeight mixHeightSurfaceBased;

    /*******************************************************************
     * Data Pane Page 7 ("STORM RELATIVE" page) weather parameters
     *******************************************************************/
    // sfc-2km and Sfc-3km sr helicity are stored in page 2
    // stormTypeToHelicityMap
    // LPL-LFC sr helicity is parcel relevant and stored in parcelMiscParamsMap
    // sfc-2km SR mean wind stored in page 2 stormTypeToSrMeanWindMap
    // 4-6km SR mean wind store in page 2 srMeanWindComp4To6km

    // SR mean wind at 9-11 km
    private WindComponent srMeanWindComp9To11km;

    /*******************************************************************
     * Data Pane Page 8 ("MEAN WIND" page) weather parameters
     *******************************************************************/
    // mean wind at LFC - EL is parcel relevant and saved in parcelMiscParamsMap
    // mean wind at 0-6 km stored in page 2

    // mean wind at 850-200 mb
    private WindComponent meanWindComp850To200mb;

    // wind shear 0-12km
    private WindComponent shearWindCompSfcTo12km;

    /*******************************************************************
     * Data Pane Page 9 ("CONVECTIVE INITIATION" page) weather parameters
     *******************************************************************/
    // CINH, CAPE, LFC, Cap, BRN, BRN Shear are parcel relevant and saved in
    // ParcelMap
    // Kindex stored in page 1
    // MeanRH, Top of Moist Lyr stored in page 4
    // Eff SREH
    private float effSreh;
    // Melting level pressure and height stored in page 1 as wbzp and wbzft

    // Rogash Rain fall rate in 1 hour in/hr
    private float RogashRainRate;

    // 3km shear in m/s, default wind-shear
    private float windShear3km;

    /*******************************************************************
     * Data Pane Page 10 ("SEVERE POTENTIAL" page) weather parameters
     *******************************************************************/
    // CAPE is parcel relevant and is saved in ParcelMap
    // WBZ, FGZ saved in page 1
    // CHI1, CHI2, Low SRW (sfc-LFC), Mid SRW (LFC - LFC+4km), upper SRW (EL-4km
    // - EL) are parcel relevant and saved in parcelMiscParamsMap

    // 700-500 mb mid level RH
    private float midLvlRH500To700;

    // Avg BL wetbulb temperature
    private float avgWetbulbTemp;

    /*******************************************************************
     * Data Pane Page 11 ("D2D Lite" page) weather parameters
     *******************************************************************/
    // All parameters can be found from page 1, page 2, page 3

    /******************************************************************
     * general Data Pane usage parameters
     *******************************************************************/

    private NcSoundingLayer sfcLayer = new NcSoundingLayer();

    private List<NcSoundingLayer> soundingLys = new ArrayList<>();

    // storm direction
    private float smdir = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // storm speed
    private float smspd = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    private EffectiveLayerPressures effLyPress;

    /*******************************************************************
     * SkewT Pane weather parameters
     *******************************************************************/
    // cloud info for draw cloud
    private NsharpCloudInfo cloudInfo;

    // pressure in mb of freezing level, for fgz line drawing
    private float fgzPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // pressure in mb of -20C level, for -20C line drawing
    private float n20CPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // height in ft of -20C level, for -20C line drawing
    private float n20CHeightFt = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // pressure in mb of -30C level, for -30C line drawing
    private float n30CPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // height in ft of -30C level, for -30C line drawing
    private float n30CHeightFt = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    // possible storm watch warning type
    // wwType,
    // 0: NONE, Gempak color 19 Gold;
    // 1: MRGL SVR, Gempak color 26 Sky Blue;
    // 2: SVR, Gempak color 6 Cyan;
    // 3: MRGL TOR, Gempak color 2 red;
    // 4: TOR, Gempak color 2 red;
    // 5: PDS TOR, Gempak color 7 Magenta;
    private int wwtype = 0;

    private RGB wwTypeColor = NsharpConstants.color_gold;

    private String wwtypeStr = "NONE";

    // lapse rate max parameters
    private LapseRateMax lrm;

    private RGB lrmColor;

    /*******************************************************************
     * HODO Pane weather parameters
     *******************************************************************/
    // effective layer top layer wind direction
    private float effLayerTopWindUComp;

    // effective layer top layer wind speed
    private float effLayerTopWindVComp;

    // effective layer bottom layer wind direction
    private float effLayerBotWindUComp;

    // effective layer bottom layer wind speed
    private float effLayerBotWindVComp;

    // critical angle
    private float criticalAngle;

    /*******************************************************************
     * WITO Pane weather parameters
     *******************************************************************/
    // map <pressure, advection>
    private Map<Float, Float> advectionMap = new HashMap<>();

    /*******************************************************************
     * INSET Pane weather parameters
     *******************************************************************/
    /**********************************
     * Vertical Sr wind vs height inset
     **********************************/
    // sfc-2km and 4-6km sr wind saved at data Pane Page 2
    // ("Sum2" page)
    // 9-11 km sr wind saved at data Pane Page 7
    // ("STORM RELATIVE" page)
    // map <Height, wind speed>
    private Map<Float, Float> verticalSrWindMap = new HashMap<>();

    /*****************************************************
     * Theta-E vs Pressure inset & Theta-E vs Height inset
     *****************************************************/
    // theta E difference already save at Data Pane Page 4 ("THERMODYNAMIC DATA"
    // page)
    // theta E at 700C
    private float thetaE700;

    // theta E at 850C
    private float thetaE850;

    // theta E at surface
    private float thetaESurface;

    // AGL at 500mb
    private float agl500mb;

    // press vs theta E for each sounding layer >= 500mb; map<Pressure, thetaE>
    private Map<Float, Float> pressThetaEMap = new HashMap<>();

    // theta E vs AGL height for each sounding layer >= 500mb; map<thetaE, AGL
    // Height>
    private Map<Float, Float> thetaEHeightMap = new HashMap<>();

    // pressure vs AGL height from 600 mb to 1000 with step of 100mb
    // used for plotting height legend for
    private Map<Float, Float> pressHeightMap = new HashMap<>();

    /**********************
     * Storm Slinky inset
     *********************/
    // NsharpStormSlinkyInfo structure store storm slinky plotting parameters
    // values that are parcel type relevant.
    // stormSlinkyInfoMap contains 6 NsharpStormSlinkyInfo structure one for one
    // parcel. Its key is "parcel number" and value is NsharpStormSlinkyInfo
    // structure. parcel number defined in NsharpLibSndglib.java
    private Map<Integer, NsharpStormSlinkyInfo> stormSlinkyInfoMap = new HashMap<>();

    /*******************************************************************
     * SPC special graphs Pane weather parameters
     *******************************************************************/
    /**********************
     * EBS graph
     *********************/
    // ebsMap: its key is integer number used for ebs computation, its value is
    // EBS value
    private Map<Integer, Float> ebsMap = new HashMap<>();

    /**********************
     * STP graph
     *********************/
    // effective bulk shear
    private float effShear;

    /*********************
     * WINTER graph
     *********************/
    private NsharpWinterInfo winterInfo;

    /*********************
     * FIRE graph
     *********************/
    private NsharpFireInfo fireInfo;

    /*********************
     * SARS graph
     *********************/
    private NsharpSarsInfo sarsInfo;

    /*********************
     * HAIL graph
     *********************/
    private HailInfoContainer hailInfoContainer;

    private int userDefdParcelMb = 850;
    
    public NsharpWeatherDataStore() {
        super();
    }

    public int getUserDefdParcelMb() {
        return userDefdParcelMb;
    }


    public void setUserDefdParcelMb(int userDefdParcelMb) {
        this.userDefdParcelMb = userDefdParcelMb;
    }


    /***************************************************************************
     * Important Note:::: This method should only be called when sounding layer
     * list is set. I.e. only be called when nsharp "current" displaying
     * sounding layer list is modified.
     *
     * Results: All weather parameters used for Nsharp GUI are computed and
     * saved in this instance and used by all Nsharp pane resources.
     *
     * @param :
     *            newly modified NcSoundingLayer List - List<NcSoundingLayer>
     *
     ***************************************************************************/
    public void computeWeatherParameters(List<NcSoundingLayer> soundingLys, String paneConfigurationName) {
        if (soundingLys == null) {
            return;
        }
        this.soundingLys = organizeSoundingForComputation(soundingLys);

        // set surface layer
        sfcLayer = NsharpLibBasics.sfc(this.soundingLys);

        /************** COMPUTE DATA PANE parameters *****************/
        // Note: the following methods invoking sequence is critical
        // do not change the sequence unless you know what you are doing.
        computeParcelParameters();
        computePage1Parameters();
        computePage2Parameters();
        // all page 3 parameters covered by computeParcelParameters()
        computePage4Parameters();
        computePage5Parameters();
        computePage6Parameters();
        computePage7Parameters();
        computePage8Parameters();
        computePage9Parameters();
        computePage10Parameters();
        // all page 11 parameters covered by computeParcelParameters(),
        // computePage1Parameters(), computePage12Parameters()

        /************** COMPUTE SKEWT PANE parameters *****************/
        computeSkewTPaneParameters();

        /************** COMPUTE HODO PANE parameters *****************/
        computeHodoPaneParameters();

        if (paneConfigurationName.equals((NsharpConstants.PANE_SPCWS_CFG_STR))) {
            /************** COMPUTE WITO PANE parameters *****************/
            computeWitoPaneParameters();

            /************** COMPUTE INSET PANE parameters *****************/
            computeInsetPaneParameters();

            /**************
             * COMPUTE SPC graphs PANE parameters
             *****************/
            computeSPCGraphPaneParameters();
        }
    }

    /***************************************************************************
     * Important Note:::: This method should only be called when storm wind is
     * manually set by user, i.e. when user click on HODO pane with left mouse
     * button
     *
     * Results: All weather parameters effected by storm wind should be
     * recomputed.
     *
     * @param :
     *            speed - storm wind speed
     * @param :
     *            direction - storm wind direction
     *
     ***************************************************************************/
    public void setStorm(float speed, float direction) {
        smdir = direction;
        smspd = speed;
        computeParcelSrWindParameters();
        Parcel mlParcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
        Parcel muParcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
        // Compute the Supercell Composite Parameter, SCP
        if (muParcel == null) {
            scp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            scp = NsharpLibXwvid.scp(soundingLys, smdir, smspd, muParcel);
        }
        // compute Significant Tornado Parameter (effective layer with CIN)
        if (muParcel == null || mlParcel == null) {
            stpCin = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            stpCin = NsharpLibXwvid.sigtorn_cin(soundingLys, smdir, smspd, muParcel, mlParcel);
        }
        // compute Significant Tornado Parameter (fixed layer)
        stpFixed = NsharpLibXwvid.sigtorn_fixed(soundingLys, smdir, smspd,
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_OBS_SFC));

        for (int i = 0; i < STORM_MOTION_TYPE_STR.length; i++) {
            if ("LCL-EL(Cloud Layer)".equals(STORM_MOTION_TYPE_STR[i])) {
                // mean wind at LCL-EL, SR mean wind at LCL-EL, wind shear at
                // LCL-EL are parcel relevant and computed @
                // computeParcelParameters() and save at ParcelMap
                continue;
            }
            float h1 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float h2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float lowerLayerPres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float upperLayerPres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            if ("Eff Inflow".equals(STORM_MOTION_TYPE_STR[i])) {

                if (effLyPress.getBottomPress() > 0 && NsharpLibBasics.qc(effLyPress.getBottomPress())) {
                    lowerLayerPres = effLyPress.getBottomPress();
                    h1 = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, lowerLayerPres));
                    upperLayerPres = effLyPress.getTopPress();
                    h2 = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, upperLayerPres));
                }
            } else if ("Lower Half SR Depth".equals(STORM_MOTION_TYPE_STR[i])) {
                Parcel parcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
                if (parcel != null) {
                    float el = NsharpLibBasics.agl(soundingLys,
                            NsharpLibBasics.i_hght(soundingLys, parcel.getElpres()));
                    if (parcel.getBplus() >= 100.0) {
                        lowerLayerPres = effLyPress.getBottomPress();
                        float base = NsharpLibBasics.agl(soundingLys,
                                NsharpLibBasics.i_hght(soundingLys, lowerLayerPres));
                        float depth = (el - base);
                        h1 = base;
                        h2 = base + (depth * 0.5f);
                        upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h2));
                    }
                }
            } else {
                h1 = STORM_MOTION_HEIGHT[i][0];
                h2 = STORM_MOTION_HEIGHT[i][1];
                lowerLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h1));
                upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h2));
            }

            // calculate pressure-weighted SR mean wind
            WindComponent srMeanWindComp = NsharpLibWinds.sr_wind(soundingLys, lowerLayerPres, upperLayerPres, smdir,
                    smspd);
            stormTypeToSrMeanWindMap.put(STORM_MOTION_TYPE_STR[i], srMeanWindComp);

            // calculate SRH helicity
            Helicity helicity = NsharpLibWinds.helicity(soundingLys, h1, h2, smdir, smspd);
            stormTypeToHelicityMap.put(STORM_MOTION_TYPE_STR[i], helicity);

        }

        // calcute 4-6km SR mean wind
        computeSrWind4To6km();

        // compute STP LR
        if (muParcel == null || mlParcel == null) {
            stpLr = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        } else {
            stpLr = NsharpLibXwvid.sigtorn_lr(soundingLys, smdir, smspd, muParcel, mlParcel, effLyPress);
        }

        // calcute 9-11km SR mean wind
        computeSrWind9To11km();

        // compute possible storm watch type
        computeWWType();

        // compute critical angle for HODO pane
        computeCriticalAngle();

        // compute height vs storm wind map for SRWind inset
        // height form 0 to 16000m with step of 250m
        verticalSrWindMap.clear();
        for (float h = 0; h <= 16_000; h += 250) {
            float layerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h));
            WindComponent srWindComp = NsharpLibWinds.sr_wind(soundingLys, layerPres, layerPres, smdir, smspd);
            verticalSrWindMap.put(h, srWindComp.getWspd());
        }
        // compute stormSlinkyInfoMap for Storm Slinky inset pane
        // compute strom slinky for all parcels
        stormSlinkyInfoMap.clear();
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            Parcel parcel = parcelMap.get(parcelNumber);
            NsharpStormSlinkyInfo stormSlinkyInfo = null;
            if (parcel != null) {
                stormSlinkyInfo = NsharpStormSlinkyInfo.computeStormSlinky(parcel.getLfcpres(), parcel.getElpres(),
                        parcel.getLplpres(), parcel.getLpltemp(), parcel.getLpldwpt(), soundingLys, smdir, smspd);
            }
            stormSlinkyInfoMap.put(parcelNumber, stormSlinkyInfo);

        }
    }

    /********************************************************************
     * remove sounding data with pressure below 100mb for computation
     *******************************************************************/
    private List<NcSoundingLayer> organizeSoundingForComputation(List<NcSoundingLayer> soundingLys) {
        // since sounding data are listed from highest pressure layer, we
        // perform
        // removal action from list bottom
        for (int i = soundingLys.size() - 1; i >= 0; i--) {
            NcSoundingLayer sndLy = soundingLys.get(i);
            if (sndLy.getPressure() < 100) {
                soundingLys.remove(i);
            }
        }
        return soundingLys;
    }

    /*******************************************************************
     * compute all Parcel relevant weather parameters
     *******************************************************************/
    private void computeParcelParameters() {
        parcelMap.clear();
        parcelMiscParamsMap.clear();
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            float layerPressure = parcelToLayerPressMap.get(parcelNumber);
            if (parcelNumber == NsharpLibSndglib.PARCELTYPE_USER_DEFINED) {
                // get user set parcel pressure, if available
                layerPressure = userDefdParcelMb;
            }
            LParcelValues lparcelVs = NsharpLibSkparams.define_parcel(soundingLys, parcelNumber, layerPressure);
            Parcel parcel = NsharpLibSkparams.parcel(soundingLys, -1.0F, -1.0F, lparcelVs.getPres(),
                    lparcelVs.getTemp(), lparcelVs.getDwpt());
            if (parcel == null) {
                continue;
            }
            // compute BRN, BrnShear
            float pressTop, pressBot;
            if (parcelNumber == NsharpLibSndglib.PARCELTYPE_OBS_SFC
                    || parcelNumber == NsharpLibSndglib.PARCELTYPE_FCST_SFC
                    || parcelNumber == NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE) {
                pressTop = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 6000.0f));
                pressBot = NsharpLibBasics.sfcPressure(soundingLys);
            } else {
                pressBot = NsharpLibBasics.i_pres(soundingLys,
                        (NsharpLibBasics.i_hght(soundingLys, parcel.getLplpres()) - 500.0f));
                if (!NsharpLibBasics.qc(pressBot)) {
                    pressBot = NsharpLibBasics.sfcPressure(soundingLys);
                }
                pressTop = NsharpLibBasics.i_pres(soundingLys,
                        (NsharpLibBasics.i_hght(soundingLys, pressBot) + 6000.0f));
            }
            float brnshear = NsharpLibSkparams.bulk_rich(soundingLys, pressTop, pressBot, parcel.getLplpres(),
                    parcel.getBplus());

            float brn = parcel.getBplus() / brnshear;
            parcel.setBrnShear(brnshear);
            parcel.setBrn(brn);

            ParcelMiscParams parcelMiscParams = new ParcelMiscParams();
            // Calculate mean wind at LFC to EL
            WindComponent meanWindCompLfcToEl = NsharpLibWinds.mean_wind(soundingLys, parcel.getLfcpres(),
                    parcel.getElpres());
            parcelMiscParams.setMeanWindCompLfcToEl(meanWindCompLfcToEl);

            // Calculate mean wind at LCL to EL
            WindComponent meanWindCompLclToEl = NsharpLibWinds.mean_wind(soundingLys, parcel.getLclpres(),
                    parcel.getElpres());
            parcelMiscParams.setMeanWindCompLclToEl(meanWindCompLclToEl);

            // Calculate wind shear at LCL to EL
            WindComponent windShearLclToEl = NsharpLibWinds.wind_shear(soundingLys, parcel.getLclpres(),
                    parcel.getElpres());
            parcelMiscParams.setWindShearLclToEl(windShearLclToEl.getWspd());

            // construct MU parcel name for data page 1 to use
            parcelMiscParams.setMuName("MU (" + Math.round(parcel.getLplpres()) + " mb)");

            parcelMiscParamsMap.put(parcelNumber, parcelMiscParams);
            // calculate mix ratio
            parcel.setMixRatio(NsharpLibThermo.mixratio(lparcelVs.getPres(), lparcelVs.getDwpt()));
            parcelMap.put(parcelNumber, parcel);
        }
        // Note: bunkers_storm_motion() and effective_inflow_layer() algorithms
        // require parcelMap information, therefore, do it after parcelMap
        // set.

        // get storm motion speed and direction for later computations need
        // This is important to assign global smdir/smspd for some wind
        // parameters computations. Therefore, should be done before other
        // computations.
        // also save storm motion wind components for PAGE 2 to use
        bunkersStormMotionWindComp = NsharpLibXwvid.bunkers_storm_motion(soundingLys);
        // BigSharp uses bunkers' storm motion right for global storm
        // direction/speed
        smdir = bunkersStormMotionWindComp[0].getWdir();
        smspd = bunkersStormMotionWindComp[0].getWspd();

        // set global effect layer pressure for alter use
        effLyPress = NsharpLibXwvid.effective_inflow_layer(soundingLys, 100, -250,
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE));

        // need smdir and smspd for SR mean wind computation, therefore does it
        // after smdir and smapd are set
        computeParcelSrWindParameters();
        avgWetbulbTemp = NsharpLibSkparams.Mean_WBtemp(soundingLys, -1, -1);
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            Parcel parcel = parcelMap.get(parcelNumber);
            ParcelMiscParams parcelMiscParams = parcelMiscParamsMap.get(parcelNumber);
            if (parcelMiscParams == null || parcel == null) {
                continue;
            }

            // CHI1, CHI2
            float chi1 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float chi2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

            chi1 = (parcel.getBplus() * parcel.getBrnShear()) / NsharpLibBasics.agl(soundingLys,
                    NsharpLibBasics.i_hght(soundingLys, NsharpLibSkparams.wb_lvl(soundingLys, 0)));
            chi2 = chi1 / avgWetbulbTemp;

            parcelMiscParams.setChi1(chi1);
            parcelMiscParams.setChi2(chi2);
        }

    }

    /*******************************************************************
     * compute only those Parcel parameters effected by storm wind
     *******************************************************************/
    private void computeParcelSrWindParameters() {
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            Parcel parcel = parcelMap.get(parcelNumber);
            ParcelMiscParams parcelMiscParams = parcelMiscParamsMap.get(parcelNumber);
            if (parcel == null || parcelMiscParams == null) {
                continue;
            }
            // Calculate SR mean wind at LCL to EL
            WindComponent srMeanWindCompSrLclToEl = NsharpLibWinds.sr_wind(soundingLys, parcel.getLclpres(),
                    parcel.getElpres(), smdir, smspd);

            parcelMiscParams.setSrMeanWindCompLclToEl(srMeanWindCompSrLclToEl);
            // SR wind at sfc-lfc
            WindComponent srMeanWindCompSfcToLfc = NsharpLibWinds.sr_wind(soundingLys,
                    NsharpLibBasics.sfcPressure(soundingLys), parcel.getLfcpres(), smdir, smspd);
            parcelMiscParams.setSrMeanWindCompSfcToLfc(srMeanWindCompSfcToLfc);
            // SR wind at lfc- lfc+4km
            float lfcP4km = NsharpLibBasics.i_pres(soundingLys,
                    NsharpLibBasics.i_hght(soundingLys, parcel.getLfcpres()) + 4000);
            WindComponent srMeanWindCompLfcToLFCP4km = NsharpLibWinds.sr_wind(soundingLys, parcel.getLfcpres(), lfcP4km,
                    smdir, smspd);
            parcelMiscParams.setSrMeanWindCompLfcToLFCP4km(srMeanWindCompLfcToLFCP4km);

            // SR wind at el-4km - el
            float elM4km = NsharpLibBasics.i_pres(soundingLys,
                    NsharpLibBasics.i_hght(soundingLys, parcel.getElpres()) - 4000);
            WindComponent srMeanWindCompElM4kmToEl = NsharpLibWinds.sr_wind(soundingLys, elM4km, parcel.getElpres(),
                    smdir, smspd);
            parcelMiscParams.setSrMeanWindCompElM4kmToEl(srMeanWindCompElM4kmToEl);

            // SR wind at el
            WindComponent srWindCompEl = NsharpLibWinds.sr_wind(soundingLys, parcel.getElpres() + 25,
                    parcel.getElpres() - 25, smdir, smspd);
            parcelMiscParams.setSrWindCompEl(srWindCompEl);

            // calculate LPL-LFC sw helicity
            Helicity helicity = NsharpLibWinds.helicity(soundingLys, -1/* lplHeight, */, -1/* lfcHeight */, smdir,
                    smspd);
            parcelMiscParams.setHelicityLplToLfc(helicity);
        }

    }

    /*******************************************************************
     * Compute Data Pane Page 1 ("Sum1" page) weather parameters
     *******************************************************************/
    private void computePage1Parameters() {
        // All parcel relevant parameters, e.g., CAPE, CINH, LCL, LI, LFC, EL
        // etc, are computed @ computeParcelParameters()

        Parcel mlParcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING);
        Parcel muParcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);

        // compute mean mix ratio
        meanMixRatio = NsharpLibSkparams.mean_mixratio(soundingLys, -1f, -1f);
        // compute AGL fgz in ft
        fgzPress = NsharpLibSkparams.temp_lvl(soundingLys, 0);
        fgzft = NsharpLibBasics.mtof(NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, fgzPress)));
        // compute MSL frz in ft
        frzft = NsharpLibBasics.mtof(NsharpLibBasics.i_hght(soundingLys, fgzPress));

        // compute precipitable water in inch
        pw = NcSoundingTools.precip_water(soundingLys, 400, -1) / 25.4f;

        // compute wbz pressure
        wbzp = NsharpLibSkparams.wb_lvl(soundingLys, 0);

        // AGL wbz to ft
        wbzft = NsharpLibBasics.mtof(NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, wbzp)));

        // MSL wbz to ft
        wbzmslft = NsharpLibBasics.mtof(NsharpLibBasics.i_hght(soundingLys, wbzp));

        // compute WNDG
        wndg = NsharpLibXwvid.damaging_wind(soundingLys, mlParcel);

        // compute K Index
        kIndex = NsharpLibSkparams.k_index(soundingLys);

        // compute dcape and downT
        LayerParameters layerParms = new LayerParameters();
        dcape = NsharpLibSkparams.dcape(soundingLys, layerParms);
        downT = layerParms.getTemperature();
        downT = NsharpLibBasics.ctof(downT);

        // compute convective temp
        convT = NsharpLibSkparams.cnvtv_temp(soundingLys, -1);
        convT = NsharpLibBasics.ctof(convT);

        // compute esp
        esp = NsharpLibXwvid.esp(soundingLys, mlParcel);

        // compute MidRh & LowRh
        float surfacePressure = NsharpLibBasics.sfcPressure(soundingLys);
        if (NsharpLibBasics.qc(surfacePressure)) {
            midRh = NsharpLibSkparams.mean_relhum(soundingLys, surfacePressure - 150, surfacePressure - 350);
            lowRh = NsharpLibSkparams.mean_relhum(soundingLys, -1.0F, surfacePressure - 150);
        }

        // compute mmp
        mmp = NsharpLibSkparams.coniglio1(soundingLys);

        // comp maxTemp (F) at default level
        maxTemp = NsharpLibBasics.ctof(NsharpLibSkparams.max_temp(soundingLys, -1));

        // compute NCAPE
        if (muParcel != null) {
            float j1 = muParcel.getBplus();
            float j2 = NsharpLibBasics.i_hght(soundingLys, muParcel.getElpres())
                    - NsharpLibBasics.i_hght(soundingLys, muParcel.getLfcpres());
            if (j2 != 0 && NsharpLibBasics.qc(j1 / j2)) {
                ncape = j1 / j2;
            }
        }
        // compute sfc-3km Agl Lapse Rate (C/km) and temperature delta
        // surface height
        float htsfc = 0;
        float tempThreeKm = 0;
        float pressThreeKm = 0;
        if (NsharpLibBasics.qc(surfacePressure)) {
            htsfc = NsharpLibBasics.i_hght(soundingLys, surfacePressure);
            float tempSfc = NsharpLibBasics.i_temp(soundingLys, surfacePressure);
            // get sfc+ 3 km pressure
            pressThreeKm = NsharpLibBasics.i_pres(soundingLys, (htsfc + 3000));
            tempThreeKm = NsharpLibBasics.i_temp(soundingLys, pressThreeKm);
            sfcTo3kmTempDelta = tempSfc - tempThreeKm;
            sfcTo3kmLapseRate = NsharpLibSkparams.lapse_rate(soundingLys, surfacePressure, pressThreeKm);
        }
        // compute 3km-6km Agl Lapse Rate (C/km) and temperature delta
        // get 6km pressure
        float pressSixKm = NsharpLibBasics.i_pres(soundingLys, (htsfc + 6000));
        // get 6 km temperature
        float tempSixKm = NsharpLibBasics.i_temp(soundingLys, pressSixKm);
        threeKmTo6kmTempDelta = tempThreeKm - tempSixKm;
        threeKmTo6kmLapseRate = NsharpLibSkparams.lapse_rate(soundingLys, pressThreeKm, pressSixKm);

        // compute 850-500 mb Lapse Rate (C/km) and temperature delta
        eight50To500mbTempDelta = NsharpLibBasics.i_temp(soundingLys, 850) - NsharpLibBasics.i_temp(soundingLys, 500);
        eight50To500mbLapseRate = NsharpLibSkparams.lapse_rate(soundingLys, 850.0F, 500.0F);

        // compute 700-500 mb Lapse Rate (C/km) and temperature delta
        sevenHundredTo500mbTempDelta = NsharpLibBasics.i_temp(soundingLys, 700)
                - NsharpLibBasics.i_temp(soundingLys, 500);
        sevenHundredTo500mbLapseRate = NsharpLibSkparams.lapse_rate(soundingLys, 700.0F, 500.0F);

        // Compute the Supercell Composite Parameter, SCP
        scp = NsharpLibXwvid.scp(soundingLys, smdir, smspd, muParcel);

        // compute Significant Tornado Parameter (effective layer with CIN)
        stpCin = NsharpLibXwvid.sigtorn_cin(soundingLys, smdir, smspd, muParcel, mlParcel);

        // compute Significant Tornado Parameter (fixed layer)
        stpFixed = NsharpLibXwvid.sigtorn_fixed(soundingLys, smdir, smspd,
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_OBS_SFC));

        // compute Hail Tornado Parameter, based on MU parcel
        // follow algorithm at cave_ship() in caveNsharp.c

        WindComponent windComp = NsharpLibWinds.wind_shear(soundingLys, NsharpLibBasics.sfcPressure(soundingLys),
                NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 6000)));
        // effective bulk shear, used for SPC STP graph
        effShear = windComp.getWspd();
        float base = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, effLyPress.getBottomPress()));
        if (base > 0.0) {
            float height = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            if (muParcel != null) {
                float el = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, muParcel.getElpres()));
                float depth = el - base;
                height = base + (depth * 0.5f);
            }

            WindComponent windCompEff = NsharpLibWinds.wind_shear(soundingLys, effLyPress.getBottomPress(),
                    NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, height)));
            effShear = windCompEff.getWspd();
        }
        ship = NsharpLibXwvid.sig_hail(soundingLys, sevenHundredTo500mbLapseRate,
                NsharpLibBasics.i_temp(soundingLys, 500), NsharpLibBasics.kt_to_mps(effShear), fgzft, mlParcel,
                muParcel);
    }

    /*******************************************************************
     * compute data Pane Page 2 ("Sum2" page) weather parameters
     *******************************************************************/
    private void computePage2Parameters() {
        // BRN Shear is computed @
        // computeParcelParameters() and save at ParcelMap
        // Bunker's storm motion is computed @
        // computeParcelParameters()

        for (int i = 0; i < STORM_MOTION_TYPE_STR.length; i++) {
            if ("LCL-EL(Cloud Layer)".equals(STORM_MOTION_TYPE_STR[i])) {
                // mean wind at LCL-EL, SR mean wind at LCL-EL, wind shear at
                // LCL-EL are parcel relevant and computed @
                // computeParcelParameters() and save at ParcelMap
                continue;
            }
            float h1 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float h2 = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float lowerLayerPres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            float upperLayerPres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            if ("Eff Inflow".equals(STORM_MOTION_TYPE_STR[i])) {

                if (effLyPress.getBottomPress() > 0 && NsharpLibBasics.qc(effLyPress.getBottomPress())) {
                    lowerLayerPres = effLyPress.getBottomPress();
                    h1 = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, lowerLayerPres));
                    upperLayerPres = effLyPress.getTopPress();
                    h2 = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, upperLayerPres));
                }
            } else if ("Eff Shear(EBWD)".equals(STORM_MOTION_TYPE_STR[i])) {
                Parcel parcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
                if (parcel != null) {
                    float el = NsharpLibBasics.agl(soundingLys,
                            NsharpLibBasics.i_hght(soundingLys, parcel.getElpres()));
                    if (parcel.getBplus() >= 100.0) {
                        lowerLayerPres = effLyPress.getBottomPress();
                        float base = NsharpLibBasics.agl(soundingLys,
                                NsharpLibBasics.i_hght(soundingLys, lowerLayerPres));
                        float depth = (el - base);
                        h1 = base;
                        h2 = base + (depth * 0.5f);
                        upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h2));
                    }
                }
            } else {
                h1 = STORM_MOTION_HEIGHT[i][0];
                h2 = STORM_MOTION_HEIGHT[i][1];
                lowerLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h1));
                upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h2));
            }

            // Calculate mean wind
            WindComponent meanwindWindComp = NsharpLibWinds.mean_wind(soundingLys, lowerLayerPres, upperLayerPres);
            stormTypeToMeanWindMap.put(STORM_MOTION_TYPE_STR[i], meanwindWindComp);

            // calculate pressure-weighted SR mean wind
            WindComponent srMeanWindComp = NsharpLibWinds.sr_wind(soundingLys, lowerLayerPres, upperLayerPres, smdir,
                    smspd);
            stormTypeToSrMeanWindMap.put(STORM_MOTION_TYPE_STR[i], srMeanWindComp);

            // Calculate wind shear
            WindComponent shearWindComp = NsharpLibWinds.wind_shear(soundingLys, lowerLayerPres, upperLayerPres);
            stormTypeToWindShearMap.put(STORM_MOTION_TYPE_STR[i], shearWindComp.getWspd());

            // calculate SRH helicity
            Helicity helicity = NsharpLibWinds.helicity(soundingLys, h1, h2, smdir, smspd);
            stormTypeToHelicityMap.put(STORM_MOTION_TYPE_STR[i], helicity);

        }

        // calcute 4-6km SR mean wind
        computeSrWind4To6km();

        // compute corfidi down and up shear
        cofidiShearWindComp = NsharpLibWinds.corfidi_MCS_motion(soundingLys);

        // compute STP-lr
        stpLr = NsharpLibXwvid.sigtorn_lr(soundingLys, smdir, smspd,
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE),
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING), effLyPress);

        // compute large hail parameter
        lhp = NsharpLibXwvid.large_hail_param(soundingLys, smdir, smspd);

        // compute Modified SHERBE
        Parcel muParcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
        if (muParcel != null) {
            modSherbe = NsharpLibXwvid.moshe(soundingLys, muParcel, effLyPress);
        }

    }

    /*******************************************************************
     * compute data Pane Page 4 ("THERMODYNAMIC DATA" page) weather parameters
     *******************************************************************/
    private void computePage4Parameters() {
        // PW, meanlowRh, 700-500 mb Lapse Rate, 850-500 mb Lapse Rate, K index,
        // MaxTemp, ConvTemp, WBZ, and FGZ are listed in Page1
        // All Parcel LIs are listed in Parcel (parcelMap)

        // compute meanRh
        float surfacePressure = NsharpLibBasics.sfcPressure(soundingLys);
        if (NsharpLibBasics.qc(surfacePressure)) {
            meanRh = NsharpLibSkparams.mean_relhum(soundingLys, -1.0F, -1.0F);
        }
        // Top of MoistLyr pressure in mb
        topMoistLyrPress = NsharpLibSkparams.top_moistlyr(soundingLys);

        // Top of MoistLyr height in ft
        topMoistLyrHeight = NsharpLibBasics
                .mtof(NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, topMoistLyrPress)));

        // Total Totals
        totTots = NsharpLibSkparams.t_totals(soundingLys);

        // sweat Index
        sweatIndex = NsharpLibSkparams.sweat_index(soundingLys);

        // ThetaE Diff in C
        thetaDiff = NsharpLibSkparams.ThetaE_diff(soundingLys);

    }

    /*******************************************************************
     * compute data Pane Page 5 ("OPC LOW LEVEL" page) weather parameters
     *******************************************************************/
    private void computePage5Parameters() {
        // compute 975mb level height and temp
        height975 = NsharpLibBasics.i_hght(soundingLys, 975);
        temp975 = NsharpLibBasics.i_temp(soundingLys, 975);

        // compute lowest inversion height
        LayerParameters lowInvParams = NsharpLibSkparams.low_inv(soundingLys);
        lowestInvHeight = NsharpLibBasics.i_hght(soundingLys, lowInvParams.getPressure());

        // lowest inversion pressure
        lowestInvPressure = lowInvParams.getPressure();

        // lowest inversion temp change
        lowestInvTempChange = lowInvParams.getTemperature();
    }

    /*******************************************************************
     * compute data Pane Page 6 ("MIXING HEIGHT" page) weather parameters
     *******************************************************************/
    private void computePage6Parameters() {

        // layer Based mix height parameters
        mixHeightLayerBased = NsharpLibSkparams.mix_height(soundingLys, 1);
        mixHeightLayerBased.setMh_hgt(NsharpLibBasics.i_hght(soundingLys, mixHeightLayerBased.getMh_pres()));

        // Surface Based mix height parameters
        mixHeightSurfaceBased = NsharpLibSkparams.mix_height(soundingLys, 0);
        mixHeightSurfaceBased.setMh_hgt(NsharpLibBasics.i_hght(soundingLys, mixHeightSurfaceBased.getMh_pres()));
    }

    /*******************************************************************
     * compute data Pane Page 7 ("STORM RELATIVE" page) weather parameters
     *******************************************************************/
    private void computePage7Parameters() {
        // sfc-2km SR mean wind, 4-6km SR mean wind, sfc-2km and Sfc-3km sr
        // helicity are computed in computePage2Parameters()
        // LPL-LFC sr helicity is parcel relevant and computed in
        // computeParcelParameters()

        // calcute 9-11km SR mean wind
        computeSrWind9To11km();
    }

    /*******************************************************************
     * compute data Pane Page 8 ("MEAN WIND" page) weather parameters
     *******************************************************************/
    private void computePage8Parameters() {
        // mean wind at LFC - EL is parcel relevant and is computed @
        // computeParcelParameters()

        // mean wind at 0-6 km is already computed at computePage2Parameters()

        // Calculate mean wind at 850 to 200 mb
        meanWindComp850To200mb = NsharpLibWinds.mean_wind(soundingLys, 850, 200);

        // Calculate wind shear at sfc-12km
        float upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 12_000));
        float surfacePressure = NsharpLibBasics.sfcPressure(soundingLys);
        shearWindCompSfcTo12km = NsharpLibWinds.wind_shear(soundingLys, surfacePressure, upperLayerPres);
    }

    /*******************************************************************
     * compute data Pane Page 9 ("CONVECTIVE INITIATION" page) weather
     * parameters
     *******************************************************************/
    private void computePage9Parameters() {
        // BRN, BRN Shear, LFC are parcel relevant and is computed @
        // computeParcelParameters()
        // CINH, CAPE, LFC, Cap, BRN, BRN Shear are parcel relevant and computed
        // @ computeParcelParameters() and saved in ParcelMap
        // Kindex computed stored in page 1
        // MeanRH, Top of Moist Lyr stored in page 4
        // Melting level pressure and height stored in page 1 as wbzp and wbzft

        // Eff SREH,
        Helicity helicity = NsharpLibWinds.helicity(soundingLys, -1, -1, smdir, smspd);
        effSreh = helicity.getTotalHelicity();

        // 3km shear, use default input as used by BigSharp
        windShear3km = NsharpLibWinds.wind_shear(soundingLys, -1f, -1f).getWspd();
        windShear3km = NsharpLibBasics.kt_to_mps(windShear3km);

        // Rogash rain fall rate
        RogashRainRate = NsharpLibSkparams.Rogash_QPF(soundingLys);
    }

    /*******************************************************************
     * compute data Pane Page 10 ("SEVERE POTENTIAL" page) weather parameters
     *******************************************************************/
    private void computePage10Parameters() {
        // CAPE is parcel relevant and is saved in ParcelMap
        // WBZ, FGZ saved in page 1
        // CHI1, CHI2, Low SRW (sfc-LFC), Mid SRW (LFC - LFC+4km), upper SRW
        // (EL-4km - EL) are parcel relevant and saved in parcelMiscParamsMap

        // 700-500 mb mid level RH
        midLvlRH500To700 = NsharpLibSkparams.mean_relhum(soundingLys, 700, 500);

        // Avg BL wetbulb temperature is computed at computeParcelParameters()
    }

    /*******************************************************************
     * compute all SkewT pane weather parameters
     *******************************************************************/
    private void computeSkewTPaneParameters() {
        // fgz pressure: fgzPress is already computed at page1 computation

        // compute and get cloud info
        cloudInfo = new NsharpCloudInfo(this.soundingLys);

        // compute -20C pressure in mb and height in ft
        n20CPress = NsharpLibSkparams.temp_lvl(soundingLys, -20);
        n20CHeightFt = NsharpLibBasics
                .mtof(NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, n20CPress)));

        // compute -30C pressure in mb and height in ft
        n30CPress = NsharpLibSkparams.temp_lvl(soundingLys, -30);
        n30CHeightFt = NsharpLibBasics
                .mtof(NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, n30CPress)));

        // compute possible storm watch type
        computeWWType();

        // compute lapse rate maximum, between 2000m and 6000m AGL, with depth
        // 2000m
        lrm = NsharpLibXwvid.lapse_rate_max(soundingLys, 2000, 6000, 2000);

        // lrm return Gempak color 2, 7, 8, 18 and 19
        switch (lrm.getColor()) {
        case 7:
        default:
            lrmColor = NsharpConstants.color_magenta;
            break;
        case 2:
            lrmColor = NsharpConstants.color_red;
            break;
        case 8:
            lrmColor = NsharpConstants.color_brown;
            break;
        case 18:
            lrmColor = NsharpConstants.color_darkorange;
            break;
        case 19:
            lrmColor = NsharpConstants.color_gold;
            break;
        }
    }

    /*******************************************************************
     * compute all Wito pane weather parameters
     *******************************************************************/
    private void computeWitoPaneParameters() {
        float surfPressure = sfcLayer.getPressure();
        advectionMap.clear();
        for (float pressure = surfPressure; pressure >= 200; pressure -= 100) {
            float advt = NsharpLibSkparams.advection_layer(soundingLys, pressure, pressure - 100);
            advectionMap.put(pressure, advt);
        }
    }

    /*******************************************************************
     * compute all Inset pane weather parameters
     *******************************************************************/
    private void computeInsetPaneParameters() {
        /***************
         * SRWind inset
         **************/
        // sfc-2km and 4-6km sr wind already computed at data Pane Page 2
        // ("Sum2" page)
        // 9-11 km sr wind already computed at data Pane Page 7
        // ("STORM RELATIVE" page)

        // compute height vs storm wind map for SRWind inset
        // height form 0 to 16000m with step of 250m
        for (float h = 0; h <= 16_000; h += 250) {
            float layerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, h));
            WindComponent srWindComp = NsharpLibWinds.sr_wind(soundingLys, layerPres, layerPres, smdir, smspd);
            verticalSrWindMap.put(h, srWindComp.getWspd());
        }

        /*****************************************************
         * Theta-E vs Pressure inset & Theta-E vs Height inset
         *****************************************************/
        // theta E at 700C
        float temp700 = NsharpLibBasics.i_temp(soundingLys, 700);
        float dewPt700 = NsharpLibBasics.i_dwpt(soundingLys, 700);
        thetaE700 = NsharpLibThermo.thetae(700, temp700, dewPt700);
        // theta E at 850C
        float temp850 = NsharpLibBasics.i_temp(soundingLys, 850);
        float dewPt850 = NsharpLibBasics.i_dwpt(soundingLys, 850);
        thetaE850 = NsharpLibThermo.thetae(850, temp850, dewPt850);

        // theta E at surface
        thetaESurface = NsharpLibThermo.thetae(sfcLayer.getPressure(), sfcLayer.getTemperature(),
                sfcLayer.getDewpoint());

        // AGL at 500mb
        agl500mb = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, 500));

        // compute pressThetaEMap, thetaEHeightMap
        // for each sounding layer >= 500mb
        for (NcSoundingLayer lys : soundingLys) {
            if ((lys.getPressure() >= 500) && (NsharpLibBasics.qc(lys.getDewpoint()))) {
                float thetaE = NsharpLibThermo.thetae(lys.getPressure(), lys.getTemperature(), lys.getDewpoint());
                // add (press,thetaE) to map<Pressure, thetaE>
                pressThetaEMap.put(lys.getPressure(), thetaE);
                // add (thetaE, aglHeight) to map<thetaE, Agl Height>
                float height = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, lys.getPressure()));
                thetaEHeightMap.put(thetaE, height);
            }
        }

        // pressure vs height from 600 mb to 1000 with step of 100mb
        // used for plotting height legend for
        for (float pres = 1000; pres > 500; pres -= 100) {
            float height = NsharpLibBasics.agl(soundingLys, NsharpLibBasics.i_hght(soundingLys, pres));
            if (NsharpLibBasics.qc(height)) {
                pressHeightMap.put(pres, height);
            }
        }

        /***********************
         * Storm Slinky inset
         **********************/
        // compute strom slinky for all parcels
        for (int parcelNumber = 1; parcelNumber <= NsharpLibSndglib.PARCEL_MAX; parcelNumber++) {
            Parcel parcel = parcelMap.get(parcelNumber);
            NsharpStormSlinkyInfo stormSlinkyInfo = null;
            if (parcel != null) {
                stormSlinkyInfo = NsharpStormSlinkyInfo.computeStormSlinky(parcel.getLfcpres(), parcel.getElpres(),
                        parcel.getLplpres(), parcel.getLpltemp(), parcel.getLpldwpt(), soundingLys, smdir, smspd);
            }
            stormSlinkyInfoMap.put(parcelNumber, stormSlinkyInfo);

        }
    }

    /*******************************************************************
     * compute SPC special graphs Pane weather parameters
     *******************************************************************/
    private void computeSPCGraphPaneParameters() {
        /********************
         * compute EBS graph
         ********************/
        Parcel parcel = parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE);
        if (parcel != null) {
            float base = NsharpLibBasics.agl(soundingLys,
                    NsharpLibBasics.i_hght(soundingLys, effLyPress.getBottomPress()));
            float depth = (NsharpLibBasics.ftom(parcel.getElAgl()) - base);
            ebsMap.clear();
            for (int i = 10; i <= 100; i = i + 10) {
                WindComponent shearWindComp = NsharpLibWinds.wind_shear(soundingLys, effLyPress.getBottomPress(),
                        NsharpLibBasics.i_pres(soundingLys,
                                NsharpLibBasics.msl(soundingLys, base + (depth * 0.1f * (i / 10)))));
                float ebs = shearWindComp.getWspd();
                if (NsharpLibBasics.qc(ebs)) {
                    if (ebs > 70) {
                        ebs = 70;
                    }
                    ebsMap.put(i, ebs);
                }
            }
        }
        /********************
         * compute STP graph
         ********************/
        // effShear, stpFixed, stpCin already computed at page 1
        // effect layer helicity is already computed at page 2

        /***********************
         * compute WINTER graph
         ***********************/
        winterInfo = NsharpWinterInfo.computeWinterInfo(soundingLys);

        /***********************
         * compute FIRE graph
         ***********************/
        fireInfo = NsharpFireInfo.computeFireInfo(soundingLys);

        /***********************
         * compute SARS graph
         ***********************/
        sarsInfo = NsharpSarsInfo.computeSarsInfo(soundingLys, this);

        /***********************
         * compute HAIL graph
         ***********************/
        NsharpHailInfo hailInfo = new NsharpHailInfo();
        hailInfoContainer = hailInfo.computeHailInfo(soundingLys, this, sarsInfo);
    }

    /*******************************************************************
     * compute all Hodo pane weather parameters
     *******************************************************************/
    private void computeHodoPaneParameters() {
        effLayerTopWindUComp = NsharpLibBasics.i_wndu(soundingLys, effLyPress.getTopPress());
        effLayerTopWindVComp = NsharpLibBasics.i_wndv(soundingLys, effLyPress.getTopPress());
        effLayerBotWindUComp = NsharpLibBasics.i_wndu(soundingLys, effLyPress.getBottomPress());
        effLayerBotWindVComp = NsharpLibBasics.i_wndv(soundingLys, effLyPress.getBottomPress());
        computeCriticalAngle();
    }

    /*******************************************************************
     * computeCriticalAngle() This function is based on trace_esrh() at xwvid1.c
     * compute critical angle from Guiliano and Esterheld (2007)
     *******************************************************************/
    private void computeCriticalAngle() {
        if (!NsharpLibBasics.qc(effLyPress.getBottomPress())) {
            criticalAngle = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
            return;
        }
        WindComponent srMeanWindComp = NsharpLibWinds.sr_wind(soundingLys, effLyPress.getBottomPress(),
                effLyPress.getTopPress(), smdir, smspd);
        float srDir = srMeanWindComp.getWdir();

        WindComponent shearWindComp = NsharpLibWinds.wind_shear(soundingLys, effLyPress.getBottomPress(),
                NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 500)));
        float shearDir = shearWindComp.getWdir();

        if (srDir <= 180) {
            criticalAngle = (srDir + 180) - shearDir;
        } else {
            if (shearDir <= 180) {
                criticalAngle = (srDir - 180) - shearDir;
            } else {
                criticalAngle = 180 - ((shearDir - 180) - (srDir - 180));
            }
        }
    }

    private void computeSrWind9To11km() {
        // calcute 9-11km SR mean wind
        float lowerLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 9000));
        float upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 11_000));
        srMeanWindComp9To11km = NsharpLibWinds.sr_wind(soundingLys, lowerLayerPres, upperLayerPres, smdir, smspd);
    }

    private void computeSrWind4To6km() {
        // calcute 4-6km SR mean wind
        float lowerLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 4000));
        float upperLayerPres = NsharpLibBasics.i_pres(soundingLys, NsharpLibBasics.msl(soundingLys, 6000));
        srMeanWindComp4To6km = NsharpLibWinds.sr_wind(soundingLys, lowerLayerPres, upperLayerPres, smdir, smspd);
    }

    private void computeWWType() {
        // compute possible storm watch type
        wwtype = NsharpLibXwvid.ww_type(soundingLys, parcelMap.get(NsharpLibSndglib.PARCELTYPE_MEAN_MIXING),
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_MOST_UNSTABLE),
                parcelMap.get(NsharpLibSndglib.PARCELTYPE_OBS_SFC), smdir, smspd);

        switch (wwtype) {
        case 1:
            wwtypeStr = "MRGL SVR";
            wwTypeColor = NsharpConstants.color_skyblue;
            break;
        case 2:
            wwtypeStr = "SVR";
            wwTypeColor = NsharpConstants.color_cyan;
            break;
        case 3:
            wwtypeStr = "MRGL TOR";
            wwTypeColor = NsharpConstants.color_red;
            break;
        case 4:
            wwtypeStr = "TOR";
            wwTypeColor = NsharpConstants.color_red;
            break;
        case 5:
            wwtypeStr = "PDS TOR";
            wwTypeColor = NsharpConstants.color_magenta;
            break;
        default:
            wwtypeStr = "NONE";
            wwTypeColor = NsharpConstants.color_gold;
            break;
        }
    }

    public Map<Integer, Parcel> getParcelMap() {
        return parcelMap;
    }

    public float getMeanMixRatio() {
        return meanMixRatio;
    }

    public float getFgzft() {
        return fgzft;
    }

    public float getFrzft() {
        return frzft;
    }

    public NcSoundingLayer getSfcLayer() {
        return sfcLayer;
    }

    public float getTemp975() {
        return temp975;
    }

    public float getHeight975() {
        return height975;
    }

    public float getPw() {
        return pw;
    }

    public float getWbzft() {
        return wbzft;
    }

    public float getWbzMslft() {
        return wbzmslft;
    }

    public float getWndg() {
        return wndg;
    }

    public float getkIndex() {
        return kIndex;
    }

    public float getDcape() {
        return dcape;
    }

    public float getDownT() {
        return downT;
    }

    public float getEsp() {
        return esp;
    }

    public float getMidRh() {
        return midRh;
    }

    public float getLowRh() {
        return lowRh;
    }

    public float getMeanRh() {
        return meanRh;
    }

    public WindComponent getMeanWindComp850To200mb() {
        return meanWindComp850To200mb;
    }

    public float getConvT() {
        return convT;
    }

    public float getMmp() {
        return mmp;
    }

    public float getMaxTemp() {
        return maxTemp;
    }

    public float getNcape() {
        return ncape;
    }

    public float getEight50To500mbLapseRate() {
        return eight50To500mbLapseRate;
    }

    public float getEight50To500mbTempDelta() {
        return eight50To500mbTempDelta;
    }

    public float getSevenHundredTo500mbLapseRate() {
        return sevenHundredTo500mbLapseRate;
    }

    public float getSevenHundredTo500mbTempDelta() {
        return sevenHundredTo500mbTempDelta;
    }

    public float getSfcTo3kmLapseRate() {
        return sfcTo3kmLapseRate;
    }

    public float getSfcTo3kmTempDelta() {
        return sfcTo3kmTempDelta;
    }

    public float getThreeKmTo6kmLapseRate() {
        return threeKmTo6kmLapseRate;
    }

    public float getThreeKmTo6kmTempDelta() {
        return threeKmTo6kmTempDelta;
    }

    public float getScp() {
        return scp;
    }

    public float getStpCin() {
        return stpCin;
    }

    public float getStpFixed() {
        return stpFixed;
    }

    public float getShip() {
        return ship;
    }

    public Map<String, WindComponent> getStormTypeToMeanWindMap() {
        return stormTypeToMeanWindMap;
    }

    public Map<String, WindComponent> getStormTypeToSrMeanWindMap() {
        return stormTypeToSrMeanWindMap;
    }

    public Map<String, Float> getStormTypeToWindShearMap() {
        return stormTypeToWindShearMap;
    }

    public Map<String, Helicity> getStormTypeToHelicityMap() {
        return stormTypeToHelicityMap;
    }

    public WindComponent getSrMeanWindComp4To6km() {
        return srMeanWindComp4To6km;
    }

    public WindComponent[] getCofidiShearWindComp() {
        return cofidiShearWindComp;
    }

    public WindComponent[] getBunkersStormMotionWindComp() {
        return bunkersStormMotionWindComp;
    }

    public float getStpLr() {
        return stpLr;
    }

    public List<NcSoundingLayer> getSoundingLys() {
        return soundingLys;
    }

    public float getSmdir() {
        return smdir;
    }

    public float getSmspd() {
        return smspd;
    }

    public EffectiveLayerPressures getEffLyPress() {
        return effLyPress;
    }

    public float getTopMoistLyrPress() {
        return topMoistLyrPress;
    }

    public float getTopMoistLyrHeight() {
        return topMoistLyrHeight;
    }

    public float getTotTots() {
        return totTots;
    }

    public float getSweatIndex() {
        return sweatIndex;
    }

    public float getThetaDiff() {
        return thetaDiff;
    }

    public float getLowestInvHeight() {
        return lowestInvHeight;
    }

    public float getLowestInvPressure() {
        return lowestInvPressure;
    }

    public float getLowestInvTempChange() {
        return lowestInvTempChange;
    }

    public MixHeight getMixHeightLayerBased() {
        return mixHeightLayerBased;
    }

    public MixHeight getMixHeightSurfaceBased() {
        return mixHeightSurfaceBased;
    }

    public Map<Integer, ParcelMiscParams> getParcelMiscParamsMap() {
        return parcelMiscParamsMap;
    }

    public WindComponent getSrMeanWindComp9To11km() {
        return srMeanWindComp9To11km;
    }

    public WindComponent getShearWindCompSfcTo12km() {
        return shearWindCompSfcTo12km;
    }

    public float getWbzp() {
        return wbzp;
    }

    public float getRogashRainRate() {
        return RogashRainRate;
    }

    public float getWindShear3km() {
        return windShear3km;
    }

    public float getMidLvlRH500To700() {
        return midLvlRH500To700;
    }

    public float getAvgWetbulbTemp() {
        return avgWetbulbTemp;
    }

    public static String[] getStormMotionTypeStr() {
        return STORM_MOTION_TYPE_STR;
    }

    public static float[][] getStormMotionHeight() {
        return STORM_MOTION_HEIGHT;
    }

    public static Map<Integer, Float> getParceltolayerpressmap() {
        return parcelToLayerPressMap;
    }

    public float getFgzPress() {
        return fgzPress;
    }

    public float getN20CPress() {
        return n20CPress;
    }

    public float getN20CHeightFt() {
        return n20CHeightFt;
    }

    public float getN30CPress() {
        return n30CPress;
    }

    public float getN30CHeightFt() {
        return n30CHeightFt;
    }

    public NsharpCloudInfo getCloudInfo() {
        return cloudInfo;
    }

    public int getWwtype() {
        return wwtype;
    }

    public RGB getWwTypeColor() {
        return wwTypeColor;
    }

    public String getWwtypeStr() {
        return wwtypeStr;
    }

    public float getEffLayerTopWindUComp() {
        return effLayerTopWindUComp;
    }

    public float getEffLayerTopWindVComp() {
        return effLayerTopWindVComp;
    }

    public float getEffLayerBotWindUComp() {
        return effLayerBotWindUComp;
    }

    public float getEffLayerBotWindVComp() {
        return effLayerBotWindVComp;
    }

    public float getCriticalAngle() {
        return criticalAngle;
    }

    public Map<Float, Float> getAdvectionMap() {
        return advectionMap;
    }

    public Map<Float, Float> getVerticalSrWindMap() {
        return verticalSrWindMap;
    }

    public float getThetaE700() {
        return thetaE700;
    }

    public float getThetaE850() {
        return thetaE850;
    }

    public float getThetaESurface() {
        return thetaESurface;
    }

    public float getAgl500mb() {
        return agl500mb;
    }

    public Map<Float, Float> getPressThetaEMap() {
        return pressThetaEMap;
    }

    public Map<Float, Float> getThetaEHeightMap() {
        return thetaEHeightMap;
    }

    public Map<Float, Float> getPressHeightMap() {
        return pressHeightMap;
    }

    public Map<Integer, NsharpStormSlinkyInfo> getStormSlinkyInfoMap() {
        return stormSlinkyInfoMap;
    }

    public Map<Integer, Float> getEbsMap() {
        return ebsMap;
    }

    public float getEffShear() {
        return effShear;
    }

    public NsharpWinterInfo getWinterInfo() {
        return winterInfo;
    }

    public NsharpFireInfo getFireInfo() {
        return fireInfo;
    }

    public NsharpSarsInfo getSarsInfo() {
        return sarsInfo;
    }

    public HailInfoContainer getHailInfoContainer() {
        return hailInfoContainer;
    }

    public float getEffSreh() {
        return effSreh;
    }

    public LapseRateMax getLrm() {
        return lrm;
    }

    public RGB getLrmColor() {
        return lrmColor;
    }

    public float getLhp() {
        return lhp;
    }

    public float getModSherbe() {
        return modSherbe;
    }
}
