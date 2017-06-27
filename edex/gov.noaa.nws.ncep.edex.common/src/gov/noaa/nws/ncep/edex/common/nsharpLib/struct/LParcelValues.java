package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store parameters for used by define_parcel().
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 08/24/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement. 
 * 
 * </pre>
 * 
 * @author Chin Chen 
 * @version 1.0
 * 
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibSndglib;

//Lift parcel value
public class LParcelValues {
    private float pres;

    private float temp;

    private float dwpt;

    private int parcelFlag; // value defined at NsharpLibSndglib.java

    public LParcelValues() {
        super();
        this.pres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.temp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.dwpt = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.parcelFlag = NsharpLibSndglib.PARCELTYPE_NULL;
    }

    public LParcelValues(String desc, float pres, float temp, float dwpt,
            int flag) {
        super();
        this.pres = pres;
        this.temp = temp;
        this.dwpt = dwpt;
        this.parcelFlag = flag;
    }

    public float getPres() {
        return pres;
    }

    public void setPres(float pres) {
        this.pres = pres;
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public float getDwpt() {
        return dwpt;
    }

    public void setDwpt(float dwpt) {
        this.dwpt = dwpt;
    }

    public int getParcelFlag() {
        return parcelFlag;
    }

    public void setParcelFlag(int parcelFlag) {
        this.parcelFlag = parcelFlag;
    }

}
