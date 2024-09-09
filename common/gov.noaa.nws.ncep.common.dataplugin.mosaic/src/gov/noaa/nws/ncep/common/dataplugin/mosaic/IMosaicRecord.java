
package gov.noaa.nws.ncep.common.dataplugin.mosaic;

import gov.noaa.nws.ncep.common.dataplugin.mosaic.util.MosaicConstants;

/**
 * TODO Add Description
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * AUG 17, 2009   143      llin      Initial creation
 * Sep 09, 2024 2037799    njensen   Moved to new common plugin
 * 
 * </pre>
 * 
 * @author llin
 * @version 1.0
 */

public interface IMosaicRecord {

    /**
     * Gets the message specific values like averages and constants
     * 
     * @param valueName
     *            - the key for the value type that you want to get back
     * @return
     */
    public String getProductVals(MosaicConstants.MapValues type, String id,
            MosaicConstants.MapValues valueName);

}
