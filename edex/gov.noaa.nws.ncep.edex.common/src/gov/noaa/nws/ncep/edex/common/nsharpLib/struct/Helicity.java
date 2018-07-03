package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store storm helicity parameters.
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

public class Helicity {
    private float posHelicity;

    private float negHelicity;

    private float totalHelicity;

    public Helicity() {
        super();
        this.posHelicity = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.negHelicity = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.totalHelicity = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    }

    public float getPosHelicity() {
        return posHelicity;
    }

    public void setPosHelicity(float posHelicity) {
        this.posHelicity = posHelicity;
    }

    public float getNegHelicity() {
        return negHelicity;
    }

    public void setNegHelicity(float negHelicity) {
        this.negHelicity = negHelicity;
    }

    public float getTotalHelicity() {
        return totalHelicity;
    }

    public void setTotalHelicity(float totalHelicity) {
        this.totalHelicity = totalHelicity;
    }
}