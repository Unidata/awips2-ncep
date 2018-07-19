package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store hydro-meteorological 
 * parameters within empirical mix boundary layer
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

public class MixHeight {
    // Pressure at mixing height (mb)
    private float mh_pres;

    // Wind direction at mixing height (deg)
    private float mh_drct;

    // Wind speed at mixing height (kts)
    private float mh_sped;

    // Layer change in temperature (C)
    private float mh_dC;

    // Layer lapse rate (C/km)
    private float mh_lr;

    // Layer maximum wind direction (deg)
    private float mh_drct_max;

    // Layer maximum wind speed (kts)
    private float mh_sped_max;

    // mixing height in M
    private float mh_hgt;

    public MixHeight() {
        super();
        this.mh_pres = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_hgt = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_drct = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_sped = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_dC = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_lr = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_drct_max = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.mh_sped_max = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public MixHeight(float mh_pres, float mh_drct, float mh_sped, float mh_dC,
            float mh_lr, float mh_drct_max, float mh_sped_max) {
        super();
        this.mh_pres = mh_pres;
        this.mh_drct = mh_drct;
        this.mh_sped = mh_sped;
        this.mh_dC = mh_dC;
        this.mh_lr = mh_lr;
        this.mh_drct_max = mh_drct_max;
        this.mh_sped_max = mh_sped_max;
    }

    public float getMh_pres() {
        return mh_pres;
    }

    public void setMh_pres(float mh_pres) {
        this.mh_pres = mh_pres;
    }

    public float getMh_drct() {
        return mh_drct;
    }

    public void setMh_drct(float mh_drct) {
        this.mh_drct = mh_drct;
    }

    public float getMh_sped() {
        return mh_sped;
    }

    public void setMh_sped(float mh_sped) {
        this.mh_sped = mh_sped;
    }

    public float getMh_dC() {
        return mh_dC;
    }

    public void setMh_dC(float mh_dC) {
        this.mh_dC = mh_dC;
    }

    public float getMh_lr() {
        return mh_lr;
    }

    public void setMh_lr(float mh_lr) {
        this.mh_lr = mh_lr;
    }

    public float getMh_drct_max() {
        return mh_drct_max;
    }

    public void setMh_drct_max(float mh_drct_max) {
        this.mh_drct_max = mh_drct_max;
    }

    public float getMh_sped_max() {
        return mh_sped_max;
    }

    public void setMh_sped_max(float mh_sped_max) {
        this.mh_sped_max = mh_sped_max;
    }

    public float getMh_hgt() {
        return mh_hgt;
    }

    public void setMh_hgt(float mh_hgt) {
        this.mh_hgt = mh_hgt;
    }

}
