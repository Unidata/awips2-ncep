/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.common.tsScaleMngr;

import java.util.List;

/**
 * Defines a GraphAttributes interface.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 15, 2014   R4875       sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public interface IXAxisScale {

    public abstract int getSize();

    public abstract int[] getDurations();

    public abstract int[] getLabelIntervals();

    public abstract int[] getMajorTickIntervals();

    public abstract int[] getMinorTickIntervals();

    public abstract String[] getLabelFormats();

    /**
     * Get the list of x-axis scale options
     * 
     * @return
     */
    public abstract List<XAxisScaleElement> getXAxisScaleElements();

    /**
     * Get the name of the x-axis scale
     * 
     * @return name
     */
    public abstract String getName();

    /**
     * Set name of x-axis scale
     * 
     * @param name
     */
    public void setName(String name);

    /**
     * Clone the scale
     * 
     * @return
     */
    public abstract IXAxisScale clone();
}
