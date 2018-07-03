package gov.noaa.nws.ncep.edex.common.nsharpLib.struct;

/**
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * This class is a data structure used by nsharpLib functions to store effect layers pressures.
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

public class EffectiveLayerPressures {
    private float bottomPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    private float topPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;

    public EffectiveLayerPressures() {
        super();
        this.bottomPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
        this.topPress = NsharpLibSndglib.NSHARP_NATIVE_INVALID_DATA;
    }

    public float getBottomPress() {
        return bottomPress;
    }

    public void setBottomPress(float bottomPress) {
        this.bottomPress = bottomPress;
    }

    public float getTopPress() {
        return topPress;
    }

    public void setTopPress(float topPress) {
        this.topPress = topPress;
    }
}