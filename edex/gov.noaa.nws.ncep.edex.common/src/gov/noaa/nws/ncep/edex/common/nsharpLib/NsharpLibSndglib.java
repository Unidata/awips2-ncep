package gov.noaa.nws.ncep.edex.common.nsharpLib;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system. 
 * 
 * All methods developed in this class are based on the algorithm developed in BigSharp native C file, sndglib.h , by John A. Hart/SPC.
 * All methods name are defined with same name as the C function name defined in native code.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 02/25/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement Phase 1&2
 * 07/10/2017   RM#34796    Chin Chen   NSHARP - Updates for March 2017 bigSharp version
 *                                     - Reformat the lower left data page
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 * 
 */

import javax.measure.UnitConverter;
import si.uom.SI;

public class NsharpLibSndglib {

    public static final float NSHARP_NATIVE_INVALID_DATA = -9999f;

    public static float ROCP = 0.28571428f; /* Rd over Cp */

    public static final float PI = 3.14159265F;

    public static final UnitConverter kelvinToCelsius = SI.KELVIN
            .getConverterTo(SI.CELSIUS);

    // PARCEL type flags to be used for define_parcel()
    public static final int PARCELTYPE_NULL = -1;

    public static final int PARCELTYPE_OBS_SFC = 1;
    
    public static final int PARCELTYPE_MEAN_MIXING = 2;
    
    public static final int PARCELTYPE_FCST_SFC = 3;
    
    public static final int PARCELTYPE_MOST_UNSTABLE = 4;    
    
    public static final int PARCELTYPE_EFF = 5;
    
    public static final int PARCELTYPE_USER_DEFINED = 6;

    public static final int PARCEL_MAX = PARCELTYPE_USER_DEFINED;

    public static final int PARCEL_D2DLITE_MAX = PARCELTYPE_MOST_UNSTABLE; // d2dlite

    // default pressure for parcel as defined in BigNsharp
    public static final float OBS_LAYER_PRESS = 0.0f;

    public static final float FCST_LAYER_PRESS = 0.0f;

    public static final float MML_LAYER_PRESS = 100.0f; /* mean-mixed layer */

    public static final float MU_LAYER_PRESS = 400.0f; /* most-unstable layer */

    // default user-defined level
    public static final float USER_LAYER_PRESS = 850.0f;

    public static final float EFF_LAYER_PRESS = MU_LAYER_PRESS;

    public static float getROCP() {
        return ROCP;
    }

    public static void setROCP(float rOCP) {
        ROCP = rOCP;
    }
}
