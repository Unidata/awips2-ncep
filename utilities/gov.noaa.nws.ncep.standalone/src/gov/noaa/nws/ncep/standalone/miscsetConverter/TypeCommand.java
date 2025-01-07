/**
 * 
 */
package gov.noaa.nws.ncep.standalone.miscsetConverter;

/**
 * @author bhebbard
 *
 */

public interface TypeCommand {
    String retrieve(TypeRecord tr, float scaleFactor);
}
