package gov.noaa.nws.ncep.edex.plugin.geomag;

import java.util.Hashtable;

/**
 * Conversion class provides a convenience method to convert H and D component
 * values when appropriate. Additional conversion capabilities in the future
 * would go here.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *                   
 * Date          Ticket#     Engineer       Description
 * -----------  ----------  ----------      --------------------------
 * 10/07/2015    R11429      sgurung,jtravis Initial creation
 * 
 * </pre>
 * 
 * @author jtravis
 * @version 1
 */
public class Conversion {

    /**
     * Raw data from some providers might not be reported in the appropriate
     * format/units. These data needs to be converted to northward component (X)
     * in nT and eastward component (Y) in nT using the general formula: X = H *
     * Cos D; Y = H Sin D;
     * 
     * @param comp1Val
     * @param comp1RefersTo
     * @param comp2Val
     * @param comp2RefersTo
     * 
     * @return Hashtable<String, Double>
     */
    public static Hashtable<String, Double> convertHandD(Double comp1Val,
            String comp1RefersTo, Double comp2Val, String comp2RefersTo) {

        Double h = null;
        Double d = null;
        Hashtable<String, Double> convertedValues = new Hashtable<String, Double>();

        if (GeoMagDecoderUtils.H_VALUE.equalsIgnoreCase(comp1RefersTo)) {
            h = comp1Val;
        }

        if (GeoMagDecoderUtils.D_VALUE.equalsIgnoreCase(comp2RefersTo)) {
            d = comp2Val;
        }

        if ((h != null) && (d != null)) {
            comp1Val = (h * Math.cos(Math.toRadians(d)));
            comp2Val = (h * Math.sin(Math.toRadians(d)));

            convertedValues.put(GeoMagDecoderUtils.COMPONENT_1, comp1Val);
            convertedValues.put(GeoMagDecoderUtils.COMPONENT_2, comp2Val);
        }

        return convertedValues;
    }

}
