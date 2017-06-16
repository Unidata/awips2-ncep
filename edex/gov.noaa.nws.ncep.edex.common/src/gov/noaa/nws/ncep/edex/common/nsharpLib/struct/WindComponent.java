package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store wind parameters
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

public class WindComponent {
    private float ucomp;

    private float vcomp;

    private float wdir;

    private float wspd;

    public WindComponent() {
        super();
        ucomp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        vcomp = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        wdir = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        wspd = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public float getUcomp() {
        return ucomp;
    }

    public void setUcomp(float ucomp) {
        this.ucomp = ucomp;
    }

    public float getVcomp() {
        return vcomp;
    }

    public void setVcomp(float vcomp) {
        this.vcomp = vcomp;
    }

    public float getWdir() {
        return wdir;
    }

    public void setWdir(float wdir) {
        this.wdir = wdir;
    }

    public float getWspd() {
        return wspd;
    }

    public void setWspd(float wspd) {
        this.wspd = wspd;
    }
}
